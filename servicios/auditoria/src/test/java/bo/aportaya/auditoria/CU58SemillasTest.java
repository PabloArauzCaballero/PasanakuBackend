package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import bo.aportaya.auditoria.dominio.ParametrosDeReporte;
import bo.aportaya.auditoria.infraestructura.ReporteRepositorio;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-58 · las definiciones de reporte SEMBRADAS se pueden ejecutar (2026-10-29).
 *
 * <p>La semilla las guardaba como objeto {@code {"campo":"uuid"}} y el motor las lee con {@code jsonb_to_recordset}
 * como arreglo {@code [{nombre,tipo,obligatorio}]}: ninguna se podia ejecutar ({@code cannot call jsonb_to_recordset on
 * a non-array}, reproducido en TEST). Esta prueba siembra el SQL generado de verdad y exige que el motor lea los
 * parametros de cada definicion con un tipo que el validador conoce.
 */
class CU58SemillasTest extends BaseDeAuditoria {

    /** Los nombres de {@code seeders/minimos/19-reportes-y-retencion.json} (la clave de conflicto de la semilla). */
    private static final String[] SEMBRADAS = {
        "Estado de cuenta del participante",
        "Estado del grupo",
        "Histórico de pagos conciliados",
        "Cartera en mora por tramo",
        "Conciliación diaria de custodia",
        "Movimiento del fondo de garantía",
        "Incumplimientos y sanciones firmes",
        "Desempeño del organizador",
        "Operaciones relevantes y alertas de monitoreo",
        "Indicadores de plataforma"
    };

    private static final List<String> TIPOS_DEL_VALIDADOR =
            List.of("UUID", "FECHA", "ENTERO", "DECIMAL", "BOOLEANO", "TEXTO");

    private static String semilla() throws IOException {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("sql/60_semillas"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("No se encontro la raiz del repositorio (sql/60_semillas)");
        }
        return Files.readString(p.resolve("sql/60_semillas/19-reportes-y-retencion.sql"), StandardCharsets.UTF_8);
    }

    private static void sembrar() throws IOException {
        dslFixtura.execute("SET search_path TO auditoria, comun, public");
        dslFixtura.execute(semilla());
    }

    private static long definiciones() {
        return dslFixtura
                .fetchOne("SELECT count(*) FROM auditoria.definicion_reporte")
                .get(0, Long.class);
    }

    @Test
    @DisplayName("idempotente: sembrar dos veces no duplica ni cambia definiciones")
    void idempotente() throws IOException {
        sembrar();
        long primera = definiciones();
        sembrar();
        assertThat(definiciones()).isEqualTo(primera);
    }

    @Test
    @DisplayName(
            "cada definición sembrada se lee con sus parámetros y tipos que el validador conoce, y acepta valores válidos")
    void elMotorLeeTodasLasSembradas() throws IOException {
        sembrar();
        // Por NOMBRE, la clave de conflicto de la semilla: otras pruebas crean definiciones con los mismos tipos.
        List<UUID> ids = dslFixtura
                .fetch("SELECT id FROM auditoria.definicion_reporte WHERE nombre = ANY(?)", (Object) SEMBRADAS)
                .getValues(0, UUID.class);
        assertThat(ids).hasSize(10);

        var repositorio = new ReporteRepositorio();
        for (UUID id : ids) {
            var definicion = repositorio.definicion(dsl, id).orElseThrow();
            Map<String, String> validos = new HashMap<>();
            for (var esperado : definicion.esperados()) {
                assertThat(TIPOS_DEL_VALIDADOR).as(definicion.nombre()).contains(esperado.tipo());
                validos.put(esperado.nombre(), ejemplo(esperado.tipo()));
            }
            assertThatCode(() -> ParametrosDeReporte.validar(definicion.esperados(), validos))
                    .as(definicion.nombre())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("límite: el grupo de la cartera en mora es opcional (antes «uuid?»); la fecha de la conciliación no")
    void obligatorios() throws IOException {
        sembrar();
        var repositorio = new ReporteRepositorio();
        var cartera = repositorio
                .definicion(dsl, idDe("CARTERA_EN_MORA"))
                .orElseThrow()
                .esperados();
        var conciliacion = repositorio
                .definicion(dsl, idDe("CONCILIACION_DIARIA"))
                .orElseThrow()
                .esperados();
        assertThat(cartera).containsExactly(new ParametrosDeReporte.Esperado("grupo_id", "UUID", false));
        assertThat(conciliacion).containsExactly(new ParametrosDeReporte.Esperado("fecha", "FECHA", true));
        assertThatCode(() -> ParametrosDeReporte.validar(cartera, Map.of())).doesNotThrowAnyException();
    }

    private static UUID idDe(String tipo) {
        return dslFixtura
                .fetchOne(
                        "SELECT id FROM auditoria.definicion_reporte WHERE tipo = ? AND nombre = ANY(?)", tipo, (Object)
                                SEMBRADAS)
                .get(0, UUID.class);
    }

    /** Un valor sintético válido para cada tipo del validador. */
    private static String ejemplo(String tipo) {
        return switch (tipo) {
            case "UUID" -> "00000000-0000-4000-8000-000000000001";
            case "FECHA" -> "2026-10-01";
            case "ENTERO" -> "1";
            case "DECIMAL" -> "1.00";
            case "BOOLEANO" -> "true";
            default -> "texto";
        };
    }
}
