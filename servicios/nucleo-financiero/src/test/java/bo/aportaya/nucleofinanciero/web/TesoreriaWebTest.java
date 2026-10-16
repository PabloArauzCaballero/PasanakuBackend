package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU50ConciliarCustodia;
import bo.aportaya.nucleofinanciero.aplicacion.CU50ConciliarCustodia.SalidaConciliacion;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H8 · {@code /custodia/conciliaciones} y {@code /contabilidad/*}: permiso,
 * forma de la respuesta y que los rechazos lleguen con su codigo, nunca como 500. El recorrido contra
 * la base vive en {@code CU50ConciliacionesTest}, {@code CU51CierresTest} y {@code CU14ReversosTest}.
 */
@PruebaWeb({CustodiaController.class, ContabilidadController.class})
class TesoreriaWebTest {
    private static final UUID CUSTODIA = UUID.fromString("eeeeeeee-0000-4000-8000-000000000050");
    private static final UUID CONCILIACION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000051");
    private static final UUID OPERADOR = UUID.fromString("eeeeeeee-0000-4000-8000-000000000052");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 16);
    private static final OffsetDateTime EN = OffsetDateTime.of(2026, 10, 16, 23, 0, 0, 0, ZoneOffset.UTC);
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000aa";
    private static final String CUERPO =
            """
            {"cuentaCustodiaId":"%s","fecha":"2026-10-16",
             "saldoCustodia":{"monto":"1000.00","moneda":"BOB"},
             "saldoEnTransito":{"monto":"25.00","moneda":"BOB"}}
            """
                    .formatted(CUSTODIA);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarTesoreria tesoreria;

    @MockitoBean
    private CU50ConciliarCustodia cu50;

    @MockitoBean
    private bo.aportaya.nucleofinanciero.aplicacion.CierreDiarioDesdeContabilidad cierres;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("201: TESORERIA concilia y el saldo emitido de la respuesta sale de custodia + transito - diferencia")
    void concilia() throws Exception {
        when(cu50.conciliarDesdeTesoreria(eq(CUSTODIA), eq(FECHA), any(), any(), any()))
                .thenReturn(new SalidaConciliacion(
                        CONCILIACION, new BigDecimal("1.025000"), true, bob("25.00"), "CUADRADA"));

        mvc.perform(post("/custodia/conciliaciones")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO)
                        .with(Sesiones.como("TESORERIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conciliacionId").value(CONCILIACION.toString()))
                .andExpect(jsonPath("$.saldoEmitido.monto").value("1000.00"))
                .andExpect(jsonPath("$.saldoEmitido.moneda").value("BOB"))
                .andExpect(jsonPath("$.diferencia.monto").value("25.00"))
                .andExpect(jsonPath("$.estado").value("CUADRADA"));
    }

    @Test
    @DisplayName("403: AUDITORIA_LEER lee las conciliaciones pero no concilia")
    void auditoriaNoConcilia() throws Exception {
        mvc.perform(post("/custodia/conciliaciones")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO)
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(cu50);
    }

    @Test
    @DisplayName("400: sin Idempotency-Key la conciliacion no llega al caso de uso")
    void sinClave() throws Exception {
        mvc.perform(post("/custodia/conciliaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO)
                        .with(Sesiones.como("TESORERIA")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu50);
    }

    @Test
    @DisplayName("422: otra conciliacion el mismo dia sale con AP-CU50-03")
    void duplicada() throws Exception {
        when(cu50.conciliarDesdeTesoreria(any(), any(), any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(50, 3), "Ya hay una conciliacion."));

        mvc.perform(post("/custodia/conciliaciones")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO)
                        .with(Sesiones.como("TESORERIA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU50-03"));
    }

    @Test
    @DisplayName("200: AUDITORIA_LEER ve las conciliaciones con importes con moneda, ratio y cursor")
    void listaConciliaciones() throws Exception {
        var fila = new ConsultarTesoreria.Conciliacion(
                CONCILIACION,
                CUSTODIA,
                FECHA,
                bob("1000.00"),
                bob("1000.00"),
                bob("0.00"),
                bob("0.00"),
                new BigDecimal("1.000000"),
                true,
                "CUADRADA",
                Optional.of(OPERADOR),
                EN,
                0);
        when(tesoreria.conciliaciones(any(), any(), eq(1), any()))
                .thenReturn(
                        new ConsultarTesoreria.Pagina<>(List.of(fila), Optional.of(FECHA), Optional.of(CONCILIACION)));

        mvc.perform(get("/custodia/conciliaciones")
                        .param("limite", "1")
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].saldoCustodia.monto").value("1000.00"))
                .andExpect(jsonPath("$.items[0].ratioCobertura").value("1.000000"))
                .andExpect(jsonPath("$.items[0].descuadresAbiertos").value(0))
                .andExpect(jsonPath("$.proximoAntesDe").value("2026-10-16"))
                .andExpect(jsonPath("$.proximoAntesDeId").value(CONCILIACION.toString()));
    }

    @Test
    @DisplayName("401: sin token no hay conciliaciones, cierres ni reversos")
    void sinToken() throws Exception {
        mvc.perform(get("/custodia/conciliaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/contabilidad/cierres-diarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/contabilidad/reversos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(tesoreria);
    }

    @Test
    @DisplayName("403: quien opera su billetera no lee cierres ni reversos")
    void participanteNoLee() throws Exception {
        mvc.perform(get("/contabilidad/cierres-diarios").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/contabilidad/reversos").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tesoreria);
    }

    @Test
    @DisplayName("200: los cierres van con importes como cadena SIN moneda (A-7) y sin cursor en la ultima pagina")
    void listaCierres() throws Exception {
        var cierre = new ConsultarTesoreria.Cierre(
                CONCILIACION,
                FECHA,
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                4,
                true,
                OPERADOR,
                EN,
                Optional.empty());
        when(tesoreria.cierres(any(), any(), eq(25), any()))
                .thenReturn(new ConsultarTesoreria.Pagina<>(List.of(cierre), Optional.empty(), Optional.empty()));

        mvc.perform(get("/contabilidad/cierres-diarios").with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].totalRecaudado").value("1000.00"))
                .andExpect(jsonPath("$.items[0].totalExcepciones").value("0.00"))
                .andExpect(jsonPath("$.items[0].cuadrado").value(true))
                .andExpect(jsonPath("$.proximoAntesDeId").doesNotExist());
        verify(tesoreria).cierres(eq(Optional.empty()), eq(Optional.empty()), eq(25), any());
    }

    @Test
    @DisplayName("200: los reversos pasan el filtro de estados al caso de uso y devuelven el monto como cadena")
    void listaReversos() throws Exception {
        var reverso = new ConsultarTesoreria.Reverso(
                CONCILIACION,
                CUSTODIA,
                Optional.empty(),
                OPERADOR,
                "ANULACION",
                "Cobro duplicado por un reintento del proveedor",
                new BigDecimal("40.00"),
                "SOLICITADO",
                EN,
                Optional.empty());
        when(tesoreria.reversos(eq(List.of("SOLICITADO")), any(), any(), eq(25), any()))
                .thenReturn(new ConsultarTesoreria.Pagina<>(List.of(reverso), Optional.empty(), Optional.empty()));

        mvc.perform(get("/contabilidad/reversos")
                        .param("estados", "SOLICITADO")
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].montoReversado").value("40.00"))
                .andExpect(jsonPath("$.items[0].estado").value("SOLICITADO"))
                .andExpect(jsonPath("$.items[0].transaccionReversoId").doesNotExist());
    }

    @Test
    @DisplayName("422: un estado desconocido o un cursor a medias en reversos sale con AP-CU14-04")
    void reversoFiltroInvalido() throws Exception {
        when(tesoreria.reversos(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(14, 4), "Filtro de listado invalido."));

        mvc.perform(get("/contabilidad/reversos")
                        .param("antesDeId", CONCILIACION.toString())
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU14-04"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 lo corta el contrato antes del caso de uso")
    void limiteFueraDeRango() throws Exception {
        mvc.perform(get("/contabilidad/cierres-diarios")
                        .param("limite", "101")
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tesoreria);
    }
}
