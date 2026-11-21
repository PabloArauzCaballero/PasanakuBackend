package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.EntregaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-22 · la autorizacion de una entrega ya fondeada.
 *
 * <p>La entrega nace SOLO del fondeo del pozo completo ({@link CU22EntregarPozoCompleto}), con el pozo afirmado por
 * aportes; la liquidacion con importes del pedido y el cierre manual con un monto libre se retiraron (F1.L2.M09,
 * F1.L2.M08, punto 3 del lote L2). Lo que queda aca es el paso de autorizacion que hoy precede a la orden de desembolso
 * (CU-28): se retira con F2.L2.M06, cuando el fondeo y la emision los dispare el sistema (F3.L2.M01/M02).
 */
@Service
public class CU22LiquidarEntrega {

    private static final org.slf4j.Logger BITACORA = org.slf4j.LoggerFactory.getLogger(CU22LiquidarEntrega.class);
    private final Datos datos;
    private final EntregaRepositorio entregas;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU22LiquidarEntrega(Datos datos, EntregaRepositorio entregas, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.entregas = entregas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /**
     * Autoriza la entrega.
     *
     * <p>Una validacion bloqueante sin resolver la frena: son las que preguntan si el
     * beneficiario sigue al dia, si la cuenta esta verificada, si no hay una incidencia
     * abierta. Saltearlas para «no demorar la entrega» es como se entrega mal.
     */
    @Transactional
    public SalidaAutorizacion autorizar(UUID entregaId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var entrega = entregas.bloquear(dsl, entregaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(22, 3), "Esa entrega no existe."));

            // AP-CU22-02.
            int bloqueantes = entregas.bloqueantesSinAprobar(dsl, entregaId);
            if (bloqueantes > 0) {
                throw new ErrorDeNegocio(
                        CodigoError.de(22, 2), "Hay " + bloqueantes + " validacion(es) bloqueante(s) sin resolver.");
            }
            if (!entregas.autorizar(dsl, entregaId, ctx.usuarioId(), ahora, entrega.version())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(22, 3), "Esa entrega ya no admite autorizacion: esta " + entrega.estado() + ".");
            }

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "entregas.entrega_autorizada",
                            "entrega_fondo",
                            entregaId,
                            Map.of(
                                    "autorizadaPor", ctx.usuarioId().toString(),
                                    "neto", entrega.neto().toString()),
                            UUID.fromString(ctx.traza().id())));

            BITACORA.info(
                    "CU-22 · Entrega autorizada: entrega={}, operador={}, neto={}",
                    entregaId,
                    ctx.usuarioId(),
                    entrega.neto());
            return new SalidaAutorizacion(entregaId, "AUTORIZADA", ctx.usuarioId());
        });
    }

    public record SalidaAutorizacion(UUID entregaId, String estado, UUID autorizadaPor) {}
}
