package bo.aportaya.identidad.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CU-134 · la fila del directorio y la ficha del participante antes de enmascarar. Datos sin comportamiento,
 * en el dominio para que la web no dependa de la persistencia.
 */
public final class RegistroDeParticipante {

    private RegistroDeParticipante() {}

    /** Una fila del directorio: lo minimo para elegir a quien abrir, sin contacto. */
    public record Fila(
            UUID usuarioId,
            String codigoPublico,
            String nombres,
            String apellidos,
            String estado,
            String nivelKyc,
            OffsetDateTime fechaRegistro,
            boolean tieneRestriccion,
            boolean tieneBloqueo,
            boolean tieneMedidaEnCurso) {}

    /** Los datos de la ficha antes de enmascarar. El valor plano no sale del organismo. */
    public record Ficha(Fila resumen, OffsetDateTime ultimoAcceso, String telefonoE164, String correo) {}
}
