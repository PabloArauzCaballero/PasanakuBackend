package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU62Permutar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-62 · permutar el turno con otro participante.
 *
 * <p>Permutar cambia quién cobra primero, así que hay tres hechos que la decisión
 * necesita y que <b>ninguno lo declara quien pide</b>: si el solicitante está al día, si
 * la contraparte lo está, y si el reglamento del grupo lo permite. Los tres se resuelven
 * antes de llamar al caso de uso —dos preguntándole a {@code aportes}, uno consultando
 * el reglamento— y esta clase fija que lleguen así.
 *
 * <p>Si el estado de pagos viniera del cuerpo, cualquiera en mora se adelantaría el
 * turno diciendo que está al día.
 */
@PruebaWeb(
        value = TurnosController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class TurnosControllerWebTest {

    private static final UUID TURNO_ORIGEN = UUID.fromString("c3000000-0000-4000-8000-000000000001");
    private static final UUID TURNO_DESTINO = UUID.fromString("c3000000-0000-4000-8000-000000000002");
    private static final UUID CONTRAPARTE = UUID.fromString("c3000000-0000-4000-8000-000000000003");
    private static final UUID SOLICITUD = UUID.fromString("c3000000-0000-4000-8000-000000000004");
    private static final String CLAVE = "c3000000-0000-4000-8000-0000000000ff";

    private static final String PERMUTA =
            """
            {"turnoOrigenId":"c3000000-0000-4000-8000-000000000001",
             "turnoDestinoId":"c3000000-0000-4000-8000-000000000002",
             "contraparteId":"c3000000-0000-4000-8000-000000000003",
             "motivo":"Me cambia el mes de cobro por un gasto medico"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU62Permutar cu62;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    private static HechosDeOtrosServicios.EstadoDePagos pagos(boolean alDia) {
        return new HechosDeOtrosServicios.EstadoDePagos(
                alDia, new BigDecimal("1000.00"), new BigDecimal("0.00"), new BigDecimal("2000.00"), 2, "BOB");
    }

    private org.springframework.test.web.servlet.ResultActions permutar(String cuerpo) throws Exception {
        return mvc.perform(post("/turnos/permutas")
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("CU-62 · 201 y la permuta nace PENDIENTE: la contraparte todavía no aceptó")
    void permutaNacePendiente() throws Exception {
        when(afuera.estadoDePagos(any())).thenReturn(pagos(true));
        when(consultas.permitePermuta(any(), any())).thenReturn(true);
        when(cu62.solicitar(any(), any())).thenReturn(SOLICITUD);

        permutar(PERMUTA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()))
                // Pedir no es permutar: del otro lado hay alguien que tiene que aceptar.
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("CU-62 · los tres hechos los resuelve el servicio, NO el cuerpo de la petición")
    void losHechosNoLosDeclaraQuienPide() throws Exception {
        when(afuera.estadoDePagos(Sesiones.USUARIO)).thenReturn(pagos(false));
        when(afuera.estadoDePagos(CONTRAPARTE)).thenReturn(pagos(true));
        when(consultas.permitePermuta(eq(TURNO_ORIGEN), any())).thenReturn(false);
        when(cu62.solicitar(any(), any())).thenReturn(SOLICITUD);

        // Se manda "alDia": true en el cuerpo a proposito. Si el controlador lo
        // creyera, un participante en mora se adelantaria el turno solo.
        permutar(PERMUTA.replace("\"motivo\"", "\"alDia\":true,\"loPermiteElReglamento\":true,\"motivo\""))
                .andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU62Permutar.EntradaPermuta.class);
        verify(cu62).solicitar(capturada.capture(), any());
        var entrada = capturada.getValue();

        org.assertj.core.api.Assertions.assertThat(entrada.solicitanteAlDia())
                .as("el estado de pagos lo dice `aportes`, no quien pide la permuta")
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(entrada.contraparteAlDia()).isTrue();
        org.assertj.core.api.Assertions.assertThat(entrada.elReglamentoLoPermite())
                .as("lo permite o no el reglamento del grupo, no el cuerpo")
                .isFalse();
        // Y el solicitante sale del token.
        org.assertj.core.api.Assertions.assertThat(entrada.solicitanteId()).isEqualTo(Sesiones.USUARIO);
        org.assertj.core.api.Assertions.assertThat(entrada.contraparteId()).isEqualTo(CONTRAPARTE);
    }

    @Test
    @DisplayName("CU-62 · se le pregunta a `aportes` por LOS DOS: por quien pide y por la contraparte")
    void sePreguntaPorLosDos() throws Exception {
        when(afuera.estadoDePagos(any())).thenReturn(pagos(true));
        when(consultas.permitePermuta(any(), any())).thenReturn(true);
        when(cu62.solicitar(any(), any())).thenReturn(SOLICITUD);

        permutar(PERMUTA).andExpect(status().isCreated());

        // Mirar solo al que pide dejaria entrar a alguien en mora por la puerta de la
        // contraparte.
        verify(afuera).estadoDePagos(Sesiones.USUARIO);
        verify(afuera).estadoDePagos(CONTRAPARTE);
    }

    @Test
    @DisplayName("CU-62 · 400: sin motivo — una permuta sin razón escrita no se puede auditar")
    void sinMotivo() throws Exception {
        permutar(
                        """
                        {"turnoOrigenId":"c3000000-0000-4000-8000-000000000001",
                         "turnoDestinoId":"c3000000-0000-4000-8000-000000000002",
                         "contraparteId":"c3000000-0000-4000-8000-000000000003"}
                        """)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu62);
        verifyNoInteractions(afuera);
    }

    @Test
    @DisplayName("CU-62 · 400: un turno que no es un identificador")
    void turnoQueNoEsUuid() throws Exception {
        permutar(PERMUTA.replace("c3000000-0000-4000-8000-000000000001", "el-mio"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu62);
    }

    @Test
    @DisplayName("CU-62 · 400: sin Idempotency-Key, dos permutas por un timeout serían dos solicitudes")
    void sinClave() throws Exception {
        mvc.perform(post("/turnos/permutas")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERMUTA))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu62);
    }
}
