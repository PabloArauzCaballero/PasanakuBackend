package bo.aportaya.auditoria.dominio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Un incidente operativo tal como lo lista Sistemas › Incidentes: sin descripcion ni causa raiz (texto de la ficha) y
 * sin {@code impacto_monetario}, que la tabla guarda sin moneda (regla 91.1.2).
 */
public record IncidenteOperativo(
        UUID id,
        String codigo,
        String titulo,
        String severidad,
        String sistemaAfectado,
        String estado,
        OffsetDateTime detectadoEn,
        Optional<OffsetDateTime> resueltoEn,
        int impactoUsuarios) {}
