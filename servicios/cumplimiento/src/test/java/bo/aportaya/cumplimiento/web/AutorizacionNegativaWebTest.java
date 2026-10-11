package bo.aportaya.cumplimiento.web;

import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU46VerificarAlcance;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
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

/**
 * H13.S1.M2 · Lo que cumplimiento rechaza por ROL: cada operacion de funcion (analista, riesgos,
 * seguridad) responde 401 sin token y 403 a quien tiene otro rol —un participante, el personal de
 * otra funcion, o el token de otro servicio—, y el caso de uso ni se entera.
 *
 * <p>La separacion de funciones es lo que se prueba: quien registra un riesgo operativo no maneja
 * un incidente de seguridad, y quien analiza no eleva la diligencia de su propio caso.
 */
@PruebaWeb
class AutorizacionNegativaWebTest {

    private static final UUID ID = UUID.fromString("c1000000-0000-4000-8000-000000000009");
    private static final String CLAVE = "c1000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU02ElevarDiligencia cu02;

    @MockitoBean
    private CU03DeclararPep cu03;

    @MockitoBean
    private CU05AceptarContrato cu05;

    @MockitoBean
    private CU05ConsultarContratosVigentes contratosVigentes;

    @MockitoBean
    private CU46VerificarAlcance cu46;

    @MockitoBean
    private CU54RegistrarRiesgoOperativo cu54;

    @MockitoBean
    private CU55GestionarIncidente cu55;

    @MockitoBean
    private ConsultarEfectoDelReclamo efecto;

    /** Ruta, y los roles que NO deben poder usarla (ninguno tiene el permiso de la ruta). */
    static Stream<Arguments> rutasDeFuncion() {
        return Stream.of(
                Arguments.of(
                        "/cumplimiento/usuarios/" + ID + "/diligencia",
                        new String[] {"PARTICIPANTE", "SERVICIO_ENTREGAS", "RESPONSABLE_SEGURIDAD", "SOPORTE"}),
                Arguments.of(
                        "/cumplimiento/riesgos/eventos",
                        new String[] {"PARTICIPANTE", "SERVICIO_INVERSIONES", "ANALISTA_CUMPLIMIENTO", "SOPORTE"}),
                Arguments.of("/cumplimiento/seguridad/incidentes", new String[] {
                    "PARTICIPANTE", "SERVICIO_ENTREGAS", "RESPONSABLE_RIESGOS", "ANALISTA_CUMPLIMIENTO"
                }),
                Arguments.of(
                        "/cumplimiento/seguridad/incidentes/" + ID + "/reportes",
                        new String[] {"PARTICIPANTE", "SERVICIO_ENTREGAS", "RESPONSABLE_RIESGOS"}),
                Arguments.of(
                        "/cumplimiento/seguridad/incidentes/" + ID + "/notificaciones",
                        new String[] {"PARTICIPANTE", "SERVICIO_INVERSIONES", "RESPONSABLE_RIESGOS"}));
    }

    private org.springframework.test.web.servlet.ResultActions llamar(
            String ruta, org.springframework.test.web.servlet.request.RequestPostProcessor sesion) throws Exception {
        var peticion = post(ruta)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        return mvc.perform(sesion == null ? peticion : peticion.with(sesion));
    }

    @ParameterizedTest(name = "rechaza: {0} sin token es 401")
    @MethodSource("rutasDeFuncion")
    void sinToken(String ruta, String[] ignorado) throws Exception {
        llamar(ruta, null).andExpect(status().isUnauthorized());
        sinInteracciones();
    }

    @ParameterizedTest(name = "rechaza: {0} con otro rol es 403 y el caso de uso no se entera")
    @MethodSource("rutasDeFuncion")
    void otroRol(String ruta, String[] roles) throws Exception {
        for (String rol : roles) {
            llamar(ruta, Sesiones.como(rol))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        }
        sinInteracciones();
    }

    @Test
    @DisplayName("rechaza: declarar el PEP y aceptar un contrato sin token es 401, con el token de un servicio es 403")
    void adhesionSinSesionONoEsUnaPersona() throws Exception {
        for (String ruta : new String[] {
            "/cumplimiento/usuarios/" + Sesiones.USUARIO + "/pep", "/cumplimiento/contratos/" + ID + "/aceptaciones"
        }) {
            llamar(ruta, null).andExpect(status().isUnauthorized());
            llamar(ruta, Sesiones.como("SERVICIO_ENTREGAS", "ENTREGA_EJECUTAR")).andExpect(status().isForbidden());
            llamar(ruta, Sesiones.como("ANALISTA_CUMPLIMIENTO", "CUMPLIMIENTO_ALERTAS"))
                    .andExpect(status().isForbidden());
        }
        sinInteracciones();
    }

    private void sinInteracciones() {
        for (Object doble : new Object[] {cu02, cu03, cu05, contratosVigentes, cu46, cu54, cu55, efecto}) {
            if (mockingDetails(doble).isMock()) {
                verifyNoInteractions(doble);
            }
        }
    }
}
