package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos.Resultado;
import bo.aportaya.nucleofinanciero.infraestructura.AsientoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaContableRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las cuentas de sistema que siembra {@code seeders/minimos/22-cuentas-de-sistema.json} (entorno TEST y produccion):
 * custodia de inversion, ingresos de la plataforma, puente de custodia y el mapeo SINTETICO de las cuentas logicas de
 * garantia. Se aplica el SQL generado de verdad sobre PostgreSQL, dentro de una transaccion que se revierte.
 */
class CU24SemillasTest extends BaseDeCU24 {

    private static final Pattern MAPEO_DEL_COMPOSE =
            Pattern.compile("\"CUENTA_LOGICA_([A-Z_]+)_(BOB|USD)\": \"(?:\\$\\{[A-Z_]+:-)?([0-9.]+)");

    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("sql/60_semillas"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("No se encontro la raiz del repositorio (sql/60_semillas)");
        }
        return p;
    }

    private static String sql(String archivo) throws IOException {
        return Files.readString(raiz().resolve("sql/60_semillas").resolve(archivo), StandardCharsets.UTF_8);
    }

    /** Aplica el plan de cuentas minimo y las cuentas de sistema, como lo hace {@code sembrar.sql} en el despliegue. */
    private void sembrar() {
        try {
            String plan = sql("01-plan-de-cuentas.sql");
            String sistema = sql("22-cuentas-de-sistema.sql");
            dsl.connection(c -> {
                try (var st = c.createStatement()) {
                    st.execute(plan);
                    st.execute(sistema);
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long contar(String tabla) {
        String consulta =
                switch (tabla) {
                    case "cuenta_contable" -> "SELECT count(*) FROM nucleo_financiero.cuenta_contable";
                    case "cuenta_billetera" -> "SELECT count(*) FROM nucleo_financiero.cuenta_billetera";
                    default -> throw new IllegalArgumentException("tabla no prevista: " + tabla);
                };
        return ((Number) dsl.fetchOne(consulta).get(0)).longValue();
    }

    @Test
    @DisplayName(
            "reintento: sembrar dos veces las cuentas de sistema no inserta nada la segunda (ids estables, ON CONFLICT)")
    void sembrarDosVecesNoDuplica() {
        transaccion.executeWithoutResult(t -> {
            sembrar();
            long contables = contar("cuenta_contable");
            long billeteras = contar("cuenta_billetera");
            sembrar();
            assertThat(contar("cuenta_contable"))
                    .as("cuentas contables tras la 2da corrida")
                    .isEqualTo(contables);
            assertThat(contar("cuenta_billetera"))
                    .as("cuentas de billetera tras la 2da corrida")
                    .isEqualTo(billeteras);
            System.out.println("SEMILLA | 22-cuentas-de-sistema 2 corridas | cuenta_contable=" + contables
                    + " cuenta_billetera=" + billeteras + " (iguales tras la 2da corrida)");
            t.setRollbackOnly();
        });
    }

    @Test
    @DisplayName(
            "cuadre: tras sembrar hay exactamente una custodia de inversion activa por moneda y una cuenta de ingresos sembrada, y la semilla no siembra saldos")
    void cuentasDeSistemaPorMoneda() throws IOException {
        // Los saldos de las cuentas los mueve el libro (otras clases de esta base compartida ya las usaron): lo que
        // esta semilla garantiza es que NO escribe saldo_disponible ni saldo_retenido, y eso se mira en el SQL.
        String sistema = sql("22-cuentas-de-sistema.sql");
        String insertDeBilletera = sistema.substring(sistema.indexOf("INSERT INTO cuenta_billetera"));
        String columnas = insertDeBilletera.substring(0, insertDeBilletera.indexOf(") VALUES"));
        assertThat(columnas).doesNotContain("saldo_disponible", "saldo_retenido");

        transaccion.executeWithoutResult(t -> {
            sembrar();
            for (String moneda : List.of("BOB", "USD")) {
                long custodias = ((Number) dsl.fetchOne(
                                        """
                                        SELECT count(*) FROM nucleo_financiero.cuenta_billetera
                                         WHERE tipo = 'CUSTODIA_INVERSION' AND moneda = ? AND estado <> 'CERRADA'
                                        """,
                                        moneda)
                                .get(0))
                        .longValue();
                assertThat(custodias)
                        .as("custodia de inversion activa en " + moneda)
                        .isEqualTo(1L);
            }
            long ingresos = ((Number) dsl.fetchOne(
                                    """
                                    SELECT count(*) FROM nucleo_financiero.cuenta_billetera
                                     WHERE numero_cuenta IN ('SYS-INGRESOS', 'SYS-INGRESOS-USD') AND estado = 'ACTIVA'
                                    """)
                            .get(0))
                    .longValue();
            assertThat(ingresos).isEqualTo(2L);
            t.setRollbackOnly();
        });
    }

    @Test
    @DisplayName(
            "cuadre: el mapeo sintetico que declara el compose existe en el plan, es de su moneda y asienta un evento de garantia")
    void mapeoSinteticoAsientaUnEvento() throws IOException {
        Map<String, String> mapeo = mapeoDelCompose();
        assertThat(mapeo).as("seis cuentas logicas: 3 bolsillos x 2 monedas").hasSize(6);
        transaccion.executeWithoutResult(t -> {
            sembrar();
            for (var e : mapeo.entrySet()) {
                String moneda = e.getKey().substring(e.getKey().indexOf('.') + 1);
                var cuenta = dsl.fetchOne(
                        "SELECT moneda, es_cuenta_de_movimiento FROM nucleo_financiero.cuenta_contable WHERE codigo = ?",
                        e.getValue());
                assertThat(cuenta).as("cuenta " + e.getValue()).isNotNull();
                assertThat(cuenta.get(0, String.class)).isEqualTo(moneda);
                assertThat(cuenta.get(1, Boolean.class)).isTrue();
            }
            var consumo = new ConsumoDeAsientos(
                    new Datos(dsl),
                    registrar,
                    new AsientoRepositorio(),
                    new CuentaContableRepositorio(),
                    (logica, moneda) -> Optional.ofNullable(mapeo.get(logica + "." + moneda.name())));
            for (String moneda : List.of("BOB", "USD")) {
                var json = new ObjectMapper().createObjectNode();
                var asiento = json.putObject("asiento")
                        .put("origenTipo", "COBERTURA")
                        .put("origenId", UUID.randomUUID().toString());
                var partidas = asiento.putArray("partidas");
                partidas.addObject()
                        .put("cuenta", "POZO_GRUPO")
                        .put("debe", "250.00")
                        .put("haber", "0.00")
                        .put("moneda", moneda);
                partidas.addObject()
                        .put("cuenta", "RESERVA_RESPALDO")
                        .put("debe", "0.00")
                        .put("haber", "250.00")
                        .put("moneda", moneda);
                var salida = consumo.procesar(new ConsumoDeAsientos.EventoRecibido(
                        UUID.randomUUID(), "garantia.cobertura_empresarial_aplicada", UUID.randomUUID(), json));
                assertThat(salida.resultado()).isEqualTo(Resultado.REGISTRADO);
                var sumas = dsl.fetchOne(
                        "SELECT SUM(debe), SUM(haber), min(moneda) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                        salida.asientoId().orElseThrow());
                assertThat(sumas.get(0).toString()).isEqualTo(sumas.get(1).toString());
                assertThat(sumas.get(2)).isEqualTo(moneda);
                System.out.println("ASIENTO | mapeo sintetico " + moneda + " | debe=" + sumas.get(0) + " haber="
                        + sumas.get(1) + " moneda=" + sumas.get(2));
            }
            t.setRollbackOnly();
        });
    }

    /** Lo que el compose desplegado le da a nucleo-financiero: {@code LOGICA.MONEDA -> codigo del plan}. */
    private static Map<String, String> mapeoDelCompose() throws IOException {
        String compose = Files.readString(raiz().resolve("scripts/generar_compose.py"), StandardCharsets.UTF_8);
        String desplegado = compose.substring(compose.indexOf("DE_ENTORNO = {"));
        Map<String, String> mapeo = new HashMap<>();
        var m = MAPEO_DEL_COMPOSE.matcher(desplegado);
        while (m.find()) {
            mapeo.put(m.group(1) + "." + m.group(2), m.group(3));
        }
        return mapeo;
    }
}
