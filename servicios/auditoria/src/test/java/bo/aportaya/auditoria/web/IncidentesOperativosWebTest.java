package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos;
import bo.aportaya.auditoria.dominio.IncidenteOperativo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sistemas › Incidentes en HTTP: la forma del contrato, los filtros que llegan al caso de uso, el importe que no sale,
 * la entrada fuera de dominio (400) y la autorizacion negativa (403). El 401 sin sesion lo cubre {@code SeguridadWebTest}.
 */
@PruebaWeb(IndicadoresController.class)
class IncidentesOperativosWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarIncidentesOperativos incidentes;

    // Comparten controlador; no se llaman aca.
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

    private static final IncidenteOperativo RESUELTO = new IncidenteOperativo(
            UUID.fromString("00000000-0000-4000-8000-000000000001"),
            "INC-1",
            "Latencia alta en cobros",
            "SEV2",
            "aportes",
            "RESUELTO",
            OffsetDateTime.parse("2026-10-20T10:00:00Z"),
            Optional.of(OffsetDateTime.parse("2026-10-20T11:00:00Z")),
            12);

    @Test
    @DisplayName(
            "correcto · con TABLERO_SISTEMAS_VER: 200 con la pagina, los filtros llegan al caso de uso y no sale importe")
    void pagina() throws Exception {
        when(incidentes.listar(eq(Optional.of("RESUELTO")), eq(Optional.of("SEV2")), eq(2), eq(10), any()))
                .thenReturn(new ConsultarIncidentesOperativos.Pagina(List.of(RESUELTO), 2, 10, 11));

        mvc.perform(get("/indicadores/operacion/incidentes?estado=RESUELTO&severidad=SEV2&pagina=2&limite=10")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].codigo").value("INC-1"))
                .andExpect(jsonPath("$.items[0].severidad").value("SEV2"))
                .andExpect(jsonPath("$.items[0].estado").value("RESUELTO"))
                .andExpect(jsonPath("$.items[0].resueltoEn").exists())
                .andExpect(jsonPath("$.items[0].impactoMonetario").doesNotExist())
                .andExpect(jsonPath("$.total").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    @DisplayName("limite · sin parametros: pagina 1 de 50; un incidente sin resolver no trae resueltoEn")
    void porOmision() throws Exception {
        var abierto = new IncidenteOperativo(
                RESUELTO.id(),
                "INC-2",
                "x",
                "SEV1",
                "identidad",
                "DETECTADO",
                RESUELTO.detectadoEn(),
                Optional.empty(),
                0);
        when(incidentes.listar(eq(Optional.empty()), eq(Optional.empty()), eq(1), eq(50), any()))
                .thenReturn(new ConsultarIncidentesOperativos.Pagina(List.of(abierto), 1, 50, 1));
        mvc.perform(get("/indicadores/operacion/incidentes").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].resueltoEn").doesNotExist());
        verify(incidentes).listar(eq(Optional.empty()), eq(Optional.empty()), eq(1), eq(50), any());
    }

    @Test
    @DisplayName("invalido · limite 0, limite 101 y un estado que no existe: 400, sin tocar la base")
    void fueraDeDominio() throws Exception {
        for (String consulta : List.of("limite=0", "limite=101", "estado=CERRADO", "severidad=SEV9")) {
            mvc.perform(get("/indicadores/operacion/incidentes?" + consulta)
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(incidentes);
    }

    @Test
    @DisplayName("autorizacion · 403 con el permiso de otro tablero, sin consultar")
    void otroTablero() throws Exception {
        mvc.perform(get("/indicadores/operacion/incidentes").with(Sesiones.como("TESORERIA", "TABLERO_FINANCIERO_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(incidentes);
    }
}
