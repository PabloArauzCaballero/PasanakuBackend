package bo.aportaya.garantia.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.garantia.aplicacion.CU25DeclararIncumplimiento;
import bo.aportaya.garantia.aplicacion.CU27RestringirDeudor;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.ReclamosEnCurso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-23 · la cobertura ya no se pide a mano (F1.L2.M02). Mora, monto y aprobador del pedido decidian dinero sin
 * expediente firme; la ruta queda {@code deprecated}, responde 410 y no hay caso de uso detras al que llamar.
 */
@PruebaWeb(IncumplimientosController.class)
class CU23CoberturaManualRetiradaWebTest {

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

    @Test
    @DisplayName(
            "Dado un expediente de incumplimiento · Cuando alguien pide la cobertura a mano con monto, mora y aprobador · Entonces responde 410 AP-CU23-14 y no se ejecuta ningún caso de uso")
    void criterio3() throws Exception {
        mvc.perform(
                        post("/incumplimientos/{id}/cobertura", "b2000000-0000-4000-8000-000000000001")
                                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"montoSolicitado":{"monto":"500.00","moneda":"BOB"},"diasMora":10,
                                 "aprobadaPor":"b2000000-0000-4000-8000-000000000009"}
                                """))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value("AP-CU23-14"))
                .andExpect(jsonPath("$.trazaId").exists());
        verifyNoInteractions(cu25, cu27, cu66, reclamosEnCurso);
    }
}
