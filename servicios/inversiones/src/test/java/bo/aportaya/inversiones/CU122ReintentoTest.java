package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.OperacionesProgramables;
import bo.aportaya.inversiones.trabajos.ReintentoDeInstrucciones;
import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M2 / M3 (cierre del hallazgo del carril D) · el reintento de las instrucciones pendientes al
 * libro, cableado: con espera exponencial, tope de intentos, plazo maximo del barrido, clave de
 * idempotencia reservada y visibilidad. PostgreSQL real; libro y aliado dobles.
 */
class CU122ReintentoTest extends BaseDeAnalisis {

    private static final String PROGRAMADO = OperacionesProgramables.PROGRAMADO;
    private static final java.time.format.DateTimeFormatter MINUTO =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /** Una orden cuya retencion no llego al libro: queda CREADA con su instruccion RETENER pendiente. */
    private UUID ordenConLibroCaido() {
        conSaldo("5000.00");
        libro.fallarEn("retener", true);
        var orden = ordenar(PRODUCTO_DPF, "1000.00");
        assertThat(orden.estado()).isEqualTo("CREADA");
        return orden.ordenId();
    }

    private String estadoDeLaInstruccion(UUID orden) {
        return dslFixtura
                .fetchOne(
                        "select estado from inversiones.instruccion_libro where origen_id = ? and tipo = 'RETENER'",
                        orden)
                .get(0, String.class);
    }

    private double medidor(String nombre) {
        return metricas.get(nombre).gauge().value();
    }

    @Test
    @DisplayName(
            "Dado el libro caido al aplicar una instruccion · Cuando el reloj reintenta antes de que pase la espera · Entonces no la toca, y despues de la espera la aplica una sola vez")
    void esperaAntesDeReintentar() {
        UUID orden = ordenConLibroCaido();
        libro.fallarEn("retener", false); // el libro volvio
        AHORA.set(AHORA.get().plusSeconds(10)); // menos que la espera

        var antes = operaciones.reintentarInstrucciones(100, false, clave(), PROGRAMADO, sistema());

        assertThat(antes).containsEntry("aplicadas", "0").containsEntry("esperando", "1");
        assertThat(estadoDeLaInstruccion(orden)).isEqualTo("PENDIENTE");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");

        AHORA.set(AHORA.get().plusSeconds(200)); // pasada la espera acumulada del primer reintento (90 s)
        var despues = operaciones.reintentarInstrucciones(100, false, clave(), PROGRAMADO, sistema());
        operaciones.reintentarInstrucciones(100, false, clave(), PROGRAMADO, sistema());

        assertThat(despues).containsEntry("aplicadas", "1");
        assertThat(estadoDeLaInstruccion(orden)).isEqualTo("APLICADA");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(libro.clavesAplicadas()).containsOnlyOnce("RETENER:" + orden);
    }

    @Test
    @DisplayName(
            "Dada una instruccion que supero el maximo de intentos · Cuando el reloj reintenta · Entonces no la toca y queda contada como atascada, y el reintento a mano la aplica")
    void atascadaYReintentoAMano() {
        UUID orden = ordenConLibroCaido();
        for (int i = 0; i < MAX_INTENTOS; i++) {
            AHORA.set(AHORA.get().plusSeconds(7200));
            operaciones.reintentarInstrucciones(100, false, clave(), PROGRAMADO, sistema());
        }
        libro.fallarEn("retener", false);
        AHORA.set(AHORA.get().plusSeconds(7200));

        var delReloj = operaciones.reintentarInstrucciones(100, false, clave(), PROGRAMADO, sistema());

        assertThat(delReloj).containsEntry("aplicadas", "0").containsEntry("atascadas", "1");
        assertThat(estadoDeLaInstruccion(orden))
                .as("no se descarta: sigue pendiente y visible")
                .isEqualTo("PENDIENTE");
        assertThat(medidor("inversiones.instrucciones.pendientes")).isEqualTo(1.0);
        assertThat(medidor("inversiones.instrucciones.atascadas")).isEqualTo(1.0);

        var aMano = operaciones.reintentarInstrucciones(100, true, clave(), "API", sistema());

        assertThat(aMano).containsEntry("aplicadas", "1").containsEntry("atascadas", "0");
        assertThat(estadoDeLaInstruccion(orden)).isEqualTo("APLICADA");
        assertThat(medidor("inversiones.instrucciones.atascadas")).isZero();
    }

    @Test
    @DisplayName(
            "Dado un barrido de reintentos que supera su plazo maximo · Cuando corre · Entonces se corta entero, queda PARCIAL y el resto sigue pendiente")
    void barridoQueSePasaDelPlazo() {
        UUID orden = ordenConLibroCaido();
        libro.fallarEn("retener", false);
        var sinTiempo = new OperacionesProgramables(
                datos,
                tx,
                corridas,
                instrucciones,
                cu120,
                cu123,
                cu123Devengo,
                aplicador,
                RELOJ,
                metricas,
                ESPERA_BASE,
                Duration.ofHours(1),
                MAX_INTENTOS,
                Duration.ZERO,
                Duration.ofMinutes(10),
                POR_VUELTA,
                LECTURA);
        String clave = clave();

        var r = sinTiempo.reintentarInstrucciones(100, true, clave, "API", sistema());

        assertThat(r).containsEntry("cortada", "true").containsEntry("aplicadas", "0");
        assertThat(estadoDeLaInstruccion(orden)).isEqualTo("PENDIENTE");
        assertThat(deLaCorrida(clave, "estado")).isEqualTo("PARCIAL");
        assertThat(deLaCorrida(clave, "origen")).isEqualTo("API");
    }

