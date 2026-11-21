package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * F1.L1.M03 · CU-14 con cuatro ojos reales (P-13, R-BIL-27): la persona A pide el reverso y solo otra
 * persona, con su propio token, lo autoriza. Ningun autorizador viaja en el cuerpo.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@org.springframework.test.context.bean.override.mockito.MockitoBean(
        types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class ReversoCuatroOjosWebTest extends BaseDeOperacionesWeb {

    private static final UUID SOLICITUD = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000a1");
    private static final UUID PIDE = Sesiones.USUARIO;
    private static final UUID AUTORIZA = Sesiones.OTRO_USUARIO;
    private static final String CUERPO =
            """
            {"transaccionOriginalId":"eeeeeeee-0000-4000-8000-000000000002",
             "tipo":"ERROR_OPERATIVO",
             "motivo":"El proveedor cobro dos veces la misma orden"}
            """;

    private void pendienteDe(UUID solicitante) {
        when(cu14.solicitadaPor(eq(SOLICITUD), any())).thenReturn(solicitante);
    }

    private org.springframework.test.web.servlet.ResultActions autorizar(
            org.springframework.test.web.servlet.request.RequestPostProcessor quien) throws Exception {
        return mvc.perform(
                post("/billetera/reversos/{id}/autorizacion", SOLICITUD).with(quien));
    }

    @Test
    @DisplayName("un autorizador en el cuerpo se rechaza como campo no declarado (400) y no se registra nada")
    void autorizadorEnElCuerpo() throws Exception {
        mvc.perform(post("/billetera/reversos")
                        .with(Sesiones.enSesion(PIDE, UUID.randomUUID(), "SOPORTE", "REVERSO_AUTORIZAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("\"tipo\"", "\"autorizadaPor\":\"" + AUTORIZA + "\",\n \"tipo\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu14);
    }

    @Test
    @DisplayName("correcto: otra persona, con su token, autoriza la solicitud y el reverso se ejecuta (201)")
    void otraPersonaAutoriza() throws Exception {
        pendienteDe(PIDE);
        when(cu14.autorizar(eq(SOLICITUD), any()))
                .thenReturn(new CU14ReversarTransaccion.SalidaReverso(UUID.randomUUID(), UUID.randomUUID(), false));

        autorizar(Sesiones.enSesion(AUTORIZA, UUID.randomUUID(), "RESPONSABLE_RIESGOS", "REVERSO_AUTORIZAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generaObligacionDeRestitucion").value(false));
        verify(cu14).autorizar(eq(SOLICITUD), any());
    }

    @Test
    @DisplayName("invalido: quien pidio no autoriza su propia solicitud, ni desde otra sesion (422 AP-CU14-02)")
    void mismaPersonaNoAutoriza() throws Exception {
        pendienteDe(PIDE);
        autorizar(Sesiones.enSesion(PIDE, UUID.randomUUID(), "RESPONSABLE_RIESGOS", "REVERSO_AUTORIZAR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU14-02"));
        verify(cu14, never()).autorizar(any(), any());
    }

    @Test
    @DisplayName("invalido: sin REVERSO_AUTORIZAR no se autoriza (403)")
    void sinPermiso() throws Exception {
        autorizar(Sesiones.enSesion(AUTORIZA, UUID.randomUUID(), "PARTICIPANTE", "BILLETERA_OPERAR"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu14);
    }

    @Test
    @DisplayName("invalido: sin token no se autoriza (401)")
    void sinToken() throws Exception {
        mvc.perform(post("/billetera/reversos/{id}/autorizacion", SOLICITUD)).andExpect(status().isUnauthorized());
        verifyNoInteractions(cu14);
    }

    @Test
    @DisplayName("invalido: un token sin sesion (servicio o step-up) no firma (401)")
    void tokenSinSesion() throws Exception {
        pendienteDe(PIDE);
        autorizar(Sesiones.de(AUTORIZA, "RESPONSABLE_RIESGOS", "REVERSO_AUTORIZAR"))
                .andExpect(status().isUnauthorized());
        verify(cu14, never()).autorizar(any(), any());
    }

    @Test
    @DisplayName("limite: autorizar de nuevo una solicitud ya ejecutada responde 422 y no ejecuta otra vez")
    void segundaAutorizacion() throws Exception {
        pendienteDe(PIDE);
        when(cu14.autorizar(eq(SOLICITUD), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(14, 5), "Esa solicitud de reverso ya fue resuelta."));
        autorizar(Sesiones.enSesion(AUTORIZA, UUID.randomUUID(), "RESPONSABLE_RIESGOS", "REVERSO_AUTORIZAR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU14-05"));
    }
}
