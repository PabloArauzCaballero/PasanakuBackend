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
import bo.aportaya.nucleofinanciero.aplicacion.QrInterno;
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

/**
 * H4.S1.M5 / M6 · El contrato HTTP de los QR internos: estados, JSON y que un QR de otro sistema
 * de pagos se rechace como tal en vez de procesarse.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@Import(QrDeLaBilletera.class)
class QrControllerWebTest {
    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000a1");
    private static final UUID QR = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000a2");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";
    private static final String CONTENIDO = "PSNK1.AAAAAAAAAAAAAAAAAAAAAA.BBBBBBBBBBBBBBBBBBBBBB";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private QrInterno qrs;

    // El controlador de la billetera trae el resto de sus colaboradores; ninguno interviene en los QR.
    @MockitoBean
    private FondeoDeLaBilletera fondeo;

    @MockitoBean
    private MovimientosDeLaBilletera movimientos;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU11RetirarSaldo cu11;

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
    @DisplayName("emitir un QR dinamico responde 201 con su contenido y vencimiento")
    void emitir() throws Exception {
        when(qrs.emitir(any(), any()))
                .thenReturn(new QrInterno.QrEmitido(
                        QR,
                        CONTENIDO,
                        "DINAMICO",
                        Optional.of(bob("50.00")),
                        Optional.of(OffsetDateTime.of(2026, 10, 8, 15, 0, 0, 0, ZoneOffset.UTC))));
        mvc.perform(post("/billetera/qr")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cuentaBilleteraId\":\"" + CUENTA + "\",\"modalidad\":\"DINAMICO\","
                                + "\"monto\":{\"monto\":\"50.00\",\"moneda\":\"BOB\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value(CONTENIDO))
                .andExpect(jsonPath("$.monto.monto").value("50.00"))
                .andExpect(jsonPath("$.modalidad").value("DINAMICO"));
    }

    @Test
    @DisplayName("leer un QR interno responde 200 con destinatario enmascarado e importe; el contenido va en el cuerpo")
    void leer() throws Exception {
        when(qrs.leer(any(), any()))
                .thenReturn(new QrInterno.QrLeido(
                        QR,
                        "DINAMICO",
                        Optional.of(bob("50.00")),
                        "****1234",
                        Optional.of("almuerzo"),
                        Optional.empty()));
        mvc.perform(post("/billetera/qr/lecturas")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"" + CONTENIDO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clase").value("INTERNO"))
                .andExpect(jsonPath("$.destinatario").value("****1234"))
                .andExpect(jsonPath("$.monto.monto").value("50.00"));
    }

    @Test
    @DisplayName("un QR bancario no es interno: 422 AP-CU12-10, no se afirma interoperabilidad")
    void qrInteroperableSeRechaza() throws Exception {
        when(qrs.leer(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(12, 10), "Este QR es de otro sistema de pagos."));
        mvc.perform(post("/billetera/qr/lecturas")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"00020101021226280012ejemplo\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU12-10"));
    }

    @Test
    @DisplayName("pagar exige la clave de idempotencia, que sea un identificador, y el cuerpo valido")
    void pagarValidaLaPeticion() throws Exception {
        String cuerpo = "{\"contenido\":\"" + CONTENIDO + "\",\"cuentaOrigenId\":\"" + CUENTA + "\"}";
        mvc.perform(post("/billetera/qr/pagos")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/billetera/qr/pagos")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", "no-es-un-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/billetera/qr/pagos")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"" + CONTENIDO + "\",\"campoNoDeclarado\":1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(qrs);
    }

    @Test
    @DisplayName("pagar responde 201 con el comprobante")
    void pagar() throws Exception {
        when(qrs.pagar(any(), any()))
                .thenReturn(new QrInterno.ComprobanteQr(
                        UUID.fromString("eeeeeeee-0000-4000-8000-0000000000a3"),
                        QR,
                        bob("50.00"),
                        "****1234",
                        Optional.empty(),
                        bob("950.00")));
        mvc.perform(post("/billetera/qr/pagos")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"" + CONTENIDO + "\",\"cuentaOrigenId\":\"" + CUENTA + "\","
                                + "\"monto\":{\"monto\":\"50.00\",\"moneda\":\"BOB\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto.monto").value("50.00"))
                .andExpect(jsonPath("$.saldoDespues.monto").value("950.00"));
    }

    @Test
    @DisplayName("sin sesion 401; con sesion pero sin el permiso de operar 403; y el dominio ni se entera")
    void sinSesionNiPermiso() throws Exception {
        mvc.perform(post("/billetera/qr/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"" + CONTENIDO + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/billetera/qr/lecturas")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"" + CONTENIDO + "\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(qrs);
    }
}
