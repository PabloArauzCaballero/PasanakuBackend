package bo.aportaya.garantia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo;
import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo;
import bo.aportaya.garantia.aplicacion.CU23ReversarCobertura;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * El contrato HTTP de {@code /garantia/respaldo}: reservas, recuperaciones, liberacion y reversa.
 *
 * <p>Lo que hace el respaldo contra la base se prueba en {@code CU23*RespaldoTest}. Aca se
 * fija lo de la capa web: que el importe viaje como cadena, que el permiso correcto abra
 * la puerta y el equivocado no llegue al caso de uso. Las coberturas del faltante estan en
 * {@link RespaldoCoberturaWebTest}, separada por la regla de tamano de archivo.
 */
@PruebaWeb(GarantiaController.class)
@Import(RespaldoWeb.class)
class RespaldoControllerWebTest extends BaseDeRespaldoWeb {

    private static final UUID RESPONSABLE = UUID.fromString("c3000000-0000-4000-8000-000000000006");

    @Test
    @DisplayName("POST /reservas · 201 con el importe como cadena y la clave de la cabecera hecha clave de operacion")
    void reservaNueva() throws Exception {
        when(reservar.reservar(any(), any()))
                .thenReturn(new CU23ReservarRespaldo.SalidaReserva(RESERVA, bob("8000.00"), bob("2000.00"), true));

        mvc.perform(
                        post("/garantia/respaldo/reservas")
                                .with(Sesiones.como("ADMIN_PLATAFORMA"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"grupoId":"c3000000-0000-4000-8000-000000000001","ciclo":1,
                                 "monto":{"monto":"8000.00","moneda":"BOB"},
                                 "responsableId":"c3000000-0000-4000-8000-000000000006"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservado.monto").value("8000.00"))
                .andExpect(jsonPath("$.capacidadDisponible.monto").value("2000.00"));

        var captor = ArgumentCaptor.forClass(CU23ReservarRespaldo.EntradaReserva.class);
        verify(reservar).reservar(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().clave().valor())
                .isEqualTo("reserva:" + CLAVE);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().responsableId())
                .isEqualTo(RESPONSABLE);
    }

    @Test
    @DisplayName("POST /reservas · 200 si la clave ya habia creado la reserva (reintento)")
    void reservaRepetida() throws Exception {
        when(reservar.reservar(any(), any()))
                .thenReturn(new CU23ReservarRespaldo.SalidaReserva(RESERVA, bob("8000.00"), bob("2000.00"), false));

        mvc.perform(
                        post("/garantia/respaldo/reservas")
                                .with(Sesiones.como("ADMIN_PLATAFORMA"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"grupoId":"c3000000-0000-4000-8000-000000000001","ciclo":1,
                                 "monto":{"monto":"8000.00","moneda":"BOB"},
                                 "responsableId":"c3000000-0000-4000-8000-000000000006"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esNueva").value(false));
    }

    @Test
    @DisplayName("POST /reservas · 400 si falta la clave de idempotencia o si el importe viaja como numero")
    void entradaInvalida() throws Exception {
        mvc.perform(post("/garantia/respaldo/reservas")
                        .with(Sesiones.como("ADMIN_PLATAFORMA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/garantia/respaldo/reservas")
                                .with(Sesiones.como("ADMIN_PLATAFORMA"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"grupoId":"c3000000-0000-4000-8000-000000000001","ciclo":1,
                                 "monto":{"monto":8000.0,"moneda":"BOB"},
                                 "responsableId":"c3000000-0000-4000-8000-000000000006"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservar);
    }

    @Test
    @DisplayName("POST /reservas · 403 si no es tesoreria (un organizador no es caja)")
    void elOrganizadorNoReserva() throws Exception {
        mvc.perform(
                        post("/garantia/respaldo/reservas")
                                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"grupoId":"c3000000-0000-4000-8000-000000000001","ciclo":1,
                                 "monto":{"monto":"8000.00","moneda":"BOB"},
                                 "responsableId":"c3000000-0000-4000-8000-000000000006"}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservar);
    }

    @Test
    @DisplayName("POST /recuperaciones · 200 con el excedente separado de lo que vuelve a la empresa")
    void recuperar() throws Exception {
        when(recuperar.recuperar(any(), any()))
                .thenReturn(new CU23RecuperarRespaldo.SalidaRecuperacion(
                        CU23RecuperarRespaldo.Resultado.RECUPERADA, bob("500.00"), bob("20.00"), bob("500.00"), true));

        mvc.perform(
                        post("/garantia/respaldo/recuperaciones")
                                .with(Sesiones.como("ADMIN_PLATAFORMA"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"pagoId":"c3000000-0000-4000-8000-0000000000aa",
                                 "obligacionId":"c3000000-0000-4000-8000-000000000004",
                                 "pago":{"monto":"520.00","moneda":"BOB"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("RECUPERADA"))
                .andExpect(jsonPath("$.recuperado.monto").value("500.00"))
                .andExpect(jsonPath("$.excedente.monto").value("20.00"));
    }

    @Test
    @DisplayName("POST /liberacion y /reversa · 403 para quien no es tesoreria; 200 para quien si")
    void liberarYReversar() throws Exception {
        when(reservar.liberar(eq(RESERVA), any(), any()))
                .thenReturn(new CU23ReservarRespaldo.SalidaLiberacion(RESERVA, bob("7000.00"), true));
        when(reversar.reversar(any(), any(), any()))
                .thenReturn(new CU23ReversarCobertura.SalidaReversa(TURNO, bob("1000.00"), true));

        mvc.perform(post("/garantia/respaldo/reservas/{id}/liberacion", RESERVA)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isForbidden());
        mvc.perform(post("/garantia/respaldo/reservas/{id}/liberacion", RESERVA)
                        .with(Sesiones.como("ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liberado.monto").value("7000.00"));
        mvc.perform(post("/garantia/respaldo/coberturas/{id}/reversa", TURNO)
                        .with(Sesiones.como("ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revertido.monto").value("1000.00"));
    }
}
