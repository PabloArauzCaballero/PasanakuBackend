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
import bo.aportaya.erp.aplicacion.CU106DescargarEstadoFinanciero;
import bo.aportaya.erp.aplicacion.CU106GenerarEstadoFinanciero;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Pagina;
import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · A6 · el contrato HTTP de los cinco listados del ERP.
 *
 * <p>Lo que solo se prueba aca: que cada listado lo lea quien opera el modulo, quien tiene reportes y el permiso
 * vecino declarado (Tesoreria ve las facturas que paga), que el resto reciba 403 AP-SEG-01 sin llegar al caso de
 * uso, que los importes salgan como cadena con moneda y que el filtro invalido llegue como 422 del CU.
 */
@PruebaWeb(ErpController.class)
class ListadosDeErpWebTest {

    private static final UUID ID = UUID.fromString("f0000000-0000-4000-8000-000000000061");
    private static final UUID OTRO = UUID.fromString("f0000000-0000-4000-8000-000000000062");

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

    @MockitoBean
    private CU106DescargarEstadoFinanciero descarga;

    @MockitoBean
    private ConsultarListadosDeErp listados;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("200: partidas con importes en cadena y moneda, y el cursor de la pagina llena")
    void partidas() throws Exception {
        var fila = new FilasDeErp.Partida(
                ID,
                OTRO,
                "Marketing 2026",
                "APROBADO",
                OTRO,
                "Centro MKT",
                OTRO,
                OTRO,
                2026,
                3,
                LocalDate.of(2026, 3, 1),
                bob("1000.10"),
                bob("1200.00"));
        when(listados.partidas(eq(Optional.of(OTRO)), any(), any()))
                .thenReturn(new Pagina<>(List.of(fila), Optional.of(LocalDate.of(2026, 3, 1)), Optional.of(ID)));

        mvc.perform(get("/erp/partidas-presupuestarias")
                        .param("ejercicioId", OTRO.toString())
                        .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_PRESUPUESTO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].montoPresupuestado.monto").value("1000.10"))
                .andExpect(jsonPath("$.items[0].montoEjecutado.moneda").value("BOB"))
                .andExpect(jsonPath("$.items[0].mes").value(3))
                .andExpect(jsonPath("$.proximoAntesDe").value("2026-03-01"))
                .andExpect(jsonPath("$.proximoAntesDeId").value(ID.toString()));
    }

