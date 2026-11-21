package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarCostosDeProveedor;
import bo.aportaya.tarifas.aplicacion.ConsultarParametrosDelGrupo;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarios;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros.ParametroCongelado;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * F3.S8.M6 · {@code GET /tarifas/parametros/grupos/{grupoId}} tal como lo consume {@code garantia} (contrato de
 * L2, §3): 200 con valor como cadena, 404 sin congelamiento, 422 incompleto; y solo la identidad de servicio de
 * {@code garantia} entra. Datos sintéticos.
 */
@PruebaWeb(TarifasController.class)
@Import(ParametrosWeb.class)
@MockitoBean(
        types = {
            ConsultarCostosDeProveedor.class,
            ConsultarTarifarioVigente.class,
            ConsultarTarifarios.class,
            CU34PublicarTarifario.class,
            CU35CerrarLiquidacion.class,
            CU36ResolverPrecio.class,
            CU132RegistroDeParametros.class,
        })
class ParametrosDelGrupoWebTest {

    private static final UUID GRUPO = UUID.fromString("c1320000-0000-4000-8000-0000000000c1");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarParametrosDelGrupo consulta;

    private org.springframework.test.web.servlet.ResultActions pedir(RequestPostProcessor quien) throws Exception {
        return mvc.perform(get("/tarifas/parametros/grupos/{g}", GRUPO).with(quien));
    }

    @Test
    @DisplayName("Correcto · 200 con el valor como cadena decimal, la unidad, el nivel y la versión; sin identidades")
    void completo() throws Exception {
        var version = UUID.randomUUID();
        when(consulta.delGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(List.of(new ParametroCongelado(
                        UUID.randomUUID(),
                        "APORTE_FONDO_GRUPO",
                        Optional.of("NIVEL_A"),
                        new BigDecimal("0.500000"),
                        Optional.of("PORCENTAJE"),
                        Optional.empty(),
                        version,
                        OffsetDateTime.parse("2026-11-21T12:00:00Z")))));

        pedir(Sesiones.como("SERVICIO_GARANTIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoId").value(GRUPO.toString()))
                .andExpect(jsonPath("$.parametros[0].valor").value("0.500000"))
                .andExpect(jsonPath("$.parametros[0].nivel").value("NIVEL_A"))
                .andExpect(jsonPath("$.parametros[0].unidad").value("PORCENTAJE"))
                .andExpect(jsonPath("$.parametros[0].versionId").value(version.toString()))
                .andExpect(jsonPath("$.parametros[0].parametroId").doesNotExist())
                .andExpect(jsonPath("$.parametros[0].propuestoPor").doesNotExist());
    }

    @Test
    @DisplayName("Límite · grupo sin congelamiento: 404 sin cuerpo")
    void sinCongelamiento() throws Exception {
        when(consulta.delGrupo(eq(GRUPO), any())).thenReturn(Optional.empty());

        pedir(Sesiones.como("SERVICIO_GARANTIA"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Inválido · incompleto: 422 AP-CU132-06")
    void incompleto() throws Exception {
        when(consulta.delGrupo(eq(GRUPO), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(132, 6), "faltan: DIAS_DE_GRACIA"));

        pedir(Sesiones.como("SERVICIO_GARANTIA"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU132-06"));
    }

    @Test
    @DisplayName("Inválido · otra identidad de servicio, una persona con permiso de tarifario o sin rol: 403")
    void soloGarantia() throws Exception {
        pedir(Sesiones.como("SERVICIO_ENTREGAS")).andExpect(status().isForbidden());
        pedir(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_ADMINISTRAR")).andExpect(status().isForbidden());
        pedir(Sesiones.sinPermisos()).andExpect(status().isForbidden());
        verifyNoInteractions(consulta);
    }

    @Test
    @DisplayName("Inválido · sin token: 401")
    void sinToken() throws Exception {
        mvc.perform(get("/tarifas/parametros/grupos/{g}", GRUPO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consulta);
    }
}
