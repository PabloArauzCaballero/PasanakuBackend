package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.aplicacion.CU15EmitirExtracto;
import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarSaldo;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /billetera}: la superficie con mas dinero del sistema.
 *
 * <p>Once operaciones y tres permisos distintos, y esa reparticion es la que se fija
 * aca. Operar la billetera propia, autorizar un reverso y bloquear por orden de
 * autoridad son tres cosas que <b>no</b> puede hacer la misma persona con el mismo
 * permiso; que el codigo lo declare no sirve de nada si nadie comprueba que se cumple
 * cuando entra una peticion.
 *
 * <p>Y una que solo existe en este nivel: el extracto de <b>otro</b> se autoriza con un
 * permiso que viene <b>en el token</b>, no en el cuerpo. Un cliente que pudiera pedir
 * «delegado: true» leeria la billetera de cualquiera.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
class BilleteraAprobacionWebTest {
    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    private static final UUID TRANSACCION = UUID.fromString("eeeeeeee-0000-4000-8000-000000000002");
    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarSaldo saldos;

    @MockitoBean
    private CU10RecargarSaldo cu10;

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

    @MockitoBean
    private MovimientosDeLaBilletera movimientos;

    @MockitoBean
    private CotizadorDeComision cotizador;

    @MockitoBean
    private SegundoFactor segundoFactor;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    /**
     * H3.S2 (ADR-049) · POST /billetera/retiros/{ordenId}/aprobacion.
     *
     * <p>Aca solo se comprueba el contrato HTTP: el permiso {@code RETIRO_APROBAR}
     * cierra el paso a nivel de filtro, y el {@code desenlace} del cuerpo decide si el
     * controlador llama a {@code aprobar} o a {@code rechazarRevision}. La segregacion
     * solicitante-distinto-de-aprobador NO se prueba aca — vive en
     * {@code CU11AprobacionTest}, contra la base real, que es donde la base y la
     * aplicacion realmente coinciden (o no).
     */
    @Nested
    @DisplayName("POST /billetera/retiros/{ordenId}/aprobacion")
    class Aprobacion {
        private static final UUID ORDEN = UUID.fromString("eeeeeeee-0000-4000-8000-000000000005");
        private static final UUID APROBADOR = UUID.fromString("eeeeeeee-0000-4000-8000-000000000006");

        @Test
        @DisplayName("403: sin RETIRO_APROBAR, ni siquiera llega al caso de uso")
        void sinElPermisoNoAprueba() throws Exception {
            mvc.perform(
                            post("/billetera/retiros/{id}/aprobacion", ORDEN)
                                    .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"desenlace":"AUTORIZADA"}
                                    """))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu11);
        }

        @Test
        @DisplayName("200 AUTORIZADA: desenlace AUTORIZADA llama a cu11.aprobar, no a rechazarRevision")
        void aprueba() throws Exception {
            when(cu11.aprobar(any(), any()))
                    .thenReturn(new CU11RetirarSaldo.SalidaAprobacion(ORDEN, "AUTORIZADA", APROBADOR));
            mvc.perform(
                            post("/billetera/retiros/{id}/aprobacion", ORDEN)
                                    .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"desenlace":"AUTORIZADA"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ordenRetiroId").value(ORDEN.toString()))
                    .andExpect(jsonPath("$.estado").value("AUTORIZADA"))
                    .andExpect(jsonPath("$.aprobadaPor").value(APROBADOR.toString()));
            verify(cu11).aprobar(eq(ORDEN), any());
            verify(cu11, org.mockito.Mockito.never()).rechazarRevision(any(), any());
        }

        @Test
        @DisplayName("200 RECHAZADA: desenlace RECHAZADA llama a cu11.rechazarRevision, no a aprobar")
        void rechaza() throws Exception {
            when(cu11.rechazarRevision(any(), any()))
                    .thenReturn(new CU11RetirarSaldo.SalidaAprobacion(ORDEN, "RECHAZADA", APROBADOR));
            mvc.perform(
                            post("/billetera/retiros/{id}/aprobacion", ORDEN)
                                    .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"desenlace":"RECHAZADA","motivo":"Documentacion insuficiente"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("RECHAZADA"));
            verify(cu11).rechazarRevision(eq(ORDEN), any());
            verify(cu11, org.mockito.Mockito.never()).aprobar(any(), any());
        }

        @Test
        @DisplayName("422: el caso de uso rechaza la auto-aprobacion, el controlador no la esconde")
        void propagaElErrorDeNegocio() throws Exception {
            when(cu11.aprobar(any(), any()))
                    .thenThrow(new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                            bo.aportaya.plataforma.dominio.CodigoError.de(11, 10),
                            "Quien solicito el retiro no puede aprobar su propia solicitud."));
            mvc.perform(
                            post("/billetera/retiros/{id}/aprobacion", ORDEN)
                                    .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"desenlace":"AUTORIZADA"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU11-10"));
        }

        @Test
        @DisplayName("400: desenlace ausente, que el contrato exige")
        void faltaElDesenlace() throws Exception {
            mvc.perform(post("/billetera/retiros/{id}/aprobacion", ORDEN)
                            .with(Sesiones.como("TESORERIA", "RETIRO_APROBAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu11);
        }
    }
}
