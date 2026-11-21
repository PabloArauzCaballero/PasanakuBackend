package bo.aportaya.plataforma.web.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Cuatro ojos reales de punta a punta en la capa web: la segunda firma es otra persona
 * con su propio token, nunca un UUID del cuerpo (F1.S2.M1).
 *
 * <p>Matriz negativa: misma persona y misma sesion, misma persona en otra sesion, token
 * sin {@code sesion_id}, sin token, rol insuficiente y cuerpo con un aprobador inventado.
 */
@PruebaWeb(ControladorDeCuatroOjosDeEnsayo.class)
class CuatroOjosWebTest {

    private static final String PERMISO = "REVERSO_AUTORIZAR";
    private static final UUID SESION_A = UUID.fromString("5e510000-0000-4000-8000-00000000000a");
    private static final UUID SESION_A2 = UUID.fromString("5e510000-0000-4000-8000-0000000000a2");
    private static final UUID SESION_B = UUID.fromString("5e510000-0000-4000-8000-00000000000b");

    @Autowired
    private MockMvc mvc;

    private RequestPostProcessor ana(UUID sesion) {
        return Sesiones.enSesion(Sesiones.USUARIO, sesion, "OPERADOR", PERMISO);
    }

    private RequestPostProcessor beto() {
        return Sesiones.enSesion(Sesiones.OTRO_USUARIO, SESION_B, "OPERADOR", PERMISO);
    }

    private String proponer(RequestPostProcessor quien) throws Exception {
        String cuerpo = mvc.perform(post("/ensayo/propuestas")
                        .with(quien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"devolucion de un cobro duplicado\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions aprobar(
            String id, RequestPostProcessor quien, String cuerpo) throws Exception {
        var peticion = post("/ensayo/propuestas/{id}/aprobacion", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
        return mvc.perform(quien == null ? peticion : peticion.with(quien));
    }

    @Test
    @DisplayName("correcto: otra persona en otra sesion aprueba; los dos actores salen del token")
    void otraPersonaAprueba() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, beto(), "{\"comentario\":\"ok\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propusoUsuario").value(Sesiones.USUARIO.toString()))
                .andExpect(jsonPath("$.aproboUsuario").value(Sesiones.OTRO_USUARIO.toString()))
                .andExpect(jsonPath("$.aproboSesion").value(SESION_B.toString()));
    }

    @Test
    @DisplayName("invalido: quien propuso no se aprueba a si mismo con su misma sesion (422)")
    void mismaSesionRechazada() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, ana(SESION_A), "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU99-01"));
    }

    @Test
    @DisplayName("limite: la misma persona abriendo otra sesion tampoco son cuatro ojos (422)")
    void mismaPersonaOtraSesionRechazada() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, ana(SESION_A2), "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU99-01"));
    }

    @Test
    @DisplayName("invalido: el aprobador inventado en el cuerpo es un campo no declarado (400 AP-VAL-02)")
    void aprobadorEnElCuerpoRechazado() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, ana(SESION_A), "{\"aprobadoPor\":\"" + Sesiones.OTRO_USUARIO + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
    }

    @Test
    @DisplayName("invalido: un token sin sesion_id no puede firmar (401)")
    void tokenSinSesionNoFirma() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, Sesiones.comoOtro("OPERADOR", PERMISO), "{}").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("invalido: sin token, 401")
    void sinToken() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, null, "{}").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("invalido: otra persona sin el permiso, 403")
    void sinPermiso() throws Exception {
        String id = proponer(ana(SESION_A));
        aprobar(id, Sesiones.enSesion(Sesiones.OTRO_USUARIO, SESION_B, "PARTICIPANTE"), "{}")
                .andExpect(status().isForbidden());
    }
}
