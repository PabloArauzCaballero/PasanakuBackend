package bo.aportaya.identidad.dominio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Las piezas del informe de accesos de CU-08 (GU-08): sus secciones y su fila. Sin logica. */
public final class InformeDeAccesos {

    private InformeDeAccesos() {}

    /** VIVOS: toda asignacion vigente · POR_VENCER: vence dentro del horizonte · SIN_TOTP: rol GLOBAL sin TOTP. */
    public enum Seccion {
        VIVOS,
        POR_VENCER,
        SIN_TOTP
    }

    /** Una asignacion vigente con lo que la revision necesita ver. Nunca correo ni telefono. */
    public record Fila(
            UUID asignacionId,
            UUID usuarioId,
            String nombre,
            String estadoCuenta,
            String rolCodigo,
            String ambito,
            Optional<UUID> ambitoId,
            UUID otorgadaPor,
            OffsetDateTime otorgadaEn,
            Optional<OffsetDateTime> vigenteHasta,
            boolean totpConfirmado) {}
}
