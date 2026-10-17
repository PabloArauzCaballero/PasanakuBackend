package bo.aportaya.garantia.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * Tableros · H8.S3.M4 — ampliar la reserva del ciclo (CU-23, contingencia de caja), la operación de
 * {@code /garantia/respaldo} que la matriz mostraba sin prueba web. Solo ADMIN_PLATAFORMA amplía: poner más plata de
 * la empresa detrás de un grupo no es una decisión de quien ejecuta la entrega.
 */
@PruebaWeb(GarantiaController.class)
@Import(RespaldoWeb.class)
class RespaldoAmpliacionWebTest extends BaseDeRespaldoWeb {

    private static final String AMPLIACION = "{\"monto\":{\"monto\":\"200.00\",\"moneda\":\"BOB\"}}";

    @Test
    @DisplayName("ampliación · 200 con lo reservado y la capacidad que queda; la clave viaja al caso de uso")
    void amplia() throws Exception {
        when(reservar.ampliar(eq(RESERVA), any(), any(), any()))
                .thenReturn(new CU23ReservarRespaldo.SalidaAmpliacion(RESERVA, bob("1000.00"), bob("4000.00"), true));

        mvc.perform(post("/garantia/respaldo/reservas/{id}/ampliacion", RESERVA)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AMPLIACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservado.monto").value("1000.00"))
                .andExpect(jsonPath("$.capacidadDisponible.monto").value("4000.00"))
                .andExpect(jsonPath("$.esNueva").value(true));

        var clave = ArgumentCaptor.forClass(ClaveIdempotencia.class);
        verify(reservar).ampliar(eq(RESERVA), eq(bob("200.00")), clave.capture(), any());
        assertThat(clave.getValue().valor()).contains(CLAVE);
    }

    @Test
    @DisplayName(
            "ampliación · 403 con ENTREGA_EJECUTAR y 400 sin Idempotency-Key o sin moneda, sin llegar al caso de uso")
    void rechazos() throws Exception {
        mvc.perform(post("/garantia/respaldo/reservas/{id}/ampliacion", RESERVA)
                        .with(Sesiones.como("TESORERIA", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AMPLIACION))
                .andExpect(status().isForbidden());
        mvc.perform(post("/garantia/respaldo/reservas/{id}/ampliacion", RESERVA)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AMPLIACION))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/garantia/respaldo/reservas/{id}/ampliacion", RESERVA)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":{\"monto\":\"200.00\"}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservar);
    }
}
