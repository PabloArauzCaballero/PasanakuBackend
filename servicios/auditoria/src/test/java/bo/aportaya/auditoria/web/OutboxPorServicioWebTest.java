package bo.aportaya.auditoria.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.OutboxPorServicio;
import bo.aportaya.auditoria.dominio.OutboxDeServicio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Sistemas › Outbox en HTTP: lo medido viaja, lo no medido se omite (no es cero), y 403 sin el tablero. */
@PruebaWeb(IndicadoresController.class)
class OutboxPorServicioWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OutboxPorServicio outbox;

    // Comparten controlador; no se llaman aca.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos incidentes;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos consultarCatalogo;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.SaludDeServicios salud;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU98PublicarTablero cu98;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente catalogoDeDatos;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU131RevisarColumna cu131;

    @Test
    @DisplayName("correcto · lo medido viaja y lo no medido se omite")
    void medido() throws Exception {
        when(outbox.consultar())
                .thenReturn(new OutboxPorServicio.Salida(
                        Instant.parse("2026-10-29T15:00:00Z"),
                        true,
                        List.of(new OutboxDeServicio(
                                "aportes",
                                Optional.of(3L),
                                Optional.empty(),
                                Optional.of(42L),
                                Optional.of(121L),
                                Optional.of(2L)))));
        mvc.perform(get("/indicadores/operacion/outbox")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuenteDisponible").value(true))
                .andExpect(jsonPath("$.servicios[0].pendientes").value(3))
                .andExpect(jsonPath("$.servicios[0].fallidos").doesNotExist())
                .andExpect(jsonPath("$.servicios[0].erroresDePublicacion24h").value(2));
    }

    @Test
    @DisplayName("limite · Prometheus caido: 200 con fuenteDisponible false, no un 500")
    void sinFuente() throws Exception {
        when(outbox.consultar())
                .thenReturn(new OutboxPorServicio.Salida(Instant.parse("2026-10-29T15:00:00Z"), false, List.of()));
        mvc.perform(get("/indicadores/operacion/outbox").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuenteDisponible").value(false));
    }

    @Test
    @DisplayName("autorizacion · 403 con el tablero financiero, sin consultar")
    void otroTablero() throws Exception {
        mvc.perform(get("/indicadores/operacion/outbox").with(Sesiones.como("TESORERIA", "TABLERO_FINANCIERO_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(outbox);
    }
}
