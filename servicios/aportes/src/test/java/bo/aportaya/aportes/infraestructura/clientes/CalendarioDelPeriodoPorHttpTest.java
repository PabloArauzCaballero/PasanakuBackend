package bo.aportaya.aportes.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * H8.S1.M1 · el LADO CONSUMIDOR del contrato {@code GET /grupos/periodos/{id}/calendario-de-aportes} contra un
 * servidor HTTP real que habla el cuerpo del productor, en tres niveles (correcto, limite, invalido).
 *
 * <p>El cuerpo "bueno" es el archivo dorado {@code servicios/grupos/src/test/resources/fixtures/calendario-de-aportes.json}:
 * el MISMO que `grupos` compara contra su respuesta real en {@code CalendarioDeAportesHttpRealTest}. Si el productor
 * cambia la forma, su prueba rompe; si cambia el dorado, esta rompe. El servidor es un doble declarado
 * ({@link DependidoDeEnsayo}); el productor real queda cubierto por la prueba de `grupos`.
 */
class CalendarioDelPeriodoPorHttpTest {

    private static final UUID PERIODO = UUID.fromString("c8100000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("c8100000-0000-4000-8000-000000000002");
    private static final UUID CUPO_1 = UUID.fromString("c8100000-0000-4000-8000-0000000000a1");
    private static final UUID TITULAR_1 = UUID.fromString("c8100000-0000-4000-8000-0000000000b1");

    /** El cuerpo real del productor (archivo dorado compartido), leido tal cual. */
    static String dorado() {
        Path p = Path.of("..", "grupos", "src", "test", "resources", "fixtures", "calendario-de-aportes.json");
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("falta el archivo dorado del productor: " + p.toAbsolutePath(), e);
        }
    }

