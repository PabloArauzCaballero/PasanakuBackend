package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /entregas}: los tres pasos por los que sale el dinero.
 *
 * <p>Lo que hace CU-22 se prueba contra PostgreSQL real en {@code CU22Test}. Lo que se
 * prueba aca es lo otro: que la ruta exista, que el cuerpo se convierta bien en los
 * dos sentidos, que el monto viaje como cadena decimal y no como coma flotante, que
 * una entrada invalida muera <b>antes</b> del caso de uso, y que autorizar y ejecutar
 * exijan permisos distintos — que es como se sostiene R-SEG-04, quien autoriza no
 * ejecuta.
 */
@PruebaWeb(EntregasController.class)
class EntregasControllerWebTest {

    private static final UUID ENTREGA = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000002");

    /** Toda operacion con efecto la exige: reintentar tiene que ser seguro. */
    private static final String CLAVE = "aaaaaaaa-0000-4000-8000-0000000000ff";

    private static final String CUERPO_VALIDO =
            """
            {
              "grupoId": "aaaaaaaa-0000-4000-8000-000000000002",
              "periodoId": "aaaaaaaa-0000-4000-8000-000000000003",
              "turnoId": "aaaaaaaa-0000-4000-8000-000000000004",
              "cupoId": "aaaaaaaa-0000-4000-8000-000000000005",
              "beneficiarioId": "aaaaaaaa-0000-4000-8000-000000000006",
              "bruto": {"monto": "1200.00", "moneda": "BOB"},
              "recaudado": {"monto": "1200.00", "moneda": "BOB"},
              "deducciones": [],
              "metodoDesembolso": "TRANSFERENCIA_BANCARIA",
              "fechaProgramada": "2026-04-01"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU22LiquidarEntrega cu22;

    // El fondeo del pozo tiene su propia prueba (FondeoDelPozoWebTest).
    @MockitoBean
    private CU22EntregarPozoCompleto pozo;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Nested
    @DisplayName("POST /entregas — liquidar")
    class Liquidar {

        @Test
        @DisplayName(
                "F1.L2.M09 · 410 AP-CU22-10: la liquidacion con importes del pedido ya no existe y no llama al caso de uso")
        void liquidacionRetirada() throws Exception {
            mvc.perform(post("/entregas")
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.codigo").value("AP-CU22-10"));
            verifyNoInteractions(cu22);
        }

        @Test
        @DisplayName(
                "F1.L2.M09 · 400: EFECTIVO_ORGANIZADOR ya no es un metodo del contrato (el organizador no es caja)")
        void efectivoAlOrganizadorNoExiste() throws Exception {
            mvc.perform(post("/entregas")
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO.replace("TRANSFERENCIA_BANCARIA", "EFECTIVO_ORGANIZADOR")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu22);
        }

        @Test
        @DisplayName("400: un metodo de desembolso que el contrato no enumera")
        void enumFueraDelContrato() throws Exception {
            mvc.perform(post("/entregas")
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO.replace("TRANSFERENCIA_BANCARIA", "EN_SOBRE_DE_PAPEL")))
                    .andExpect(status().isBadRequest());

            // El caso de uso ni se entera: una entrada que el contrato rechaza no
            // llega a la regla de negocio.
            verifyNoInteractions(cu22);
        }

        @Test
        @DisplayName("400: una fecha que no es una fecha")
        void fechaInvalida() throws Exception {
            mvc.perform(post("/entregas")
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO.replace("2026-04-01", "31/04/2026")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu22);
        }
    }

    @Nested
    @DisplayName("R-SEG-04 — quien autoriza no ejecuta")
    class SegregacionDeFunciones {

        @Test
        @DisplayName("autorizar exige ENTREGA_AUTORIZAR, y ENTREGA_EJECUTAR no alcanza")
        void autorizarNoSeHaceConElPermisoDeEjecutar() throws Exception {
            mvc.perform(post("/entregas/{id}/autorizacion", ENTREGA)
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR")))
                    .andExpect(status().isForbidden());
            verify(cu22, never()).autorizar(any(), any());
        }

        @Test
        @DisplayName("ejecutar exige ENTREGA_EJECUTAR, y ENTREGA_AUTORIZAR no alcanza")
        void ejecutarNoSeHaceConElPermisoDeAutorizar() throws Exception {
            mvc.perform(post("/entregas/{id}/ejecucion", ENTREGA)
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_AUTORIZAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"montoEntregado\":{\"monto\":\"1150.00\",\"moneda\":\"BOB\"}}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu22);
        }

        @Test
        @DisplayName("con el permiso correcto, autorizar responde 200 y dice quien autorizo")
        void autorizarConSuPermiso() throws Exception {
            when(cu22.autorizar(eq(ENTREGA), any()))
                    .thenReturn(new CU22LiquidarEntrega.SalidaAutorizacion(ENTREGA, "AUTORIZADA", Sesiones.USUARIO));

            mvc.perform(post("/entregas/{id}/autorizacion", ENTREGA)
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_AUTORIZAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("AUTORIZADA"))
                    .andExpect(jsonPath("$.autorizadaPor").value(Sesiones.USUARIO.toString()));
        }

        @Test
        @DisplayName(
                "F1.L2.M08 · con el permiso, ejecutar responde 410 AP-CU22-11: la entrega no se cierra con un monto libre")
        void ejecutarRetirado() throws Exception {
            mvc.perform(post("/entregas/{id}/ejecucion", ENTREGA)
                            .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"montoEntregado\":{\"monto\":\"1150.00\",\"moneda\":\"BOB\"}}"))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.codigo").value("AP-CU22-11"));
            verifyNoInteractions(cu22);
        }
    }
}
