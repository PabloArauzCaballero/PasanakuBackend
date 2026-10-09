package bo.aportaya.organizador.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** La solicitud de habilitación tal como se ve en la bandeja y en el expediente. */
public record ExpedienteDeHabilitacion(
        UUID solicitudId,
        UUID usuarioId,
        String estado,
        BigDecimal reputacionAlSolicitar,
        UUID kycReforzadoId,
        String motivoRechazo,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaResolucion,
        int revision) {}
