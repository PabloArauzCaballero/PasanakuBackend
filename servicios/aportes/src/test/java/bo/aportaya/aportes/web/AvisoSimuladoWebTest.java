package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU100EmitirAvisoDePagoSimulado;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Resultado;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code POST /qr/simulador/avisos-de-pago}: forma, 400, 401, 403 y 422. El endpoint no opera sobre un
 * recurso de una persona (la orden es del doble, no de un usuario), asi que la matriz negativa es sin token y roles que no
 * son ADMIN_PLATAFORMA — incluidos los que si operan dinero. Que la ruta NO existe fuera de local/test lo prueba
 * {@code PasarelaSimuladaPerfilTest}.
 */
@PruebaWeb(PasarelaSimuladaController.class)
@ActiveProfiles("test")
class AvisoSimuladoWebTest {

    private static final String RUTA = "/qr/simulador/avisos-de-pago";
    private static final String CUERPO = "{\"referenciaProveedor\":\"SIM-0123456789ABCDEF\"}";
    private static final UUID WEBHOOK = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    private static final UUID PAGO = UUID.fromString("00000000-0000-4000-8000-0000000000a2");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU100EmitirAvisoDePagoSimulado avisos;

    @Test
    @DisplayName("correcto · ADMIN_PLATAFORMA: PROCESADO con webhookId y pagoId")
    void procesado() throws Exception {
        when(avisos.emitir(eq("SIM-0123456789ABCDEF"), any()))
                .thenReturn(new Resultado(Resultado.Estado.PROCESADO, WEBHOOK, PAGO, null));

        mvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO)
                        .with(Sesiones.como("ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PROCESADO"))
                .andExpect(jsonPath("$.webhookId").value(WEBHOOK.toString()))
                .andExpect(jsonPath("$.pagoId").value(PAGO.toString()));
        verify(avisos).emitir(eq("SIM-0123456789ABCDEF"), any());
    }

    @Test
    @DisplayName("limite · repetir devuelve DUPLICADO sin pagoId nuevo; DESCARTADO lleva su motivo")
    void duplicadoYDescartado() throws Exception {
        when(avisos.emitir(eq("SIM-0123456789ABCDEF"), any()))
                .thenReturn(new Resultado(Resultado.Estado.DUPLICADO, WEBHOOK, PAGO, null))
                .thenReturn(
                        new Resultado(Resultado.Estado.DESCARTADO, WEBHOOK, null, "el monto no coincide con el pago"));
        for (String esperado : List.of("DUPLICADO", "DESCARTADO")) {
            mvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO)
                            .with(Sesiones.como("ADMIN_PLATAFORMA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value(esperado));
        }
    }

    @Test
    @DisplayName(
            "invalido · sin sesion 401; roles que no son ADMIN_PLATAFORMA 403 (tambien los que operan dinero); sin tocar el caso de uso")
    void autorizacionNegativa() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        for (var sesion : List.of(
                Sesiones.como("SOPORTE", "SOPORTE"),
                Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER"),
                Sesiones.como("TESORERIA", "BILLETERA_OPERAR"),
                Sesiones.como("PARTICIPANTE", "BILLETERA_VER"),
                Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_OPERAR"),
                Sesiones.sinPermisos())) {
            mvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO)
                            .with(sesion))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(avisos);
    }

    @Test
    @DisplayName(
            "invalido · cuerpo sin referencia, con campo no declarado, con caracteres fuera de lista o demasiado largo: 400 sin tocar el caso de uso")
    void cuerpoInvalido() throws Exception {
        for (String cuerpo : List.of(
                "{}",
                "{\"referenciaProveedor\":\"SIM-1\",\"monto\":\"1.00\"}",
                "{\"referenciaProveedor\":\"SIM 1; DROP\"}",
                "{\"referenciaProveedor\":\"" + "A".repeat(81) + "\"}",
                "{\"referenciaProveedor\":\"\"}")) {
            mvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo)
                            .with(Sesiones.como("ADMIN_PLATAFORMA")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(avisos);
    }

    @Test
    @DisplayName("invalido · orden desconocida o verificacion fallida: 422 con el codigo de la regla, no un 500")
    void reglaDeNegocio() throws Exception {
        when(avisos.emitir(eq("SIM-0123456789ABCDEF"), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(100, 11), "La pasarela simulada no emitio esa orden."));

        mvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO)
                        .with(Sesiones.como("ADMIN_PLATAFORMA")))
                .andExpect(status().isUnprocessableEntity());
    }
}
