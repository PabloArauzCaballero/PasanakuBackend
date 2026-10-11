package bo.aportaya.notificaciones.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta;
import bo.aportaya.notificaciones.aplicacion.ConsultarSupresion;
import bo.aportaya.notificaciones.aplicacion.SolicitarAviso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * H13.S1.M2 / M3 · Lo que notificaciones rechaza, con dos personas distintas y los roles que NO
 * deben poder.
 *
 * <p>Cada prueba pasa por la guardia real (filtros encendidos): lo que se afirma es que el caso
 * de uso ni se entera. La consulta de supresion es ademas un oraculo («este contacto pidio no
 * recibir»): el contacto viaja en el cuerpo, nunca en la URL, y cada actor tiene un tope.
 */
@PruebaWeb(NotificacionesController.class)
class AutorizacionNegativaWebTest {

    // Sintetico, con la forma de un telefono boliviano: no es de nadie.
    private static final String CONTACTO = "+59170000001";
    private static final String CLAVE = "d0000000-0000-4000-8000-0000000000ff";
    private static final String CONSULTA = "{\"identificador\":\"" + CONTACTO + "\",\"categoria\":\"COMERCIAL\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitarAviso avisos;

    @MockitoBean
    private ConsultarSupresion supresiones;

    @MockitoBean
    private CU82ProcesarRespuesta cu82;

    private ResultActions preguntar(org.springframework.test.web.servlet.request.RequestPostProcessor sesion)
            throws Exception {
        var peticion = post("/notificaciones/supresion/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CONSULTA);
        return mvc.perform(sesion == null ? peticion : peticion.with(sesion));
    }

    @Test
    @DisplayName("la consulta de supresion lleva el contacto en el CUERPO: la URL no tiene ningun dato")
    void elContactoNoViajaEnLaUrl() throws Exception {
        when(supresiones.estaSuprimido(any(), any(), any())).thenReturn(true);

        var resultado = preguntar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suprimido").value(true))
                .andReturn();

        assertThat(resultado.getRequest().getRequestURI() + "?"
                        + resultado.getRequest().getQueryString())
                .doesNotContain(CONTACTO)
                .doesNotContain("COMERCIAL");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain(CONTACTO);
    }

    @Test
    @DisplayName("rechaza: sin token no se pregunta por un contacto (401) y el caso de uso no se entera")
    void sinToken() throws Exception {
        preguntar(null).andExpect(status().isUnauthorized());
        verifyNoInteractions(supresiones);
    }

    @Test
    @DisplayName("rechaza: un participante sin GRUPO_ADMINISTRAR no consulta la baja de un contacto ajeno")
    void rolInsuficiente() throws Exception {
        preguntar(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")).andExpect(status().isForbidden());
        verifyNoInteractions(supresiones);
    }

    @Test
    @DisplayName("rechaza: un token de servicio de otro servicio (entregas) no es un usuario que invita")
    void tokenDeServicioNoEsUsuario() throws Exception {
        preguntar(Sesiones.como("SERVICIO_ENTREGAS", "ENTREGA_EJECUTAR")).andExpect(status().isForbidden());
        verifyNoInteractions(supresiones);
    }

    @Test
    @DisplayName("rechaza: un campo de mas en la consulta es 400, no se ignora en silencio")
    void campoDeMas() throws Exception {
        mvc.perform(post("/notificaciones/supresion/consultas")
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"" + CONTACTO
                                + "\",\"categoria\":\"COMERCIAL\",\"soloMios\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        verifyNoInteractions(supresiones);
    }

    @Test
    @DisplayName("rechaza: la consulta sin identificador es 400")
    void sinIdentificador() throws Exception {
        mvc.perform(post("/notificaciones/supresion/consultas")
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"COMERCIAL\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(supresiones);
    }

    @Test
    @DisplayName("rechaza: pasado el tope por minuto el actor recibe 429 con Retry-After, y OTRO actor sigue pudiendo")
    void topeDeConsultasPorActor() throws Exception {
        when(supresiones.estaSuprimido(any(), any(), any())).thenReturn(false);
        // Actores propios de esta prueba: el contexto de Spring se comparte entre clases y el reloj de las pruebas
        // esta parado, asi que el cupo de un actor compartido no se renueva.
        var abusador = Sesiones.de(UUID.randomUUID(), "ORGANIZADOR", "GRUPO_ADMINISTRAR");

        for (int i = 0; i < 30; i++) {
            preguntar(abusador).andExpect(status().isOk());
        }
        var corte = preguntar(abusador)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.codigo").value("AP-LIM-01"))
                .andReturn();
        assertThat(corte.getResponse().getContentAsString()).doesNotContain(CONTACTO);

        // La otra persona, con su propio cupo, no queda bloqueada por lo que hizo la primera.
        preguntar(Sesiones.de(UUID.randomUUID(), "ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("rechaza: emitir un aviso exige SOPORTE; ni un participante ni un token de servicio lo emiten")
    void emitirExigeSoporte() throws Exception {
        String cuerpo =
                "{\"destinatarioId\":\"" + UUID.randomUUID() + "\",\"evento\":\"grupo.invitacion\",\"datos\":{}}";
        for (var sesion : new org.springframework.test.web.servlet.request.RequestPostProcessor[] {
            Sesiones.como("PARTICIPANTE", "BILLETERA_VER"),
            Sesiones.como("SERVICIO_INVERSIONES", "ENTREGA_EJECUTAR"),
            Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")
        }) {
            mvc.perform(post("/notificaciones")
                            .with(sesion)
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/notificaciones")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(avisos);
    }

    @Test
    @DisplayName(
            "el GET deprecado sigue pidiendo GRUPO_ADMINISTRAR (mientras grupos migra, no es una puerta mas abierta)")
    void elGetDeprecadoNoEsMasAbierto() throws Exception {
        mvc.perform(get("/notificaciones/supresion")
                        .param("identificador", CONTACTO)
                        .param("categoria", "COMERCIAL")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(supresiones);
    }
}
