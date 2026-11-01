package bo.aportaya.notificaciones.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.dominio.PlataformaDeMensajeria;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Sistemas › Proveedores y Webhooks de notificaciones en HTTP: forma, lo que no sale, 400 y 403. */
@PruebaWeb(NotificacionesController.class)
class PlataformaDeMensajeriaWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarMensajeria mensajeria;

    // Comparten controlador; no se llaman aca.
    @MockitoBean
    private bo.aportaya.notificaciones.aplicacion.SolicitarAviso avisos;

    @MockitoBean
    private bo.aportaya.notificaciones.aplicacion.ConsultarSupresion supresiones;

    @MockitoBean
    private bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta cu82;

    @MockitoBean
    private bo.aportaya.notificaciones.aplicacion.GestionarAvisosInternos gestionarAvisosInternos;

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("correcto · proveedores con salud en texto exacto, sin credenciales ni costo")
    void proveedores() throws Exception {
        when(mensajeria.proveedores(any()))
                .thenReturn(List.of(new PlataformaDeMensajeria.Proveedor(
                        ID, "SMSB", "SMS Bolivia", "SMS", true, 1, new BigDecimal("97.50"))));
        mvc.perform(get("/notificaciones/proveedores").with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saludPorcentaje").value("97.50"))
                .andExpect(jsonPath("$[0].costoPorMensaje").doesNotExist())
                .andExpect(jsonPath("$[0].referenciaCredenciales").doesNotExist());
    }

    @Test
    @DisplayName("correcto · acuses sin payload; sin error no trae codigoError; limite por omision 50")
    void acuses() throws Exception {
        var t = OffsetDateTime.parse("2026-10-29T10:00:00Z");
        when(mensajeria.eventosDeEntrega(eq(50), any()))
                .thenReturn(List.of(new PlataformaDeMensajeria.EventoDeEntrega(
                        ID, "SMSB", "SMS", "ENTREGADO", t, t, Optional.empty())));
        mvc.perform(get("/notificaciones/webhooks-recibidos").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoEvento").value("ENTREGADO"))
                .andExpect(jsonPath("$[0].codigoError").doesNotExist())
                .andExpect(jsonPath("$[0].payloadCrudo").doesNotExist());
    }

    @Test
    @DisplayName("invalido y autorizacion · limite 0 es 400; con un permiso de grupos es 403; nunca se consulta")
    void rechazos() throws Exception {
        mvc.perform(get("/notificaciones/webhooks-recibidos?limite=0")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/notificaciones/proveedores").with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mensajeria);
    }
}
