package bo.aportaya.grupos.dominio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Un acuerdo del grupo como se lista (CU-63): lo que hace falta para encontrar una votacion. */
public record ResumenDeAcuerdo(
        UUID id,
        String tipo,
        String estado,
        String descripcion,
        Optional<UUID> afectado,
        Optional<UUID> usuarioEntrante,
        OffsetDateTime cierraEn) {}
