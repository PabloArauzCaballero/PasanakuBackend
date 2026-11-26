package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Entrada;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Resultado;
import bo.aportaya.aportes.dominio.PasarelaDePagos;
import bo.aportaya.aportes.dominio.SimuladorDePagoDePasarela;
import bo.aportaya.aportes.dominio.SimuladorDePagoDePasarela.WebhookFirmado;
import bo.aportaya.aportes.infraestructura.AuditoriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispara en {@code local}/{@code test} el aviso de pago de una orden de la pasarela SIMULADA y lo entrega a
 * {@link CU100RecibirWebhookPasarela} por el camino REAL: el doble firma con el secreto del entorno, y CU-100 verifica
 * firma y ventana, deduplica por {@code (proveedor_id, clave_idempotencia)} y concilia. No saltea ninguna verificacion.
 *
 * <p>Idempotente: el {@code idEvento} del aviso sale de la referencia, asi que repetir la llamada devuelve DUPLICADO
 * (la misma clave que ampara {@code uq_webhook_idem}). Cada llamada deja rastro en la bitacora de auditoria. Los logs
 * llevan la referencia tecnica de la orden, nunca importes ni datos de personas (regla 90.2). Es un DOBLE: solo existe
 * con la pasarela simulada y fuera de {@code local}/{@code test} el bean no se crea.
 */
@Service
@Profile({"local", "test"})
@ConditionalOnProperty(name = "aportaya.aportes.pasarela.modo", havingValue = "simulada", matchIfMissing = true)
public class CU100EmitirAvisoDePagoSimulado {

    private static final Logger BITACORA = LoggerFactory.getLogger(CU100EmitirAvisoDePagoSimulado.class);

    private final PasarelaDePagos pasarela;
    private final SimuladorDePagoDePasarela simulador;
    private final CU100RecibirWebhookPasarela webhook;
    private final AuditoriaRepositorio auditoria;
    private final Datos datos;
    private final Reloj reloj;

    public CU100EmitirAvisoDePagoSimulado(
            PasarelaDePagos pasarela,
            SimuladorDePagoDePasarela simulador,
            CU100RecibirWebhookPasarela webhook,
            AuditoriaRepositorio auditoria,
            Datos datos,
            Reloj reloj) {
        this.pasarela = pasarela;
        this.simulador = simulador;
        this.webhook = webhook;
        this.auditoria = auditoria;
        this.datos = datos;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado emitir(String referenciaProveedor, ContextoSesion ctx) {
        // AP-CU100-11: solo se avisa el pago de una orden que el doble emitio (no se inventan ordenes).
        if (pasarela.consultarEstado(referenciaProveedor).isEmpty()) {
            throw new ErrorDeNegocio(CodigoError.de(100, 11), "La pasarela simulada no emitio esa orden.");
        }
        WebhookFirmado aviso = simulador.emitirWebhookDePago(referenciaProveedor);
        Resultado resultado = webhook.recibir(
                new Entrada(aviso.proveedorCodigo(), aviso.cuerpoCrudo(), aviso.firma(), aviso.timestamp()));
        // AP-CU100-12: CU-100 responde igual a firma mala, ventana vencida y proveedor inactivo (a proposito): aca,
        // donde
        // el emisor es el propio doble, se dice que revisar sin revelar nada que la ruta publica no revele.
        if (resultado.estado() == Resultado.Estado.FIRMA_INVALIDA) {
            throw new ErrorDeNegocio(
                    CodigoError.de(100, 12),
                    "El aviso simulado no paso la verificacion: confirme que el proveedor "
                            + aviso.proveedorCodigo()
                            + " este activo con webhook en proveedor_pago y que el secreto sea el del entorno.");
        }
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        UUID entidadId = resultado.webhookId() != null
                ? resultado.webhookId()
                : UUID.nameUUIDFromBytes(referenciaProveedor.getBytes(StandardCharsets.UTF_8));
        datos.conContexto(ctx, dsl -> {
            auditoria.registrar(
                    dsl,
                    "webhook_pasarela",
                    entidadId,
                    "CONCILIACION",
                    ctx.usuarioId(),
                    ctx.rol(),
                    UUID.fromString(ctx.traza().id()),
                    null,
                    "{\"estado\":\"%s\",\"referencia\":\"%s\",\"simulado\":true}"
                            .formatted(resultado.estado(), referenciaProveedor),
                    null,
                    ahora);
            return null;
        });
        BITACORA.info("aviso de pago SIMULADO entregado: {} -> {}", referenciaProveedor, resultado.estado());
        return resultado;
    }
}
