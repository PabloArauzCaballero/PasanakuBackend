package bo.aportaya.tarifas.dominio;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * CU-132 · Qué versión rige en un momento, y qué falta. Puro.
 *
 * <p>Rige la versión APROBADA más reciente con {@code vigente_desde <= momento}, una por parámetro y nivel. Lo que
 * se congela para un grupo es lo que regía cuando inició (ADR-056 §6): un cambio posterior solo vale para grupos
 * nuevos.
 */
public final class VigenciaDeParametros {

    private VigenciaDeParametros() {}

    /** Las versiones que rigen en {@code momento}, una por parámetro y nivel, en el orden de {@code versiones}. */
    public static List<VersionDelParametro> vigentesAl(List<VersionDelParametro> versiones, OffsetDateTime momento) {
        Map<String, VersionDelParametro> porClave = new LinkedHashMap<>();
        versiones.stream()
                .filter(v -> "APROBADA".equals(v.estado()))
                .filter(v -> !v.vigenteDesde().isAfter(momento))
                .sorted((a, b) -> b.vigenteDesde().compareTo(a.vigenteDesde()))
                .forEach(v ->
                        porClave.putIfAbsent(v.parametroId() + "|" + v.nivel().orElse(""), v));
        return List.copyOf(porClave.values());
    }

    /** Versiones vigentes para el conjunto completo de niveles fijado por Transparencia al iniciar el grupo. */
    public static List<VersionDelParametro> vigentesAl(
            List<VersionDelParametro> versiones,
            OffsetDateTime momento,
            List<ParametroDelCatalogo> catalogo,
            List<String> nivelesAplicables) {
        Map<String, VersionDelParametro> disponibles = new LinkedHashMap<>();
        versiones.stream()
                .filter(v -> "APROBADA".equals(v.estado()) && !v.vigenteDesde().isAfter(momento))
                .sorted((a, b) -> b.vigenteDesde().compareTo(a.vigenteDesde()))
                .forEach(v -> disponibles.putIfAbsent(clave(v.parametroId(), v.nivel().orElse(null)), v));
        List<VersionDelParametro> resultado = new java.util.ArrayList<>();
        for (ParametroDelCatalogo parametro : catalogo) {
            if (parametro.catalogo().porNivel()) {
                for (String nivel : nivelesAplicables) {
                    var v = disponibles.get(clave(parametro.id(), nivel));
                    if (v != null) resultado.add(v);
                }
            } else {
                var v = disponibles.get(clave(parametro.id(), null));
                if (v != null) resultado.add(v);
            }
        }
        return List.copyOf(resultado);
    }

    /** Los códigos del catálogo sin ningún valor entre {@code parametrosConValor}, ordenados. */
    public static Set<String> faltantes(List<ParametroDelCatalogo> catalogo, Set<UUID> parametrosConValor) {
        Set<String> faltan = new TreeSet<>();
        catalogo.stream()
                .filter(p -> !parametrosConValor.contains(p.id()))
                .forEach(p -> faltan.add(p.catalogo().codigo()));
        return faltan;
    }

    /** Identifica cada parámetro y nivel sin versión aprobada; los parámetros no segmentados usan nivel «único». */
    public static Set<String> faltantes(
            List<ParametroDelCatalogo> catalogo,
            List<String> nivelesAplicables,
            List<ParametroCongelado> congelados) {
        Set<String> presentes = new java.util.HashSet<>();
        congelados.forEach(c -> presentes.add(clave(c.parametroId(), c.nivel().orElse(null))));
        Set<String> faltan = new TreeSet<>();
        for (ParametroDelCatalogo parametro : catalogo) {
            if (parametro.catalogo().porNivel()) {
                for (String nivel : nivelesAplicables) {
                    if (!presentes.contains(clave(parametro.id(), nivel))) {
                        faltan.add(parametro.catalogo().codigo() + "[" + nivel + "]");
                    }
                }
            } else if (!presentes.contains(clave(parametro.id(), null))) {
                faltan.add(parametro.catalogo().codigo());
            }
        }
        return faltan;
    }

    private static String clave(UUID parametroId, String nivel) {
        return parametroId + "|" + (nivel == null ? "" : nivel);
    }

    /** Lo congelado de un grupo, tal como lo consume {@code garantia} (contrato de L2, §3). Sin identidades. */
    public record ParametroCongelado(
            UUID parametroId,
            String codigo,
            Optional<String> nivel,
            java.math.BigDecimal valor,
            Optional<String> unidad,
            Optional<String> moneda,
            UUID versionId,
            OffsetDateTime congeladoEn) {}
}
