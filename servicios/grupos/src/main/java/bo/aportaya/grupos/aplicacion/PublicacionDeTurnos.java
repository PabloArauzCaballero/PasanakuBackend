package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.TurnoAsignado;
import bo.aportaya.grupos.infraestructura.TurnosAsignadosRepositorio;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * F3.L2.M01 · {@code grupos.turnos_asignados}: a quién le toca el pozo de cada periodo, para que {@code entregas}
 * lo sepa sin llamar a {@code grupos} en el camino del dinero (regla 98.2.5).
 *
 * <p>Va por el outbox en la MISMA transacción que crea los turnos: si el sorteo no se revela, el evento no sale, y
 * si se revela, no se pierde (regla 98.3.3). Sin estado: el repositorio es de solo lectura.
 */
final class PublicacionDeTurnos {

    static final String TIPO = "grupos.turnos_asignados";

    private static final TurnosAsignadosRepositorio TURNOS = new TurnosAsignadosRepositorio();

    private PublicacionDeTurnos() {}

    static void emitir(DSLContext dsl, Outbox outbox, UUID grupoId, UUID correlationId) {
        var turnos = TURNOS.delGrupo(dsl, grupoId);
        outbox.emitir(
                dsl, new EventoDominio(TIPO, "grupo", grupoId, TurnoAsignado.carga(grupoId, turnos), correlationId));
    }
}
