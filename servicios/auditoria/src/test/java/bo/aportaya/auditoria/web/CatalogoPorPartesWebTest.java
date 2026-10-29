package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos;
import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Catalogo de datos · H2 — el catalogo por partes en HTTP: resumen, pagina de tablas y ficha. Lo que solo se ve aca:
 * el permiso, los parametros que el contrato valida, el 404 de una tabla que no esta y la forma de la respuesta.
 */
@PruebaWeb(IndicadoresController.class)
class CatalogoPorPartesWebTest {

    private static final String PAGO =
            """
            {"tabla":"pago","esquema":"aportes","tipo":"NEGOCIO","modulo":"03","nombreModulo":"Aportes",
             "servicio":"aportes","appendOnly":false,
             "clasificacion":{"personales":1,"financieros":3,"secretos":0,"derivados":0},
             "negocio":{"queEs":"Plata que se movio"},"reglas":[],
             "columnas":[{"nombre":"obligacion_id","tipo":"UUID","nulable":false,"pk":false,
               "referencia":"obligacion_aporte","esquemaReferencia":"aportes","sistema":"UUID, obligatoria.",
               "sensibilidad":"NINGUNA"}],
             "relaciones":{"salientes":[{"columna":"obligacion_id","tabla":"obligacion_aporte","esquema":"aportes",
               "columnaDestino":"id","restriccion":"fk_pago_obligacion_id","alBorrar":"RESTRICT",
               "cardinalidad":"N:1","obligatoria":true}],"entrantes":[]},
             "sistema":"Vive en el esquema `aportes`.",
             "operaciones":[{"casoDeUso":"CU-21","titulo":"Cobrar el aporte del período","endpoints":[
               {"servicio":"aportes","metodo":"POST","ruta":"/aportes/obligaciones/{obligacionId}/pagos",
                "operationId":"cobrarAporte"}]}]}
            """;

    @Autowired
    private MockMvc mvc;

    // Sistemas › Incidentes: comparte controlador; no se llama aca.

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos consultarIncidentes;

    @MockitoBean
    private ConsultarCatalogoDeDatos consultarCatalogo;

    // El resto del controlador: no se llama aca.
    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente vigente;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU98PublicarTablero cu98;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.SaludDeServicios saludDeServicios;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU131RevisarColumna cu131;

    private static ConsultarCatalogoDeDatos.Fila fila() {
        return new ConsultarCatalogoDeDatos.Fila(
                "pago",
                "aportes",
                "NEGOCIO",
                "03",
                "Aportes",
                Optional.of("aportes"),
                Optional.empty(),
                false,
                19,
                0,
                14,
                6,
                new ConsultarCatalogoDeDatos.Clasificacion(1, 3, 0, 0),
                FichaDeGobierno.porOmision());
    }

