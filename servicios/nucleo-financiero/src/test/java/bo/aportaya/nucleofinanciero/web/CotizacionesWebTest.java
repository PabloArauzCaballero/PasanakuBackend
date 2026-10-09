package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.aplicacion.CU15EmitirExtracto;
import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.CuentaPropia;
import bo.aportaya.nucleofinanciero.aplicacion.OrdenesExistentes;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** H11.S1.M1 · El contrato HTTP de la cotizacion previa: lo que la persona ve antes de confirmar. */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@Import(FondeoDeLaBilletera.class)
class CotizacionesWebTest {
    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000b1");
    private static final UUID COTIZACION = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000b2");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";
    private static final String CUERPO = "{\"operacion\":\"RECARGA\",\"cuentaBilleteraId\":\"" + CUENTA
            + "\",\"monto\":{\"monto\":\"500.00\",\"moneda\":\"BOB\"}}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CotizarOperacion cotizaciones;

    @MockitoBean
    private CuentaPropia cuentaPropia;

    @MockitoBean
    private RecargasConProveedor recargas;

    @MockitoBean
    private CU11RetirarSaldo cu11;

    @MockitoBean
    private OrdenesExistentes existentes;

    @MockitoBean
    private SegundoFactor segundoFactor;

    @MockitoBean
    private QrDeLaBilletera qr;

    @MockitoBean
    private MovimientosDeLaBilletera movimientos;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU13RetenerSaldo cu13;

    @MockitoBean
    private CU14ReversarTransaccion cu14;

    @MockitoBean
    private CU15EmitirExtracto cu15;

    @MockitoBean
    private CU17BloquearPorAutoridad cu17;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("200 con base, comision, impuesto, costo total, neto, version y vigencia, todo como cadena decimal")
    void cotizar() throws Exception {
        when(cotizaciones.cotizar(any(), any(), any(), any()))
                .thenReturn(new CotizarOperacion.CotizacionVista(
                        Optional.of(COTIZACION),
                        bob("500.00"),
                        bob("4.00"),
                        bob("1.00"),
                        bob("5.00"),
                        bob("495.00"),
                        Optional.of(OffsetDateTime.of(2026, 10, 8, 15, 0, 0, 0, ZoneOffset.UTC)),
                        false));
        mvc.perform(post("/billetera/cotizaciones")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotizacionId").value(COTIZACION.toString()))
                .andExpect(jsonPath("$.base.monto").value("500.00"))
                .andExpect(jsonPath("$.comision.monto").value("4.00"))
                .andExpect(jsonPath("$.impuesto.monto").value("1.00"))
                .andExpect(jsonPath("$.costoTotal.monto").value("5.00"))
                .andExpect(jsonPath("$.neto.monto").value("495.00"))
                .andExpect(jsonPath("$.gratuita").value(false));
    }

    @Test
    @DisplayName("sin poder cotizar la respuesta es 422 con su codigo: no se muestra un precio inventado")
    void sinPrecio() throws Exception {
        when(cotizaciones.cotizar(any(), any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(10, 5), "No se pudo confirmar el costo."));
        mvc.perform(post("/billetera/cotizaciones")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU10-05"));
    }

    @Test
    @DisplayName(
            "400 sin clave, con clave que no es identificador, con operacion desconocida o con campos no declarados")
    void peticionInvalida() throws Exception {
        var sesion = Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR");
        mvc.perform(post("/billetera/cotizaciones")
                        .with(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/billetera/cotizaciones")
                        .with(sesion)
                        .header("Idempotency-Key", "no-es-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/billetera/cotizaciones")
                        .with(sesion)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("RECARGA", "TRANSFERENCIA")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/billetera/cotizaciones")
                        .with(sesion)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("\"operacion\"", "\"costoPropuesto\":\"0.00\",\"operacion\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cotizaciones);
    }

    @Test
    @DisplayName("sin sesion 401; sin el permiso de operar 403")
    void sinSesionNiPermiso() throws Exception {
        mvc.perform(post("/billetera/cotizaciones")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/billetera/cotizaciones")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cotizaciones);
    }
}
