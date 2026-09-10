package bo.aportaya.garantia.web;

import static org.mockito.ArgumentMatchers.any;
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
 * CU-23 · cubrir un incumplimiento con el fondo de garantia.
 *
 * <p>Separado de {@link IncumplimientosControllerWebTest} por la regla de tamano de
 * archivo: el debido proceso y la cobertura son dos temas.
 *
 * <p>Lo que se fija aca: <b>cubrir no perdona</b> —la respuesta trae la deuda que queda
 * contra quien incumplio— y <b>por encima del umbral no se cubre solo</b>: la respuesta
 * dice que requiere aprobacion y no devuelve cobertura. Automatizar la cobertura de
 * montos grandes es como se vacia un fondo sin que nadie mire.
 */
@PruebaWeb(IncumplimientosController.class)
class CoberturaWebTest {

    private static final UUID EXPEDIENTE = UUID.fromString("b2000000-0000-4000-8000-000000000001");
    private static final UUID DESCARGO = UUID.fromString("b2000000-0000-4000-8000-000000000002");
    private static final UUID COBERTURA = UUID.fromString("b2000000-0000-4000-8000-000000000003");
    private static final UUID DEUDA = UUID.fromString("b2000000-0000-4000-8000-000000000004");
    private static final String CLAVE = "b2000000-0000-4000-8000-0000000000ff";

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

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Nested
    @DisplayName("CU-23 · cubrir no perdona")
    class Cobertura {

        @Test
        @DisplayName("CU-23 · la cobertura devuelve la DEUDA que queda contra quien incumplió")
        void cubrirDejaDeuda() throws Exception {
            when(cu23.cubrir(any(), any()))
                    .thenReturn(new CU23CubrirIncumplimiento.SalidaCobertura(
                            COBERTURA, DEUDA, bob("350.00"), "TOPE_POR_PARTICIPANTE", false, true));

            mvc.perform(
                            post("/incumplimientos/{id}/cobertura", EXPEDIENTE)
                                    .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"montoSolicitado":{"monto":"350.00","moneda":"BOB"},"diasMora":15}
                                    """))
                    .andExpect(status().isCreated())
                    // Sin la deuda, el fondo paga y nadie sabe a quien cobrarle.
                    .andExpect(jsonPath("$.deudaId").value(DEUDA.toString()))
                    .andExpect(jsonPath("$.montoCubierto.monto").value("350.00"))
                    // Que limite mando es lo que explica por que se cubrio ESE monto.
                    .andExpect(jsonPath("$.limiteQueMando").value("TOPE_POR_PARTICIPANTE"))
                    .andExpect(jsonPath("$.requiereAprobacion").value(false));
        }

        @Test
        @DisplayName("CU-23 · por encima del umbral, la respuesta dice que requiere aprobación y NO cubre")
        void porEncimaDelUmbralPideAprobacion() throws Exception {
            when(cu23.cubrir(any(), any()))
                    .thenReturn(new CU23CubrirIncumplimiento.SalidaCobertura(
                            null, null, bob("0.00"), "TOPE_POR_PERIODO", true, true));

            mvc.perform(
                            post("/incumplimientos/{id}/cobertura", EXPEDIENTE)
                                    .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"montoSolicitado":{"monto":"9000.00","moneda":"BOB"},"diasMora":40}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.requiereAprobacion").value(true))
                    // Automatizar la cobertura de montos grandes es como se vacia un
                    // fondo sin que nadie mire.
                    .andExpect(jsonPath("$.coberturaId").doesNotExist());
        }

        @Test
        @DisplayName("CU-23 · 400: días de mora negativos")
        void diasDeMoraNegativos() throws Exception {
            mvc.perform(
                            post("/incumplimientos/{id}/cobertura", EXPEDIENTE)
                                    .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"montoSolicitado":{"monto":"350.00","moneda":"BOB"},"diasMora":-5}
                                    """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu23);
        }
    }
}
