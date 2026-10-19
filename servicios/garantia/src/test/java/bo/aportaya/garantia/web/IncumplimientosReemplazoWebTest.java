package bo.aportaya.garantia.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import bo.aportaya.garantia.aplicacion.ReclamosEnCurso;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tableros · H8.S3.M4 — proponer el reemplazo de un participante incumplidor (CU-66), la operación de
 * {@code /incumplimientos} que la matriz mostraba sin prueba web. La deuda que el entrante no asume queda del saliente:
 * perdonarla se la cobraría a los demás.
 */
@PruebaWeb(IncumplimientosController.class)
class IncumplimientosReemplazoWebTest {
    private static final UUID EXPEDIENTE = UUID.fromString("b2000000-0000-4000-8000-000000000011");
    private static final UUID CUPO = UUID.fromString("b2000000-0000-4000-8000-000000000012");

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

    @MockitoBean
    private ReclamosEnCurso reclamosEnCurso;

    private static final String SIN_CANDIDATO =
            """
            {"cupoId":"%s","entranteId":null,"deudaQueAsumeElEntrante":{"monto":"0.00","moneda":"BOB"},
             "conservaOrdenDeTurno":true}"""
                    .formatted(CUPO);

    @Test
    @DisplayName("reemplazo · 201 BUSCANDO sin candidato todavía: la deuda entera sigue siendo del saliente")
    void sinCandidato() throws Exception {
        when(cu66.proponer(any(), any()))
                .thenReturn(new CU66ReemplazarParticipante.SalidaReemplazo(
                        UUID.randomUUID(),
                        "BUSCANDO",
                        Dinero.de("0.00", Moneda.BOB),
                        Dinero.de("500.00", Moneda.BOB),
                        true));

        mvc.perform(post("/incumplimientos/{id}/reemplazo", EXPEDIENTE)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIN_CANDIDATO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BUSCANDO"))
                .andExpect(jsonPath("$.deudaRetenidaPorElSaliente.monto").value("500.00"));

        var entrada = ArgumentCaptor.forClass(CU66ReemplazarParticipante.EntradaReemplazo.class);
        verify(cu66).proponer(entrada.capture(), any());
        assertThat(entrada.getValue().entranteId()).isNull();
        assertThat(entrada.getValue().expedienteId()).isEqualTo(EXPEDIENTE);
    }

    @Test
    @DisplayName("reemplazo · 403 a un participante y 400 sin conservaOrdenDeTurno, sin llegar al caso de uso")
    void rechazos() throws Exception {
        mvc.perform(post("/incumplimientos/{id}/reemplazo", EXPEDIENTE)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIN_CANDIDATO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/incumplimientos/{id}/reemplazo", EXPEDIENTE)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIN_CANDIDATO.replace(",\n \"conservaOrdenDeTurno\":true", "")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu66);
    }
}
