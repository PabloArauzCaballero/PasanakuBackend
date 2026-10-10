package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.PlanDeCaja;
import bo.aportaya.garantia.infraestructura.PlanDeContingenciaRepositorio;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * H8.S1.M6 · Ejecuta la contingencia de caja DENTRO de la transaccion de quien la detecta.
 *
 * <p>No abre transaccion propia: se invoca con el {@code dsl} de {@link CU23CubrirConRespaldo} (que detecta
 * el respaldo insuficiente) o de {@link CU23ReservarRespaldo} (que consulta si hay una activa), de modo que
 * el plan, el evento de escalamiento y la cobertura quedan o no quedan juntos.
 *
 * <p>Activar es idempotente: la base solo admite un plan vivo por grupo (R-GAR-13); el segundo intento no
 * emite un segundo escalamiento. Resolver tambien: cerrar lo ya cerrado no hace nada.
 */
@Component
public class ContingenciaDeCaja {

    private final PlanDeContingenciaRepositorio planes;
    private final Outbox outbox;

    public ContingenciaDeCaja(PlanDeContingenciaRepositorio planes, Outbox outbox) {
        this.planes = planes;
        this.outbox = outbox;
    }

    /**
     * Respaldo insuficiente (o sin reserva) sobre un compromiso que ya existe: emite el incidente de siempre
     * ({@code garantia.respaldo_insuficiente}) y ejecuta la contencion (plan + escalamiento). Una sola vez por grupo.
     */
    public void insuficiente(DSLContext dsl, PlanDeCaja plan, UUID correlacion, OffsetDateTime ahora) {
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "garantia.respaldo_insuficiente",
                        "cobertura_respaldo",
                        plan.turnoId(),
                        java.util.Map.of(
                                "grupoId", plan.grupoId().toString(),
                                "turnoId", plan.turnoId().toString(),
                                "motivo", plan.motivo(),
                                "faltante", plan.faltante().toString(),
                                "disponible", plan.disponible().toString(),
                                "sinCubrir", plan.sinCubrir().toString(),
                                "moneda", plan.faltante().moneda().name()),
                        correlacion));
        activar(dsl, plan, correlacion, ahora);
    }

    /** @return true si se abrio un plan nuevo (y se escalo); false si el grupo ya tenia uno vivo */
    public boolean activar(DSLContext dsl, PlanDeCaja plan, UUID correlacion, OffsetDateTime ahora) {
        var id = planes.abrir(
                dsl, plan.grupoId(), plan.descripcion(), plan.sinCubrir().monto(), ahora);
        id.ifPresent(planId -> outbox.emitir(
                dsl,
                new EventoDominio(
                        "garantia.contingencia_de_caja_activada",
                        "plan_contingencia",
                        planId,
                        plan.comoCarga(planId),
                        correlacion)));
        return id.isPresent();
    }

    /** Cierra la contingencia viva del grupo (el faltante ya no existe o se cubrio). */
    public boolean resolver(DSLContext dsl, UUID grupoId, UUID turnoId, UUID correlacion, OffsetDateTime ahora) {
        var id = planes.resolver(dsl, grupoId, ahora);
        id.ifPresent(planId -> outbox.emitir(
                dsl,
                new EventoDominio(
                        "garantia.contingencia_de_caja_resuelta",
                        "plan_contingencia",
                        planId,
                        java.util.Map.of(
                                "planId",
                                planId.toString(),
                                "grupoId",
                                grupoId.toString(),
                                "turnoId",
                                turnoId.toString()),
                        correlacion)));
        return id.isPresent();
    }

    public boolean estaActiva(DSLContext dsl) {
        return planes.hayAlguna(dsl);
    }
}
