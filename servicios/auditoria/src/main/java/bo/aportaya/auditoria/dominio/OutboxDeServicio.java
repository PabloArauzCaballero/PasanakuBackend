package bo.aportaya.auditoria.dominio;

import java.util.Optional;

/**
 * El outbox de un servicio segun lo que su relevo publica en Prometheus. Cada dato es opcional: un servicio que no lo
 * publico no tiene cero, no tiene dato.
 */
public record OutboxDeServicio(
        String servicio,
        Optional<Long> pendientes,
        Optional<Long> fallidos,
        Optional<Long> edadMasViejoSegundos,
        Optional<Long> publicados24h,
        Optional<Long> errores24h) {}