    @Test
    @DisplayName(
            "Dado el libro que vuelve · Cuando corre el trabajo de reintento del reloj · Entonces aplica lo pendiente y deja su corrida PROGRAMADO, y sin nada pendiente no abre ninguna")
    void elTrabajoDelRelojReintenta() {
        var trabajo = new ReintentoDeInstrucciones(operaciones, RELOJ);
        // Un minuto que ninguna otra prueba uso: la clave del reloj es la del minuto y la base se comparte.
        AHORA.set(LUNES.plusSeconds(
                60L * java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 200000)));
        String sinPendientes = "REINT:" + MINUTO.format(java.time.ZonedDateTime.ofInstant(AHORA.get(), Reloj.ZONA));

        trabajo.correr();
        assertThat(deLaCorrida(sinPendientes, "estado"))
                .as("nada pendiente: ninguna corrida")
                .isNull();

        UUID orden = ordenConLibroCaido();
        libro.fallarEn("retener", false);
        AHORA.set(AHORA.get().plusSeconds(60L * 60 * 5)); // cinco horas despues, otro minuto
        String clave = "REINT:" + MINUTO.format(java.time.ZonedDateTime.ofInstant(AHORA.get(), Reloj.ZONA));
        trabajo.correr();
        trabajo.correr(); // la misma ventana de un minuto: no repite

        assertThat(estadoDeLaInstruccion(orden)).isEqualTo("APLICADA");
        assertThat(contar("select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ?", clave))
                .isEqualTo(1);
        assertThat(deLaCorrida(clave, "origen")).isEqualTo("PROGRAMADO");
        assertThat(deLaCorrida(clave, "estado")).isEqualTo("OK");
        assertThat(deLaCorrida(clave, "resultado")).contains("\"aplicadas\":\"1\"");
    }

    @Test
    @DisplayName("reintento: la misma clave con el mismo contenido devuelve el resultado guardado y no vuelve a correr")
    void claveRepetida() {
        UUID orden = ordenConLibroCaido();
        libro.fallarEn("retener", false);
        String clave = clave();

        var primera = operaciones.reintentarInstrucciones(100, true, clave, "API", sistema());
        var segunda = operaciones.reintentarInstrucciones(100, true, clave, "API", sistema());

        assertThat(primera).containsEntry("aplicadas", "1");
        assertThat(segunda)
                .as("si hubiera corrido otra vez, ya no habria nada que aplicar")
                .isEqualTo(primera);
        assertThat(contar("select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ?", clave))
                .isEqualTo(1);
        assertThat(libro.clavesAplicadas()).containsOnlyOnce("RETENER:" + orden);
    }

    @Test
    @DisplayName(
            "rechaza: la misma clave con otro contenido es AP-CU122-03 y una corrida en curso con la misma clave es AP-CU122-04")
    void claveConOtroContenidoYCorridaEnCurso() {
        String clave = clave();
        operaciones.reintentarInstrucciones(100, true, clave, "API", sistema());

        assertThatThrownBy(() -> operaciones.reintentarInstrucciones(50, true, clave, "API", sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU122-03"));

        String enCurso = clave();
        dslFixtura.execute(
                "insert into inversiones.corrida_trabajo (id, operacion, clave_idempotencia, hash_solicitud, origen, estado, iniciada_en)"
                        + " values (gen_random_uuid(), 'REINTENTAR_INSTRUCCIONES', ?, ?, 'API', 'EN_CURSO', cast(? as timestamptz))",
                enCurso,
                hashDe("REINTENTAR_INSTRUCCIONES|100|true"),
                AHORA.get().toString());
        assertThatThrownBy(() -> operaciones.reintentarInstrucciones(100, true, enCurso, "API", sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU122-04"));

        AHORA.set(AHORA.get().plusSeconds(11 * 60)); // la corrida en curso ya es vieja: se la da por caida y se retoma
        var retomada = operaciones.reintentarInstrucciones(100, true, enCurso, "API", sistema());
        assertThat(retomada).containsKey("aplicadas");
        assertThat(contar(
                        "select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ? and estado = 'OK'",
                        enCurso))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: ocho reintentos simultaneos con la misma clave corren UNA vez y dejan una sola corrida")
    void concurrenciaConLaMismaClave() throws Exception {
        UUID orden = ordenConLibroCaido();
        libro.fallarEn("retener", false);
        String clave = clave();
        var pool = Executors.newFixedThreadPool(8);
        var salida = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                try {
                    return operaciones
                            .reintentarInstrucciones(100, true, clave, "API", sistema())
                            .get("aplicadas");
                } catch (bo.aportaya.plataforma.dominio.ErrorDeNegocio enCurso) {
                    return enCurso.codigo().valor();
                }
            }));
        }
        salida.countDown();
        List<String> resultados = new ArrayList<>();
        for (Future<String> f : futuros) {
            resultados.add(f.get());
        }
        pool.shutdown();

        assertThat(resultados).containsOnly("1", "AP-CU122-04");
        assertThat(contar("select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ?", clave))
                .isEqualTo(1);
        assertThat(libro.clavesAplicadas()).containsOnlyOnce("RETENER:" + orden);
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("cuadre: los medidores de pendientes y atascadas coinciden con lo que hay en la base")
    void medidoresCoincidenConLaBase() {
        ordenConLibroCaido();

        assertThat(medidor("inversiones.instrucciones.pendientes"))
                .isEqualTo(
                        contar("select count(*)::int from inversiones.instruccion_libro where estado = 'PENDIENTE'"));
        assertThat(medidor("inversiones.instrucciones.atascadas")).isZero();
    }

    private static String hashDe(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
