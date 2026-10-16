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
import bo.aportaya.plataforma.dominio.ContextoSesion;
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
 * CU-103 · El alta de la factura: quien la aprueba lo dicta el token, no el cuerpo.
 *
 * <p>Amenaza (regla 90.6): R-CTB-05 separa a quien aprueba de quien paga. Si el cuerpo del
 * alta trajera {@code aprobadaPor}, quien registra podria dejar la factura «aprobada por»
 * otra persona (o por nadie que la conozca) y la segregacion dejaria de significar algo.
 * Control: el contrato no declara el campo, la entrada es estricta (400 si llega) y el caso
 * de uso lo toma de {@code ContextoSesion}, que sale del sujeto del token. Estas pruebas
 * ejercen la capa web real; el caso de uso va doblado y la regla se prueba contra
 * PostgreSQL en {@code CU103RechazosTest}.
 */
@PruebaWeb(ErpController.class)
class RegistroDeFacturaWebTest {

    private static final UUID TERCERO = UUID.fromString("f0000000-0000-4000-8000-000000000021");
    private static final UUID FACTURA = UUID.fromString("f0000000-0000-4000-8000-000000000022");
    private static final String CLAVE = "f0000000-0000-4000-8000-0000000000dd";
    private static final String BASE = "\"terceroComercialId\":\"" + TERCERO + "\",\"numeroFactura\":\"F-0001\","
            + "\"fechaEmision\":\"2026-10-01\",\"fechaVencimiento\":\"2026-10-31\",\"monto\":\"1000.00\","
            + "\"moneda\":\"BOB\"";

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
    private bo.aportaya.erp.aplicacion.CU106DescargarEstadoFinanciero descarga;

    private ResultActions registrar(
            org.springframework.test.web.servlet.request.RequestPostProcessor sesion, String cuerpo) throws Exception {
        return mvc.perform(post("/erp/facturas-de-proveedor")
                .with(sesion)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private static CU103FacturaDeProveedor.SalidaFactura aprobada() {
        return new CU103FacturaDeProveedor.SalidaFactura(
                FACTURA, "APROBADA", new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("201 · el aprobador es el sujeto del token: el cuerpo solo dice si aprueba, nunca quien")
    void elAprobadorEsElSujetoDelToken() throws Exception {
        when(cu103.registrar(any(), any())).thenReturn(aprobada());

        registrar(
                        Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR"),
                        "{" + BASE + ",\"aprobar\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        var entrada = ArgumentCaptor.forClass(CU103FacturaDeProveedor.EntradaFactura.class);
        var sesion = ArgumentCaptor.forClass(ContextoSesion.class);
        verify(cu103).registrar(entrada.capture(), sesion.capture());
        assertThat(entrada.getValue().aprobar()).isTrue();
        assertThat(sesion.getValue().usuarioId()).isEqualTo(Sesiones.USUARIO);
    }

    @Test
    @DisplayName("201 · sin `aprobar` la factura se registra sin aprobar")
    void sinAprobarPorOmision() throws Exception {
        when(cu103.registrar(any(), any())).thenReturn(aprobada());

        registrar(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR"), "{" + BASE + "}")
                .andExpect(status().isCreated());

        var entrada = ArgumentCaptor.forClass(CU103FacturaDeProveedor.EntradaFactura.class);
        verify(cu103).registrar(entrada.capture(), any());
        assertThat(entrada.getValue().aprobar()).isFalse();
    }

    @Test
    @DisplayName("400 · rechaza `aprobadaPor` en el cuerpo: el cliente no dicta quien aprueba")
    void rechazaAprobadaPorEnElCuerpo() throws Exception {
        String conAprobador = "{" + BASE + ",\"aprobadaPor\":\"" + Sesiones.OTRO_USUARIO + "\"}";

        registrar(Sesiones.como("CONTABILIDAD", "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR"), conAprobador)
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("401 · sin token no se registra nada")
    void sinTokenNoSeRegistra() throws Exception {
        mvc.perform(post("/erp/facturas-de-proveedor")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + BASE + "}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("401 · un token sin rol o con un sujeto que no es un usuario no registra facturas")
    void tokenSinSujetoUtilNoRegistra() throws Exception {
        registrar(Sesiones.sinRol(), "{" + BASE + "}").andExpect(status().isUnauthorized());
        registrar(Sesiones.conSujetoInvalido(), "{" + BASE + "}").andExpect(status().isUnauthorized());

        verifyNoInteractions(cu103);
    }

    @Test
    @DisplayName("403 · rol insuficiente: ni sin permisos ni con el permiso de pagar se registra una factura")
    void rolInsuficiente() throws Exception {
        registrar(Sesiones.sinPermisos(), "{" + BASE + "}").andExpect(status().isForbidden());
        registrar(Sesiones.como("TESORERIA", "CONTABILIDAD_ERP_PAGAR"), "{" + BASE + "}")
                .andExpect(status().isForbidden());

        verifyNoInteractions(cu103);
    }
}