    @Test
    @DisplayName("200: ordenes sin descripcion libre; los estados separados por coma y el limite llegan al caso de uso")
    @SuppressWarnings("unchecked")
    void ordenes() throws Exception {
        var fila = new FilasDeErp.Orden(
                ID,
                "OC-1",
                OTRO,
                "Imprenta SRL",
                Optional.empty(),
                Optional.empty(),
                "BORRADOR",
                bob("90.00"),
                OffsetDateTime.parse("2026-03-01T15:00:00Z"));
        when(listados.ordenes(any(), any()))
                .thenReturn(new Pagina<>(List.of(fila), Optional.empty(), Optional.empty()));

        mvc.perform(get("/erp/ordenes-de-compra")
                        .param("estados", "BORRADOR,APROBADA")
                        .param("limite", "10")
                        .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].monto.monto").value("90.00"))
                .andExpect(jsonPath("$.items[0].terceroRazonSocial").value("Imprenta SRL"))
                .andExpect(jsonPath("$.items[0].descripcion").doesNotExist())
                .andExpect(jsonPath("$.proximoAntesDe").doesNotExist());
        ArgumentCaptor<Filtro<OffsetDateTime>> filtro = ArgumentCaptor.forClass(Filtro.class);
        verify(listados).ordenes(filtro.capture(), any());
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().estados()).containsExactly("BORRADOR", "APROBADA");
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().limite()).isEqualTo(10);
    }

    @Test
    @DisplayName(
            "200: Tesoreria (CONTABILIDAD_ERP_PAGAR) ve las facturas que paga, con aprobadaPorLaSesion del servidor")
    void facturasParaTesoreria() throws Exception {
        var fila = new FilasDeErp.Factura(
                ID,
                "F-1",
                OTRO,
                "Imprenta SRL",
                Optional.empty(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "PAGADA_PARCIAL",
                bob("1000.00"),
                bob("400.25"),
                bob("599.75"),
                false,
                Optional.empty());
        when(listados.facturas(any(), any()))
                .thenReturn(new Pagina<>(List.of(fila), Optional.empty(), Optional.empty()));

        mvc.perform(get("/erp/facturas-de-proveedor").with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].saldoPendiente.monto").value("599.75"))
                .andExpect(jsonPath("$.items[0].estado").value("PAGADA_PARCIAL"))
                .andExpect(jsonPath("$.items[0].aprobadaPorLaSesion").value(false));
    }

    @Test
    @DisplayName("200: cuentas por cobrar y activos con su permiso de modulo, y CONTABILIDAD_ERP_REPORTES lee todos")
    void cuentasYActivos() throws Exception {
        var cuenta = new FilasDeErp.CuentaPorCobrar(
                ID,
                "VENTA_SERVICIO",
                OTRO,
                Optional.of(OTRO),
                Optional.of("Cliente SA"),
                LocalDate.of(2026, 6, 30),
                "COBRADA_PARCIAL",
                bob("500.00"),
                bob("120.40"),
                bob("379.60"));
        var activo = new FilasDeErp.Activo(
                ID,
                "INV-1",
                "Equipo de computo",
                "Computacion",
                36,
                LocalDate.of(2026, 2, 10),
                "ACTIVO",
                bob("3600.00"),
                bob("0.00"),
                bob("100.00"),
                bob("3500.00"),
                true);
        when(listados.cuentasPorCobrar(any(), any()))
                .thenReturn(new Pagina<>(List.of(cuenta), Optional.empty(), Optional.empty()));
        when(listados.activos(any(), any()))
                .thenReturn(new Pagina<>(List.of(activo), Optional.empty(), Optional.empty()));
        when(listados.partidas(any(), any(), any()))
                .thenReturn(new Pagina<>(List.of(), Optional.empty(), Optional.empty()));
        when(listados.ordenes(any(), any())).thenReturn(new Pagina<>(List.of(), Optional.empty(), Optional.empty()));
        when(listados.facturas(any(), any())).thenReturn(new Pagina<>(List.of(), Optional.empty(), Optional.empty()));

        mvc.perform(get("/erp/cuentas-por-cobrar").with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_COBRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cobrado.monto").value("120.40"))
                .andExpect(jsonPath("$.items[0].origenTipo").value("VENTA_SERVICIO"));
        mvc.perform(get("/erp/activos").with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_ACTIVOS_FIJOS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].valorEnLibros.monto").value("3500.00"))
                .andExpect(jsonPath("$.items[0].depreciadoEnElPeriodoVigente").value(true));
        for (String ruta : List.of(
                "/erp/partidas-presupuestarias",
                "/erp/ordenes-de-compra",
                "/erp/facturas-de-proveedor",
                "/erp/cuentas-por-cobrar",
                "/erp/activos")) {
            mvc.perform(get(ruta).with(Sesiones.como("AUDITOR_INTERNO", "CONTABILIDAD_ERP_REPORTES")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("403 AP-SEG-01: un participante no lee ningun listado, y quien solo cobra no ve las cuentas por pagar")
    void sinPermiso() throws Exception {
        for (String ruta : List.of(
                "/erp/partidas-presupuestarias",
                "/erp/ordenes-de-compra",
                "/erp/facturas-de-proveedor",
                "/erp/cuentas-por-cobrar",
                "/erp/activos")) {
            mvc.perform(get(ruta).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        }
        mvc.perform(get("/erp/facturas-de-proveedor").with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_COBRAR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        mvc.perform(get("/erp/activos").with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(listados);
    }

    @Test
    @DisplayName("401 sin sesion; 400 con limite fuera de 1..100 o fecha mal formada, sin llegar al caso de uso")
    void sinSesionYMalFormado() throws Exception {
        mvc.perform(get("/erp/activos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/erp/cuentas-por-cobrar")
                        .param("limite", "101")
                        .with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_COBRAR")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/erp/facturas-de-proveedor")
                        .param("antesDe", "31-03-2026")
                        .param("antesDeId", ID.toString())
                        .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listados);
    }

    @Test
    @DisplayName("422: el filtro invalido llega con el codigo del CU (AP-CU105-04), no como 500 ni como lista vacia")
    void filtroInvalido() throws Exception {
        when(listados.activos(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(105, 4), "Filtro de listado invalido."));

        mvc.perform(get("/erp/activos")
                        .param("antesDe", "2026-03-01")
                        .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_ACTIVOS_FIJOS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU105-04"));
    }
}
