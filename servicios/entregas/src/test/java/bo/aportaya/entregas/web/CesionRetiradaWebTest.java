package bo.aportaya.entregas.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * La cesion de turnos retirada (D-L2-1): la subasta y el mercado responden 410 con su codigo y sin efecto, y el hecho
 * que pide nucleo-financiero antes de pagarle a un vendedor responde el 422 del pacto (no hay oferta que pagar).
 *
 * <p>Que no haya efecto lo prueba la ausencia de dobles: el controlador ya no tiene casos de uso del mercado ni de la
 * subasta a los que llamar, y los de CU-22 que levanta el corte no se tocan.
 */
@PruebaWeb(EntregasController.class)
class CesionRetiradaWebTest {

    private static final String ID = "c9000000-0000-4000-8000-000000000001";
    private static final String CLAVE = "c9000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU22LiquidarEntrega cu22;

    @MockitoBean
    private CU22EntregarPozoCompleto pozo;

    private static MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder pedido, String cuerpo) {
        return pedido.header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    static Stream<Arguments> retiradas() {
        String oferta =
                """
                {"turnoId":"%s","precio":{"monto":"5500.00","moneda":"BOB"},
                 "cargos":{"monto":"50.00","moneda":"BOB"},"vigenteHasta":"2026-12-20T12:00:00Z"}
                """
                        .formatted(ID);
        String subasta =
                """
                {"grupoId":"%s","cupoId":"%s","montoBase":{"monto":"5000.00","moneda":"BOB"},"horas":24}
                """
                        .formatted(ID, ID);
        String puja =
                """
                {"monto":{"monto":"5100.00","moneda":"BOB"},"aceptoReglamento":true}
                """;
        return Stream.of(
                Arguments.of("publicarOferta", conCuerpo(post("/entregas/mercado/ofertas"), oferta), "AP-CU130-10"),
                Arguments.of("listarOfertas", get("/entregas/mercado/ofertas"), "AP-CU130-10"),
                Arguments.of("cancelarOferta", post("/entregas/mercado/ofertas/" + ID + "/cancelacion"), "AP-CU130-10"),
                Arguments.of(
                        "comprarOferta",
                        post("/entregas/mercado/ofertas/" + ID + "/compra").header("Idempotency-Key", CLAVE),
                        "AP-CU130-10"),
                Arguments.of("abrirSubastaDeCupo", conCuerpo(post("/entregas/subastas"), subasta), "AP-CU136-09"),
                Arguments.of("listarSubastasDeCupo", get("/entregas/subastas"), "AP-CU136-09"),
                Arguments.of("consultarSubastaDeCupo", get("/entregas/subastas/" + ID), "AP-CU136-09"),
                Arguments.of(
                        "pujarEnSubasta", conCuerpo(post("/entregas/subastas/" + ID + "/pujas"), puja), "AP-CU136-09"),
                Arguments.of(
                        "cancelarSubastaDeCupo", post("/entregas/subastas/" + ID + "/cancelacion"), "AP-CU136-09"));
    }

    @ParameterizedTest(name = "{0} · 410 con {2}")
    @MethodSource("retiradas")
    @DisplayName("la operacion retirada responde 410 con su codigo estable y la traza, sin efecto")
    void retirada(String operacion, MockHttpServletRequestBuilder pedido, String codigo) throws Exception {
        mvc.perform(pedido.with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.trazaId").exists());
    }

    @ParameterizedTest(name = "{0} · 401 sin sesion")
    @MethodSource("retiradas")
    @DisplayName("sin sesion la operacion retirada sigue respondiendo 401: no le cuenta a un anonimo que existio")
    void sinSesion(String operacion, MockHttpServletRequestBuilder pedido, String codigo) throws Exception {
        mvc.perform(pedido).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("el hecho para pagar al vendedor responde el 422 AP-CU130-05 del pacto: nucleo no paga")
    void ofertaParaPagoSinOferta() throws Exception {
        mvc.perform(get("/entregas/mercado/ofertas/" + ID).with(Sesiones.como("SERVICIO_ENTREGAS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU130-05"));
    }

    @Test
    @DisplayName("un participante que pide el hecho para pagar recibe 403: sigue siendo solo de servicio")
    void ofertaParaPagoSoloServicio() throws Exception {
        mvc.perform(get("/entregas/mercado/ofertas/" + ID).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
    }
}
