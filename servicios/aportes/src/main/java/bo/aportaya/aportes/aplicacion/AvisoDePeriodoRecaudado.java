package bo.aportaya.aportes.aplicacion;

import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * {@code aportes.periodo_recaudado} (pacto {@code contratos/pactos/aportes-entregas-periodo-recaudado.json}, pedido de
 * L2 para F3.L2.M01): el aviso de que el recaudo de un periodo quedo listo para fondear la entrega del turno.
 *
 * <p>Motivo {@code COMPLETO}: en la MISMA transaccion que deja en PAGADO la ultima cuota periodica no anulada del
 * periodo (outbox, regla 98.3.3). Cuenta las mismas cuotas que el recaudo ({@code APORTE_PERIODICO}, no anuladas). El
 * candado de transaccion por periodo serializa el chequeo: si dos ultimos pagos llegan juntos, el segundo espera al
 * primero y ve su PAGADO, asi que el aviso sale una vez y nunca cero.
 *
 * <p>Sin importes: quien lo consume vuelve a preguntar el recaudo (la unica fuente de las cifras). El aviso puede
 * llegar mas de una vez; el consumidor es idempotente.
 */
public class AvisoDePeriodoRecaudado {

    private final Outbox outbox;

    public AvisoDePeriodoRecaudado(Outbox outbox) {
        this.outbox = outbox;
    }

    /** Emite el aviso si con este pago el periodo quedo sin cuotas por cobrar. Devuelve si lo emitio. */
    public boolean siQuedoCompleto(
            DSLContext dsl, UUID periodoId, UUID grupoId, OffsetDateTime ahora, UUID correlationId) {
        dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?::text, 0))", "periodo_recaudado:" + periodoId);
        Integer porCobrar = dsl.fetchOne(
                        """
                        SELECT count(*)::int FROM aportes.obligacion_aporte
                         WHERE periodo_id = ?::uuid AND tipo = 'APORTE_PERIODICO'
                           AND estado NOT IN ('ANULADO', 'PAGADO')
                        """,
                        periodoId)
                .get(0, Integer.class);
        if (porCobrar == null || porCobrar > 0) {
            return false;
        }
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "aportes.periodo_recaudado",
                        "periodo",
                        periodoId,
                        Map.of(
                                "periodoId", periodoId.toString(),
                                "grupoId", grupoId.toString(),
                                "motivo", "COMPLETO",
                                "ocurridoEn", ahora.toString()),
                        correlationId));
        return true;
    }
}
