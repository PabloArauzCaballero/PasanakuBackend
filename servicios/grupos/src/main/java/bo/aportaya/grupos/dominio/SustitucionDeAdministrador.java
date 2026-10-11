package bo.aportaya.grupos.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * El registro íntegro de un cambio de administrador: quién salió, quién entró, por qué, quién lo decidió y qué
 * obligaciones conserva el saliente. No se reescribe; se lee igual en el primer intento y en cada reintento.
 * Vive en el dominio porque lo leen la aplicación, la persistencia y la capa web sin que ninguna dependa de otra.
 */
public record SustitucionDeAdministrador(
        UUID id,
        UUID grupoId,
        UUID salienteParticipanteId,
        UUID entranteParticipanteId,
        UUID clave,
        String motivo,
        UUID actorId,
        String obligacionesConservadas,
        OffsetDateTime ocurridaEn,
        UUID correlacionId) {}
