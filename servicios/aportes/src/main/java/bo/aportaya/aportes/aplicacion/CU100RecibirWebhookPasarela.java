package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.ProveedorDeSecretoWebhook;
import bo.aportaya.aportes.dominio.VerificadorDeFirmaWebhook;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.aportes.infraestructura.ProveedorPagoRepositorio;
import bo.aportaya.aportes.infraestructura.WebhookRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-100 · Recibir el webhook de la pasarela (Q-02, DECIDIDA 2026-09-21).
 *
 * <p>Tres pasos, EN ORDEN, y ninguno se salta:
 *
 * <ol>
 *   <li>Firma (HMAC-SHA256 sobre el cuerpo crudo) y ventana temporal (+-5 minutos):
 *       si cualquiera de las dos falla, se responde igual (firma invalida) y no se
 *       escribe una sola fila — ni para auditar. Un atacante que prueba firmas no
 *       puede aprender nada del sistema mirando la base.
 *   <li>Deduplicación por {@code (proveedor_id, clave_idempotencia)}
 *       ({@code uq_webhook_idem}, invariante 7): el mismo evento entregado dos veces
 *       (la pasarela reintenta) no se reprocesa ni duplica el efecto financiero.
 *   <li>La regla de negocio: el monto y la moneda del evento tienen que coincidir con
 *       el {@code pago} ya registrado por su referencia de proveedor. Si no hay pago
 *       o el monto no cuadra, el webhook se archiva DESCARTADO — nunca se acredita
 *       "lo que vino" (regla 91.3.2 / `payments-qr-integration` §4.3).
 * </ol>
 *
 * <p><b>Simplificación de alcance, declarada</b>: no existe en este repo la cadena
 * {@code orden_cobro → intento_pago} que describe la skill de pagos QR;
 * {@code CU21CobrarAporte} acredita de forma síncrona. Este caso de uso conecta el
 * webhook con el {@code pago} ya existente por su referencia de proveedor. Construir
 * la cadena asíncrona completa es una feature de producto fuera de este carril de
 * seguridad transversal (ver {@code idempotencia-scope.md} y
 * {@code carriles/PR4-seguridad.md} §H1.S2).
 */
@Service
public class CU100RecibirWebhookPasarela {

    private static final Logger BITACORA = LoggerFactory.getLogger(CU100RecibirWebhookPasarela.class);

    private final Datos datos;
    private final ProveedorPagoRepositorio proveedores;
    private final WebhookRepositorio webhooks;
    private final PagoRepositorio pagos;
    private final ProveedorDeSecretoWebhook secretos;
    private final Reloj reloj;
    private final ObjectMapper json;

    public CU100RecibirWebhookPasarela(
            Datos datos,
            ProveedorPagoRepositorio proveedores,
            WebhookRepositorio webhooks,
            PagoRepositorio pagos,
            ProveedorDeSecretoWebhook secretos,
            Reloj reloj,
            ObjectMapper json) {
        this.datos = datos;
        this.proveedores = proveedores;
        this.webhooks = webhooks;
        this.pagos = pagos;
        this.secretos = secretos;
        this.reloj = reloj;
        this.json = json;
    }

    @Transactional
    public Resultado recibir(Entrada entrada) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                UUID.nameUUIDFromBytes("webhook-pasarela".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                new Traza(UUID.randomUUID().toString()));

