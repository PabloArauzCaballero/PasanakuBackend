package bo.aportaya.erp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo;
import bo.aportaya.erp.aplicacion.CU101Presupuestar;
import bo.aportaya.erp.aplicacion.CU102AltaDeTercero;
import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor;
import bo.aportaya.erp.aplicacion.CU104CobrarCuenta;
import bo.aportaya.erp.aplicacion.CU105DepreciarActivo;
import bo.aportaya.erp.aplicacion.CU106GenerarEstadoFinanciero;
import bo.aportaya.erp.dominio.CuadreContable;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /erp}: la contabilidad de la empresa.
 *
 * <p>Quince operaciones y <b>ocho permisos distintos</b>, y esa repartición es el
 * control interno: quien compra no paga, quien paga no cierra el período, quien cierra
 * no emite los estados financieros. Que el código lo declare no vale nada si nadie
 * comprueba que se cumple cuando entra una petición — es exactamente el defecto que la
 * capa web encontró.
 *
 * <p>Y una afirmación sobre el resultado: un estado financiero viaja diciendo <b>si
 * cuadra</b> y con el <b>hash de su contenido</b>. Un balance sin hash no se puede
 * cotejar después contra el que se presentó, y uno que no dice si cuadra obliga a
 * confiar en que sí.
 */
@PruebaWeb(ErpController.class)
class ErpControllerWebTest {

    private static final UUID PERIODO = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final UUID ESTADO = UUID.fromString("f0000000-0000-4000-8000-000000000002");
    private static final UUID EJERCICIO = UUID.fromString("f0000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "f0000000-0000-4000-8000-0000000000ff";

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

    @Nested
    @DisplayName("El ejercicio fiscal y los estados financieros")
    class Contabilidad {

        @Test
        @DisplayName("CU-100 · 201 al abrir el ejercicio, con sus doce períodos")
        void abrirEjercicio() throws Exception {
            when(cu100.abrirEjercicio(eq(2026), any()))
                    .thenReturn(new CU100AbrirCerrarPeriodo.SalidaEjercicio(EJERCICIO, 2026, 12));

            mvc.perform(post("/erp/ejercicios")
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CERRAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\":2026}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ejercicioId").value(EJERCICIO.toString()))
                    .andExpect(jsonPath("$.anio").value(2026))
                    .andExpect(jsonPath("$.periodos").value(12));
        }

        @ParameterizedTest(name = "400 · año fuera del rango del contrato: {0}")
        @ValueSource(ints = {1999, 3000, 0, -2026})
        @DisplayName("CU-100 · un año fuera del rango no abre nada")
        void anioFueraDeRango(int anio) throws Exception {
            mvc.perform(post("/erp/ejercicios")
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CERRAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\":" + anio + "}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu100);
        }

        @Test
        @DisplayName("CU-106 · 201 con «cuadra» y el hash del contenido")
        void estadoFinancieroQueCuadra() throws Exception {
            when(cu106.generar(any(), any(), any()))
                    .thenReturn(new CU106GenerarEstadoFinanciero.SalidaEstado(
                            ESTADO,
                            "BALANCE_GENERAL",
                            new CuadreContable.Estado(
                                    "BALANCE_GENERAL",
                                    List.of(new CuadreContable.Renglon("Activo", new BigDecimal("125000.00"))),
                                    true,
                                    BigDecimal.ZERO),
                            "ab".repeat(32),
                            OffsetDateTime.of(2026, 2, 1, 6, 0, 0, 0, ZoneOffset.UTC)));

            mvc.perform(post("/erp/periodos/{id}/estados-financieros", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"BALANCE_GENERAL\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("BALANCE_GENERAL"))
                    .andExpect(jsonPath("$.cuadra").value(true))
                    // Sin hash no hay forma de cotejar despues contra el que se presento.
                    .andExpect(jsonPath("$.hashContenido").value("ab".repeat(32)));
        }

        @Test
        @DisplayName("CU-106 · un estado que NO cuadra sale igual, diciendo que no cuadra")
        void estadoFinancieroQueNoCuadra() throws Exception {
            when(cu106.generar(any(), any(), any()))
                    .thenReturn(new CU106GenerarEstadoFinanciero.SalidaEstado(
                            ESTADO,
                            "BALANCE_GENERAL",
                            new CuadreContable.Estado("BALANCE_GENERAL", List.of(), false, new BigDecimal("0.03")),
                            "cd".repeat(32),
                            OffsetDateTime.of(2026, 2, 1, 6, 0, 0, 0, ZoneOffset.UTC)));

            // Esconderlo con un error seria peor: el descuadre queda registrado y
            // visible, que es lo que permite ir a buscarlo.
            mvc.perform(post("/erp/periodos/{id}/estados-financieros", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"BALANCE_GENERAL\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cuadra").value(false));
        }

        @Test
        @DisplayName("CU-106 · 400: un tipo de estado financiero que el contrato no enumera")
        void tipoFueraDelContrato() throws Exception {
            mvc.perform(post("/erp/periodos/{id}/estados-financieros", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"FLUJO_DE_CAJA\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu106);
        }
    }

    @Nested
    @DisplayName("Ocho permisos, y ninguno sirve para el trabajo del otro")
    class ControlInterno {

        /** Comprar, pagar, cerrar y reportar son cuatro actos de cuatro personas. */
        @Test
        @DisplayName("quien COMPRA no paga: pagar una factura exige CONTABILIDAD_ERP_PAGAR")
        void quienCompraNoPaga() throws Exception {
            mvc.perform(post("/erp/facturas-de-proveedor/{id}/pagos", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_COMPRAS"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu103);
        }

        @Test
        @DisplayName("quien PAGA no cierra el período: cerrar exige CONTABILIDAD_ERP_CERRAR")
        void quienPagaNoCierra() throws Exception {
            mvc.perform(post("/erp/periodos/{id}/cierre", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_PAGAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu100);
        }

        @Test
        @DisplayName("quien CIERRA no emite los estados financieros")
        void quienCierraNoReporta() throws Exception {
            mvc.perform(post("/erp/periodos/{id}/estados-financieros", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CERRAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"BALANCE_GENERAL\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu106);
        }

        @Test
        @DisplayName("el presupuesto tiene su propio permiso: cobrar no alcanza")
        void cobrarNoPresupuesta() throws Exception {
            mvc.perform(post("/erp/presupuestos")
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_COBRAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu101);
        }

        @Test
        @DisplayName("los activos fijos tienen su propio permiso: reportar no alcanza")
        void reportarNoDeprecia() throws Exception {
            mvc.perform(post("/erp/periodos/{id}/depreciaciones", PERIODO)
                            .with(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_REPORTES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu105);
        }
    }
}
