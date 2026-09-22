package bo.aportaya.plataforma.web.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.plataforma.web.errores.ControladorDeEnsayo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code @Permiso} autoriza de verdad.
 *
 * <p><b>Esta prueba nace roja.</b> Antes de {@link GuardiaDePermiso}, la anotacion
 * estaba en los treinta y dos controladores, el token ya traia los permisos que CU-08
 * calculaba, {@code TodoEndpointDecideSuAcceso} comprobaba al arrancar que cada
 * endpoint declarara algo... y nadie leia el valor en una peticion. Cualquier
 * participante autenticado podia llamar {@code ENTREGA_AUTORIZAR}. La declaracion
 * existia; la autorizacion no. Se descubrio al escribir la sabana de seguridad, que es
 * exactamente para lo que sirve tener una.
 *
 * <p>Lo que fija esta clase, y que ninguna prueba de integracion puede fijar —porque
 * llaman al caso de uso directamente y nunca pasan por la guardia—:
 *
 * <ul>
 *   <li>sin sesion es {@code 401} y no {@code 403}: hay que volver a entrar;
 *   <li>con sesion y sin el permiso es {@code 403} y no {@code 401}: entrar de nuevo no
 *       cambia nada;
 *   <li>el {@code 403} no dice que permiso falta, porque decirlo ya cuenta que el
 *       recurso existe;
 *   <li>{@code @Publico} abre sin sesion, y solo eso.
 * </ul>
 */
@PruebaWeb(ControladorDeEnsayo.class)
class GuardiaDePermisoWebTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("R-SEG: sin sesion, una ruta protegida responde 401")
    void sinSesionEs401() throws Exception {
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("R-SEG: con sesion y sin el permiso, responde 403 — y 403 no es 401")
    void autenticadoSinPermisoEs403() throws Exception {
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO).with(Sesiones.sinPermisos()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
    }

    @Test
    @DisplayName("R-SEG: el permiso de OTRO endpoint no sirve para este")
    void unPermisoNoAbreTodo() throws Exception {
        // El caso que el sistema real vive: una sesion legitima, con permisos
        // legitimos, que no incluyen el de esta operacion.
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE", "GRUPO_CREAR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("R-SEG: con el permiso exacto, pasa")
    void conElPermisoPasa() throws Exception {
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Sesiones.USUARIO.toString()));
    }

    @Test
    @DisplayName("R-SEG: cuando @Permiso nombra un ROL del catalogo, el rol del token basta")
    void elRolTambienSatisface() throws Exception {
        // Nueve de los treinta y tres codigos usados son de la tabla `rol` y no de
        // `permiso`, y ninguno esta en las dos. Sin esta regla, esos nueve endpoints
        // serian inalcanzables para todo el mundo.
        mvc.perform(get("/ensayo/solo-rol").with(Sesiones.como("ORGANIZADOR"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("R-SEG: un rol distinto no alcanza aunque sea un rol real")
    void otroRolNoSatisface() throws Exception {
        mvc.perform(get("/ensayo/solo-rol").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("R-SEG: el 403 no dice que permiso falta")
    void laNegativaNoCuentaNada() throws Exception {
        String cuerpo = mvc.perform(
                        get("/ensayo/recursos/{id}", Sesiones.USUARIO).with(Sesiones.sinPermisos()))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("BILLETERA_VER")
                .doesNotContain(Sesiones.ROL_SIN_NADA);
    }

    @Test
    @DisplayName("R-SEG: @Publico abre sin sesion")
    void publicoAbre() throws Exception {
        mvc.perform(get("/ensayo/abierto")).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "token verificado pero inutil: {0}")
    @ValueSource(strings = {"sinRol", "conSujetoInvalido"})
    @DisplayName("R-SEG: un token con firma valida y contenido incompleto es 401, no 500")
    void tokenIncompletoEs401(String caso) throws Exception {
        var sesion = "sinRol".equals(caso) ? Sesiones.sinRol() : Sesiones.conSujetoInvalido();
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO).with(sesion))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AP-SES-01"));
    }
}
