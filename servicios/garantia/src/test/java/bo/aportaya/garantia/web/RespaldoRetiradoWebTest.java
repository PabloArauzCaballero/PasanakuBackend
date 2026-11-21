package bo.aportaya.garantia.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.CU67DisolverGrupo;
import bo.aportaya.garantia.aplicacion.ReclamosEnCurso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * F2.L2.M02 · El respaldo empresarial se retiro (ADR-055 E7): la empresa no reserva ni cubre nada. Las seis rutas de
 * {@code /garantia/respaldo} responden 410 AP-CU23-15 con su permiso y sin efecto; sin sesion siguen respondiendo 401.
 *
 * <p>Que no haya efecto lo prueba la estructura: el controlador ya no tiene casos de uso del respaldo a los que llamar,
 * y los que si tiene (reemplazo y disolucion) no se tocan.
 */
@PruebaWeb(GarantiaController.class)
class RespaldoRetiradoWebTest {

    private static final String ID = "b5000000-0000-4000-8000-000000000001";
    private static final String CLAVE = "b5000000-0000-4000-8000-0000000000ff";
    private static final String DINERO = "{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU66ReemplazarParticipante cu66;

    @MockitoBean
    private CU67DisolverGrupo cu67;

    @MockitoBean
    private ReclamosEnCurso reclamosEnCurso;

    private static MockHttpServletRequestBuilder con(String ruta, String cuerpo) {
        var pedido = post(ruta).header("Idempotency-Key", CLAVE);
        return cuerpo == null
                ? pedido
                : pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    static Stream<Arguments> retiradas() {
        return Stream.of(
                Arguments.of(
                        "reservarRespaldo",
                        con(
                                "/garantia/respaldo/reservas",
                                "{\"grupoId\":\"%s\",\"ciclo\":1,\"monto\":%s,\"responsableId\":\"%s\"}"
                                        .formatted(ID, DINERO, ID)),
                        "ADMIN_PLATAFORMA"),
                Arguments.of(
                        "ampliarRespaldo",
                        con("/garantia/respaldo/reservas/" + ID + "/ampliacion", "{\"monto\":%s}".formatted(DINERO)),
                        "ADMIN_PLATAFORMA"),
                Arguments.of(
                        "liberarRespaldo",
                        con("/garantia/respaldo/reservas/" + ID + "/liberacion", null),
                        "ADMIN_PLATAFORMA"),
                Arguments.of(
                        "cubrirConRespaldo",
                        con(
                                "/garantia/respaldo/coberturas",
                                "{\"grupoId\":\"%s\",\"periodoId\":\"%s\",\"turnoId\":\"%s\"}".formatted(ID, ID, ID)),
                        "ENTREGA_EJECUTAR"),
                Arguments.of(
                        "reversarCoberturaRespaldo",
                        con("/garantia/respaldo/coberturas/" + ID + "/reversa", null),
                        "ADMIN_PLATAFORMA"),
                Arguments.of(
                        "recuperarRespaldo",
                        con(
                                "/garantia/respaldo/recuperaciones",
                                "{\"pagoId\":\"%s\",\"obligacionId\":\"%s\",\"pago\":%s}".formatted(ID, ID, DINERO)),
                        "ADMIN_PLATAFORMA"));
    }

    @ParameterizedTest(name = "{0} · 410 AP-CU23-15")
    @MethodSource("retiradas")
    void retirada(String operacion, MockHttpServletRequestBuilder pedido, String permiso) throws Exception {
        mvc.perform(pedido.with(Sesiones.como("ADMIN", permiso)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value("AP-CU23-15"))
                .andExpect(jsonPath("$.trazaId").exists());
        verifyNoInteractions(cu66, cu67, reclamosEnCurso);
    }

    @ParameterizedTest(name = "{0} · 401 sin sesion")
    @MethodSource("retiradas")
    void sinSesion(String operacion, MockHttpServletRequestBuilder pedido, String permiso) throws Exception {
        mvc.perform(pedido).andExpect(status().isUnauthorized());
    }
}
