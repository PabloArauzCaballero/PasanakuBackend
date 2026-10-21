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
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.dominio.OpcionDeRetencion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CU-130 · lo que solo se ve en HTTP: la ficha superpuesta al catalogo, el permiso de edicion y el cuerpo estricto. */
@PruebaWeb(IndicadoresController.class)
class FichaDeGobiernoWebTest {

    private static final String CATALOGO =
            """
            {"esquemas":["aportes"],"tablas":[
             {"tabla":"pago","esquema":"aportes","modulo":"03","nombreModulo":"Aportes","appendOnly":false,
              "negocio":{},"reglas":[],"columnas":[],"relaciones":{"salientes":[],"entrantes":[]},"sistema":"s"},
             {"tabla":"grupo","esquema":"grupos","modulo":"02","nombreModulo":"Grupos","appendOnly":false,
              "negocio":{},"reglas":[],"columnas":[],"relaciones":{"salientes":[],"entrantes":[]},"sistema":"s"}]}
            """;

    private static final UUID RETENCION = UUID.fromString("00000000-0000-4000-8000-0000000000c1");

    private static final String CUERPO =
            """
            {"estado":"CANDIDATA_A_RETIRO","estadoRevision":"APROBADA","responsable":"Sistemas",
             "criticaParaAuditoria":true,"version":0}
            """;

    @Autowired
    private MockMvc mvc;


    // Catalogo de datos · H2: comparte controlador; no se llama aca.

    @MockitoBean

    private bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos consultarCatalogo;

    @MockitoBean
    private CatalogoDeDatosVigente vigente;

    @MockitoBean
    private CU130GobernarFichaDeTabla cu130;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU131RevisarColumna cu131;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU98PublicarTablero cu98;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.EndpointsDelDia endpointsDelDia;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.SaludDeServicios saludDeServicios;

    @BeforeEach
    void catalogo() throws Exception {
        when(vigente.catalogo()).thenReturn(new ObjectMapper().readTree(CATALOGO));
        when(cu131.consultar(any())).thenReturn(Map.of());
    }

    @Test
    @DisplayName("catálogo · gobierno: la tabla sin ficha sale ACTIVA y DETECTADA; la guardada, con lo suyo")
    void superpone() throws Exception {
        FichaDeGobierno retirada = new FichaDeGobierno(
                "RETIRADA",
                "APROBADA",
                Optional.of("Sistemas"),
                Optional.of(RETENCION),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                3);
        when(cu130.consultar(any()))
                .thenReturn(new CU130GobernarFichaDeTabla.Vista(
                        Map.of("grupos.grupo", retirada),
                        List.of(new OpcionDeRetencion(RETENCION, "pago", 120, "ARCHIVAR"))));

        mvc.perform(get("/indicadores/catalogo-de-datos")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tablas[0].gobierno.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.tablas[0].gobierno.estadoRevision").value("DETECTADA"))
                .andExpect(jsonPath("$.tablas[0].gobierno.version").value(0))
                .andExpect(jsonPath("$.tablas[0].gobierno.responsable").doesNotExist())
                .andExpect(jsonPath("$.tablas[1].gobierno.estado").value("RETIRADA"))
                .andExpect(jsonPath("$.tablas[1].gobierno.responsable").value("Sistemas"))
                .andExpect(jsonPath("$.tablas[1].gobierno.version").value(3))
                .andExpect(jsonPath("$.politicasDeRetencion[0].entidad").value("pago"));
    }

    @Test
    @DisplayName("catálogo · gobierno: con CATALOGO_DATOS_EDITAR, 200 con la ficha guardada")
    void guarda() throws Exception {
        when(cu130.guardar(eq("aportes"), eq("pago"), any(), any()))
                .thenReturn(new FichaDeGobierno(
                        "CANDIDATA_A_RETIRO",
                        "APROBADA",
                        Optional.of("Sistemas"),
                        Optional.empty(),
                        true,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        1));

        mvc.perform(put("/indicadores/catalogo-de-datos/aportes/pago/gobierno")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "CATALOGO_DATOS_EDITAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANDIDATA_A_RETIRO"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("catálogo · gobierno: 403 con solo permiso de lectura, sin llamar al caso de uso")
    void soloLectura() throws Exception {
        mvc.perform(put("/indicadores/catalogo-de-datos/aportes/pago/gobierno")
                        .with(Sesiones.como("AUDITOR_INTERNO", "TABLERO_SISTEMAS_VER", "AUDITORIA_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu130);
    }

    @Test
    @DisplayName("catálogo · gobierno: 400 AP-VAL-02 con un campo que el contrato no declara o un estado inventado")
    void cuerpoEstricto() throws Exception {
        for (String cuerpo : List.of(
                "{\"estado\":\"ACTIVA\",\"estadoRevision\":\"APROBADA\",\"criticaParaAuditoria\":false,"
                        + "\"version\":0,\"contieneDatosPersonales\":true}",
                "{\"estado\":\"BORRADA\",\"estadoRevision\":\"APROBADA\",\"criticaParaAuditoria\":false,"
                        + "\"version\":0}")) {
            mvc.perform(put("/indicadores/catalogo-de-datos/aportes/pago/gobierno")
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "CATALOGO_DATOS_EDITAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }
        verifyNoInteractions(cu130);
    }
}
