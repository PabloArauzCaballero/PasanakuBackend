package bo.aportaya.garantia.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU25DeclararIncumplimiento;
import bo.aportaya.garantia.aplicacion.CU27RestringirDeudor;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.ReclamosEnCurso;
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
 * <p>La cobertura manual se retiro (F1.L2.M02): el 410 es un criterio de CU-23 y vive en
 * {@link CU23CoberturaManualRetiradaWebTest}. Aca queda la validacion del cuerpo, que el
 * contrato sigue exigiendo antes de responder.
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
    private CU25DeclararIncumplimiento cu25;

    @MockitoBean
    private CU27RestringirDeudor cu27;

    @MockitoBean
    private CU66ReemplazarParticipante cu66;

    @MockitoBean
    private ReclamosEnCurso reclamosEnCurso;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Nested
    @DisplayName("CU-23 · cubrir no perdona")
    class Cobertura {

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
        }
    }
}
