package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class BilleteraControllerWebTest {
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

    @Nested
    @DisplayName("GET /billetera/{cuentaId}/saldo")
    class Saldo {
        @Test
        @DisplayName("200 con disponible y retenido separados, los dos como cadena decimal")
        void consultarSaldo() throws Exception {
            when(saldos.ejecutar(any(), any()))
                    .thenReturn(new ConsultarSaldo.Salida(
                            CUENTA,
                            bob("1234.56"),
                            bob("200.00"),
                            OffsetDateTime.of(2026, 3, 15, 14, 30, 0, 0, ZoneOffset.UTC)));
            mvc.perform(get("/billetera/{id}/saldo", CUENTA).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cuentaId").value(CUENTA.toString()))
                    // Separados y no sumados: el retenido no se puede gastar, y un
                    // «saldo» unico haria creer que si.
                    .andExpect(jsonPath("$.disponible.monto").value("1234.56"))
                    .andExpect(jsonPath("$.retenido.monto").value("200.00"))
                    .andExpect(jsonPath("$.disponible.moneda").value("BOB"));
        }

        @Test
        @DisplayName("400: un identificador de cuenta que no es un identificador")
        void cuentaQueNoEsUuid() throws Exception {
            mvc.perform(get("/billetera/{id}/saldo", "la-cuenta-de-al-lado")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(saldos);
        }
    }

    @Nested
    @DisplayName("Tres operaciones, tres permisos — y ninguno sirve para el otro")
    class SeparacionDePermisos {
        @Test
        @DisplayName("reversar exige REVERSO_AUTORIZAR: operar la billetera no alcanza")
        void reversarNoSeHaceConElPermisoDeOperar() throws Exception {
            mvc.perform(
                            post("/billetera/reversos")
                                    .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR", "BILLETERA_VER"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"transaccionOriginalId":"eeeeeeee-0000-4000-8000-000000000002",
                                     "tipo":"ERROR_OPERATIVO",
                                     "motivo":"El proveedor cobro dos veces la misma orden",
                                     "autorizadaPor":"eeeeeeee-0000-4000-8000-000000000004"}
                                    """))
                    .andExpect(status().isForbidden());
            // Un reverso mueve dinero al reves. Que lo pueda pedir cualquiera que opera
            // su billetera convertiria el permiso de operar en el de deshacer.
            verifyNoInteractions(cu14);
        }

        @Test
        @DisplayName("bloquear por autoridad exige DATOS_SENSIBLES_LEER, no operar ni reversar")
        void bloquearEsDeCumplimiento() throws Exception {
            mvc.perform(post("/billetera/bloqueos")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR", "REVERSO_AUTORIZAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu17);
        }

        @Test
        @DisplayName("con REVERSO_AUTORIZAR, el reverso pasa y responde 201")
        void reversarConSuPermiso() throws Exception {
            when(cu14.ejecutar(any(), any()))
                    .thenReturn(new CU14ReversarTransaccion.SalidaReverso(
                            UUID.fromString("eeeeeeee-0000-4000-8000-000000000003"), TRANSACCION, true));
            mvc.perform(
                            post("/billetera/reversos")
                                    .with(Sesiones.como("ADMIN_PLATAFORMA", "REVERSO_AUTORIZAR"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"transaccionOriginalId":"eeeeeeee-0000-4000-8000-000000000002",
                                     "tipo":"ERROR_OPERATIVO",
                                     "motivo":"El proveedor cobro dos veces la misma orden",
                                     "autorizadaPor":"eeeeeeee-0000-4000-8000-000000000004"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.generaObligacionDeRestitucion").value(true));
        }
    }

    @Nested
    @DisplayName("GET /billetera/{cuentaId}/extracto — el extracto de otro")
    class Extracto {
        private static CU15EmitirExtracto.SalidaExtracto salida() {
            return new CU15EmitirExtracto.SalidaExtracto(
                    CUENTA,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    bob("1234.56"),
                    12,
                    "0f".repeat(32),
                    true,
                    null);
        }

        private org.springframework.test.web.servlet.ResultActions pedir(
                org.springframework.test.web.servlet.request.RequestPostProcessor sesion) throws Exception {
            return mvc.perform(get("/billetera/{id}/extracto", CUENTA)
                    .param("desde", "2026-03-01")
                    .param("hasta", "2026-03-31")
                    .with(sesion));
        }

        @Test
        @DisplayName("sin BILLETERA_VER_TERCEROS en el token, la peticion NO se marca como delegada")
        void sinElPermisoDelegadoNoEsDelegada() throws Exception {
            when(cu15.emitir(any(), any())).thenReturn(salida());
            pedir(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")).andExpect(status().isOk());
            var capturada = ArgumentCaptor.forClass(CU15EmitirExtracto.EntradaExtracto.class);
            verify(cu15).emitir(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().tienePermisoDelegado())
                    .as("una sesion sin el permiso de terceros no puede pedir el extracto de otro")
                    .isFalse();
        }

        @Test
        @DisplayName("con BILLETERA_VER_TERCEROS en el token, si — y el permiso sale del TOKEN")
        void elPermisoDelegadoSaleDelToken() throws Exception {
            when(cu15.emitir(any(), any())).thenReturn(salida());
            pedir(Sesiones.como("SOPORTE", "BILLETERA_VER", "BILLETERA_VER_TERCEROS"))
                    .andExpect(status().isOk());
            var capturada = ArgumentCaptor.forClass(CU15EmitirExtracto.EntradaExtracto.class);
            verify(cu15).emitir(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().tienePermisoDelegado())
                    .isTrue();
        }

        @Test
        @DisplayName("400: un rango de fechas que no son fechas")
        void fechasInvalidas() throws Exception {
            mvc.perform(get("/billetera/{id}/extracto", CUENTA)
                            .param("desde", "el-mes-pasado")
                            .param("hasta", "2026-03-31")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu15);
        }

        @Test
        @DisplayName("400: falta el rango, que el contrato exige")
        void faltaElRango() throws Exception {
            mvc.perform(get("/billetera/{id}/extracto", CUENTA).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu15);
        }
    }
}
