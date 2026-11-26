package bo.aportaya.aportes.infraestructura.pasarela.simulada;

import bo.aportaya.aportes.dominio.PasarelaDePagos;
import bo.aportaya.aportes.dominio.ProveedorDeSecretoWebhook;
import bo.aportaya.aportes.dominio.SimuladorDePagoDePasarela;
import bo.aportaya.aportes.dominio.VerificadorDeFirmaWebhook;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DOBLE de la pasarela de pagos (regla 65): cumple {@link PasarelaDePagos} sin hablar con ningun proveedor y emite
 * los avisos de pago firmados que la pasarela real mandaria. NO es la pasarela: sus ordenes no se pagan en ningun
 * banco, y todo lo verificado contra el queda pendiente de verificar contra la real.
 *
 * <p>Determinista: la referencia de una orden sale de la clave de idempotencia (SHA-256), el {@code idEvento} de la
 * referencia y el timestamp del {@link Reloj}. Estado en memoria: un reinicio olvida las ordenes, y
 * {@link #emitirWebhookDePago(String)} ya no las encuentra (la consulta contesta «no la conozco», como la real ante
 * un 404). Solo existe con {@code aportaya.aportes.pasarela.modo=simulada} y {@link GuardiaDePasarelaSimulada} la
 * prohibe fuera de {@code local}/{@code test}.
 *
 * <p>Firma con el secreto de verificacion del entorno ({@link ProveedorDeSecretoWebhook}), el MISMO que usa CU-100
 * para verificar: no hace falta ningun secreto del proveedor. Los logs llevan la referencia tecnica, nunca importes
 * ni datos de personas (regla 90.2).
 */
@Component
@ConditionalOnProperty(name = "aportaya.aportes.pasarela.modo", havingValue = "simulada", matchIfMissing = true)
public class PasarelaSimulada implements PasarelaDePagos, SimuladorDePagoDePasarela {

    private static final Logger BITACORA = LoggerFactory.getLogger(PasarelaSimulada.class);

    /** Lo que se le muestra a quien paga: deja claro que es de prueba, como el QR de recargas de nucleo-financiero. */
    static final String PREFIJO_QR = "PASANAKU-SIMULADA:";

    static final String PREFIJO_REFERENCIA = "SIM-";

    private final ProveedorDeSecretoWebhook secretos;
    private final Reloj reloj;
    private final ObjectMapper json;
    private final String proveedorCodigo;
    private final Map<String, OrdenDeCobro> ordenesPorClave = new ConcurrentHashMap<>();
    private final Map<String, OrdenDeCobro> ordenesPorReferencia = new ConcurrentHashMap<>();

    public PasarelaSimulada(
            ProveedorDeSecretoWebhook secretos,
            Reloj reloj,
            ObjectMapper json,
            @Value("${aportaya.aportes.pasarela.proveedor-codigo}") String proveedorCodigo) {
        this.secretos = Objects.requireNonNull(secretos, "secretos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.json = Objects.requireNonNull(json, "json");
        if (proveedorCodigo == null || proveedorCodigo.isBlank()) {
            throw new IllegalStateException(
                    "Falta aportaya.aportes.pasarela.proveedor-codigo: el doble firma avisos para un proveedor del catalogo.");
        }
        this.proveedorCodigo = proveedorCodigo;
        BITACORA.warn("PASARELA SIMULADA activa (doble, regla 65): las ordenes no se pagan en ningun banco");
    }

    @Override
    public OrdenDeCobro crearOrdenDeCobro(SolicitudDeCobro solicitud) {
        Objects.requireNonNull(solicitud, "solicitud");
        OrdenDeCobro orden = ordenesPorClave.compute(solicitud.claveIdempotencia(), (clave, previa) -> {
            if (previa == null) {
                return abrir(clave, solicitud.monto());
            }
            if (!previa.monto().equals(solicitud.monto())) {
                throw new IllegalStateException("La clave de idempotencia ya identifica una orden con otro monto.");
            }
            return previa;
        });
        ordenesPorReferencia.putIfAbsent(orden.referenciaProveedor(), orden);
        BITACORA.info("pasarela simulada: orden de cobro {}", orden.referenciaProveedor());
        return orden;
    }

    @Override
    public boolean registraOrdenPorCobro() {
        return true;
    }

    @Override
    public Optional<CobroConsultado> consultarEstado(String referenciaProveedor) {
        if (referenciaProveedor == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ordenesPorReferencia.get(referenciaProveedor))
                .map(o -> new CobroConsultado(o.referenciaProveedor(), o.monto(), o.estado()));
    }

    @Override
    public WebhookFirmado emitirWebhookDePago(String referenciaProveedor) {
        OrdenDeCobro orden = ordenesPorReferencia.get(referenciaProveedor);
        if (orden == null) {
            throw new IllegalArgumentException("La pasarela simulada no emitio esa orden.");
        }
        WebhookFirmado aviso =
                emitirWebhookDePago(referenciaProveedor, "sim-evt-" + referenciaProveedor, orden.monto());
        marcarPagada(orden);
        return aviso;
    }

    @Override
    public WebhookFirmado emitirWebhookDePago(String referenciaProveedor, String idEvento, Dinero montoInformado) {
        Objects.requireNonNull(referenciaProveedor, "referenciaProveedor");
        Objects.requireNonNull(idEvento, "idEvento");
        Objects.requireNonNull(montoInformado, "montoInformado");
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("evento", "pago.acreditado");
        cuerpo.put("idEvento", idEvento);
        cuerpo.put("referenciaProveedor", referenciaProveedor);
        cuerpo.put("monto", montoInformado.monto().toPlainString());
        cuerpo.put("moneda", montoInformado.moneda().name());
        String cuerpoCrudo;
        try {
            cuerpoCrudo = json.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el aviso simulado", e);
        }
        String timestamp = String.valueOf(reloj.ahora().getEpochSecond());
        String secreto = secretos.secretoDe(proveedorCodigo)
                .orElseThrow(() ->
                        new IllegalStateException("No hay secreto de verificacion para firmar el aviso simulado."));
        String firma = VerificadorDeFirmaWebhook.firmar(timestamp, cuerpoCrudo, secreto);
        BITACORA.info("pasarela simulada: aviso de pago emitido para {}", referenciaProveedor);
        return new WebhookFirmado(proveedorCodigo, cuerpoCrudo, firma, timestamp);
    }

    private void marcarPagada(OrdenDeCobro orden) {
        OrdenDeCobro pagada = new OrdenDeCobro(
                orden.referenciaProveedor(), orden.monto(), orden.contenidoQr(), EstadoDeCobro.PAGADO, true);
        ordenesPorReferencia.put(orden.referenciaProveedor(), pagada);
    }

    private static OrdenDeCobro abrir(String clave, Dinero monto) {
        String referencia = PREFIJO_REFERENCIA + huella(clave);
        return new OrdenDeCobro(referencia, monto, PREFIJO_QR + referencia, EstadoDeCobro.PENDIENTE, true);
    }

    private static String huella(String clave) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8).toUpperCase(java.util.Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 es JDK estandar", e);
        }
    }
}
