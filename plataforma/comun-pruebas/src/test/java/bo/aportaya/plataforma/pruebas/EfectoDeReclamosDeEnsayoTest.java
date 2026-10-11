package bo.aportaya.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Caso;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Decision;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Que;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * H13.S1.M6 · El doble del efecto de reclamo, en los tres niveles del contrato (regla 65).
 *
 * <p>Lo que se prueba es la REGLA DEL CONSUMIDOR que garantia y organizador copian al adaptador
 * real: ante cualquier duda, FRENA. Todo esto corre contra el doble ({@code DependidoDeEnsayo}),
 * no contra {@code cumplimiento}: la integracion real queda pendiente de quien implemente el
 * consumidor, y asi se declara.
 */
class EfectoDeReclamosDeEnsayoTest {

    private static final UUID PERSONA = UUID.fromString("a3000000-0000-4000-8000-000000000001");

    @Nested
    @DisplayName("correcto: el contrato dice que si y que no, y se obedece")
    class Correcto {

        @Test
        @DisplayName("sin reclamos: la deuda procede y la sancion procede")
        void sinReclamos() {
            try (var doble = new EfectoDeReclamosDeEnsayo().sinReclamos(PERSONA)) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.PROCEDE);
                assertThat(doble.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.PROCEDE);
            }
        }

        @Test
        @DisplayName("un reclamo en revision frena las dos cosas")
        void enRevision() {
            try (var doble = new EfectoDeReclamosDeEnsayo().conCasos(PERSONA, Caso.queFrenaTodo("EN_REVISION"))) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
                assertThat(doble.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.FRENA);
            }
        }
    }

    @Nested
    @DisplayName("limite: los bordes del contrato")
    class Limite {

        @Test
        @DisplayName("un solo caso que frena entre diez libres frena (el OR de los casos)")
        void unoEntreDiez() {
            Caso[] casos = new Caso[10];
            for (int i = 0; i < casos.length; i++) {
                casos[i] = i == 7 ? Caso.queFrenaTodo("PLAZO_DE_ELEVACION_ABIERTO") : Caso.libre();
            }
            try (var doble = new EfectoDeReclamosDeEnsayo().conCasos(PERSONA, casos)) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
            }
        }

        @Test
        @DisplayName("diez casos terminados sin bloqueo: procede")
        void diezLibres() {
            Caso[] casos = new Caso[10];
            java.util.Arrays.fill(casos, Caso.libre());
            try (var doble = new EfectoDeReclamosDeEnsayo().conCasos(PERSONA, casos)) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.PROCEDE);
                assertThat(doble.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.PROCEDE);
            }
        }

        @Test
        @DisplayName("lo que frena la sancion no frena por eso la deuda: cada indicador es suyo")
        void indicadoresIndependientes() {
            try (var doble = new EfectoDeReclamosDeEnsayo()
                    .conCasos(PERSONA, Caso.soloFrenaLaSancion("INSTANCIA_SIN_RESOLVER"))) {
                assertThat(doble.consultar(PERSONA, Que.SANCION_FIRME)).isEqualTo(Decision.FRENA);
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.PROCEDE);
            }
        }

        @Test
        @DisplayName("un indicador agregado en false con un caso que frena no engana: manda el caso")
        void agregadoIncoherente() {
            String incoherente = "{\"usuarioId\":\"" + PERSONA + "\",\"bloqueaSancionFirme\":false,"
                    + "\"bloqueaDeudaEjecutable\":false,\"casos\":[{\"bloqueaSancionFirme\":true,"
                    + "\"bloqueaDeudaEjecutable\":true,\"motivo\":\"EN_REVISION\"}]}";
            assertThat(EfectoDeReclamosDeEnsayo.leer(200, incoherente, Que.DEUDA_EJECUTABLE))
                    .isEqualTo(Decision.FRENA);
        }
    }

    @Nested
    @DisplayName("invalido: lo que el contrato no promete, frena")
    class Invalido {

        @Test
        @DisplayName("el servicio caido (conexion rechazada) frena, y no se reintenta a ciegas")
        void caido() {
            try (var doble = new EfectoDeReclamosDeEnsayo().sinReclamos(PERSONA)) {
                doble.caer();
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
                assertThat(doble.peticionesRecibidas()).isZero();
            }
        }

        @ParameterizedTest(name = "un {0} frena")
        @ValueSource(ints = {401, 403, 404, 500, 502, 503})
        void errorDelServicio(int estado) {
            try (var doble = new EfectoDeReclamosDeEnsayo().respondeEstado(estado)) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
            }
        }

        @ParameterizedTest(name = "el cuerpo «{0}» frena")
        @ValueSource(
                strings = {
                    "",
                    "no es json",
                    "{}",
                    "[]",
                    "{\"casos\":[]}",
                    "{\"bloqueaDeudaEjecutable\":false}",
                    "{\"bloqueaDeudaEjecutable\":\"no\",\"casos\":[]}",
                    "{\"bloqueaDeudaEjecutable\":null,\"casos\":[]}"
                })
        void cuerpoQueNoEsElContrato(String cuerpo) {
            try (var doble = new EfectoDeReclamosDeEnsayo().respondeCuerpo(cuerpo)) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
            }
        }

        @Test
        @DisplayName("una respuesta que tarda mas que el tiempo maximo frena")
        void demasiadoLento() {
            try (var doble = new EfectoDeReclamosDeEnsayo().sinReclamos(PERSONA).tardar(1_800)) {
                assertThat(doble.consultar(PERSONA, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.FRENA);
            }
        }
    }
}
