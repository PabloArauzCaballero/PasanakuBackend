package bo.aportaya.cumplimiento.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * Las colas del backoffice de cumplimiento, riesgo y gobierno (backoffice carril A · H9): reportes UIF
 * (CU-43), requerimientos de autoridad (CU-45), eventos de riesgo (CU-54), incidentes (CU-55), pruebas de
 * continuidad (CU-56) y actas de comite (CU-94).
 *
 * <p>Respuesta minima: ninguna cola devuelve textos libres (alcance, descripcion, hallazgos, decisiones) ni
 * enlaces a documentos; solo lo necesario para ordenar el trabajo. Cursor {@code (columna, id)} descendente,
 * con {@code null} = primera pagina; los estados llegan ya validados.
 */
@Component
public class TableroDeCumplimientoRepositorio {

    public List<Reporte> reportes(DSLContext dsl, List<String> estados, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, catalogo_reporte_id, periodo, fecha_corte, fecha_limite, estado, cantidad_registros,
                       reporte_en_cero, monto_total, generado_en, aprobado_por
                  FROM cumplimiento.reporte_regulatorio
                 WHERE estado = ANY(?::varchar[])
                   AND (?::date IS NULL OR (fecha_corte, id) < (?::date, ?::uuid))
                 ORDER BY fecha_corte DESC, id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Reporte(
                        f.get("id", UUID.class),
                        f.get("catalogo_reporte_id", UUID.class),
                        f.get("periodo", String.class),
                        f.get("fecha_corte", LocalDate.class),
                        f.get("fecha_limite", LocalDate.class),
                        f.get("estado", String.class),
                        f.get("cantidad_registros", Integer.class),
                        f.get("reporte_en_cero", Boolean.class),
                        f.get("monto_total", BigDecimal.class),
                        f.get("generado_en", OffsetDateTime.class),
                        f.get("aprobado_por", UUID.class)));
    }

    public List<Requerimiento> requerimientos(
            DSLContext dsl, List<String> estados, OffsetDateTime antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, autoridad, numero_oficio, fecha_recepcion, plazo_respuesta, estado,
                       usuario_afectado_id, bloqueo_saldo_id IS NOT NULL AS con_bloqueo, respondido_por,
                       respondido_en
                  FROM cumplimiento.requerimiento_autoridad
                 WHERE estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (fecha_recepcion, id) < (?::timestamptz, ?::uuid))
                 ORDER BY fecha_recepcion DESC, id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Requerimiento(
                        f.get("id", UUID.class),
                        f.get("autoridad", String.class),
                        f.get("numero_oficio", String.class),
                        f.get("fecha_recepcion", OffsetDateTime.class),
                        f.get("plazo_respuesta", OffsetDateTime.class),
                        f.get("estado", String.class),
                        f.get("usuario_afectado_id", UUID.class),
                        f.get("con_bloqueo", Boolean.class),
                        f.get("respondido_por", UUID.class),
                        f.get("respondido_en", OffsetDateTime.class)));
    }

    public List<EventoDeRiesgo> eventos(
            DSLContext dsl, List<String> estados, OffsetDateTime antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, codigo, categoria_evento, factor_riesgo, linea_negocio, fecha_ocurrencia,
                       fecha_deteccion, perdida_bruta, recuperacion, perdida_neta, moneda, estado,
                       reportado_central_riesgo_operativo
                  FROM cumplimiento.evento_riesgo_operativo
                 WHERE estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (fecha_deteccion, id) < (?::timestamptz, ?::uuid))
                 ORDER BY fecha_deteccion DESC, id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new EventoDeRiesgo(
                        f.get("id", UUID.class),
                        f.get("codigo", String.class),
                        f.get("categoria_evento", String.class),
                        f.get("factor_riesgo", String.class),
                        f.get("linea_negocio", String.class),
                        f.get("fecha_ocurrencia", OffsetDateTime.class),
                        f.get("fecha_deteccion", OffsetDateTime.class),
                        f.get("perdida_bruta", BigDecimal.class),
                        f.get("recuperacion", BigDecimal.class),
                        f.get("perdida_neta", BigDecimal.class),
                        f.get("moneda", String.class).trim(),
                        f.get("estado", String.class),
                        f.get("reportado_central_riesgo_operativo", Boolean.class)));
    }

    public List<Incidente> incidentes(
            DSLContext dsl, List<String> estados, OffsetDateTime antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, codigo, tipo, severidad, datos_personales_afectados, usuarios_afectados, detectado_en,
                       contenido_en, plazo_reporte, reportado_al_organismo_en, notificado_a_titulares_en, estado
                  FROM cumplimiento.incidente_seguridad
                 WHERE estado = ANY(?::varchar[])
                   AND (?::timestamptz IS NULL OR (detectado_en, id) < (?::timestamptz, ?::uuid))
                 ORDER BY detectado_en DESC, id DESC
                 LIMIT ?
                """,
                        estados.toArray(String[]::new),
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(TableroDeCumplimientoRepositorio::incidente);
    }

    public List<Prueba> pruebas(DSLContext dsl, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, plan_continuidad_id, acta_comite_id, ejecutada_por, tipo, fecha,
                       rto_obtenido_minutos, rpo_obtenido_minutos, resultado
                  FROM cumplimiento.prueba_continuidad
                 WHERE (?::date IS NULL OR (fecha, id) < (?::date, ?::uuid))
                 ORDER BY fecha DESC, id DESC
                 LIMIT ?
                """,
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Prueba(
                        f.get("id", UUID.class),
                        f.get("plan_continuidad_id", UUID.class),
                        f.get("acta_comite_id", UUID.class),
                        f.get("ejecutada_por", UUID.class),
                        f.get("tipo", String.class),
                        f.get("fecha", LocalDate.class),
                        f.get("rto_obtenido_minutos", Integer.class),
                        f.get("rpo_obtenido_minutos", Integer.class),
                        f.get("resultado", String.class)));
    }

    public List<Acta> actas(DSLContext dsl, LocalDate antesDe, UUID antesDeId, int limite) {
        return dsl.fetch(
                        """
                SELECT id, comite_gobierno_id, numero, fecha, cumple_quorum, hash_documento
                  FROM cumplimiento.acta_comite
                 WHERE (?::date IS NULL OR (fecha, id) < (?::date, ?::uuid))
                 ORDER BY fecha DESC, id DESC
                 LIMIT ?
                """,
                        antesDe,
                        antesDe,
                        antesDeId,
                        limite)
                .map(f -> new Acta(
                        f.get("id", UUID.class),
                        f.get("comite_gobierno_id", UUID.class),
                        f.get("numero", String.class),
                        f.get("fecha", LocalDate.class),
                        f.get("cumple_quorum", Boolean.class),
                        f.get("hash_documento", String.class)));
    }

    private static Incidente incidente(Record f) {
        return new Incidente(
                f.get("id", UUID.class),
                f.get("codigo", String.class),
                f.get("tipo", String.class),
                f.get("severidad", String.class),
                f.get("datos_personales_afectados", Boolean.class),
                f.get("usuarios_afectados", Integer.class),
                f.get("detectado_en", OffsetDateTime.class),
                f.get("contenido_en", OffsetDateTime.class),
                f.get("plazo_reporte", OffsetDateTime.class),
                f.get("reportado_al_organismo_en", OffsetDateTime.class),
                f.get("notificado_a_titulares_en", OffsetDateTime.class),
                f.get("estado", String.class));
    }

    public record Reporte(
            UUID id,
            UUID catalogoReporteId,
            String periodo,
            LocalDate fechaCorte,
            LocalDate fechaLimite,
            String estado,
            int cantidadRegistros,
            boolean reporteEnCero,
            BigDecimal montoTotal,
            OffsetDateTime generadoEn,
            UUID aprobadoPor) {}

    public record Requerimiento(
            UUID id,
            String autoridad,
            String numeroOficio,
            OffsetDateTime fechaRecepcion,
            OffsetDateTime plazoRespuesta,
            String estado,
            UUID usuarioAfectadoId,
            boolean conBloqueo,
            UUID respondidoPor,
            OffsetDateTime respondidoEn) {}

    public record EventoDeRiesgo(
            UUID id,
            String codigo,
            String categoria,
            String factor,
            String lineaNegocio,
            OffsetDateTime fechaOcurrencia,
            OffsetDateTime fechaDeteccion,
            BigDecimal perdidaBruta,
            BigDecimal recuperacion,
            BigDecimal perdidaNeta,
            String moneda,
            String estado,
            boolean reportadoCentral) {}

    public record Incidente(
            UUID id,
            String codigo,
            String tipo,
            String severidad,
            boolean datosPersonalesAfectados,
            int usuariosAfectados,
            OffsetDateTime detectadoEn,
            OffsetDateTime contenidoEn,
            OffsetDateTime plazoReporte,
            OffsetDateTime reportadoAlOrganismoEn,
            OffsetDateTime notificadoATitularesEn,
            String estado) {}

    public record Prueba(
            UUID id,
            UUID planContinuidadId,
            UUID actaComiteId,
            UUID ejecutadaPor,
            String tipo,
            LocalDate fecha,
            int rtoMinutos,
            int rpoMinutos,
            String resultado) {}

    public record Acta(
            UUID id,
            UUID comiteGobiernoId,
            String numero,
            LocalDate fecha,
            boolean cumpleQuorum,
            String hashDocumento) {}
}
