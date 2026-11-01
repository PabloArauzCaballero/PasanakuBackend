package bo.aportaya.entregas.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la subasta del cupo le pregunta y le pide a {@code grupos} (contrato en {@code grupos.yaml}, CU-64 por
 * subasta). Las dos consultas viajan con el token de la persona (el titular, quien puja); el traspaso, con la identidad
 * de servicio de {@code entregas}. Si {@code grupos} no contesta, la respuesta es {@code NO_DISPONIBLE} y quien
 * pregunta deniega: nunca se supone que un cupo es de alguien ni que alguien puede entrar.
 */
public interface GruposParaSubasta {

    enum Respuesta {
        OK,
        NO_ES_TITULAR,
        NO_EXISTE,
        RECHAZADO,
        NO_DISPONIBLE
    }

    /** Los hechos del cupo para abrir la subasta; {@code motivo} vacio si es subastable. */
    record Cupo(
            UUID participanteId,
            boolean subastable,
            Optional<String> motivo,
            Dinero aporte,
            String nombreDelGrupo,
            String periodicidad) {}

    record Subastabilidad(Respuesta respuesta, Optional<Cupo> cupo) {}

    record Elegibilidad(Respuesta respuesta, boolean elegible, Optional<String> motivo) {}

    record Traspaso(Respuesta respuesta, Optional<UUID> traspasoId) {}

    Subastabilidad subastabilidad(UUID grupoId, UUID cupoId);

    Elegibilidad elegibilidad(UUID grupoId);

    /** {@code RECHAZADO}: el cupo ya no se puede pasar (AP-CU64-01); {@code NO_DISPONIBLE}: se reintenta. */
    Traspaso traspasar(UUID grupoId, UUID cupoId, UUID subastaId, UUID ganadorUsuarioId, UUID salienteParticipanteId);
}
