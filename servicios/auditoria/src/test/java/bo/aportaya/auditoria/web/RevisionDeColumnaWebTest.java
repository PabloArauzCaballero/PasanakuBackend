package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla;
import bo.aportaya.auditoria.aplicacion.CU131RevisarColumna;
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CU-131 · lo que solo se ve en HTTP: la revision superpuesta a la columna, el permiso de edicion y el cuerpo estricto. */
@PruebaWeb(IndicadoresController.class)
class RevisionDeColumnaWebTest {

    private static final String CATALOGO =
            """
            {"esquemas":["aportes"],"tablas":[
             {"tabla":"pago","esquema":"aportes","modulo":"03","nombreModulo":"Aportes","appendOnly":false,
              "negocio":{},"reglas":[],
              "columnas":[{"nombre":"monto","tipo":"DECIMAL(14,2)","nulable":false,"pk":false,"sistema":"s","sensibilidad":"FINANCIERO"},
                          {"nombre":"canal","tipo":"VARCHAR(30)","nulable":false,"pk":false,"sistema":"s","sensibilidad":"NINGUNA"}],
              "relaciones":{"salientes":[],"entrantes":[]},"sistema":"s"}]}
            """;
    private static final String RUTA = "/indicadores/catalogo-de-datos/aportes/pago/columnas/monto/revision";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogoDeDatosVigente vigente;

    @MockitoBean
    private CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private CU131RevisarColumna cu131;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU98PublicarTablero cu98;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.SaludDeServicios saludDeServicios;

    @BeforeEach
    void catalogo() throws Exception {
        when(vigente.catalogo()).thenReturn(new ObjectMapper().readTree(CATALOGO));
        when(cu130.consultar(any())).thenReturn(new CU130GobernarFichaDeTabla.Vista(Map.of(), List.of()));
    }

    @Test
    @DisplayName("catálogo · revisión: la columna revisada trae su revisión y la otra no trae nada")
    void superpone() throws Exception {
        when(cu131.consultar(any()))
                .thenReturn(Map.of(
                        "aportes.pago.monto",
                        new RevisionDeColumna(
                                "APROBADA",
                                Optional.of("ALTA"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                2)));
        mvc.perform(get("/indicadores/catalogo-de-datos")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tablas[0].columnas[0].revision.decision").value("APROBADA"))
                .andExpect(
                        jsonPath("$.tablas[0].columnas[0].revision.confianza").value("ALTA"))
                .andExpect(jsonPath("$.tablas[0].columnas[0].revision.version").value(2))
                .andExpect(jsonPath("$.tablas[0].columnas[1].revision").doesNotExist());
    }

    @Test
    @DisplayName("catálogo · revisión: con CATALOGO_DATOS_EDITAR, 200 con la revisión guardada")
    void guarda() throws Exception {
        when(cu131.guardar(eq("aportes"), eq("pago"), eq("monto"), any(), any()))
                .thenReturn(new RevisionDeColumna(
                        "RECHAZADA",
                        Optional.empty(),
                        Optional.of("No es dinero"),
                        Optional.empty(),
                        Optional.empty(),
                        1));
        mvc.perform(put(RUTA)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "CATALOGO_DATOS_EDITAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RECHAZADA\",\"motivo\":\"No es dinero\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivo").value("No es dinero"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName(
            "catálogo · revisión: 403 con solo lectura y 400 AP-VAL-02 con una decisión inventada, sin llamar al caso")
    void permisoYCuerpo() throws Exception {
        mvc.perform(put(RUTA)
                        .with(Sesiones.como("AUDITOR_INTERNO", "TABLERO_SISTEMAS_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APROBADA\",\"version\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(RUTA)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "CATALOGO_DATOS_EDITAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"DUDOSA\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        verifyNoInteractions(cu131);
    }
}
