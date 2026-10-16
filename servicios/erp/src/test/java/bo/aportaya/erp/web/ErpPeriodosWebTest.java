package bo.aportaya.erp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo;
import bo.aportaya.erp.aplicacion.CU101Presupuestar;
import bo.aportaya.erp.aplicacion.CU102AltaDeTercero;
import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor;
import bo.aportaya.erp.aplicacion.CU104CobrarCuenta;
import bo.aportaya.erp.aplicacion.CU105DepreciarActivo;
import bo.aportaya.erp.aplicacion.CU106GenerarEstadoFinanciero;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H4 · el contrato HTTP de {@code GET /erp/ejercicios} y {@code GET /erp/periodos/vigente}.
 *
 * <p>Lo que solo se prueba aca: que cualquiera de los permisos que abren la seccion Contabilidad alcance
 * para elegir periodo (no solo el de reportes), que un participante reciba 403, y que la falta de periodo
 * vigente llegue como AP-CU100-04 y no como un 500 ni como un periodo inventado.
 */
@PruebaWeb(ErpController.class)
class ErpPeriodosWebTest {

    private static final UUID EJERCICIO = UUID.fromString("f0000000-0000-4000-8000-000000000031");
    private static final UUID PERIODO = UUID.fromString("f0000000-0000-4000-8000-000000000032");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU100AbrirCerrarPeriodo cu100;

    @MockitoBean
    private CU101Presupuestar cu101;

    @MockitoBean
    private CU102AltaDeTercero cu102;

    @MockitoBean
    private CU103FacturaDeProveedor cu103;

    @MockitoBean
    private CU104CobrarCuenta cu104;

    @MockitoBean
    private CU105DepreciarActivo cu105;

    @MockitoBean
    private CU106GenerarEstadoFinanciero cu106;

    private static CU100AbrirCerrarPeriodo.PeriodoVigente marzo() {
        return new CU100AbrirCerrarPeriodo.PeriodoVigente(
                PERIODO, EJERCICIO, 2026, 3, "ABIERTO", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("200: con CONTABILIDAD_ERP_REPORTES lista los ejercicios con sus periodos y el cursor por anio")
    void listaEjercicios() throws Exception {
        when(cu100.ejercicios(eq(Optional.empty()), eq(5), any()))
                .thenReturn(new CU100AbrirCerrarPeriodo.PaginaDeEjercicios(
                        List.of(new CU100AbrirCerrarPeriodo.EjercicioConPeriodos(
                                EJERCICIO,
                                2026,
                                "ABIERTO",
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 12, 31),
                                List.of(marzo()))),
                        Optional.of(2026)));

        mvc.perform(get("/erp/ejercicios").with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].anio").value(2026))
                .andExpect(jsonPath("$.items[0].periodos[0].mes").value(3))
                .andExpect(jsonPath("$.items[0].periodos[0].fechaInicio").value("2026-03-01"))
                .andExpect(jsonPath("$.proximoAntesDeAnio").value(2026));
    }

    @Test
    @DisplayName("200: el cursor antesDeAnio llega al caso de uso")
    void cursorPorAnio() throws Exception {
        when(cu100.ejercicios(any(), any(Integer.class), any()))
                .thenReturn(new CU100AbrirCerrarPeriodo.PaginaDeEjercicios(List.of(), Optional.empty()));

        mvc.perform(get("/erp/ejercicios")
                        .param("antesDeAnio", "2026")
                        .param("limite", "2")
                        .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES")))
                .andExpect(status().isOk());
        verify(cu100).ejercicios(eq(Optional.of(2026)), eq(2), any());
    }

    @Test
    @DisplayName("200: quien solo paga facturas tambien puede elegir el periodo (permiso alternativo de la seccion)")
    void permisoAlternativo() throws Exception {
        when(cu100.vigente(any())).thenReturn(marzo());

        mvc.perform(get("/erp/periodos/vigente").with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoId").value(PERIODO.toString()))
                .andExpect(jsonPath("$.estado").value("ABIERTO"));
    }

    @Test
    @DisplayName("422: sin periodo que contenga hoy la respuesta es AP-CU100-04, no un periodo inventado")
    void sinPeriodoVigente() throws Exception {
        when(cu100.vigente(any()))
                .thenThrow(new ErrorDeNegocio(
                        CodigoError.de(100, 4), "Ningun periodo contable contiene la fecha de hoy."));

        mvc.perform(get("/erp/periodos/vigente").with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU100-04"));
    }

    @Test
    @DisplayName("403: un participante no ve la contabilidad de la empresa")
    void participanteNoVe() throws Exception {
        mvc.perform(get("/erp/ejercicios").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/erp/periodos/vigente").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu100);
    }

    @Test
    @DisplayName("401 sin sesion, y 400 si el limite pasa de 20")
    void sinSesionYLimite() throws Exception {
        mvc.perform(get("/erp/periodos/vigente")).andExpect(status().isUnauthorized());
        mvc.perform(get("/erp/ejercicios")
                        .param("limite", "21")
                        .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu100);
    }
}
