package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CU07EjercerDerechos;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-07 · el contrato HTTP de {@code POST /auditoria/derechos}.
 *
 * <p>Lo que se fija aca: que la respuesta le diga al titular <b>qué quedó conservado y
 * con qué base legal</b>. Un derecho de cancelación que contesta «listo» y calla que
 * hay datos retenidos por obligación regulatoria es una respuesta que incumple mientras
 * dice que cumple.
 */
@PruebaWeb(DerechosController.class)
class DerechosControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "a0000000-0000-4000-8000-0000000000ff";

    private static final String CUERPO =
            """
            {"usuarioId":"a0000000-0000-4000-8000-000000000001","tipo":"CANCELACION",
             "descripcion":"Solicito la cancelacion de mis datos personales",
             "ultimaActividad":"2026-01-15"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU07EjercerDerechos cu07;

    private org.springframework.test.web.servlet.ResultActions pedir(String cuerpo) throws Exception {
        return mvc.perform(post("/auditoria/derechos")
                .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("CU-07 · 201 con el plazo legal y lo que quedó retenido por ley")
    void caminoFeliz() throws Exception {
        when(cu07.ejecutar(any(), any()))
                .thenReturn(new CU07EjercerDerechos.SalidaDerechos(
                        SOLICITUD,
                        OffsetDateTime.of(2026, 2, 14, 12, 0, 0, 0, ZoneOffset.UTC),
                        "PARCIAL",
                        List.of("movimientos de billetera · retencion de 10 anios (UIF)"),
                        Optional.empty(),
                        null));

        pedir(CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()))
                .andExpect(jsonPath("$.estado").value("PARCIAL"))
                // El plazo legal es del titular, no interno: si no viaja, nadie de
                // afuera puede reclamar que se venció.
                .andExpect(jsonPath("$.fechaLimiteLegal").exists())
                .andExpect(jsonPath("$.datosRetenidosPorLey[0]").exists());
    }

    @Test
    @DisplayName("CU-07 · el cuerpo llega al caso de uso con el tipo y la fecha exactos")
    void loQueLlegaAlCasoDeUso() throws Exception {
        when(cu07.ejecutar(any(), any()))
                .thenReturn(new CU07EjercerDerechos.SalidaDerechos(
                        SOLICITUD,
                        OffsetDateTime.of(2026, 2, 14, 12, 0, 0, 0, ZoneOffset.UTC),
                        "RECIBIDA",
                        List.of(),
                        Optional.empty(),
                        null));

        pedir(CUERPO).andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU07EjercerDerechos.EntradaDerechos.class);
        verify(cu07).ejecutar(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().usuarioId())
                .isEqualTo(USUARIO);
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().tipo()).isEqualTo("CANCELACION");
        // La fecha decide el plazo legal: un desfase de un día cambia cuándo vence.
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().ultimaActividad())
                .isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @ParameterizedTest(name = "400 · tipo de derecho fuera del contrato: {0}")
    @ValueSource(strings = {"SUPRESION", "borrar", "", "ACCESO_TOTAL"})
    @DisplayName("CU-07 · un derecho que el contrato no enumera muere antes del caso de uso")
    void tipoFueraDelContrato(String tipo) throws Exception {
        pedir(CUERPO.replace("CANCELACION", tipo)).andExpect(status().isBadRequest());
        verifyNoInteractions(cu07);
    }

    @Test
    @DisplayName("CU-07 · 400: una descripción más corta que el mínimo del contrato")
    void descripcionDemasiadoCorta() throws Exception {
        pedir(CUERPO.replace("Solicito la cancelacion de mis datos personales", "borrar"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu07);
    }

    @Test
    @DisplayName("CU-07 · 400: JSON corrupto no llega al caso de uso")
    void jsonCorrupto() throws Exception {
        pedir("{ roto").andExpect(status().isBadRequest());
        verifyNoInteractions(cu07);
    }
}
