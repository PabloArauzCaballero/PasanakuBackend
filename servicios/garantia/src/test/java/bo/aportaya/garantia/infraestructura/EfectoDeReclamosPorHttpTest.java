package bo.aportaya.garantia.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.garantia.dominio.puertos.EfectoDeReclamos.Decision;
import bo.aportaya.garantia.dominio.puertos.EfectoDeReclamos.Que;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Caso;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * H13.S1.M6 · el adaptador REAL {@link EfectoDeReclamosPorHttp} contra el doble del contrato de cumplimiento
 * ({@code EfectoDeReclamosDeEnsayo}), en los tres niveles de siempre: correcto, limite e invalido. Lo que se afirma
 * es que el adaptador lee la respuesta como la lectura de referencia del contrato
 * ({@code evidencia/carril-A3/contrato-efecto-reclamo.md}): lo que no es un 200 entendible FRENA.
 *
 * <p>Es el lado CONSUMIDOR contra un doble declarado. El servicio real de cumplimiento respondiendole a este mismo
 * adaptador (token real, base real) lo ejerce {@code despliegue/ensayos/integracion}.
 */
class EfectoDeReclamosPorHttpTest {

    private static final UUID PERSONA = UUID.fromString("f7000000-0000-4000-8000-000000000001");

    private static EfectoDeReclamosPorHttp contra(EfectoDeReclamosDeEnsayo cumplimiento, Duration espera) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout((int) espera.toMillis());
        return new EfectoDeReclamosPorHttp(RestClient.builder().requestFactory(fabrica), cumplimiento.urlBase());
    }

    private static EfectoDeReclamosPorHttp contra(EfectoDeReclamosDeEnsayo cumplimiento) {
        return contra(cumplimiento, Duration.ofMillis(400));
    }

    // ------------------------------------------------------------------ correcto

    @Test
    @DisplayName("CORRECTO · sin reclamos la deuda procede y la sancion tambien")
    void sinReclamos() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().sinReclamos(PERSONA)) {
            var efecto = contra(cumplimiento);

            assertThat(efecto.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.PROCEDE);
            assertThat(efecto.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.PROCEDE);
        }
    }

    @Test
    @DisplayName("CORRECTO · un reclamo en revision frena la deuda y la sancion: no es una deuda ejecutable")
    void reclamoEnRevision() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().conCasos(PERSONA, Caso.queFrenaTodo("EN_REVISION"))) {
            var efecto = contra(cumplimiento);

            assertThat(efecto.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
            assertThat(efecto.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.FRENA);
        }
    }

    // -------------------------------------------------------------------- limite

    @Test
    @DisplayName("LIMITE · un solo caso que frena entre diez libres frena; diez libres proceden")
    void unoEntreDiez() {
        Caso[] libres = new Caso[10];
        java.util.Arrays.fill(libres, Caso.libre());
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().conCasos(PERSONA, libres)) {
            assertThat(contra(cumplimiento).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                    .isEqualTo(Decision.PROCEDE);
        }
        Caso[] unoQueFrena = new Caso[10];
        java.util.Arrays.fill(unoQueFrena, Caso.libre());
        unoQueFrena[7] = Caso.queFrenaTodo("INSTANCIA_SIN_RESOLVER");
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().conCasos(PERSONA, unoQueFrena)) {
            assertThat(contra(cumplimiento).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                    .isEqualTo(Decision.FRENA);
        }
    }

    @Test
    @DisplayName("LIMITE · cada indicador es suyo: un caso que solo frena la sancion NO frena la deuda")
    void cadaIndicadorEsSuyo() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo()
                .conCasos(PERSONA, Caso.soloFrenaLaSancion("PLAZO_DE_ELEVACION_ABIERTO"))) {
            var efecto = contra(cumplimiento);

            assertThat(efecto.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.FRENA);
            assertThat(efecto.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.PROCEDE);
        }
    }

    @Test
    @DisplayName("LIMITE · un agregado en false con un caso que frena FRENA: manda el caso")
    void elCasoMandaSobreElAgregado() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo()
                .respondeCuerpo("{\"usuarioId\":\"" + PERSONA + "\",\"bloqueaSancionFirme\":false,"
                        + "\"bloqueaDeudaEjecutable\":false,\"casos\":[{\"bloqueaSancionFirme\":false,"
                        + "\"bloqueaDeudaEjecutable\":true,\"motivo\":\"EN_REVISION\"}]}")) {
            assertThat(contra(cumplimiento).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                    .isEqualTo(Decision.FRENA);
        }
    }

    // ------------------------------------------------------------------ invalido

    @Test
    @DisplayName("INVALIDO · 401, 403, 404, 500, 502 y 503 FRENAN: no saber nunca es no haber reclamo")
    void respuestasDeError() {
        for (int estado : List.of(401, 403, 404, 500, 502, 503)) {
            try (var cumplimiento = new EfectoDeReclamosDeEnsayo().respondeEstado(estado)) {
                assertThat(contra(cumplimiento).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                        .as("HTTP %d", estado)
                        .isEqualTo(Decision.FRENA);
            }
        }
    }

    @Test
    @DisplayName("INVALIDO · vacio, no-JSON, {}, [], sin casos, tipos equivocados y null FRENAN")
    void cuerposQueNoSonElContrato() {
        for (String cuerpo : List.of(
                "",
                "esto no es json",
                "{}",
                "[]",
                "{\"bloqueaDeudaEjecutable\":false}",
                "{\"bloqueaDeudaEjecutable\":\"false\",\"casos\":[]}",
                "{\"bloqueaDeudaEjecutable\":null,\"casos\":[]}",
                "{\"bloqueaDeudaEjecutable\":false,\"casos\":{}}",
                "{\"bloqueaDeudaEjecutable\":false,\"casos\":[{\"motivo\":\"EN_REVISION\"}]}")) {
            try (var cumplimiento = new EfectoDeReclamosDeEnsayo().respondeCuerpo(cuerpo)) {
                assertThat(contra(cumplimiento).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                        .as("cuerpo [%s]", cuerpo)
                        .isEqualTo(Decision.FRENA);
            }
        }
    }

    @Test
    @DisplayName("INVALIDO · la respuesta de OTRA persona FRENA aunque diga que no hay reclamos")
    void respuestaDeOtraPersona() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().sinReclamos(UUID.randomUUID())) {
            assertThat(contra(cumplimiento).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                    .isEqualTo(Decision.FRENA);
        }
    }

    @Test
    @DisplayName("INVALIDO · cumplimiento mas lento que el tiempo maximo y cumplimiento caido FRENAN")
    void lentoOCaido() {
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().sinReclamos(PERSONA).tardar(1500)) {
            assertThat(contra(cumplimiento, Duration.ofMillis(300)).consultar(PERSONA, Que.DEUDA_EJECUTABLE))
                    .isEqualTo(Decision.FRENA);
        }
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().sinReclamos(PERSONA)) {
            var efecto = contra(cumplimiento);
            cumplimiento.caer();
            assertThat(efecto.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
        }
    }

    @Test
    @DisplayName("INVALIDO · con la ventana llena de caidas el circuito se abre y deja de llamar, y FRENA igual")
    void circuitoAbiertoNoLlama() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().respondeEstado(503)) {
            var efecto = contra(cumplimiento);
            for (int i = 0; i < 10; i++) {
                assertThat(efecto.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
            }
            int llamadas = cumplimiento.peticionesRecibidas();
            assertThat(efecto.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);

            assertThat(efecto.estadoDelCircuito())
                    .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
            assertThat(cumplimiento.peticionesRecibidas())
                    .as("con el circuito abierto no sale ninguna peticion")
                    .isEqualTo(llamadas);
        }
    }
}
