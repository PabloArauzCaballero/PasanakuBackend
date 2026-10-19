package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoDetalle;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoResumen;
import bo.aportaya.auditoria.web.generado.modelo.ElementoCatalogoOperativo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@PruebaWeb(CatalogosOperativosController.class)
class CatalogosOperativosWebTest {

    private static final OffsetDateTime FECHA = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarCatalogosOperativos casoDeUso;

    @Test
    @DisplayName("401: sin sesion rechaza la consulta de catalogos")
    void sinSesionRechaza() throws Exception {
        mvc.perform(get("/auditoria/catalogos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(casoDeUso);
    }

    @Test
    @DisplayName("403: sin permiso adecuado rechaza la consulta de catalogos")
    void sinPermisoRechaza() throws Exception {
        mvc.perform(get("/auditoria/catalogos").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(casoDeUso);
    }

    @Test
    @DisplayName("200: con AUDITORIA_LEER lista catalogos operativos")
    void listarCatalogosExitoso() throws Exception {
        CatalogoOperativoResumen r = new CatalogoOperativoResumen();
        r.setCodigo("HECHOS_GENERADORES");
        r.setNombre("Hechos generadores de comisiones");
        r.setDueno("Tarifas y Comisiones");
        r.setVersion("2026.1");
        r.setTotalElementos(4);
        r.setElementosActivos(4);
        r.setVigenteDesde(FECHA);
        r.setEstado(CatalogoOperativoResumen.EstadoEnum.VIGENTE);
        r.setDescripcion("Eventos del negocio que devengan comisiones.");

        when(casoDeUso.listarCatalogos()).thenReturn(List.of(r));

        mvc.perform(get("/auditoria/catalogos").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("HECHOS_GENERADORES"))
                .andExpect(jsonPath("$[0].nombre").value("Hechos generadores de comisiones"))
                .andExpect(jsonPath("$[0].totalElementos").value(4))
                .andExpect(jsonPath("$[0].elementosActivos").value(4));
    }

    @Test
    @DisplayName("200: consulta detalle de un catalogo por codigo")
    void consultarDetalleExitoso() throws Exception {
        CatalogoOperativoDetalle d = new CatalogoOperativoDetalle();
        d.setCodigo("HECHOS_GENERADORES");
        d.setNombre("Hechos generadores de comisiones");
        d.setDueno("Tarifas y Comisiones");
        d.setVersion("2026.1");
        d.setTotalElementos(4);
        d.setElementosActivos(4);
        d.setVigenteDesde(FECHA);
        d.setEstado(CatalogoOperativoDetalle.EstadoEnum.VIGENTE);
        d.setDescripcion("Eventos de negocio.");
        d.setTablaFisica("tarifas.catalogo_hecho_generador");
        d.setBaseNormativa("Circular ASFI/123/2025");

        when(casoDeUso.consultarCatalogo("HECHOS_GENERADORES")).thenReturn(Optional.of(d));

        mvc.perform(get("/auditoria/catalogos/HECHOS_GENERADORES").with(Sesiones.como("ADMIN", "CATALOGO_EDITAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("HECHOS_GENERADORES"))
                .andExpect(jsonPath("$.tablaFisica").value("tarifas.catalogo_hecho_generador"));
    }

    @Test
    @DisplayName("404: consulta de catalogo inexistente responde 404")
    void catalogoNoEncontrado() throws Exception {
        when(casoDeUso.consultarCatalogo("INEXISTENTE")).thenReturn(Optional.empty());

        mvc.perform(get("/auditoria/catalogos/INEXISTENTE").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("200: lista elementos de un catalogo operativo")
    void listarElementosExitoso() throws Exception {
        CatalogoOperativoDetalle d = new CatalogoOperativoDetalle();
        d.setCodigo("HECHOS_GENERADORES");
        when(casoDeUso.consultarCatalogo("HECHOS_GENERADORES")).thenReturn(Optional.of(d));

        ElementoCatalogoOperativo e = new ElementoCatalogoOperativo();
        e.setId("hg-1");
        e.setCatalogoCodigo("HECHOS_GENERADORES");
        e.setCodigo("ENTREGA_FONDO_ACREDITADA");
        e.setNombre("Entrega de fondo acreditada");
        e.setDescripcion("Bolsa entregada al ganador.");
        e.setActivo(true);
        e.setOrden(1);
        e.setAtributos(Map.of("modulo", "11"));
        e.setVigenteDesde(FECHA);
        e.setActualizadoEn(FECHA);
        e.setActualizadoPor("SISTEMA");

        when(casoDeUso.listarElementos(eq("HECHOS_GENERADORES"), any(), any())).thenReturn(List.of(e));

        mvc.perform(get("/auditoria/catalogos/HECHOS_GENERADORES/elementos").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ENTREGA_FONDO_ACREDITADA"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    @DisplayName("200: PATCH actualiza estado de un elemento con motivo y permiso CATALOGO_EDITAR")
    void actualizarEstadoExitoso() throws Exception {
        ElementoCatalogoOperativo actualizado = new ElementoCatalogoOperativo();
        actualizado.setId("hg-1");
        actualizado.setCatalogoCodigo("HECHOS_GENERADORES");
        actualizado.setCodigo("ENTREGA_FONDO_ACREDITADA");
        actualizado.setNombre("Entrega de fondo acreditada");
        actualizado.setDescripcion("Bolsa entregada al ganador.");
        actualizado.setActivo(false);
        actualizado.setOrden(1);
        actualizado.setVigenteDesde(FECHA);
        actualizado.setActualizadoEn(FECHA);
        actualizado.setActualizadoPor("OPERADOR");

        when(casoDeUso.actualizarEstado(eq("HECHOS_GENERADORES"), eq("hg-1"), eq(false), eq("Desactivación temporal por mantenimiento de regla"), any()))
                .thenReturn(Optional.of(actualizado));

        String body = """
                {
                  "activo": false,
                  "motivo": "Desactivación temporal por mantenimiento de regla"
                }
                """;

        mvc.perform(patch("/auditoria/catalogos/HECHOS_GENERADORES/elementos/hg-1")
                        .with(Sesiones.como("OPERADOR", "CATALOGO_EDITAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("400: PATCH sin motivo suficiente es rechazado por validación")
    void actualizarSinMotivoRechaza() throws Exception {
        String body = """
                {
                  "activo": false,
                  "motivo": "no"
                }
                """;

        mvc.perform(patch("/auditoria/catalogos/HECHOS_GENERADORES/elementos/hg-1")
                        .with(Sesiones.como("OPERADOR", "CATALOGO_EDITAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404: PATCH a elemento inexistente devuelve 404")
    void actualizarInexistenteDevuelve404() throws Exception {
        when(casoDeUso.actualizarEstado(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(Optional.empty());

        String body = """
                {
                  "activo": true,
                  "motivo": "Reactivación de elemento operativo"
                }
                """;

        mvc.perform(patch("/auditoria/catalogos/HECHOS_GENERADORES/elementos/elem-999")
                        .with(Sesiones.como("OPERADOR", "CATALOGO_EDITAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
