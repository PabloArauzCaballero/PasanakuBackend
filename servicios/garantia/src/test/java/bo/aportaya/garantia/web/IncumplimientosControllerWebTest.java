package bo.aportaya.garantia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU23CubrirIncumplimiento;
import bo.aportaya.garantia.aplicacion.CU25DeclararIncumplimiento;
import bo.aportaya.garantia.aplicacion.CU26EjecutarAval;
import bo.aportaya.garantia.aplicacion.CU27RestringirDeudor;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
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
 * CU-23, CU-25, CU-26 y CU-27 · el debido proceso de un incumplimiento.
 *
 * <p>Este controlador implementa el patrón de {@code debido-proceso}: se declara, el
 * afectado <b>descarga</b>, otro resuelve, y recién después vienen la cobertura y la
 * restricción. Lo que la capa web fija de ese patrón:
 *
 * <p><b>Presentar el descargo es del PARTICIPANTE, resolverlo es del organizador.</b> Si
 * el mismo permiso sirviera para los dos, quien declara el incumplimiento podría
 * escribir el descargo y rechazarlo — que es el patrón sin debido proceso.
 *
 * <p><b>La declaración devuelve hasta cuándo se puede descargar.</b> Un plazo que el
 * afectado no ve es un plazo que se le vence sin enterarse.
 *
 * <p><b>Cubrir no perdona.</b> La respuesta trae la deuda que quedó contra quien
 * incumplió; si no viajara, el fondo pagaría y nadie sabría a quién cobrarle.
 */
@PruebaWeb(IncumplimientosController.class)
class IncumplimientosControllerWebTest {

    private static final UUID EXPEDIENTE = UUID.fromString("b2000000-0000-4000-8000-000000000001");
    private static final UUID DESCARGO = UUID.fromString("b2000000-0000-4000-8000-000000000002");
    private static final UUID COBERTURA = UUID.fromString("b2000000-0000-4000-8000-000000000003");
    private static final UUID DEUDA = UUID.fromString("b2000000-0000-4000-8000-000000000004");
    private static final String CLAVE = "b2000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU23CubrirIncumplimiento cu23;

    @MockitoBean
    private CU25DeclararIncumplimiento cu25;

    @MockitoBean
    private CU26EjecutarAval cu26;

    @MockitoBean
    private CU27RestringirDeudor cu27;

    @MockitoBean
    private CU66ReemplazarParticipante cu66;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Nested
    @DisplayName("El debido proceso: declarar, descargar, resolver")
    class DebidoProceso {

