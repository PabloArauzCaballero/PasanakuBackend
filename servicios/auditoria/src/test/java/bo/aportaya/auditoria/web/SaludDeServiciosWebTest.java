package bo.aportaya.auditoria.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.SaludDeServicios;
import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio.Estado;
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

/**
 * operacion/estado · lo que solo se ve en HTTP: los tres estados viajan como los dice el contrato, la latencia de un
 * servicio que no contesto se omite (no es un cero), y la salud no le llega a quien no tiene el tablero de sistemas.
 * El 401 sin sesion lo cubre {@code SeguridadWebTest}, que barre todas las rutas.
 */
@PruebaWeb(IndicadoresController.class)
class SaludDeServiciosWebTest {

    @Autowired
    private MockMvc mvc;

    // Catalogo de datos · H2: comparte controlador; no se llama aca.

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos consultarCatalogo;

    @MockitoBean
    private SaludDeServicios salud;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU98PublicarTablero cu98;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente catalogoDeDatos;

    // CU-130: el mismo controlador guarda la ficha de gobierno.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU131RevisarColumna cu131;

    @Test
    @DisplayName(
            "estado · con TABLERO_SISTEMAS_VER: 200 con ARRIBA, DEGRADADO y CAIDO, y sin latencia el que no contestó")
    void tresEstados() throws Exception {
        when(salud.consultar())
                .thenReturn(new SaludDeServicios.Salida(
                        Instant.parse("2026-10-17T15:00:00Z"),
                        List.of(
                                new SaludDeServicio("identidad", Estado.ARRIBA, Optional.of(12L)),
                                new SaludDeServicio("aportes", Estado.DEGRADADO, Optional.of(40L)),
                                new SaludDeServicio("erp", Estado.CAIDO, Optional.empty()))));

        mvc.perform(get("/indicadores/operacion/servicios")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultadoEn").value("2026-10-17T15:00:00Z"))
                .andExpect(jsonPath("$.servicios[0].estado").value("ARRIBA"))
                .andExpect(jsonPath("$.servicios[0].latenciaMs").value(12))
                .andExpect(jsonPath("$.servicios[1].estado").value("DEGRADADO"))
                .andExpect(jsonPath("$.servicios[2].servicio").value("erp"))
                .andExpect(jsonPath("$.servicios[2].estado").value("CAIDO"))
                .andExpect(jsonPath("$.servicios[2].latenciaMs").doesNotExist());
    }

    @Test
    @DisplayName("estado · AUDITORIA_LEER también la ve")
    void auditor() throws Exception {
        when(salud.consultar())
                .thenReturn(new SaludDeServicios.Salida(Instant.parse("2026-10-17T15:00:00Z"), List.of()));
        mvc.perform(get("/indicadores/operacion/servicios").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("estado · 403 con el permiso de otro tablero, sin preguntarle a ningún servicio")
    void otroTablero() throws Exception {
        mvc.perform(get("/indicadores/operacion/servicios").with(Sesiones.como("TESORERIA", "TABLERO_FINANCIERO_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(salud);
    }
}
