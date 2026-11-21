package bo.aportaya.tarifas.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** CU-132 · Una versión de un parámetro: PROPUESTA, APROBADA o RECHAZADA. Lo aprobado no se edita (R-TAR-16). */
public record VersionDelParametro(
        UUID id,
        UUID parametroId,
        String codigo,
        Optional<String> nivel,
        BigDecimal valor,
        Optional<String> moneda,
        OffsetDateTime vigenteDesde,
        UUID propuestoPor,
        Optional<UUID> aprobadoPor,
        String estado) {}