        @Test
        @DisplayName("CU-25 · la declaración dice HASTA CUÁNDO se puede descargar")
        void laDeclaracionPublicaElPlazo() throws Exception {
            when(cu25.declarar(any(), any()))
                    .thenReturn(new CU25DeclararIncumplimiento.SalidaDeclaracion(
                            EXPEDIENTE,
                            "EXP-2026-000123",
                            "ABIERTO",
                            OffsetDateTime.of(2026, 3, 25, 23, 59, 0, 0, ZoneOffset.UTC),
                            true));

            mvc.perform(post("/incumplimientos")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(declaracion()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigoExpediente").value("EXP-2026-000123"))
                    // Sin este plazo en la respuesta, al afectado se le vence sin que
                    // pueda saber que corria.
                    .andExpect(jsonPath("$.puedeDescargarHasta").exists())
                    .andExpect(jsonPath("$.esNuevo").value(true));
        }

        @Test
        @DisplayName("CU-25 · declarar dos veces el mismo expediente responde 200, no abre otro")
        void declararDosVecesNoAbreDos() throws Exception {
            when(cu25.declarar(any(), any()))
                    .thenReturn(new CU25DeclararIncumplimiento.SalidaDeclaracion(
                            EXPEDIENTE, "EXP-2026-000123", "ABIERTO", null, false));

            mvc.perform(post("/incumplimientos")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(declaracion()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNuevo").value(false));
        }

        @Test
        @DisplayName("CU-25 · el DESCARGO es del participante: el organizador no lo escribe por él")
        void elOrganizadorNoEscribeElDescargo() throws Exception {
            // Si GRUPO_ADMINISTRAR alcanzara, quien declara podria redactar el descargo
            // del afectado y despues rechazarlo. Eso es el patron sin debido proceso.
            mvc.perform(post("/incumplimientos/{id}/descargo", EXPEDIENTE)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"argumento\":\"no fue asi\",\"evidenciasJson\":\"[]\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu25);
        }

        @Test
        @DisplayName("CU-25 · el participante presenta su descargo y queda registrado")
        void elParticipantePresentaSuDescargo() throws Exception {
            when(cu25.presentarDescargo(any(), any()))
                    .thenReturn(new CU25DeclararIncumplimiento.SalidaDescargo(DESCARGO, "PRESENTADO", true));

            mvc.perform(
                            post("/incumplimientos/{id}/descargo", EXPEDIENTE)
                                    .with(Sesiones.como("PARTICIPANTE"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"argumento":"El pago se hizo el 3 y el banco lo acredito el 6",
                                     "evidenciasJson":"[{\\"tipo\\":\\"COMPROBANTE\\"}]"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.descargoId").value(DESCARGO.toString()))
                    .andExpect(jsonPath("$.estado").value("PRESENTADO"));

            var capturada = ArgumentCaptor.forClass(CU25DeclararIncumplimiento.EntradaDescargo.class);
            verify(cu25).presentarDescargo(capturada.capture(), any());
            // El argumento entero, sin recortar: es la defensa de una persona.
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().argumento())
                    .isEqualTo("El pago se hizo el 3 y el banco lo acredito el 6");
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().expedienteId())
                    .isEqualTo(EXPEDIENTE);
        }

        @Test
        @DisplayName("CU-25 · 400: un descargo sin argumento no es un descargo")
        void descargoSinArgumento() throws Exception {
            mvc.perform(post("/incumplimientos/{id}/descargo", EXPEDIENTE)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"evidenciasJson\":\"[]\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu25);
        }

        @Test
        @DisplayName("CU-25 · RESOLVER el descargo es de quien administra, no del que lo presentó")
        void elParticipanteNoResuelveSuPropioDescargo() throws Exception {
            mvc.perform(post("/incumplimientos/{id}/descargo/resolucion", EXPEDIENTE)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"aceptado\":true,\"resolucion\":\"tiene razon\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu25);
        }

        @Test
        @DisplayName("CU-25 · la resolución dice cómo quedaron el descargo Y el expediente")
        void laResolucionDiceLosDosEstados() throws Exception {
            when(cu25.resolverDescargo(any(), any()))
                    .thenReturn(new CU25DeclararIncumplimiento.SalidaResolucion(
                            DESCARGO, "ACEPTADO", "CERRADO_SIN_SANCION"));

            mvc.perform(post("/incumplimientos/{id}/descargo/resolucion", EXPEDIENTE)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"aceptado\":true,\"resolucion\":\"El comprobante coincide\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estadoDelDescargo").value("ACEPTADO"))
                    // Los dos: aceptar el descargo sin cerrar el expediente dejaria a la
                    // persona absuelta y sancionada a la vez.
                    .andExpect(jsonPath("$.estadoDelExpediente").value("CERRADO_SIN_SANCION"));
        }
    }

    private static String declaracion() {
        return """
                {"codigoExpediente":"EXP-2026-000123",
                 "usuarioId":"b2000000-0000-4000-8000-00000000000a",
                 "participanteId":"b2000000-0000-4000-8000-00000000000b",
                 "grupoId":"b2000000-0000-4000-8000-00000000000c",
                 "periodoId":"b2000000-0000-4000-8000-00000000000d",
                 "cupoId":"b2000000-0000-4000-8000-00000000000e",
                 "obligacionId":"b2000000-0000-4000-8000-00000000000f",
                 "tipo":"APORTE_IMPAGO","severidad":"GRAVE","origenDeteccion":"AUTOMATICO_VENCIMIENTO",
                 "montoInvolucrado":{"monto":"350.00","moneda":"BOB"},
                 "diasMora":15,"afectoALaEntrega":true,
                 "tipoDeEvidencia":"LOG_SISTEMA",
                 "descripcionDeLaEvidencia":"obligacion vencida sin pago",
                 "urlDeLaEvidencia":"https://aportaya.bo/ev/1",
                 "hashDeLaEvidencia":"%s"}
                """
                .formatted("ab".repeat(32));
    }
}