        return datos.conContexto(ctx, dsl -> {
            OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

            // Paso 1 — firma y ventana. Sin distinguir "proveedor desconocido" de
            // "firma mala": las dos vuelven FIRMA_INVALIDA para no darle a quien
            // prueba una forma de descubrir que proveedores existen.
            var proveedor = proveedores.activoConWebhook(dsl, entrada.proveedorCodigo());
            if (proveedor.isEmpty()) {
                BITACORA.warn("webhook rechazado: proveedor sin soporte de webhook o inexistente");
                return Resultado.firmaInvalida();
            }
            var secreto = secretos.secretoDe(entrada.proveedorCodigo());
            if (secreto.isEmpty()
                    || !VerificadorDeFirmaWebhook.firmaValida(entrada.cuerpoCrudo(), entrada.firma(), secreto.get())) {
                BITACORA.warn("webhook rechazado: firma invalida");
                return Resultado.firmaInvalida();
            }
            long timestamp;
            try {
                timestamp = Long.parseLong(entrada.timestamp().trim());
            } catch (NumberFormatException e) {
                BITACORA.warn("webhook rechazado: X-Timestamp no es un entero");
                return Resultado.firmaInvalida();
            }
            if (!VerificadorDeFirmaWebhook.dentroDeVentana(timestamp, ahora.toInstant())) {
                BITACORA.warn("webhook rechazado: fuera de la ventana de +-5 minutos");
                return Resultado.firmaInvalida();
            }

            // A partir de aca la firma esta verificada: recien ahora se lee el cuerpo.
            JsonNode evento;
            try {
                evento = json.readTree(entrada.cuerpoCrudo());
            } catch (Exception e) {
                BITACORA.error("webhook con firma valida pero cuerpo no es JSON");
                return Resultado.descartado(null, "cuerpo no es JSON valido");
            }

            String claveEvento = textoDe(evento, "idEvento");
            String tipoEvento = textoDe(evento, "evento");
            if (claveEvento == null || claveEvento.isBlank()) {
                BITACORA.error("webhook con firma valida pero sin idEvento");
                return Resultado.descartado(null, "falta idEvento");
            }

            // Paso 2 — deduplicacion por la MISMA identidad que uq_webhook_idem.
            var existente = webhooks.porClaveDeEvento(dsl, proveedor.get().id(), claveEvento);
            if (existente.isPresent()) {
                return Resultado.duplicado(existente.get().id(), existente.get().pagoId());
            }

            UUID webhookId = webhooks.registrar(
                    dsl,
                    proveedor.get().id(),
                    tipoEvento == null ? "desconocido" : tipoEvento,
                    entrada.cuerpoCrudo(),
                    entrada.firma(),
                    claveEvento,
                    ahora);

            // Paso 3 — la regla de negocio: nunca se acredita "lo que vino".
            String referencia = textoDe(evento, "referenciaProveedor");
            var pagoOpt = referencia == null
                    ? java.util.Optional.<PagoRepositorio.PagoParaConciliar>empty()
                    : pagos.porReferenciaProveedor(dsl, referencia);
            if (pagoOpt.isEmpty()) {
                webhooks.marcarProcesado(
                        dsl, webhookId, "DESCARTADO", "no existe pago con esa referencia", null, ahora);
                return Resultado.descartado(webhookId, "no existe pago con esa referencia de proveedor");
            }
            var pago = pagoOpt.get();
            Dinero montoDelEvento = montoDe(evento);
            if (montoDelEvento == null || !montoDelEvento.equals(pago.monto())) {
                webhooks.marcarProcesado(
                        dsl, webhookId, "DESCARTADO", "el monto del webhook no coincide con el pago", pago.id(), ahora);
                return Resultado.descartado(webhookId, "el monto no coincide con el pago");
            }

            webhooks.marcarProcesado(dsl, webhookId, "PROCESADO", null, pago.id(), ahora);
            return Resultado.procesado(webhookId, pago.id());
        });
    }

    private static String textoDe(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private static Dinero montoDe(JsonNode nodo) {
        String monto = null;
        JsonNode nodoMonto = nodo.get("monto");
        if (nodoMonto != null && !nodoMonto.isNull()) {
            monto = nodoMonto.asText();
        }
        String moneda = null;
        JsonNode nodoMoneda = nodo.get("moneda");
        if (nodoMoneda != null && !nodoMoneda.isNull()) {
            moneda = nodoMoneda.asText();
        }
        if (monto == null || moneda == null) {
            return null;
        }
        try {
            return Dinero.de(monto, Moneda.valueOf(moneda));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public record Entrada(String proveedorCodigo, String cuerpoCrudo, String firma, String timestamp) {}

    /** {@code estado} nunca es null; {@code webhookId}/{@code pagoId}/{@code motivo} sí, segun el caso. */
    public record Resultado(Estado estado, UUID webhookId, UUID pagoId, String motivo) {

        public enum Estado {
            FIRMA_INVALIDA,
            PROCESADO,
            DUPLICADO,
            DESCARTADO
        }

        static Resultado firmaInvalida() {
            return new Resultado(Estado.FIRMA_INVALIDA, null, null, null);
        }

        static Resultado procesado(UUID webhookId, UUID pagoId) {
            return new Resultado(Estado.PROCESADO, webhookId, pagoId, null);
        }

        static Resultado duplicado(UUID webhookId, UUID pagoId) {
            return new Resultado(Estado.DUPLICADO, webhookId, pagoId, null);
        }

        static Resultado descartado(UUID webhookId, String motivo) {
            return new Resultado(Estado.DESCARTADO, webhookId, null, motivo);
        }
    }
}
