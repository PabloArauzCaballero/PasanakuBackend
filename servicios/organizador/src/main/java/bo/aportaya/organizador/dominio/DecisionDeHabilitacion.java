package bo.aportaya.organizador.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una resolución humana sobre una habilitación, conservada íntegra.
 *
 * <p>La revisión de una resolución anterior es una fila nueva con la fase {@code REVISION}: lo ya
 * resuelto nunca se reescribe. {@code evidenciaRequisitos} guarda qué se evaluó y qué faltaba en el
 * instante de decidir, para poder reconstruir con qué se habilitó a alguien.
 */
public record DecisionDeHabilitacion(
        UUID id,
        UUID solicitudId,
        UUID clave,
        String fase,
        String decision,
        UUID actorId,
        String motivo,
        int revision,
        UUID organizadorId,
        String evidenciaRequisitos,
        OffsetDateTime ocurridaEn,
        UUID correlacionId) {}
