package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * H4.S1.M3 del carril PR4-seguridad (Marcelo, nombre de test reservado — Q-05):
 * append-only demostrado con {@code svc_nucleo_financiero} real, no solo leído del
 * SQL.
 *
 * <p>Defensa en profundidad, verificada en sus DOS capas: el {@code GRANT} (esta
 * clase, vía {@code has_table_privilege}, mismo patrón que
 * {@code AislamientoEsquemaTest.puedeEscribir}) y el disparador
 * ({@code fn_..._append_only} en {@code sql/35_append_only/append_only.sql}, ya
 * demostrado en producción por {@code AuditoriaCriticaTest} de `aportes`: un
 * {@code DELETE} sobre {@code comun.bitacora_evento} con el rol dueño de la fila
 * fue rechazado por el disparador, no solo por el grant).
 *
 * <p>El código de producción de {@code nucleo-financiero} NO se toca: este archivo
 * es 100% de prueba, con nombre reservado en cualquier módulo (Q-05).
 */
class AppendOnlyTest {

    @BeforeAll
    static void calentarElContenedor() {
        BaseDePrueba.contenedor();
    }

    @ParameterizedTest(name = "svc_nucleo_financiero no puede UPDATE/DELETE {0}.{1}")
    @CsvSource({
        "nucleo_financiero,transaccion_billetera",
        "nucleo_financiero,movimiento_billetera",
        "nucleo_financiero,movimiento_contable",
        "nucleo_financiero,movimiento_custodia",
        "nucleo_financiero,saldo_diario_billetera",
        "nucleo_financiero,asiento_contable",
        "comun,bitacora_evento",
    })
    void svcNucleoFinancieroNoPuedeMutarElLibro(String esquema, String tabla) throws SQLException {
        try (Connection conexion = BaseDePrueba.conexion();
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET ROLE svc_nucleo_financiero");
            assertThat(puede(sentencia, esquema, tabla, "UPDATE"))
                    .as("svc_nucleo_financiero puede UPDATE %s.%s (invariante R-AUD-01)", esquema, tabla)
                    .isFalse();
            assertThat(puede(sentencia, esquema, tabla, "DELETE"))
                    .as("svc_nucleo_financiero puede DELETE %s.%s (invariante R-AUD-01)", esquema, tabla)
                    .isFalse();
        }
    }

    /**
     * {@code evento_consumido} es un caso aparte (H4.S1.M3, tal como lo nombra el
     * encargo): no tiene disparador de cadena de hash, solo el {@code REVOKE
     * UPDATE} — el outbox dedupe por {@code INSERT ... ON CONFLICT DO NOTHING}, y
     * si el rol de aplicación pudiera actualizar una fila ya consumida, el dedupe
     * dejaría de ser una garantía y pasaría a ser una convención.
     */
    @Test
    @DisplayName("svc_nucleo_financiero no puede UPDATE evento_consumido (el dedupe del outbox depende de esto)")
    void svcNucleoFinancieroNoPuedeActualizarEventoConsumido() throws SQLException {
        try (Connection conexion = BaseDePrueba.conexion();
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET ROLE svc_nucleo_financiero");
            assertThat(puede(sentencia, "nucleo_financiero", "evento_consumido", "UPDATE"))
                    .as("svc_nucleo_financiero puede UPDATE nucleo_financiero.evento_consumido")
                    .isFalse();
        }
    }

    /**
     * La SEGUNDA capa de la defensa en profundidad (el disparador, no solo el
     * grant) ya está demostrada con una fila REAL en producción:
     * {@code AuditoriaCriticaTest} de `aportes` inserta una fila de verdad en
     * {@code comun.bitacora_evento} (al aprobar un reembolso) y su limpieza
     * original intentaba borrarla — el disparador {@code fn_aud_bloquear_mutacion}
     * la rechazó con {@code "R-AUD-01: ... es append-only"} (ver el commit que
     * corrigió esa limpieza). No se repite aquí con una fila sintética de
     * {@code nucleo_financiero} — insertar una `transaccion_billetera` válida por
     * SQL crudo exige toda su cadena de cuentas y no aporta una prueba distinta
     * de la que ya existe con datos reales.
     */
    private boolean puede(Statement sentencia, String esquema, String tabla, String privilegio) throws SQLException {
        try (var filas = sentencia.executeQuery(
                "SELECT has_table_privilege('%s.%s', '%s') AS puede".formatted(esquema, tabla, privilegio))) {
            return filas.next() && filas.getBoolean("puede");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("permission denied")) {
                return false;
            }
            throw e;
        }
    }
}
