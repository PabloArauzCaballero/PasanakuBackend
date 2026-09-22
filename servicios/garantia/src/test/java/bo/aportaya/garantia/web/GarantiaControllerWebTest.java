package bo.aportaya.garantia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU29DevolverFondo;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.CU67DisolverGrupo;
import bo.aportaya.garantia.dominio.CuadreDeDisolucion;
import bo.aportaya.garantia.dominio.DevolucionDelFondo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
 * CU-29, CU-66 y CU-67 · devolver el fondo, reemplazar un cupo y disolver el grupo.
 *
 * <p>Las tres son operaciones de cierre, y todas terminan repartiendo plata. Lo que la
 * capa web fija es que <b>el reparto viaje completo y cuadrado</b>: quién aportó
 * cuánto, cuánto se devuelve y cuánto se cobra. Un total sin el detalle obliga a cada
 * participante a creer en la suma, y el sentido de un pasanaku es exactamente lo
 * contrario.
 */
@PruebaWeb(GarantiaController.class)
class GarantiaControllerWebTest {

    private static final UUID GRUPO = UUID.fromString("b3000000-0000-4000-8000-000000000001");
    private static final UUID FONDO = UUID.fromString("b3000000-0000-4000-8000-000000000002");
    private static final UUID DISOLUCION = UUID.fromString("b3000000-0000-4000-8000-000000000003");
    private static final UUID REEMPLAZO = UUID.fromString("b3000000-0000-4000-8000-000000000004");
    private static final UUID PARTICIPANTE = UUID.fromString("b3000000-0000-4000-8000-000000000005");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU29DevolverFondo cu29;

    @MockitoBean
    private CU66ReemplazarParticipante cu66;

    @MockitoBean
    private CU67DisolverGrupo cu67;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Nested
    @DisplayName("CU-29 · devolver el fondo de garantía")
    class DevolverFondo {

        @Test
        @DisplayName("CU-29 · el reparto viaja con el detalle por participante, no solo el total")
        void repartoConDetalle() throws Exception {
            when(cu29.devolver(any(), any()))
                    .thenReturn(new CU29DevolverFondo.SalidaDevolucion(
                            FONDO,
                            bob("3000.00"),
                            bob("2650.00"),
                            bob("350.00"),
                            List.of(new DevolucionDelFondo.Devolucion(PARTICIPANTE, bob("1000.00"), bob("883.34")))));

            mvc.perform(post("/garantia/fondos/{id}/devolucion", GRUPO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grupoCerrado\":true,\"deudasVivas\":0}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fondoId").value(FONDO.toString()))
                    .andExpect(jsonPath("$.totalAportado.monto").value("3000.00"))
                    .andExpect(jsonPath("$.totalDevuelto.monto").value("2650.00"))
                    // Lo que el fondo se comio cubriendo incumplimientos: sin este
                    // numero, la diferencia entre lo aportado y lo devuelto no se
                    // explica y parece un faltante.
                    .andExpect(jsonPath("$.consumidoPorCoberturas.monto").value("350.00"))
                    // Y el detalle: cada uno tiene que poder ver SU numero.
                    .andExpect(jsonPath("$.devoluciones[0].participanteId").value(PARTICIPANTE.toString()))
                    .andExpect(jsonPath("$.devoluciones[0].aportado.monto").value("1000.00"))
                    .andExpect(jsonPath("$.devoluciones[0].aDevolver.monto").value("883.34"));
        }

