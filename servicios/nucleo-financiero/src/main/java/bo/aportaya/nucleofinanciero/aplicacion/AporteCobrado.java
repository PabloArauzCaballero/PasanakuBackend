package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que {@code aportes.aporte_cobrado} trae, ya con tipos: el borde con Kafka valida y convierte. {@code tipo} es
 * opcional porque los eventos publicados antes de 2026-10-16 no lo traen: sin él, el cobro va al grupo
 * ({@link DestinoDelCobro}).
 */
public record AporteCobrado(
        UUID eventoId,
        UUID correlationId,
        UUID pagoId,
        UUID obligacionId,
        UUID grupoId,
        UUID usuarioId,
        Dinero monto,
        String canal,
        Optional<String> tipo) {}
