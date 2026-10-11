package bo.aportaya.grupos.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una decisión humana (o la propuesta que la precede) sobre una solicitud de ingreso.
 *
 * <p>{@code apartamiento} es verdadero cuando la persona decidió en contra de la lectura del motor: aceptó
 * pese a una alerta o SIN_DATOS, o rechazó cuando el motor recomendaba aceptar. Quedan la recomendación, la
 * versión del motor y la evidencia, para que la contradicción sea visible y no una sospecha.
 *
 * <p>Se conserva íntegra y no se reescribe: revisar es agregar una decisión nueva. Vive en el dominio
 * porque la leen la aplicación, la persistencia y la capa web sin que ninguna dependa de otra.
 */
public record DecisionDeIngreso(
        UUID id,
        UUID solicitudId,
        UUID clave,
        String fase,
        String decision,
        UUID actorId,
        String motivo,
        UUID propuestaId,
        UUID participanteId,
        int revision,
        String evidenciaAlgoritmo,
        String versionMotor,
        String recomendacionAlgoritmo,
        boolean apartamiento,
        OffsetDateTime ocurridaEn,
        UUID correlacionId) {}