    static CalendarioDelPeriodoPorHttp contra(DependidoDeEnsayo grupos, int ventana, long esperaMs) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return new CalendarioDelPeriodoPorHttp(
                RestClient.builder().requestFactory(fabrica),
                grupos.urlBase(),
                new ConfiguracionDeCircuito(ventana, 50, java.time.Duration.ofMillis(esperaMs), 3));
    }

    private static CalendarioDelPeriodoPorHttp contra(DependidoDeEnsayo grupos) {
        return contra(grupos, 10, 30_000);
    }

    // ---------------------------------------------------------------- correcto

    @Test
    @DisplayName("CORRECTO · el cuerpo del productor se arma como periodo con titulares, importes exactos y fechas")
    void correcto() {
        try (var grupos = new DependidoDeEnsayo().responder(200, dorado())) {
            var periodo = contra(grupos).deLosCupos(PERIODO).orElseThrow();

            assertThat(periodo.periodoId()).isEqualTo(PERIODO);
            assertThat(periodo.grupoId()).isEqualTo(GRUPO);
            assertThat(periodo.numero()).isEqualTo(3);
            assertThat(periodo.estado()).isEqualTo("ABIERTO");
            assertThat(periodo.fechaLimitePago()).isEqualTo(LocalDate.of(2026, 6, 5));
            assertThat(periodo.fechaFinGracia()).isEqualTo(LocalDate.of(2026, 6, 8));
            assertThat(periodo.cupos()).hasSize(2);
            assertThat(periodo.cupos().get(0).cupoId()).isEqualTo(CUPO_1);
            assertThat(periodo.cupos().get(0).participanteId()).isEqualTo(TITULAR_1);
            assertThat(periodo.cupos().get(0).montoEsperado()).isEqualTo(Dinero.de("500.00", Moneda.BOB));
            assertThat(periodo.cupos().get(1).montoEsperado()).isEqualTo(Dinero.de("250.00", Moneda.BOB));
            assertThat(grupos.peticionesRecibidas()).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------- limite

    @Test
    @DisplayName(
            "LIMITE · un periodo sin cupos ocupados llega vacio de cupos (el caso de uso lo rechaza), no se inventa nadie")
    void periodoSinCupos() {
        String sinCupos = dorado().replaceAll("(?s)\"cupos\":\\s*\\[.*\\]", "\"cupos\": []");
        try (var grupos = new DependidoDeEnsayo().responder(200, sinCupos)) {
            var periodo = contra(grupos).deLosCupos(PERIODO).orElseThrow();

            assertThat(periodo.cupos()).isEmpty();
        }
    }

    @Test
    @DisplayName("LIMITE · campos que el contrato agrega despues (fechaEntregaPrevista) no rompen al consumidor")
    void ignoraCamposDeMas() {
        try (var grupos = new DependidoDeEnsayo().responder(200, dorado().replace("{", "{\"campoNuevo\":1,"))) {
            assertThat(contra(grupos).deLosCupos(PERIODO)).isPresent();
        }
    }

    // ---------------------------------------------------------------- invalido

    @Test
    @DisplayName(
            "INVALIDO · 422 (periodo desconocido), 404, 403 y 401 son vacio: quien pregunta rechaza y no se asume nada")
    void respuestasDeNegocioSonVacio() {
        for (int estado : new int[] {422, 404, 403, 401}) {
            try (var grupos = new DependidoDeEnsayo().responder(estado, "{\"codigo\":\"AP-CU21-08\"}")) {
                var adaptador = contra(grupos);
                assertThat(adaptador.deLosCupos(PERIODO))
                        .as("estado %s", estado)
                        .isEmpty();
                assertThat(adaptador.estadoDelCircuito())
                        .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
            }
        }
    }

    @Test
    @DisplayName("INVALIDO · 500, JSON roto y cuerpo vacio son vacio (falla cerrado)")
    void fallasDelDependidoSonVacio() {
        for (var par : new Object[][] {{500, "{}"}, {503, "{}"}, {200, "esto no es json"}, {200, ""}}) {
            try (var grupos = new DependidoDeEnsayo().responder((Integer) par[0], (String) par[1])) {
                assertThat(contra(grupos).deLosCupos(PERIODO))
                        .as("%s %s", par[0], par[1])
                        .isEmpty();
            }
        }
    }

    @Test
    @DisplayName(
            "INVALIDO · un cuerpo al que le falta un campo del contrato, o con un importe o moneda malos, es vacio")
    void cuerpoQueNoCumpleElContratoEsVacio() {
        String[] malos = {
            dorado().replace("\"fechaFinGracia\": \"2026-06-08\",", ""),
            dorado().replace("\"numero\": 3,", ""),
            dorado().replace("\"periodoId\": \"c8100000-0000-4000-8000-000000000001\",", ""),
            dorado().replaceFirst("\"participanteId\": \"[^\"]*\",", ""),
            dorado().replace("\"500.00\"", "\"5oo\""),
            dorado().replaceFirst("\"BOB\"", "\"XXX\""),
            dorado().replace("\"fechaLimitePago\": \"2026-06-05\"", "\"fechaLimitePago\": \"no-es-fecha\""),
            "null",
            "[]",
        };
        for (String cuerpo : malos) {
            try (var grupos = new DependidoDeEnsayo().responder(200, cuerpo)) {
                assertThat(contra(grupos).deLosCupos(PERIODO)).as(cuerpo).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("INVALIDO · grupos lento: el tiempo maximo corta la espera y el resultado es vacio")
    void grupoLento() {
        try (var grupos = new DependidoDeEnsayo().tardar(1500).responder(200, dorado())) {
            long antes = System.nanoTime();

            assertThat(contra(grupos).deLosCupos(PERIODO)).isEmpty();

            assertThat((System.nanoTime() - antes) / 1_000_000).isLessThan(1200);
        }
    }

    @Test
    @DisplayName("INVALIDO · grupos apagado: nadie escucha y el resultado es vacio")
    void grupoApagado() {
        try (var grupos = new DependidoDeEnsayo().responder(200, dorado())) {
            var adaptador = contra(grupos);
            grupos.caer();

            assertThat(adaptador.deLosCupos(PERIODO)).isEmpty();
        }
    }
}