    @Test
    @DisplayName("catálogo · tablas: 200 con la página, sin columnas, y los filtros llegan al caso de uso")
    void pagina() throws Exception {
        when(consultarCatalogo.tablas(any(), anyInt(), anyInt(), any()))
                .thenReturn(new ConsultarCatalogoDeDatos.Pagina(List.of(fila()), 2, 20, 21, 2));

        mvc.perform(get("/indicadores/catalogo-de-datos/tablas")
                        .param("esquema", "aportes")
                        .param("estado", "ACTIVA")
                        .param("pagina", "2")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tabla").value("pago"))
                .andExpect(jsonPath("$.items[0].clasificacion.financieros").value(3))
                .andExpect(jsonPath("$.items[0].gobierno.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.items[0].columnas").value(19))
                .andExpect(jsonPath("$.items[0].queEs").doesNotExist())
                .andExpect(jsonPath("$.total").value(21))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        verify(consultarCatalogo)
                .tablas(
                        eq(new ConsultarCatalogoDeDatos.Filtro(
                                Optional.empty(),
                                Optional.of("aportes"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of("ACTIVA"),
                                Optional.empty(),
                                Optional.empty())),
                        eq(2),
                        eq(20),
                        any());
    }

    @Test
    @DisplayName("catálogo · tablas: 400 con un límite fuera de 1..100 o un estado que no existe, sin consultar")
    void parametrosInvalidos() throws Exception {
        mvc.perform(get("/indicadores/catalogo-de-datos/tablas")
                        .param("limite", "0")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores/catalogo-de-datos/tablas")
                        .param("estado", "BORRADA")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultarCatalogo);
    }

    @Test
    @DisplayName("catálogo · tablas y resumen: 403 con el permiso de otro tablero, sin consultar")
    void otroTablero() throws Exception {
        mvc.perform(get("/indicadores/catalogo-de-datos/tablas")
                        .with(Sesiones.como("COMERCIAL", "TABLERO_COMERCIAL_VER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/indicadores/catalogo-de-datos/resumen")
                        .with(Sesiones.como("COMERCIAL", "TABLERO_COMERCIAL_VER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/indicadores/catalogo-de-datos/tablas/aportes/pago")
                        .with(Sesiones.como("COMERCIAL", "TABLERO_COMERCIAL_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultarCatalogo);
    }

    @Test
    @DisplayName("catálogo · resumen: 200 con cifras, conteos por estado y esquemas, con AUDITORIA_LEER")
    void resumen() throws Exception {
        var esquema = new ConsultarCatalogoDeDatos.Esquema(
                "aportes", Optional.of("aportes"), "Aportes.", 22, 18, 213, 38, 0, 1);
        var catalogo =
                new ConsultarCatalogoDeDatos.Esquema("catalogo", Optional.empty(), "Compartidos.", 8, 8, 60, 0, 0, 0);
        when(consultarCatalogo.resumen(any()))
                .thenReturn(new ConsultarCatalogoDeDatos.Resumen(
                        30,
                        26,
                        4,
                        273,
                        0,
                        38,
                        1,
                        9,
                        0,
                        30,
                        Map.of(
                                "ACTIVA",
                                30,
                                "PLANIFICADA",
                                0,
                                "CANDIDATA_A_RETIRO",
                                0,
                                "RETIRADA",
                                0,
                                "DESHABILITADA",
                                0),
                        Map.of("DETECTADA", 30, "NECESITA_REVISION", 0, "APROBADA", 0, "RECHAZADA", 0),
                        List.of(esquema, catalogo),
                        List.of(new ConsultarCatalogoDeDatos.Modulo("03", "Aportes", 18)),
                        List.of()));

        mvc.perform(get("/indicadores/catalogo-de-datos/resumen")
                        .with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tablas").value(30))
                .andExpect(jsonPath("$.porEstado.ACTIVA").value(30))
                .andExpect(jsonPath("$.porRevision.DETECTADA").value(30))
                .andExpect(jsonPath("$.esquemas[0].servicio").value("aportes"))
                .andExpect(jsonPath("$.esquemas[1].servicio").doesNotExist())
                .andExpect(jsonPath("$.modulos[0].codigo").value("03"));
    }

    @Test
    @DisplayName("catálogo · ficha: 200 con relaciones enriquecidas y la ficha de gobierno superpuesta")
    void ficha() throws Exception {
        when(consultarCatalogo.tabla(eq("aportes"), eq("pago"), any()))
                .thenReturn(Optional.of(new ConsultarCatalogoDeDatos.Ficha(
                        new ObjectMapper().readTree(PAGO), FichaDeGobierno.porOmision(), Map.of())));

        mvc.perform(get("/indicadores/catalogo-de-datos/tablas/aportes/pago")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relaciones.salientes[0].restriccion").value("fk_pago_obligacion_id"))
                .andExpect(jsonPath("$.relaciones.salientes[0].alBorrar").value("RESTRICT"))
                .andExpect(jsonPath("$.relaciones.salientes[0].cardinalidad").value("N:1"))
                .andExpect(jsonPath("$.columnas[0].esquemaReferencia").value("aportes"))
                .andExpect(jsonPath("$.gobierno.estadoRevision").value("DETECTADA"))
                .andExpect(jsonPath("$.operaciones[0].casoDeUso").value("CU-21"))
                .andExpect(jsonPath("$.operaciones[0].endpoints[0].operationId").value("cobrarAporte"));
    }

    @Test
    @DisplayName("catálogo · ficha: 404 cuando la tabla no está en el catálogo")
    void tablaQueNoEsta() throws Exception {
        when(consultarCatalogo.tabla(any(), any(), any())).thenReturn(Optional.empty());

        mvc.perform(get("/indicadores/catalogo-de-datos/tablas/aportes/no_existe")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isNotFound());
    }
}
