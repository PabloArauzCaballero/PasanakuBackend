package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-20, CU-59 y CU-68 · crear el grupo, contar los plazos y postularse.
 *
 * <p>Tres afirmaciones que decide la respuesta HTTP.
 *
 * <p><b>Crear un grupo responde 202, no 201.</b> El grupo queda pedido, no armado: la
 * habilitación del organizador y la licencia las contesta otro servicio. Un {@code 201}
 * prometería un grupo listo para recibir plata.
 *
 * <p><b>El plazo hábil devuelve QUÉ DÍAS se saltearon y por qué.</b> Un vencimiento sin
 * el detalle obliga a creer en la cuenta, y de esa cuenta cuelga una sanción.
 *
 * <p><b>La postulación devuelve el puntaje y sus motivos.</b> Un rechazo sin motivos es
 * un rechazo que no se puede discutir.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class GruposControllerWebTest {

    private static final UUID GRUPO = UUID.fromString("c4000000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("c4000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "c4000000-0000-4000-8000-0000000000ff";

    private static final String GRUPO_NUEVO =
            """
            {"nombre":"Pasanaku del barrio","montoAporte":{"monto":"500.00","moneda":"BOB"},
             "periodicidad":"MENSUAL","cupos":10,"diaCobro":5,
             "modalidadTurnos":"SORTEO_ALEATORIO","fechaDeInicio":"2026-05-01",
             "permitePermutaDeTurnos":true}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU59CalcularPlazo cu59;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private InvitacionesWeb invitaciones;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @MockitoBean
    private RespuestasAOtrosServicios respuestas;

    @MockitoBean
    private SorteoDelGrupo sorteo;

    @Nested
    @DisplayName("CU-20 · crear el grupo")
    class Crear {

        private org.springframework.test.web.servlet.ResultActions crear(String cuerpo) throws Exception {
            return mvc.perform(post("/grupos")
                    .with(Sesiones.como("ORGANIZADOR", "GRUPO_CREAR"))
                    .header("Idempotency-Key", CLAVE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo));
        }

        @Test
        @DisplayName("CU-20 · 202: el grupo queda PEDIDO, no armado")
        void crearGrupo() throws Exception {
            when(cu20.ejecutar(any(), any())).thenReturn(new CU20CrearGrupo.SalidaCreacion(GRUPO, "250.00"));

            crear(GRUPO_NUEVO)
                    // 202 y no 201: la habilitacion del organizador y la licencia las
                    // contesta otro servicio. Un 201 prometeria un grupo listo para
                    // recibir plata.
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.grupoId").value(GRUPO.toString()))
                    // Lo que cada uno va a aportar al fondo de garantia por periodo:
                    // sin esto, nadie sabe cuanto le van a cobrar de mas.
                    .andExpect(jsonPath("$.fondoPorPeriodo").value("250.00"));
        }

        @ParameterizedTest(name = "400 · periodicidad fuera del contrato: {0}")
        @ValueSource(strings = {"DIARIA", "mensual", "CADA_TANTO"})
        @DisplayName("CU-20 · una periodicidad que el contrato no enumera no crea nada")
        void periodicidadFueraDelContrato(String periodicidad) throws Exception {
            crear(GRUPO_NUEVO.replace("MENSUAL", periodicidad)).andExpect(status().isBadRequest());
            verifyNoInteractions(cu20);
        }

        @Test
        @DisplayName("CU-20 · 400: una modalidad de turnos que el contrato no enumera")
        void modalidadFueraDelContrato() throws Exception {
            crear(GRUPO_NUEVO.replace("SORTEO_ALEATORIO", "EL_QUE_PIDA_PRIMERO"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu20);
        }

        @Test
        @DisplayName("CU-20 · 400: sin fecha de inicio, que el contrato exige")
        void sinFechaDeInicio() throws Exception {
            crear(GRUPO_NUEVO.replace("\"fechaDeInicio\":\"2026-05-01\",", "")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu20);
        }

        @Test
        @DisplayName("CU-20 · 400: sin Idempotency-Key — dos grupos por un timeout serían dos grupos")
        void sinClave() throws Exception {
            mvc.perform(post("/grupos")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_CREAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(GRUPO_NUEVO))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu20);
        }
    }

    @Nested
    @DisplayName("CU-59 · contar un plazo hábil")
    class Plazos {

        @Test
        @DisplayName("CU-59 · devuelve la fecha límite Y qué días se saltearon, con su motivo")
        void plazoConDiasSalteados() throws Exception {
            when(cu59.ejecutar(any(), any()))
                    .thenReturn(new CU59CalcularPlazo.SalidaPlazo(
                            LocalDate.of(2026, 3, 20),
                            List.of(
                                    new CU59CalcularPlazo.DiaSalteado(LocalDate.of(2026, 3, 14), "sabado"),
                                    new CU59CalcularPlazo.DiaSalteado(LocalDate.of(2026, 3, 16), "feriado nacional"))));

            mvc.perform(get("/grupos/calendario/calcular")
                            .param("desde", "2026-03-10")
                            .param("dias", "5")
                            .param("alcance", "NACIONAL")
                            .with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fechaLimite").value("2026-03-20"))
                    // El detalle es lo que hace auditable el plazo: de esta cuenta
                    // cuelga una sancion, y sin los dias salteados hay que creerle.
                    .andExpect(jsonPath("$.diasSalteados[0].fecha").value("2026-03-14"))
                    .andExpect(jsonPath("$.diasSalteados[0].descripcion").value("sabado"))
                    .andExpect(jsonPath("$.diasSalteados[1].descripcion").value("feriado nacional"));
        }

        @Test
        @DisplayName("CU-59 · los parámetros llegan al caso de uso tal como se pidieron")
        void loQueLlegaAlCasoDeUso() throws Exception {
            when(cu59.ejecutar(any(), any()))
                    .thenReturn(new CU59CalcularPlazo.SalidaPlazo(LocalDate.of(2026, 3, 20), List.of()));

            mvc.perform(get("/grupos/calendario/calcular")
                            .param("desde", "2026-03-10")
                            .param("dias", "5")
                            .param("alcance", "DEPARTAMENTAL")
                            .with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isOk());

            var capturada = ArgumentCaptor.forClass(CU59CalcularPlazo.EntradaPlazo.class);
            verify(cu59).ejecutar(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().desde())
                    .isEqualTo(LocalDate.of(2026, 3, 10));
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().dias())
                    .isEqualTo(5);
            // El alcance decide QUE calendario de feriados se usa: uno departamental y
            // uno nacional dan fechas distintas.
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().alcance())
                    .isEqualTo("DEPARTAMENTAL");
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().referenciaId())
                    .isEmpty();
        }

        @Test
        @DisplayName("CU-59 · 400: sin `desde`, que el contrato exige")
        void faltaLaFechaDeInicio() throws Exception {
            mvc.perform(get("/grupos/calendario/calcular")
                            .param("dias", "5")
                            .param("alcance", "NACIONAL")
                            .with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu59);
        }

        @Test
        @DisplayName("CU-59 · 400: una fecha que no es una fecha")
        void fechaInvalida() throws Exception {
            mvc.perform(get("/grupos/calendario/calcular")
                            .param("desde", "el lunes")
                            .param("dias", "5")
                            .param("alcance", "NACIONAL")
                            .with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu59);
        }
    }
}
