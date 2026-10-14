package bo.aportaya.identidad.dominio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que ve quien revisa un expediente en el portal de riesgo.
 *
 * <p><b>El numero del documento no viaja</b>: solo el tipo y el lugar de expedicion.
 * Quien decide mira la foto y la compara con lo declarado; para eso no hace falta que
 * la cola sea un padron de numeros de cedula, y una lista que los lleva es una
 * filtracion esperando que alguien la exporte.
 *
 * <p>Vive en el dominio y no en infraestructura porque lo devuelve el caso de uso y lo
 * consume la pagina — y la pagina no depende de infraestructura (ArquitecturaTest).
 */
public record ExpedienteDeIdentidad(
        UUID verificacionId,
        UUID usuarioId,
        String nombreCompleto,
        String documento,
        String estado,
        OffsetDateTime iniciadaEn,
        OffsetDateTime resueltaEn,
        String motivoRechazo,
        List<String> fotos,
        /** Hasta cuando vale el documento; {@code null} si el alta no la trajo. */
        LocalDate fechaExpiracionDocumento) {

    /** Sin fecha de vencimiento: los usos anteriores a que el alta la pidiera. */
    public ExpedienteDeIdentidad(
            UUID verificacionId,
            UUID usuarioId,
            String nombreCompleto,
            String documento,
            String estado,
            OffsetDateTime iniciadaEn,
            OffsetDateTime resueltaEn,
            String motivoRechazo,
            List<String> fotos) {
        this(
                verificacionId,
                usuarioId,
                nombreCompleto,
                documento,
                estado,
                iniciadaEn,
                resueltaEn,
                motivoRechazo,
                fotos,
                null);
    }

    /** Las cinco caras que exige la verificacion (contrato: {@code CaraDelExpediente}). */
    public static final List<String> CARAS =
            List.of("ANVERSO", "REVERSO", "SELFIE", "PERFIL_IZQUIERDO", "PERFIL_DERECHO");

    /** Sin las cinco fotos no se aprueba: aprobar a ciegas es no revisar. */
    public boolean completo() {
        return fotos.containsAll(CARAS);
    }

    /** Solo se decide lo que espera una decision; lo resuelto no se vuelve a decidir. */
    public boolean esperaDecision() {
        return "PENDIENTE".equals(estado) || "EN_REVISION".equals(estado);
    }

    /**
     * Vigente hoy: con fecha y sin pasar. Sin fecha es NO vigente a efectos de aprobar —
     * no hay forma de comprobarlo, y aprobar lo que no se puede comprobar es no revisar.
     */
    public boolean documentoVigente(LocalDate hoy) {
        return fechaExpiracionDocumento != null && !fechaExpiracionDocumento.isBefore(hoy);
    }
}