        @Test
        @DisplayName("CU-29 · el estado del grupo y las deudas vivas llegan al caso de uso")
        void loQueLlegaAlCasoDeUso() throws Exception {
            when(cu29.devolver(any(), any()))
                    .thenReturn(new CU29DevolverFondo.SalidaDevolucion(
                            FONDO, bob("0.00"), bob("0.00"), bob("0.00"), List.of()));

            mvc.perform(post("/garantia/fondos/{id}/devolucion", GRUPO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grupoCerrado\":false,\"deudasVivas\":2}"))
                    .andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU29DevolverFondo.EntradaDevolucion.class);
            verify(cu29).devolver(capturada.capture(), any());
            // Devolver el fondo con deudas vivas o con el grupo abierto es lo que el
            // caso de uso tiene que poder rechazar; si estos dos no llegaran, no podria.
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().grupoCerrado())
                    .isFalse();
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().deudasVivas())
                    .isEqualTo(2);
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().grupoId())
                    .isEqualTo(GRUPO);
        }

        @Test
        @DisplayName("CU-29 · 400: sin decir si el grupo está cerrado, que el contrato exige")
        void faltaElEstadoDelGrupo() throws Exception {
            mvc.perform(post("/garantia/fondos/{id}/devolucion", GRUPO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deudasVivas\":0}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu29);
        }
    }

    @Nested
    @DisplayName("CU-67 · disolver el grupo")
    class Disolucion {

        private static final String CUERPO =
                """
                {"causal":"MORA_GENERALIZADA","motivo":"seis de diez cupos en mora por tres periodos",
                 "totalAportado":{"monto":"12000.00","moneda":"BOB"},
                 "totalEntregado":{"monto":"9000.00","moneda":"BOB"},
                 "masaDisponible":{"monto":"3000.00","moneda":"BOB"},
                 "posiciones":[{"participanteId":"b3000000-0000-4000-8000-000000000005",
                                "aportado":{"monto":"1200.00","moneda":"BOB"},
                                "recibido":{"monto":"0.00","moneda":"BOB"}}]}
                """;

        @Test
        @DisplayName("CU-67 · 201 con la masa repartida y la liquidación de cada participante")
        void disolucionCuadrada() throws Exception {
            when(cu67.iniciar(any(), any()))
                    .thenReturn(new CU67DisolverGrupo.SalidaDisolucion(
                            DISOLUCION,
                            "INICIADA",
                            bob("3000.00"),
                            bob("2000.00"),
                            bob("1000.00"),
                            List.of(new CuadreDeDisolucion.Liquidacion(
                                    PARTICIPANTE, bob("1200.00"), bob("0.00"), bob("1200.00"), bob("0.00"))),
                            true));

            mvc.perform(post("/garantia/grupos/{id}/disolucion", GRUPO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.masaARepartir.monto").value("3000.00"))
                    // A devolver y a cobrar por separado: en una disolucion hay quien
                    // recibe y quien todavia debe, y sumarlos escondería a los segundos.
                    .andExpect(jsonPath("$.totalADevolver.monto").value("2000.00"))
                    .andExpect(jsonPath("$.totalACobrar.monto").value("1000.00"))
                    .andExpect(jsonPath("$.liquidaciones[0].aDevolver.monto").value("1200.00"))
                    .andExpect(jsonPath("$.esNueva").value(true));
        }

        @Test
        @DisplayName("CU-67 · iniciar dos veces la misma disolución responde 200, no abre otra")
        void disolucionRepetida() throws Exception {
            when(cu67.iniciar(any(), any()))
                    .thenReturn(new CU67DisolverGrupo.SalidaDisolucion(
                            DISOLUCION, "INICIADA", bob("0.00"), bob("0.00"), bob("0.00"), List.of(), false));

            mvc.perform(post("/garantia/grupos/{id}/disolucion", GRUPO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNueva").value(false));
        }

        @Test
        @DisplayName("CU-67 · 400: una causal de disolución que el contrato no enumera")
        void causalFueraDelContrato() throws Exception {
            mvc.perform(post("/garantia/grupos/{id}/disolucion", GRUPO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO.replace("MORA_GENERALIZADA", "SE_PELEARON")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu67);
        }

        @Test
        @DisplayName("CU-67 · el cierre deja constancia de cuándo se cerró")
        void cierre() throws Exception {
            when(cu67.cerrar(eq(DISOLUCION), any()))
                    .thenReturn(new CU67DisolverGrupo.SalidaCierre(
                            DISOLUCION, "CERRADA", OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC)));

            mvc.perform(post("/garantia/disoluciones/{id}/cierre", DISOLUCION)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CERRADA"))
                    .andExpect(jsonPath("$.cerradaEn").exists());
        }
    }

    @Nested
    @DisplayName("CU-66 · reemplazar un cupo caído")
    class Reemplazo {

        @Test
        @DisplayName("CU-66 · la ejecución dice qué deuda asume el entrante y cuál retiene el saliente")
        void laDeudaNoSeEvapora() throws Exception {
            when(cu66.ejecutar(eq(REEMPLAZO), any()))
                    .thenReturn(new CU66ReemplazarParticipante.SalidaReemplazo(
                            REEMPLAZO, "EJECUTADO", bob("0.00"), bob("700.00"), true));

            mvc.perform(post("/garantia/reemplazos/{id}/ejecucion", REEMPLAZO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EJECUTADO"))
                    // Que entre alguien nuevo no borra lo que debia el que se fue: si
                    // estos dos numeros no viajaran, la deuda desapareceria en el cambio.
                    .andExpect(jsonPath("$.deudaAsumidaPorElEntrante.monto").value("0.00"))
                    .andExpect(jsonPath("$.deudaRetenidaPorElSaliente.monto").value("700.00"));
        }

        @Test
        @DisplayName("CU-66 · aprobar contesta si quedó aprobado, y nada más")
        void aprobar() throws Exception {
            when(cu66.aprobar(eq(REEMPLAZO), any())).thenReturn(true);

            mvc.perform(post("/garantia/reemplazos/{id}/aprobacion", REEMPLAZO)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aprobado").value(true));
        }
    }
}
