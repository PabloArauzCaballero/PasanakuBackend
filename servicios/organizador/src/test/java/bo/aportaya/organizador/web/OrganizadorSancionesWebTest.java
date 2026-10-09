package bo.aportaya.organizador.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.organizador.aplicacion.CU90PostularOrganizador;
import bo.aportaya.organizador.aplicacion.CU91FirmarContrato;
import bo.aportaya.organizador.aplicacion.CU92EvaluarDesempeno;
import bo.aportaya.organizador.aplicacion.CU93SancionarOrganizador;
import bo.aportaya.organizador.aplicacion.ConsultarHabilitacion;
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
 * CU-93 · sancionar a un organizador, con derecho a apelar.
 *
 * <p>Separado de {@link OrganizadoresControllerWebTest} por la regla de tamano de
 * archivo: habilitar a alguien y sancionarlo son dos temas, y juntos pasaban las
 * trescientas lineas.
 *
 * <p>Lo que se fija aca es el {@code debido-proceso} completo: <b>la sancion publica
 * hasta cuando se puede apelar</b>, <b>quien sanciono no apela</b> y <b>quien apela no
 * resuelve su propia apelacion</b>. Las tres son la diferencia entre una sancion y una
 * decision unilateral.
 */
@PruebaWeb(OrganizadoresController.class)
class OrganizadorSancionesWebTest {

    private static final UUID ORGANIZADOR = UUID.fromString("b8000000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("b8000000-0000-4000-8000-000000000002");
    private static final UUID CONTRATO = UUID.fromString("b8000000-0000-4000-8000-000000000003");
    private static final UUID SANCION = UUID.fromString("b8000000-0000-4000-8000-000000000004");
    private static final UUID KYC = UUID.fromString("b8000000-0000-4000-8000-000000000005");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarHabilitacion habilitaciones;

    @MockitoBean
    private CU90PostularOrganizador cu90;

    @MockitoBean
    private CU91FirmarContrato cu91;

    @MockitoBean
    private CU92EvaluarDesempeno cu92;

    @MockitoBean
    private CU93SancionarOrganizador cu93;

    @Nested
    @DisplayName("CU-93 · sancionar, apelar y resolver")
    class DebidoProceso {

        @Test
        @DisplayName("CU-93 · la sanción dice HASTA CUÁNDO se puede apelar")
        void laSancionPublicaElPlazoDeApelacion() throws Exception {
            when(cu93.sancionar(any(), any()))
                    .thenReturn(new CU93SancionarOrganizador.SalidaSancion(
                            SANCION,
                            "SUSPENSION",
                            "SUSPENDIDO",
                            OffsetDateTime.of(2026, 3, 25, 23, 59, 0, 0, ZoneOffset.UTC)));

            mvc.perform(
                            post("/organizadores/{id}/sanciones", ORGANIZADOR)
                                    .with(Sesiones.como("ADMIN_PLATAFORMA"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"tipo":"SUSPENSION","motivo":"No rindio la entrega del turno 4 en plazo"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("SUSPENSION"))
                    .andExpect(jsonPath("$.estadoDelOrganizador").value("SUSPENDIDO"))
                    // Una sancion sin plazo de apelacion publicado es una sancion sin
                    // defensa: el plazo se vence sin que el afectado sepa que corria.
                    .andExpect(jsonPath("$.puedeApelarHasta").exists());
        }

        @Test
        @DisplayName("CU-93 · 400: una sanción sin motivo escrito")
        void sancionSinMotivo() throws Exception {
            mvc.perform(post("/organizadores/{id}/sanciones", ORGANIZADOR)
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"SUSPENSION\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu93);
        }

        @Test
        @DisplayName("CU-93 · 400: un tipo de sanción que el contrato no enumera")
        void tipoDeSancionFueraDelContrato() throws Exception {
            mvc.perform(post("/organizadores/{id}/sanciones", ORGANIZADOR)
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"MULTA\",\"motivo\":\"por las dudas\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu93);
        }

        @Test
        @DisplayName("CU-93 · apelar es del ORGANIZADOR sancionado, no de quien sancionó")
        void quienSancionaNoApela() throws Exception {
            mvc.perform(post("/organizadores/sanciones/{id}/apelacion", SANCION)
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"argumento\":\"me equivoque\",\"evidenciasJson\":\"[]\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu93);
        }

        @Test
        @DisplayName("CU-93 · el organizador apela y su argumento llega entero")
        void elOrganizadorApela() throws Exception {
            when(cu93.apelar(any(), any()))
                    .thenReturn(new CU93SancionarOrganizador.SalidaApelacion(
                            UUID.fromString("b8000000-0000-4000-8000-00000000000b"), "PRESENTADA", true));

            mvc.perform(
                            post("/organizadores/sanciones/{id}/apelacion", SANCION)
                                    .with(Sesiones.como("ORGANIZADOR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"argumento":"La entrega se hizo el 12, el comprobante esta adjunto",
                                     "evidenciasJson":"[{\\"tipo\\":\\"COMPROBANTE\\"}]"}
                                    """))
                    // 201 y no 200: el contrato declara 200 para esta operacion y el
                    // codigo devuelve 201 cuando la apelacion es nueva. Se fija la
                    // conducta REAL —la que ve el cliente— y la divergencia queda en la
                    // deuda de scripts/verificar_pruebas_web.py.
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("PRESENTADA"));

            var capturada = ArgumentCaptor.forClass(CU93SancionarOrganizador.EntradaApelacion.class);
            verify(cu93).apelar(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().argumento())
                    .isEqualTo("La entrega se hizo el 12, el comprobante esta adjunto");
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().sancionId())
                    .isEqualTo(SANCION);
        }

        @Test
        @DisplayName("CU-93 · resolver la apelación es de la plataforma, no del que apeló")
        void elQueApelaNoResuelve() throws Exception {
            mvc.perform(post("/organizadores/sanciones/{id}/resolucion", SANCION)
                            .with(Sesiones.como("ORGANIZADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu93);
        }
    }
}
