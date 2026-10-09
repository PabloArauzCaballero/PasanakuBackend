package bo.aportaya.garantia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo;
import bo.aportaya.garantia.aplicacion.CU23CubrirFaltanteDelCorte;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * El contrato HTTP de {@code POST /garantia/respaldo/coberturas}.
 *
 * <p>Separada de {@link RespaldoControllerWebTest} por la regla de tamano de archivo. Se fija
 * que el pedido solo lleve identificadores (el importe lo afirma aportes, no quien pide), que
 * solo tesoreria cubra y que un faltante sin cubrir sea una respuesta de negocio (200) y no
 * una excepcion.
 */
@PruebaWeb(GarantiaController.class)
@Import(RespaldoWeb.class)
class RespaldoCoberturaWebTest extends BaseDeRespaldoWeb {

    private static final UUID GRUPO = UUID.fromString("c3000000-0000-4000-8000-000000000001");
    private static final UUID PERIODO = UUID.fromString("c3000000-0000-4000-8000-000000000003");

    /** Solo identificadores: el importe lo afirma aportes, no quien pide. */
    private static final String COBERTURA_VALIDA =
            """
            {"grupoId":"c3000000-0000-4000-8000-000000000001","periodoId":"c3000000-0000-4000-8000-000000000003",
             "turnoId":"c3000000-0000-4000-8000-000000000002"}
            """;

    @Test
    @DisplayName("POST /coberturas · 200 APLICADA con exposicion; con el permiso de ejecutar la entrega")
    void cubrirAplicada() throws Exception {
        when(cubrir.cubrir(any(), any()))
                .thenReturn(new CU23CubrirConRespaldo.SalidaCobertura(
                        CU23CubrirConRespaldo.Resultado.APLICADA,
                        UUID.randomUUID(),
                        RESERVA,
                        bob("1000.00"),
                        bob("0.00"),
                        bob("7000.00"),
                        bob("1000.00"),
                        true));

        mvc.perform(post("/garantia/respaldo/coberturas")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBERTURA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APLICADA"))
                .andExpect(jsonPath("$.cubierto.monto").value("1000.00"))
                .andExpect(jsonPath("$.exposicionDespues.monto").value("1000.00"));

        var captor = ArgumentCaptor.forClass(CU23CubrirFaltanteDelCorte.Pedido.class);
        verify(cubrir).cubrir(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().turnoId()).isEqualTo(TURNO);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().grupoId()).isEqualTo(GRUPO);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().periodoId())
                .isEqualTo(PERIODO);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().faltanteEsperado())
                .isNull();
    }

    @Test
    @DisplayName("POST /coberturas · INSUFICIENTE es una respuesta de negocio 200, no un error")
    void cubrirInsuficiente() throws Exception {
        when(cubrir.cubrir(any(), any()))
                .thenReturn(new CU23CubrirConRespaldo.SalidaCobertura(
                        CU23CubrirConRespaldo.Resultado.INSUFICIENTE,
                        null,
                        RESERVA,
                        bob("0.00"),
                        bob("200.00"),
                        bob("800.00"),
                        bob("0.00"),
                        true));

        mvc.perform(post("/garantia/respaldo/coberturas")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBERTURA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("INSUFICIENTE"))
                .andExpect(jsonPath("$.sinCubrir.monto").value("200.00"))
                .andExpect(jsonPath("$.coberturaId").doesNotExist());
    }

    @Test
    @DisplayName(
            "POST /coberturas · MONTO INFLADO: pozo, confirmado o lineas en el cuerpo NO llegan al caso de uso (el pedido solo puede llevar identificadores); un faltanteEsperado numerico es 400")
    void elClienteNoDictaImportes() throws Exception {
        when(cubrir.cubrir(any(), any()))
                .thenReturn(new CU23CubrirConRespaldo.SalidaCobertura(
                        CU23CubrirConRespaldo.Resultado.SIN_RESERVA,
                        null,
                        null,
                        bob("0.00"),
                        bob("1000.00"),
                        bob("0.00"),
                        bob("0.00"),
                        true));
        String inflado =
                """
                {"grupoId":"c3000000-0000-4000-8000-000000000001","periodoId":"c3000000-0000-4000-8000-000000000003",
                 "turnoId":"c3000000-0000-4000-8000-000000000002",
                 "pozo":{"monto":"999999.00","moneda":"BOB"},"confirmado":{"monto":"0.00","moneda":"BOB"},
                 "lineas":[{"obligacionId":"c3000000-0000-4000-8000-000000000004","monto":{"monto":"999999.00","moneda":"BOB"}}]}
                """;

        mvc.perform(post("/garantia/respaldo/coberturas")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inflado))
                .andExpect(status().isOk());
        // El tipo del pedido no tiene donde llevar un importe del cliente: solo ids y, opcional, el contraste.
        var captor = ArgumentCaptor.forClass(CU23CubrirFaltanteDelCorte.Pedido.class);
        verify(cubrir).cubrir(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().faltanteEsperado())
                .isNull();

        mvc.perform(post("/garantia/respaldo/coberturas")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBERTURA_VALIDA.replace(
                                "}", ",\"faltanteEsperado\":{\"monto\":1000,\"moneda\":\"BOB\"}}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(
            "POST /coberturas · ROL INSUFICIENTE: participante y organizador reciben 403, sin sesion 401, y el caso de uso no se invoca")
    void soloTesoreria() throws Exception {
        for (var sesion : java.util.List.of(
                Sesiones.como("PARTICIPANTE"),
                Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"),
                Sesiones.como("OPERADOR", "BILLETERA_OPERAR"))) {
            mvc.perform(post("/garantia/respaldo/coberturas")
                            .with(sesion)
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COBERTURA_VALIDA))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/garantia/respaldo/coberturas")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBERTURA_VALIDA))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cubrir);
    }

    @Test
    @DisplayName(
            "POST /coberturas · el faltanteEsperado viaja al caso de uso solo como contraste, y el rechazo de negocio sale 422 con su codigo")
    void faltanteInformadoYRechazo() throws Exception {
        when(cubrir.cubrir(any(), any()))
                .thenThrow(new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                        bo.aportaya.plataforma.dominio.CodigoError.de(23, 13), "El faltante informado no coincide."));

        mvc.perform(post("/garantia/respaldo/coberturas")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBERTURA_VALIDA.replace(
                                "}", ",\"faltanteEsperado\":{\"monto\":\"5000.00\",\"moneda\":\"BOB\"}}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU23-13"));

        var captor = ArgumentCaptor.forClass(CU23CubrirFaltanteDelCorte.Pedido.class);
        verify(cubrir).cubrir(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().faltanteEsperado())
                .isEqualTo(bob("5000.00"));
    }
}
