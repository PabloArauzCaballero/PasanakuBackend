package bo.aportaya.cumplimiento.infraestructura;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Las bandejas de alertas y casos LFT (CU-44, backoffice carril A · H5) y la ficha de un caso: solo lectura.
 *
 * <p>Deber de reserva y respuesta minima: ninguna de las dos bandejas lee {@code usuario_id},
 * montos, {@code detalle}, {@code resumen} ni {@code hallazgos}. Lo que no se lee no se puede
 * filtrar por error a la respuesta.
 */
@Component
public class BandejasLftRepositorio {

    public List<AlertaEnBandeja> alertas(
            DSLContext dsl,
            List<String> estados,
            List<String> severidades,
            Optional<OffsetDateTime> despuesDeFecha,
            Optional<UUID> despuesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        parametros.add(severidades.toArray(String[]::new));
        String cursor = "";
        if (despuesDeFecha.isPresent() && despuesDeId.isPresent()) {
            cursor = " AND (a.detectada_en, a.id) > (?::timestamptz, ?::uuid)";
            parametros.add(despuesDeFecha.get());
            parametros.add(despuesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT a.id, r.codigo, r.tipologia, a.severidad, a.estado, a.detectada_en, a.asignada_a,
                       a.caso_id, a.cerrada_en
                  FROM cumplimiento.alerta_monitoreo_lft a
                  JOIN cumplimiento.regla_monitoreo_lft r ON r.id = a.regla_monitoreo_id
                 WHERE a.estado = ANY(?::varchar[]) AND a.severidad = ANY(?::varchar[])"""
                                + cursor
                                + " ORDER BY a.detectada_en, a.id LIMIT ?",
                        parametros.toArray())
                .map(f -> new AlertaEnBandeja(
                        f.get("id", UUID.class),
                        f.get("codigo", String.class),
                        f.get("tipologia", String.class),
                        f.get("severidad", String.class),
                        f.get("estado", String.class),
                        f.get("detectada_en", OffsetDateTime.class),
                        f.get("asignada_a", UUID.class),
                        f.get("caso_id", UUID.class),
                        f.get("cerrada_en", OffsetDateTime.class)));
    }

    public List<CasoEnBandeja> casos(
            DSLContext dsl,
            List<String> estados,
            Optional<OffsetDateTime> despuesDePlazo,
            Optional<UUID> despuesDeId,
            int limite) {
        var parametros = new ArrayList<Object>();
        parametros.add(estados.toArray(String[]::new));
        String cursor = "";
        if (despuesDePlazo.isPresent() && despuesDeId.isPresent()) {
            cursor = " AND (c.plazo_limite, c.id) > (?::timestamptz, ?::uuid)";
            parametros.add(despuesDePlazo.get());
            parametros.add(despuesDeId.get());
        }
        parametros.add(limite);
        return dsl.fetch(
                        """
                SELECT c.id, c.codigo, c.origen, c.estado, c.prioridad, c.analista_id, c.revisado_por,
                       c.abierto_en, c.plazo_limite, c.decision, c.cerrado_en
                  FROM cumplimiento.caso_investigacion_lft c
                 WHERE c.estado = ANY(?::varchar[])"""
                                + cursor
                                + " ORDER BY c.plazo_limite, c.id LIMIT ?",
                        parametros.toArray())
                .map(f -> new CasoEnBandeja(
                        f.get("id", UUID.class),
                        f.get("codigo", String.class),
                        f.get("origen", String.class),
                        f.get("estado", String.class),
                        f.get("prioridad", String.class),
                        f.get("analista_id", UUID.class),
                        f.get("revisado_por", UUID.class),
                        f.get("abierto_en", OffsetDateTime.class),
                        f.get("plazo_limite", OffsetDateTime.class),
                        f.get("decision", String.class),
                        f.get("cerrado_en", OffsetDateTime.class)));
    }

    /**
     * La ficha de un caso (CU-44, 2026-11-24): la fila del caso, su alerta enlazada mas antigua y cuantas tiene.
     *
     * <p>Es la unica consulta de este repositorio que lee {@code usuario_id}, y lo lee solo para que
     * {@link FichaDeCaso#titularEnmascarado()} lo reduzca a cuatro caracteres antes de salir de aca: el
     * seudonimo entero no lo ve ni el caso de uso. Sigue sin leer {@code resumen} ni {@code hallazgos}.
     */
    public Optional<FichaDeCaso> ficha(DSLContext dsl, UUID casoId) {
        var caso = dsl.fetchOptional(
                """
                SELECT c.id, c.codigo, c.origen, c.estado, c.prioridad, c.abierto_en, c.plazo_limite,
                       c.usuario_id::text AS titular
                  FROM cumplimiento.caso_investigacion_lft c
                 WHERE c.id = ?""",
                casoId);
        if (caso.isEmpty()) {
            return Optional.empty();
        }
        var f = caso.get();
        // La de origen es la mas antigua; el desempate por id deja el resultado estable.
        Optional<AlertaDeOrigen> origen = dsl.fetchOptional(
                        """
                SELECT a.id, r.codigo, r.tipologia, a.severidad, a.detectada_en
                  FROM cumplimiento.alerta_monitoreo_lft a
                  JOIN cumplimiento.regla_monitoreo_lft r ON r.id = a.regla_monitoreo_id
                 WHERE a.caso_id = ?
                 ORDER BY a.detectada_en, a.id
                 LIMIT 1""",
                        casoId)
                .map(a -> new AlertaDeOrigen(
                        a.get("id", UUID.class),
                        a.get("codigo", String.class),
                        a.get("tipologia", String.class),
                        a.get("severidad", String.class),
                        a.get("detectada_en", OffsetDateTime.class)));
        Integer enlazadas = dsl.fetchOne(
                        "SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft WHERE caso_id = ?", casoId)
                .get(0, Integer.class);
        return Optional.of(new FichaDeCaso(
                f.get("id", UUID.class),
                f.get("codigo", String.class),
                f.get("origen", String.class),
                f.get("estado", String.class),
                f.get("prioridad", String.class),
                f.get("abierto_en", OffsetDateTime.class),
                f.get("plazo_limite", OffsetDateTime.class),
                origen,
                enlazadas == null ? 0 : enlazadas,
                enmascarar(f.get("titular", String.class))));
    }

    /**
     * Cuatro asteriscos y los cuatro ultimos caracteres del seudonimo (el patron de {@code CuentaEnmascarada} de
     * entregas). Distingue titulares entre casos sin identificar a ninguno: el nombre y el documento ni siquiera
     * viven en este servicio.
     */
    static String enmascarar(String seudonimo) {
        String limpio = seudonimo.replace("-", "").toLowerCase(java.util.Locale.ROOT);
        return "****" + limpio.substring(limpio.length() - 4);
    }

    public record AlertaEnBandeja(
            UUID id,
            String reglaCodigo,
            String tipologia,
            String severidad,
            String estado,
            OffsetDateTime detectadaEn,
            UUID asignadaA,
            UUID casoId,
            OffsetDateTime cerradaEn) {}

    public record CasoEnBandeja(
            UUID id,
            String codigo,
            String origen,
            String estado,
            String prioridad,
            UUID analistaId,
            UUID revisadoPor,
            OffsetDateTime abiertoEn,
            OffsetDateTime plazoLimite,
            String decision,
            OffsetDateTime cerradoEn) {}

    public record AlertaDeOrigen(
            UUID alertaId, String reglaCodigo, String tipologia, String severidad, OffsetDateTime detectadaEn) {}

    public record FichaDeCaso(
            UUID id,
            String codigo,
            String origen,
            String estado,
            String prioridad,
            OffsetDateTime abiertoEn,
            OffsetDateTime plazoLimite,
            Optional<AlertaDeOrigen> alertaDeOrigen,
            int alertasEnlazadas,
            String titularEnmascarado) {}
}
