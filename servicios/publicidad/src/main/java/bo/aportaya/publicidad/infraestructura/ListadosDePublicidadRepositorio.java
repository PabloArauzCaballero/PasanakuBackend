package bo.aportaya.publicidad.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Los listados del backoffice de publicidad (CU-110/111/112, carril A · H7): solo lectura.
 *
 * <p>Ninguna consulta lee el documento del socio ni datos del organizador: lo que no se lee no se
 * puede filtrar por error a la respuesta.
 */
@Component
public class ListadosDePublicidadRepositorio {

    /** Anunciantes del mas nuevo al mas viejo, con su cuenta publicitaria (la mas reciente). */
    public List<Anunciante> anunciantes(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        String cursor = "";
        if (antesDe.isPresent() && antesDeId.isPresent()) {
            cursor = " AND (a.creado_en, a.id) < (?::timestamptz, ?::uuid)";
            parametros.add(antesDe.get());
            parametros.add(antesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT a.id, a.tipo, a.razon_social_facturacion, a.estado, a.creado_en,
                       c.id AS cuenta_id, c.estado AS estado_cuenta, c.limite_gasto_mensual, c.moneda
                  FROM publicidad.anunciante a
                  LEFT JOIN LATERAL (
                       SELECT id, estado, limite_gasto_mensual, moneda FROM publicidad.cuenta_publicitaria
                        WHERE anunciante_id = a.id ORDER BY creada_en DESC, id DESC LIMIT 1) c ON true
                 WHERE a.estado = ANY(?::varchar[])"""
                                + cursor
                                + " ORDER BY a.creado_en DESC, a.id DESC LIMIT ?",
                        parametros.toArray())
                .map(f -> new Anunciante(
                        f.get("id", UUID.class),
                        f.get("tipo", String.class),
                        f.get("razon_social_facturacion", String.class),
                        f.get("estado", String.class),
                        f.get("creado_en", OffsetDateTime.class),
                        f.get("cuenta_id", UUID.class),
                        f.get("estado_cuenta", String.class),
                        f.get("limite_gasto_mensual", BigDecimal.class),
                        f.get("moneda", String.class)));
    }

    /** Campanas, la que empieza antes primero (la cola de aprobacion mira lo mas urgente). */
    public List<Campana> campanas(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> despuesDe,
            Optional<UUID> despuesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        String cursor = "";
        if (despuesDe.isPresent() && despuesDeId.isPresent()) {
            cursor = " AND (c.fecha_inicio, c.id) > (?::timestamptz, ?::uuid)";
            parametros.add(despuesDe.get());
            parametros.add(despuesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT c.id, c.cuenta_publicitaria_id, cu.anunciante_id, c.nombre, c.objetivo, c.presupuesto_total,
                       c.presupuesto_consumido, c.moneda, c.fecha_inicio, c.fecha_fin, c.estado, c.aprobada_por
                  FROM publicidad.campana_publicitaria c
                  JOIN publicidad.cuenta_publicitaria cu ON cu.id = c.cuenta_publicitaria_id
                 WHERE c.estado = ANY(?::varchar[])"""
                                + cursor
                                + " ORDER BY c.fecha_inicio, c.id LIMIT ?",
                        parametros.toArray())
                .map(f -> new Campana(
                        f.get("id", UUID.class),
                        f.get("cuenta_publicitaria_id", UUID.class),
                        f.get("anunciante_id", UUID.class),
                        f.get("nombre", String.class),
                        f.get("objetivo", String.class),
                        f.get("presupuesto_total", BigDecimal.class),
                        f.get("presupuesto_consumido", BigDecimal.class),
                        f.get("moneda", String.class),
                        f.get("fecha_inicio", OffsetDateTime.class),
                        f.get("fecha_fin", OffsetDateTime.class),
                        f.get("estado", String.class),
                        f.get("aprobada_por", UUID.class)));
    }

    /** Piezas en un estado de moderacion, la mas antigua primero (cola FIFO del moderador). */
    public List<Pieza> piezas(
            DSLContext dsl, String estado, Optional<OffsetDateTime> despuesDe, Optional<UUID> despuesDeId, int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estado);
        String cursor = "";
        if (despuesDe.isPresent() && despuesDeId.isPresent()) {
            cursor = " AND (p.creada_en, p.id) > (?::timestamptz, ?::uuid)";
            parametros.add(despuesDe.get());
            parametros.add(despuesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT p.id, p.anunciante_id, p.titulo, p.texto, p.url_recurso, p.tipo_recurso, p.estado_moderacion,
                       p.creada_en
                  FROM publicidad.pieza_creativa p
                 WHERE p.estado_moderacion = ?"""
                                + cursor
                                + " ORDER BY p.creada_en, p.id LIMIT ?",
                        parametros.toArray())
                .map(f -> new Pieza(
                        f.get("id", UUID.class),
                        f.get("anunciante_id", UUID.class),
                        f.get("titulo", String.class),
                        f.get("texto", String.class),
                        f.get("url_recurso", String.class),
                        f.get("tipo_recurso", String.class),
                        f.get("estado_moderacion", String.class),
                        f.get("creada_en", OffsetDateTime.class)));
    }

    public record Anunciante(
            UUID id,
            String tipo,
            String razonSocialFacturacion,
            String estado,
            OffsetDateTime creadoEn,
            UUID cuentaId,
            String estadoCuenta,
            BigDecimal limiteGastoMensual,
            String moneda) {}

    public record Campana(
            UUID id,
            UUID cuentaId,
            UUID anuncianteId,
            String nombre,
            String objetivo,
            BigDecimal presupuestoTotal,
            BigDecimal presupuestoConsumido,
            String moneda,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            String estado,
            UUID aprobadaPor) {}

    public record Pieza(
            UUID id,
            UUID anuncianteId,
            String titulo,
            String texto,
            String urlRecurso,
            String tipoRecurso,
            String estadoModeracion,
            OffsetDateTime creadaEn) {}
}
