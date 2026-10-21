package bo.aportaya.auditoria.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tableros · H7 — el catalogo de datos. Lo que solo se ve en HTTP: que describe la estructura y no viaja a quien no
 * tiene permiso, y que lo que falta en una ficha se omite en vez de viajar como {@code null}.
 */
@PruebaWeb(IndicadoresController.class)
class CatalogoDeDatosWebTest {

    // Un catalogo minimo, sintetico: una tabla con una columna de cada origen de narrativa.
    private static final String CATALOGO =
            """
            {"esquemas":["aportes"],"tablas":[{"tabla":"obligacion_aporte","esquema":"aportes","modulo":"03",
             "nombreModulo":"Aportes","servicio":"aportes","appendOnly":false,
             "negocio":{"queEs":"Lo que un cupo debe"},"reglas":[],
             "columnas":[{"nombre":"monto_esperado","tipo":"DECIMAL(14,2)","nulable":false,"pk":false,
               "negocio":"lo que se pacto","origenNegocio":"ficha","sistema":"DECIMAL(14,2), obligatoria.",
               "sensibilidad":"FINANCIERO"},
              {"nombre":"grupo_id","tipo":"UUID","nulable":false,"pk":false,"referencia":"grupo",
               "sistema":"UUID, obligatoria, referencia a `grupo`.","sensibilidad":"NINGUNA"}],
             "relaciones":{"salientes":[{"columna":"grupo_id","tabla":"grupo","esquema":"grupos"}],"entrantes":[]},
             "sistema":"Vive en el esquema `aportes`."}]}
            """;

    @Autowired
    private MockMvc mvc;


    // Catalogo de datos · H2: comparte controlador; no se llama aca.

    @MockitoBean

    private bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos consultarCatalogo;

    @MockitoBean
    private CatalogoDeDatosVigente vigente;

    // El mismo controlador publica el tablero y el desglose del dia: no se llaman aca.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU98PublicarTablero cu98;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    // operacion/estado comparte controlador: no se llama aca.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.SaludDeServicios saludDeServicios;

    // CU-130: el GET superpone la ficha de gobierno; aca, ninguna guardada.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU131RevisarColumna cu131;

    @BeforeEach
    void catalogo() throws Exception {
        when(vigente.catalogo()).thenReturn(new ObjectMapper().readTree(CATALOGO));
        when(cu130.consultar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla.Vista(
                        java.util.Map.of(), java.util.List.of()));
        when(cu131.consultar(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.Map.of());
    }

    @Test
    @DisplayName("tableros · catálogo: con TABLERO_SISTEMAS_VER, 200 con la tabla, su narrativa y sus relaciones")
    void sistemas() throws Exception {
        mvc.perform(get("/indicadores/catalogo-de-datos")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tablas[0].tabla").value("obligacion_aporte"))
                .andExpect(jsonPath("$.tablas[0].negocio.queEs").value("Lo que un cupo debe"))
                .andExpect(jsonPath("$.tablas[0].columnas[0].sensibilidad").value("FINANCIERO"))
                .andExpect(
                        jsonPath("$.tablas[0].relaciones.salientes[0].esquema").value("grupos"));
    }

    @Test
    @DisplayName("tableros · catálogo: lo que la ficha no dice se omite, no viaja como null")
    void sinNulos() throws Exception {
        mvc.perform(get("/indicadores/catalogo-de-datos").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tablas[0].columnas[1].negocio").doesNotExist())
                .andExpect(jsonPath("$.tablas[0].negocio.porQue").doesNotExist());
    }

    @Test
    @DisplayName("tableros · catálogo: 403 con el permiso de otro tablero, sin leer el catálogo")
    void otroTablero() throws Exception {
        mvc.perform(get("/indicadores/catalogo-de-datos").with(Sesiones.como("COMERCIAL", "TABLERO_COMERCIAL_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(vigente);
    }
}
