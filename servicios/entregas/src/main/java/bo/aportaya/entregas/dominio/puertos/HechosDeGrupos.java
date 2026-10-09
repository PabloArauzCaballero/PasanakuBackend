package bo.aportaya.entregas.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que {@code grupos} sabe y este servicio necesita para vender un derecho de cobro.
 *
 * <p>CONTRATO PROPUESTO (hoy {@code grupos} no lo expone y no es de este carril): quien es el
 * titular de un turno, si alguien es miembro activo de un grupo y en cuales grupos lo es. Se
 * pregunta FUERA de transaccion (invariante 6). {@link Optional#empty()} significa «no se pudo
 * saber» y quien pregunta deniega: jamas se asume que alguien es titular o miembro.
 *
 * <p>Ninguno de estos datos le llega a este servicio por el cliente: vender un turno ajeno o
 * comprar en un grupo del que no se es miembro es exactamente lo que este puerto impide.
 */
public interface HechosDeGrupos {

    /** El turno tal como lo ve grupos. {@code montoDelDerecho} es lo que cobra su titular. */
    record Turno(
            UUID turnoId,
            UUID grupoId,
            UUID cupoId,
            UUID participanteId,
            UUID usuarioId,
            String estado,
            Dinero montoDelDerecho) {}

    record Miembro(UUID participanteId, boolean activo) {}

    Optional<Turno> turno(UUID turnoId);

    Optional<Miembro> miembro(UUID grupoId, UUID usuarioId);

    /** Los grupos donde la persona es miembro activo; vacio si no se pudo saber. */
    Optional<Set<UUID>> gruposActivosDe(UUID usuarioId);
}
