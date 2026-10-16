package bo.aportaya.cumplimiento.infraestructura;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Lo que las acciones HTTP del backoffice (carril A · H9) necesitan leer ANTES de delegar en los casos de
 * uso: el estado guardado que decide si un pedido es un reintento de lo mismo o algo distinto, y los datos
 * que el servidor no le acepta al cliente (el plazo y el numero de un oficio ya registrado).
 */
@Component
public class AccionesDeCumplimientoRepositorio {

    public Optional<OficioGuardado> oficioPorNumero(DSLContext dsl, String numeroOficio) {
        return dsl.fetchOptional(
                        """
                SELECT id, numero_oficio, plazo_respuesta, estado, hash_documento, respondido_por, respuesta_url
                  FROM cumplimiento.requerimiento_autoridad
                 WHERE numero_oficio = ?
                """,
                        numeroOficio)
                .map(f -> oficio(f));
    }

    public Optional<OficioGuardado> oficio(DSLContext dsl, UUID id) {
        return dsl.fetchOptional(
                        """
                SELECT id, numero_oficio, plazo_respuesta, estado, hash_documento, respondido_por, respuesta_url
                  FROM cumplimiento.requerimiento_autoridad
                 WHERE id = ?
                """,
                        id)
                .map(f -> oficio(f));
    }

    public Optional<PruebaGuardada> prueba(
            DSLContext dsl, UUID planId, LocalDate fecha, String tipo, UUID ejecutadaPor) {
        return dsl.fetchOptional(
                        """
                SELECT id, rto_obtenido_minutos, rpo_obtenido_minutos, resultado
                  FROM cumplimiento.prueba_continuidad
                 WHERE plan_continuidad_id = ? AND fecha = ? AND tipo = ? AND ejecutada_por = ?
                 ORDER BY id
                 LIMIT 1
                """,
                        planId,
                        fecha,
                        tipo,
                        ejecutadaPor)
                .map(f -> new PruebaGuardada(
                        f.get("id", UUID.class),
                        f.get("rto_obtenido_minutos", Integer.class),
                        f.get("rpo_obtenido_minutos", Integer.class),
                        f.get("resultado", String.class)));
    }

    private static OficioGuardado oficio(org.jooq.Record f) {
        return new OficioGuardado(
                f.get("id", UUID.class),
                f.get("numero_oficio", String.class),
                f.get("plazo_respuesta", OffsetDateTime.class),
                f.get("estado", String.class),
                f.get("hash_documento", String.class),
                f.get("respondido_por", UUID.class),
                f.get("respuesta_url", String.class));
    }

    public record OficioGuardado(
            UUID id,
            String numeroOficio,
            OffsetDateTime plazoRespuesta,
            String estado,
            String hashDocumento,
            UUID respondidoPor,
            String respuestaUrl) {}

    public record PruebaGuardada(UUID id, int rtoMinutos, int rpoMinutos, String resultado) {}
}
