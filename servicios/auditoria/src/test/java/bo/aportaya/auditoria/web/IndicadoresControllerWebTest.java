package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CU98PublicarTablero;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-98 · el tablero de indicadores.
 *
 * <p>Tres cosas que solo se ven en la respuesta HTTP: que <b>provisorio</b> viaje
 * marcado como tal —un número que todavía puede cambiar y no lo dice es un número que
 * alguien va a citar en un comité—, que un indicador suprimido por privacidad viaje
 * <b>sin valor</b>, y que las cifras salgan como cadena decimal.
 */
@PruebaWeb(IndicadoresController.class)
class IndicadoresControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU98PublicarTablero cu98;

    private static CU98PublicarTablero.Indicador indicador(
            String codigo, Optional<BigDecimal> valor, boolean suprimido, boolean provisorio) {
        return new CU98PublicarTablero.Indicador(
                codigo,
                "Tasa de mora",
                valor,
                "PORCENTAJE",
                Optional.of(new BigDecimal("5.00")),
                Optional.of(false),
                Optional.of(new BigDecimal("0.75")),
                List.of(new CU98PublicarTablero.PuntoDeSerie("2025-12", new BigDecimal("6.10"))),
                suprimido,
                suprimido ? Optional.empty() : Optional.of(240),
                30,
                provisorio,
                "RIESGO",
                "Responsable de riesgos",
                "v3",
                OffsetDateTime.of(2026, 2, 1, 6, 0, 0, 0, ZoneOffset.UTC));
    }

    private org.springframework.test.web.servlet.ResultActions pedir(String periodo, String dimension)
            throws Exception {
        return mvc.perform(get("/indicadores")
                .param("periodo", periodo)
                .param("dimension", dimension)
                .with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")));
    }

    @Test
    @DisplayName("CU-98 · 200 con las cifras como cadena decimal y lo provisorio marcado")
    void caminoFeliz() throws Exception {
        when(cu98.ejecutar(any(), any()))
                .thenReturn(new CU98PublicarTablero.SalidaTablero(
                        "2026-01",
                        "GLOBAL",
                        Optional.empty(),
                        true,
                        List.of(indicador("MORA_30", Optional.of(new BigDecimal("6.85")), false, true))));

        pedir("2026-01", "GLOBAL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-01"))
                // Un tablero provisorio que no lo dice es el que alguien cita en un
                // comité como si fuera definitivo.
                .andExpect(jsonPath("$.provisorio").value(true))
                .andExpect(jsonPath("$.indicadores[0].codigo").value("MORA_30"))
                .andExpect(jsonPath("$.indicadores[0].valor").value("6.85"))
                .andExpect(jsonPath("$.indicadores[0].definicionVersion").value("v3"))
                .andExpect(jsonPath("$.indicadores[0].duenoFamilia").value("Responsable de riesgos"));
    }

    @Test
    @DisplayName("CU-98 · un indicador suprimido por privacidad viaja SIN valor y sin casos")
    void supresionPorMinimoDeCasos() throws Exception {
        when(cu98.ejecutar(any(), any()))
                .thenReturn(new CU98PublicarTablero.SalidaTablero(
                        "2026-01",
                        "POR_GRUPO",
                        Optional.empty(),
                        false,
                        List.of(indicador("MORA_30", Optional.empty(), true, false))));

        pedir("2026-01", "POR_GRUPO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores[0].suprimidoPorPrivacidad").value(true))
                // Con pocos casos, publicar el valor identifica a la persona. Que el
                // campo no exista es la supresión; mandarlo en cero sería publicarlo.
                .andExpect(jsonPath("$.indicadores[0].valor").doesNotExist())
                .andExpect(jsonPath("$.indicadores[0].casos").doesNotExist())
                .andExpect(jsonPath("$.indicadores[0].minimoCasos").value(30));
    }

    @Test
    @DisplayName("CU-98 · sin periodosDeSerie, la serie tiene el largo por omisión")
    void largoDeSeriePorOmision() throws Exception {
        when(cu98.ejecutar(any(), any()))
                .thenReturn(
                        new CU98PublicarTablero.SalidaTablero("2026-01", "GLOBAL", Optional.empty(), false, List.of()));

        pedir("2026-01", "GLOBAL").andExpect(status().isOk());

        var capturada = ArgumentCaptor.forClass(CU98PublicarTablero.EntradaTablero.class);
        verify(cu98).ejecutar(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().periodosDeSerie())
                .isEqualTo(6);
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().dimensionId())
                .isEmpty();
    }

    @ParameterizedTest(name = "dimension fuera del contrato: «{0}»")
    @ValueSource(strings = {"POR_USUARIO", "global", "TODO"})
    @DisplayName("CU-98 · el enum de un parametro de CONSULTA no lo aplica el borde: llega crudo al caso de uso")
    void elEnumDeConsultaNoSeAplicaEnElBorde(String dimension) throws Exception {
        // Hallazgo, fijado a proposito. `dimension` esta declarada con enum en el
        // OpenAPI, pero el generador la produce como `String` con `@NotNull` y nada
        // mas: a diferencia de un enum del CUERPO —que si se rechaza con 400—, este
        // llega crudo. Rechazarlo es responsabilidad del caso de uso.
        //
        // La prueba pina ese reparto. Si algun dia el generador empieza a validarlo,
        // esta prueba falla y hay que decidir a proposito quien rechaza, en vez de
        // descubrirlo cuando un valor invalido llegue a una consulta.
        when(cu98.ejecutar(any(), any()))
                .thenReturn(
                        new CU98PublicarTablero.SalidaTablero("2026-01", "GLOBAL", Optional.empty(), false, List.of()));

        pedir("2026-01", dimension).andExpect(status().isOk());

        var capturada = ArgumentCaptor.forClass(CU98PublicarTablero.EntradaTablero.class);
        verify(cu98).ejecutar(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().dimension())
                .as("el valor llega sin tocar, para que el caso de uso pueda rechazarlo")
                .isEqualTo(dimension);
    }

    @Test
    @DisplayName("CU-98 · 400: sin periodo, que el contrato exige")
    void faltaElPeriodo() throws Exception {
        mvc.perform(get("/indicadores")
                        .param("dimension", "GLOBAL")
                        .with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu98);
    }
}
