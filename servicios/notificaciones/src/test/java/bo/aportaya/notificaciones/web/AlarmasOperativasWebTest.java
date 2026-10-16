package bo.aportaya.notificaciones.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta;
import bo.aportaya.notificaciones.aplicacion.ConsultarSupresion;
import bo.aportaya.notificaciones.aplicacion.SolicitarAviso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * H13.S1.M5 · El aviso de una alarma operativa entra por {@code POST /notificaciones}, y asi tiene que
 * llegar al caso de uso. Es el DOBLE del lado receptor (regla 65): el evaluador de alarmas
 * ({@code despliegue/alertas/evaluar_alarmas.py}) arma estos cuerpos; aca se fija que el servicio los
 * acepta tal cual, y que nadie sin el rol de soporte puede fabricar una alarma.
 *
 * <p>Los tres niveles: lo correcto (las cuatro alarmas), el limite (datos vacios, el evento mas largo), y
 * lo invalido (sin destinatario, campos de mas, un participante intentando emitir).
 */
@PruebaWeb(NotificacionesController.class)
class AlarmasOperativasWebTest {

    private static final String CLAVE = "d0000000-0000-4000-8000-0000000000fe";
    private static final String RESPONSABLE = "d0000000-0000-4000-8000-0000000000a1";
    private static final List<String> EVENTOS = List.of(
            "OPERACION_BRECHA_DE_CAJA",
            "OPERACION_ATRASO_DE_APORTES",
            "OPERACION_COLA_DETENIDA",
            "OPERACION_CONCILIACION_VENCIDA");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitarAviso avisos;

    @MockitoBean
    private ConsultarSupresion supresiones;

    @MockitoBean
    private CU82ProcesarRespuesta cu82;

    @MockitoBean
    private bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria consultarMensajeria;

    private ResultActions emitir(RequestPostProcessor sesion, String cuerpo) throws Exception {
        var peticion = post("/notificaciones")
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
        return mvc.perform(sesion == null ? peticion : peticion.with(sesion));
    }

    private static String alarma(String evento, String datos) {
        return "{\"destinatarioId\":\"" + RESPONSABLE + "\",\"evento\":\"" + evento + "\",\"datos\":" + datos + "}";
    }

    @Test
    @DisplayName("correcto: las cuatro alarmas se aceptan con 202 y llegan al caso de uso con su evento y sus datos")
    void lasCuatroAlarmas() throws Exception {
        when(avisos.ejecutar(any(), any())).thenReturn(new SolicitarAviso.Salida(UUID.randomUUID(), List.of("IN_APP")));

        for (String evento : EVENTOS) {
            emitir(
                            Sesiones.como("SOPORTE"),
                            alarma(
                                    evento,
                                    "{\"regla\":\"ALR-01\",\"severidad\":\"CRITICA\",\"conteo\":\"2\","
                                            + "\"procedimiento\":\"docs/operacion/alarmas/ALR-01-brecha-de-caja.md\"}"))
                    .andExpect(status().isAccepted());
        }

        var capturada = ArgumentCaptor.forClass(SolicitarAviso.Entrada.class);
        verify(avisos, org.mockito.Mockito.times(EVENTOS.size())).ejecutar(capturada.capture(), any());
        assertThat(capturada.getAllValues())
                .extracting(SolicitarAviso.Entrada::evento)
                .containsExactlyElementsOf(EVENTOS);
        assertThat(capturada.getAllValues())
                .allSatisfy(e -> assertThat(e.destinatarioId()).isEqualTo(UUID.fromString(RESPONSABLE)));
        assertThat(capturada.getValue().datos()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) capturada.getValue().datos()).get("procedimiento"))
                .as("el procedimiento viaja en el aviso: quien lo recibe sabe que hacer")
                .isEqualTo("docs/operacion/alarmas/ALR-01-brecha-de-caja.md");
    }

    @Test
    @DisplayName("limite: datos vacios y el evento mas largo (29 de 40 caracteres) siguen siendo un aviso valido")
    void losBordes() throws Exception {
        when(avisos.ejecutar(any(), any())).thenReturn(new SolicitarAviso.Salida(UUID.randomUUID(), List.of("IN_APP")));

        emitir(Sesiones.como("SOPORTE"), alarma("OPERACION_CONCILIACION_VENCIDA", "{}"))
                .andExpect(status().isAccepted());
        assertThat("OPERACION_CONCILIACION_VENCIDA".length()).isLessThanOrEqualTo(40);
    }

    @Test
    @DisplayName("rechaza: sin destinatario, con campos de mas o con datos que no son un objeto, el aviso no se acepta")
    void rechazaLoInvalido() throws Exception {
        emitir(Sesiones.como("SOPORTE"), "{\"evento\":\"OPERACION_COLA_DETENIDA\",\"datos\":{}}")
                .andExpect(status().isBadRequest());
        emitir(
                        Sesiones.como("SOPORTE"),
                        "{\"destinatarioId\":\"" + RESPONSABLE
                                + "\",\"evento\":\"OPERACION_COLA_DETENIDA\",\"datos\":{}," + "\"canal\":\"CORREO\"}")
                .andExpect(status().isBadRequest());
        // `datos` es un objeto, no una cadena ni una lista: pasa el generador (lo tipa Object) y lo ataja el
        // controlador.
        emitir(Sesiones.como("SOPORTE"), alarma("OPERACION_COLA_DETENIDA", "\"no es un objeto\""))
                .andExpect(status().isUnprocessableEntity());
        emitir(Sesiones.como("SOPORTE"), alarma("OPERACION_COLA_DETENIDA", "[1,2]"))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(avisos);
    }

    @Test
    @DisplayName("rechaza: un participante no puede fabricar una alarma, ni con un rol que no es el de soporte")
    void rechazaQuienNoEsSoporte() throws Exception {
        for (String rol : List.of("PARTICIPANTE", "GRUPO_ADMINISTRAR", "TESORERIA")) {
            emitir(Sesiones.como(rol), alarma("OPERACION_BRECHA_DE_CAJA", "{}")).andExpect(status().isForbidden());
        }
        emitir(null, alarma("OPERACION_BRECHA_DE_CAJA", "{}")).andExpect(status().isUnauthorized());

        verifyNoInteractions(avisos);
    }
}
