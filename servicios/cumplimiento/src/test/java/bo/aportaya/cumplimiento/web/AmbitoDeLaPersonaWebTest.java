package bo.aportaya.cumplimiento.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H13.S1.M2 · Un participante no actua sobre otra persona (declarar PEP, aceptar un
 * contrato). El permiso {@code PARTICIPANTE} lo tiene cualquiera: lo que separa a una
 * persona de otra es que el titular de la ruta o del cuerpo sea el de la sesion.
 *
 * <p>La negativa tiene que ser la misma que la de «sin permiso»: no cuenta si el otro
 * usuario existe, ni que dato pedia, ni a quien le habria correspondido.
 */
@PruebaWeb(CumplimientoController.class)
class AmbitoDeLaPersonaWebTest {

    private static final UUID OTRA_PERSONA = Sesiones.OTRO_USUARIO;
    private static final UUID CONTRATO = UUID.fromString("c1000000-0000-4000-8000-000000000002");
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
    private CU54RegistrarRiesgoOperativo cu54;

    @MockitoBean
    private CU55GestionarIncidente cu55;

    @Test
    @DisplayName("rechaza: un participante no declara el PEP de otra persona, y el caso de uso ni se entera")
    void rechazaDeclararElPepDeOtro() throws Exception {
        mvc.perform(post("/cumplimiento/usuarios/{id}/pep", OTRA_PERSONA)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPep\":false,\"beneficiariosFinales\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(OTRA_PERSONA.toString()))));

        verifyNoInteractions(cu03);
    }

    @Test
    @DisplayName("rechaza: un participante no acepta un contrato en nombre de otra persona")
    void rechazaAceptarEnNombreDeOtro() throws Exception {
        mvc.perform(post("/cumplimiento/contratos/{id}/aceptaciones", CONTRATO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"" + OTRA_PERSONA + "\",\"version\":3,\"consentimientos\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));

        verifyNoInteractions(cu05);
    }

    @Test
    @DisplayName("rechaza: la negativa por ambito es indistinguible de la negativa por falta de permiso")
    void laNegativaNoDistingue() throws Exception {
        String porAmbito = mvc.perform(post("/cumplimiento/usuarios/{id}/pep", OTRA_PERSONA)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPep\":false,\"beneficiariosFinales\":[]}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String porPermiso = mvc.perform(post("/cumplimiento/usuarios/{id}/pep", OTRA_PERSONA)
                        .with(Sesiones.sinPermisos())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPep\":false,\"beneficiariosFinales\":[]}"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Mismo codigo, mismo mensaje: solo cambia la traza de la peticion.
        org.assertj.core.api.Assertions.assertThat(sinTraza(porAmbito)).isEqualTo(sinTraza(porPermiso));
    }

    @Test
    @DisplayName("el titular sigue pudiendo declarar lo suyo")
    void elTitularDeclaraLoSuyo() throws Exception {
        Mockito.when(cu03.ejecutar(any(), any()))
                .thenReturn(new CU03DeclararPep.SalidaDeclaracion(UUID.randomUUID(), false, "BAJO"));

        mvc.perform(post("/cumplimiento/usuarios/{id}/pep", Sesiones.USUARIO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPep\":false,\"beneficiariosFinales\":[]}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName(
            "rechaza: un campo que el contrato no declara devuelve 400 (additionalProperties: false) y nada se ejecuta")
    void rechazaElCampoDeMas() throws Exception {
        mvc.perform(post("/cumplimiento/contratos/{id}/aceptaciones", CONTRATO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"" + Sesiones.USUARIO
                                + "\",\"version\":3,\"consentimientos\":[],\"nivelDeRiesgo\":\"BAJO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));

        verifyNoInteractions(cu05);
    }

    @Test
    @DisplayName("rechaza: EntradaPep ya no absorbe un campo de mas (sin @JsonAnySetter, hallazgo 7 cerrado)")
    void entradaPepRechazaElCampoDeMas() throws Exception {
        mvc.perform(post("/cumplimiento/usuarios/{id}/pep", Sesiones.USUARIO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPep\":false,\"beneficiariosFinales\":[],\"nivelDeRiesgo\":\"BAJO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));

        verifyNoInteractions(cu03);
    }

    @Test
    @DisplayName("rechaza: un campo de mas dentro de un beneficiario final tampoco se ignora")
    void beneficiarioConCampoDeMas() throws Exception {
        mvc.perform(post("/cumplimiento/usuarios/{id}/pep", Sesiones.USUARIO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPep\":false,\"beneficiariosFinales\":[{\"nombre\":\"A\",\"documento\":\"1\","
                                + "\"ingresos\":\"9999.00\"}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cu03);
    }

    private static String sinTraza(String cuerpo) {
        return cuerpo.replaceAll("\"trazaId\"\\s*:\\s*\"[^\"]*\"", "\"trazaId\":\"-\"");
    }
}
