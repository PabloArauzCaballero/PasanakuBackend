package bo.aportaya.cumplimiento.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Backoffice carril A · H3 · el contrato HTTP de la bandeja, la ficha y la respuesta de CU-52.
 *
 * <p>Dos permisos y no uno: la cola (sin texto ni persona) la ve quien tiene {@code RECLAMO_VER};
 * el texto y la respuesta, solo quien atiende ({@code RECLAMO_ATENDER}). Lo que hace el caso de uso
 * contra la base vive en {@code CU52BandejaTest} y {@code CU52RespuestaTest}.
 */
@PruebaWeb(ReclamosController.class)
class ReclamosBandejaWebTest {

    private static final UUID RECLAMO = UUID.fromString("a3000000-0000-4000-8000-0000000000d1");
    private static final String CLAVE = "a3000000-0000-4000-8000-0000000000ff";
    private static final OffsetDateTime PLAZO = OffsetDateTime.of(2026, 10, 23, 23, 59, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarEfectoDelReclamo efecto;

    @MockitoBean
    private ConsultarBandejaDeReclamos bandeja;

    @MockitoBean
    private CU52AtenderReclamo cu52;

    private static RequestPostProcessor quienVe() {
        return Sesiones.como("OFICIAL_CUMPLIMIENTO", "RECLAMO_VER");
    }

    private static RequestPostProcessor quienAtiende() {
        return Sesiones.como("PUNTO_RECLAMO", "RECLAMO_VER", "RECLAMO_ATENDER");
    }

    private static ConsultarBandejaDeReclamos.Fila fila() {
        return new ConsultarBandejaDeReclamos.Fila(
                RECLAMO,
                "REC-2026-0001",
                "COMISION",
                "BILLETERA",
                "APP",
                Optional.of("18.00"),
                "INGRESADO",
                PLAZO.minusDays(7),
                PLAZO,
                Optional.empty(),
                5,
                PLAZO,
                false,
                Optional.empty());
    }

    @Test
    @DisplayName("200: con RECLAMO_VER la bandeja trae plazos, monto como cadena y cursor, sin texto ni persona")
    void bandejaConPermiso() throws Exception {
        when(bandeja.bandeja(any(), any(), any(), eq(1), any()))
                .thenReturn(new ConsultarBandejaDeReclamos.Pagina(
                        List.of(fila()), Optional.of(PLAZO), Optional.of(RECLAMO)));

        String cuerpo = mvc.perform(get("/reclamos").param("limite", "1").with(quienVe()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reclamoId").value(RECLAMO.toString()))
                .andExpect(jsonPath("$.items[0].montoReclamado").value("18.00"))
                .andExpect(jsonPath("$.items[0].limiteVigente").exists())
                .andExpect(jsonPath("$.items[0].vencido").value(false))
                .andExpect(jsonPath("$.proximoId").value(RECLAMO.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(cuerpo).doesNotContain("descripcion").doesNotContain("usuarioId");
    }

    @Test
    @DisplayName("200: el filtro de estados llega entero al caso de uso, y sin el va vacio (abiertos por omision)")
    void filtroDeEstados() throws Exception {
        when(bandeja.bandeja(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(new ConsultarBandejaDeReclamos.Pagina(List.of(), Optional.empty(), Optional.empty()));

        mvc.perform(get("/reclamos").param("estados", "CERRADO,ELEVADO").with(quienVe()))
                .andExpect(status().isOk());
        verify(bandeja).bandeja(eq(List.of("CERRADO", "ELEVADO")), any(), any(), eq(25), any());
    }

    @Test
    @DisplayName("422: un filtro invalido sale con AP-CU52-07, nunca como 500")
    void filtroInvalido() throws Exception {
        when(bandeja.bandeja(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(52, 7), "Filtro de bandeja invalido."));

        mvc.perform(get("/reclamos").param("estados", "PERDIDO").with(quienVe()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU52-07"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 lo corta el contrato antes del caso de uso")
    void limiteFueraDeRango() throws Exception {
        mvc.perform(get("/reclamos").param("limite", "0").with(quienVe())).andExpect(status().isBadRequest());
        verifyNoInteractions(bandeja);
    }

    @Test
    @DisplayName("403: un participante no ve la cola de reclamos de nadie")
    void participanteNoVe() throws Exception {
        mvc.perform(get("/reclamos").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(bandeja);
    }

    @Test
    @DisplayName("200: la ficha con el texto del reclamo es para quien lo atiende")
    void fichaParaQuienAtiende() throws Exception {
        when(bandeja.ficha(eq(RECLAMO), any()))
                .thenReturn(new ConsultarBandejaDeReclamos.Ficha(
                        fila(),
                        "Me cobraron una comision que no figura en el tarifario.",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));

        mvc.perform(get("/reclamos/{id}", RECLAMO).with(quienAtiende()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Me cobraron una comision que no figura en el tarifario."))
                .andExpect(jsonPath("$.reclamo.codigo").value("REC-2026-0001"));
    }

    @Test
    @DisplayName("403: RECLAMO_VER no alcanza para leer el texto del reclamo")
    void verNoAlcanzaParaElTexto() throws Exception {
        mvc.perform(get("/reclamos/{id}", RECLAMO).with(quienVe())).andExpect(status().isForbidden());
        verifyNoInteractions(bandeja);
    }

    @Test
    @DisplayName("200: responder llega al caso de uso con resultado, texto y devolucion, y devuelve el recibo")
    void responde() throws Exception {
        when(cu52.responder(any(), any()))
                .thenReturn(new CU52AtenderReclamo.SalidaReclamo(RECLAMO, null, PLAZO, 5, "CERRADO"));

        mvc.perform(post("/reclamos/{id}/respuesta", RECLAMO)
                        .with(quienAtiende())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"resultado\":\"DESFAVORABLE\",\"respuesta\":\"Corresponde al tarifario vigente.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reclamoId").value(RECLAMO.toString()))
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.resultado").value("DESFAVORABLE"));
        verify(cu52)
                .responder(
                        eq(new CU52AtenderReclamo.EntradaRespuesta(
                                RECLAMO, "DESFAVORABLE", "Corresponde al tarifario vigente.", null)),
                        any());
    }

    @Test
    @DisplayName("422: otra respuesta sobre un reclamo cerrado sale con AP-CU52-06")
    void yaCerrado() throws Exception {
        when(cu52.responder(any(), any()))
                .thenThrow(
                        new ErrorDeNegocio(CodigoError.de(52, 6), "Ese reclamo ya estaba cerrado con otra respuesta."));

        mvc.perform(post("/reclamos/{id}/respuesta", RECLAMO)
                        .with(quienAtiende())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"PARCIAL\",\"respuesta\":\"Otra respuesta.\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU52-06"));
    }

    @Test
    @DisplayName("400: un resultado que el contrato no declara, o un campo de mas, no llega al caso de uso")
    void cuerpoInvalido() throws Exception {
        mvc.perform(post("/reclamos/{id}/respuesta", RECLAMO)
                        .with(quienAtiende())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"GANADO\",\"respuesta\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/reclamos/{id}/respuesta", RECLAMO)
                        .with(quienAtiende())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"DESFAVORABLE\",\"respuesta\":\"x\",\"responsableId\":\"" + RECLAMO
                                + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu52);
    }

    @Test
    @DisplayName("403: quien solo ve la cola no puede responder")
    void verNoAlcanzaParaResponder() throws Exception {
        mvc.perform(post("/reclamos/{id}/respuesta", RECLAMO)
                        .with(quienVe())
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"DESFAVORABLE\",\"respuesta\":\"Corresponde.\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu52);
    }
}
