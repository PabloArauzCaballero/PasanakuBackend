package bo.aportaya.identidad.dominio;

import java.time.LocalDate;

/** Datos recolectados durante el onboarding para retomar el proceso si quedó incompleto. */
public record BorradorOnboardingDatos(
        String correo,
        String telefonoE164,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String tipoDocumento,
        String numeroDocumento,
        String lugarExpedicion,
        LocalDate fechaExpiracion,
        String actividadEconomica,
        String detalleActividad,
        String origenDeFondos,
        String detalleOrigen,
        String montoMensualEstimado,
        String pasoPendiente) {}
