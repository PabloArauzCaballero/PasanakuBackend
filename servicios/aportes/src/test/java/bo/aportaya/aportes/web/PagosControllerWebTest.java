package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago;
import bo.aportaya.aportes.aplicacion.CU99EnrutarProveedor;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /pagos}: devolver dinero, disputarlo y elegir por donde
 * cobrar.
 *
 * <p>Cuatro operaciones y cuatro permisos <b>distintos</b>, y esa es la afirmacion que
 * se prueba: pedir un reembolso, aprobarlo, registrar una disputa y dar de alta un
 * proveedor de pago son actos de cuatro personas diferentes. Que quien pide un
 * reembolso pueda aprobarlo es la definicion de no tener control interno, y sin la
 * guardia funcionando la anotacion no lo impedia.
 */
@PruebaWeb(PagosController.class)
class PagosControllerWebTest {

    private static final UUID PAGO = UUID.fromString("ffffffff-0000-4000-8000-000000000001");
    private static final UUID REEMBOLSO = UUID.fromString("ffffffff-0000-4000-8000-000000000002");
    private static final UUID DISPUTA = UUID.fromString("ffffffff-0000-4000-8000-000000000003");
    private static final String CLAVE = "ffffffff-0000-4000-8000-0000000000ff";

    private static final String REEMBOLSO_PEDIDO =
            """
            {"monto":{"monto":"350.00","moneda":"BOB"},"motivo":"DUPLICADO"}
            """;

    private static final String DISPUTA_PEDIDA =
            """
            {"tipo":"CONTRACARGO","descripcion":"El titular desconoce el cargo",
             "montoDisputado":{"monto":"350.00","moneda":"BOB"},
             "referenciaDelProveedor":"REF-000123"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU19ReembolsarPago cu19;

    @MockitoBean
    private CU99EnrutarProveedor cu99;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Nested
    @DisplayName("Pedir un reembolso y aprobarlo son dos actos de dos personas")
    class SegregacionDelReembolso {

        @Test
        @DisplayName("201 al pedirlo, con lo que queda disponible del pago")
        void pedirReembolso() throws Exception {
            when(cu19.solicitar(any(), any()))
                    .thenReturn(new CU19ReembolsarPago.SalidaSolicitud(REEMBOLSO, "SOLICITADO", bob("0.00")));

            mvc.perform(post("/pagos/{id}/reembolsos", PAGO)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REEMBOLSO_PEDIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reembolsoId").value(REEMBOLSO.toString()))
                    .andExpect(jsonPath("$.estado").value("SOLICITADO"))
                    .andExpect(jsonPath("$.disponibleRestante.monto").value("0.00"));
        }

        @Test
        @DisplayName("quien lo pidio NO puede aprobarlo: aprobar exige REVERSO_AUTORIZAR")
        void quienPideNoAprueba() throws Exception {
            mvc.perform(post("/pagos/reembolsos/{id}/aprobacion", REEMBOLSO)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu19);
        }

        @Test
        @DisplayName("con REVERSO_AUTORIZAR, la aprobacion responde 200 y devuelve la obligacion tocada")
        void aprobarConSuPermiso() throws Exception {
            when(cu19.aprobar(eq(REEMBOLSO), any()))
                    .thenReturn(new CU19ReembolsarPago.SalidaEjecucion(REEMBOLSO, "EJECUTADO", PAGO, "PENDIENTE"));

            mvc.perform(post("/pagos/reembolsos/{id}/aprobacion", REEMBOLSO)
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "REVERSO_AUTORIZAR"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EJECUTADO"))
                    // Devolver dinero reabre la obligacion: si no se dijera, el
                    // participante quedaria al dia sin haber pagado.
                    .andExpect(jsonPath("$.estadoObligacion").value("PENDIENTE"));
        }

        @Test
        @DisplayName("400: un motivo de reembolso que el contrato no enumera")
        void motivoFueraDelContrato() throws Exception {
            mvc.perform(post("/pagos/{id}/reembolsos", PAGO)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REEMBOLSO_PEDIDO.replace("DUPLICADO", "PORQUE_SI")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu19);
        }
    }

    @Nested
    @DisplayName("POST /pagos/{id}/disputas — es de soporte, y es idempotente")
    class Disputas {

        @Test
        @DisplayName("201 la primera vez, 200 cuando la disputa ya estaba abierta")
        void registrarDisputaEsIdempotente() throws Exception {
            var limite = OffsetDateTime.of(2026, 4, 15, 12, 0, 0, 0, ZoneOffset.UTC);

            when(cu19.registrarDisputa(any(), any()))
                    .thenReturn(new CU19ReembolsarPago.SalidaDisputa(DISPUTA, limite, true));
            mvc.perform(post("/pagos/{id}/disputas", PAGO)
                            .with(Sesiones.como("SOPORTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DISPUTA_PEDIDA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.esNueva").value(true))
                    .andExpect(jsonPath("$.disputaId").value(DISPUTA.toString()));

            when(cu19.registrarDisputa(any(), any()))
                    .thenReturn(new CU19ReembolsarPago.SalidaDisputa(DISPUTA, limite, false));
            mvc.perform(post("/pagos/{id}/disputas", PAGO)
                            .with(Sesiones.como("SOPORTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DISPUTA_PEDIDA))
                    // Registrar dos veces la misma disputa no abre dos: el segundo
                    // intento devuelve la que hay, y el estado lo dice.
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNueva").value(false))
                    .andExpect(jsonPath("$.disputaId").value(DISPUTA.toString()));
        }

        @Test
        @DisplayName("un participante no registra disputas: es un acto de soporte")
        void elParticipanteNoRegistraDisputas() throws Exception {
            mvc.perform(post("/pagos/{id}/disputas", PAGO)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DISPUTA_PEDIDA))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu19);
        }

        @Test
        @DisplayName("400: falta la referencia del proveedor, que el contrato exige")
        void faltaLaReferencia() throws Exception {
            mvc.perform(
                            post("/pagos/{id}/disputas", PAGO)
                                    .with(Sesiones.como("SOPORTE"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"tipo":"CONTRACARGO","descripcion":"x",
                                     "montoDisputado":{"monto":"350.00","moneda":"BOB"}}
                                    """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu19);
        }
    }

    @Test
    @DisplayName("dar de alta un proveedor de pago es de ADMIN_PLATAFORMA, y de nadie mas")
    void altaDeProveedorEsDelAdministrador() throws Exception {
        mvc.perform(post("/pagos/proveedores")
                        .with(Sesiones.como("SOPORTE", "BILLETERA_OPERAR", "REVERSO_AUTORIZAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu99);
    }
}
