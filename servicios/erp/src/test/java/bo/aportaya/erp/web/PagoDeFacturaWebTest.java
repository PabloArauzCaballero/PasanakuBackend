package bo.aportaya.erp.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * CU-103 · El pago de una factura: quien lo autoriza lo dicta el token, no el cuerpo.
 *
 * <p>Amenaza (regla 90.6): R-CTB-05 separa a quien aprueba de quien paga. Si el cuerpo
 * trajera {@code autorizadoPor}, quien aprobo la factura escribiria el identificador de
 * otra persona y se pagaria a si mismo con una peticion hecha a mano. Control: el
 * contrato no declara el campo, la entrada es estricta (rechaza campos de mas con 400) y
 * el caso de uso toma el autorizante de {@code ContextoSesion}, que sale del sujeto del
 * token. Estas pruebas ejercen la capa web real (filtros, deserializador, guardia,
 * manejador de errores); el caso de uso va doblado y se prueba contra PostgreSQL en
 * {@code CU103RechazosTest}.
 */
@PruebaWeb(ErpController.class)
class PagoDeFacturaWebTest {

    private static final UUID FACTURA = UUID.fromString("f0000000-0000-4000-8000-000000000011");
    private static final String CLAVE = "f0000000-0000-4000-8000-0000000000ee";
    private static final String CUERPO = "{\"monto\":\"400.00\",\"moneda\":\"BOB\",\"formaPago\":\"TRANSFERENCIA\"}";

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

    private ResultActions pagar(org.springframework.test.web.servlet.request.RequestPostProcessor sesion, String cuerpo)
            throws Exception {
        return mvc.perform(post("/erp/facturas-de-proveedor/{id}/pagos", FACTURA)
                .with(sesion)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private static CU103FacturaDeProveedor.SalidaFactura parcial() {
        return new CU103FacturaDeProveedor.SalidaFactura(
                FACTURA,
                "PAGADA_PARCIAL",
                new BigDecimal("1000.00"),
                new BigDecimal("400.00"),
                new BigDecimal("600.00"));
    }

    @Test
    @DisplayName("201 · el autorizante que llega al caso de uso es el sujeto del token y el cuerpo no lo trae")
    void elAutorizanteEsElSujetoDelToken() throws Exception {
        when(cu103.pagar(any(), any())).thenReturn(parcial());

        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PAGADA_PARCIAL"))
                .andExpect(jsonPath("$.saldoPendiente").value("600.00"));

        var entrada = ArgumentCaptor.forClass(CU103FacturaDeProveedor.EntradaPago.class);
        var sesion = ArgumentCaptor.forClass(ContextoSesion.class);
        verify(cu103).pagar(entrada.capture(), sesion.capture());
        assertThat(sesion.getValue().usuarioId()).isEqualTo(Sesiones.USUARIO);
        assertThat(entrada.getValue().facturaId()).isEqualTo(FACTURA);
        assertThat(entrada.getValue().monto()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("400 · rechaza `autorizadoPor` en el cuerpo: el cliente no dicta quien autoriza")
    void rechazaAutorizadoPorEnElCuerpo() throws Exception {
        String conAutorizante = "{\"monto\":\"400.00\",\"moneda\":\"BOB\",\"formaPago\":\"TRANSFERENCIA\","
                + "\"autorizadoPor\":\"" + Sesiones.OTRO_USUARIO + "\"}";

        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), conAutorizante)
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("401 · sin token no se paga nada")
    void sinTokenNoSePaga() throws Exception {
        mvc.perform(post("/erp/facturas-de-proveedor/{id}/pagos", FACTURA)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("401 · un token sin rol o con un sujeto que no es un usuario no autoriza pagos")
    void tokenSinSujetoUtilNoAutoriza() throws Exception {
        pagar(Sesiones.sinRol(), CUERPO).andExpect(status().isUnauthorized());
        pagar(Sesiones.conSujetoInvalido(), CUERPO).andExpect(status().isUnauthorized());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("403 · rol insuficiente: ni sin permisos ni con el permiso de otro oficio se paga")
    void rolInsuficiente() throws Exception {
        pagar(Sesiones.sinPermisos(), CUERPO).andExpect(status().isForbidden());
        pagar(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR"), CUERPO)
                .andExpect(status().isForbidden());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("422 · quien aprobo la factura no la paga: R-CTB-05 sale como regla de negocio")
    void mismoActorQueAprobo() throws Exception {
        when(cu103.pagar(any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        CodigoError.de(103, 2),
                        "Quien aprobo la factura no puede ademas autorizar su pago (R-CTB-05)."));

        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), CUERPO)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU103-02"));
    }

    @Test
    @DisplayName("400 · sin Idempotency-Key no se paga, y el caso de uso ni se entera")
    void sinClaveDeIdempotencia() throws Exception {
        mvc.perform(post("/erp/facturas-de-proveedor/{id}/pagos", FACTURA)
                        .with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("201 · la Idempotency-Key de la cabecera llega al caso de uso: el reintento no es un pago nuevo")
    void laClaveDeIdempotenciaLlegaAlCasoDeUso() throws Exception {
        when(cu103.pagar(any(), any())).thenReturn(parcial());

        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), CUERPO).andExpect(status().isCreated());
        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), CUERPO).andExpect(status().isCreated());

        var entrada = ArgumentCaptor.forClass(CU103FacturaDeProveedor.EntradaPago.class);
        verify(cu103, org.mockito.Mockito.times(2)).pagar(entrada.capture(), any());
        assertThat(entrada.getAllValues()).extracting(CU103FacturaDeProveedor.EntradaPago::claveIdempotencia)
                .containsOnly(UUID.fromString(CLAVE));
    }

    @Test
    @DisplayName("422 · la misma clave con otro pago sale como regla de negocio AP-CU103-05")
    void claveReutilizadaConOtroPago() throws Exception {
        when(cu103.pagar(any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        CodigoError.de(103, 5), "Esa clave de idempotencia ya se uso para otro pago de esta factura."));

        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), CUERPO)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU103-05"));
    }

    @Test
    @DisplayName("400 · una Idempotency-Key que no es un UUID no paga nada")
    void claveQueNoEsUuid() throws Exception {
        mvc.perform(post("/erp/facturas-de-proveedor/{id}/pagos", FACTURA)
                        .with(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"))
                        .header("Idempotency-Key", "no-es-un-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("400 · QR no es una forma de pago a proveedor: el contrato la alinea con ck_pago_a_proveedor_forma_pago")
    void qrNoEsFormaDePagoAProveedor() throws Exception {
        pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"),
                        "{\"monto\":\"400.00\",\"moneda\":\"BOB\",\"formaPago\":\"QR\"}")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("201 · las tres formas que admite la base (TRANSFERENCIA, CHEQUE, EFECTIVO) pasan el contrato")
    void formasQueAdmiteLaBase() throws Exception {
        when(cu103.pagar(any(), any())).thenReturn(parcial());

        for (String forma : new String[] {"TRANSFERENCIA", "CHEQUE", "EFECTIVO"}) {
            pagar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"),
                            "{\"monto\":\"400.00\",\"moneda\":\"BOB\",\"formaPago\":\"" + forma + "\"}")
                    .andExpect(status().isCreated());
        }
    }
}
