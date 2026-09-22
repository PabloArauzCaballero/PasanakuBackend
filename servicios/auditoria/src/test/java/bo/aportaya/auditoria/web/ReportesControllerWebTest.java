package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CU58DescargarExportacion;
import bo.aportaya.auditoria.aplicacion.CU58EjecutarReporte;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-58 · sacar datos de la plataforma.
 *
 * <p>Lo que se fija aca y no en otro nivel: <b>los permisos con los que se ejecuta el
 * reporte salen del token</b>. Cada definición declara el suyo, así que el caso de uso
 * necesita saber qué trae la sesión — y si esa lista viniera del cuerpo, cualquiera
 * pediría el reporte que quisiera diciendo que puede.
 *
 * <p>Y que el resultado viaje con su <b>hash</b>: una exportación sin huella no se
 * puede auditar después.
 */
@PruebaWeb(ReportesController.class)
class ReportesControllerWebTest {

    private static final UUID DEFINICION = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private static final UUID EJECUCION = UUID.fromString("a1000000-0000-4000-8000-000000000002");
    private static final UUID EXPORTACION = UUID.fromString("a1000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "a1000000-0000-4000-8000-0000000000ff";

    private static final String CUERPO =
            """
            {"parametros":{"periodo":"2026-01"},"formato":"CSV","justificacion":"pedido de auditoria interna"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU58EjecutarReporte cu58;

    @MockitoBean
    private CU58DescargarExportacion descargas;

    private org.springframework.test.web.servlet.ResultActions ejecutar(String cuerpo, String... permisos)
            throws Exception {
        return mvc.perform(post("/reportes/{id}/ejecuciones", DEFINICION)
                .with(Sesiones.como("AUDITOR_INTERNO", permisos))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("CU-58 · 201 con el hash del resultado y el identificador de la exportación")
    void caminoFeliz() throws Exception {
        when(cu58.ejecutar(any(), any()))
                .thenReturn(new CU58EjecutarReporte.SalidaReporte(
                        EJECUCION, "COMPLETADA", 1420, "0a".repeat(32), Optional.of(EXPORTACION)));

        ejecutar(CUERPO, "AUDITORIA_LEER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ejecucionId").value(EJECUCION.toString()))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.filasGeneradas").value(1420))
                // Sin hash no hay forma de demostrar despues que el archivo entregado
                // es el que se generó.
                .andExpect(jsonPath("$.hashResultado").value("0a".repeat(32)))
                .andExpect(jsonPath("$.exportacionId").value(EXPORTACION.toString()));
    }

    @Test
    @DisplayName("CU-58 · los permisos con los que corre el reporte salen del TOKEN, no del cuerpo")
    void losPermisosSalenDelToken() throws Exception {
        when(cu58.ejecutar(any(), any()))
                .thenReturn(new CU58EjecutarReporte.SalidaReporte(
                        EJECUCION, "COMPLETADA", 0, "0a".repeat(32), Optional.empty()));

        ejecutar(CUERPO, "AUDITORIA_LEER", "DATOS_SENSIBLES_LEER").andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU58EjecutarReporte.EntradaReporte.class);
        verify(cu58).ejecutar(capturada.capture(), any());

        org.assertj.core.api.Assertions.assertThat(capturada.getValue().permisos())
                .as("la definicion del reporte decide que permiso exige; la sesion tiene que decir cual trae")
                .containsExactlyInAnyOrder("AUDITORIA_LEER", "DATOS_SENSIBLES_LEER");
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().definicionId())
                .isEqualTo(DEFINICION);
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().parametros())
                .containsEntry("periodo", "2026-01");
    }

    @Test
    @DisplayName("CU-58 · sin formato se ejecuta y NO se exporta: consultar no es sacar datos")
    void sinFormatoNoHayExportacion() throws Exception {
        when(cu58.ejecutar(any(), any()))
                .thenReturn(new CU58EjecutarReporte.SalidaReporte(
                        EJECUCION, "COMPLETADA", 12, "0a".repeat(32), Optional.empty()));

        ejecutar("{\"parametros\":{\"periodo\":\"2026-01\"}}", "AUDITORIA_LEER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exportacionId").doesNotExist());

        var capturada = ArgumentCaptor.forClass(CU58EjecutarReporte.EntradaReporte.class);
        verify(cu58).ejecutar(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().formato())
                .isEmpty();
    }

    @Test
    @DisplayName("CU-58 · 400: un formato de exportación que el contrato no enumera")
    void formatoFueraDelContrato() throws Exception {
        ejecutar(CUERPO.replace("CSV", "SQLITE"), "AUDITORIA_LEER").andExpect(status().isBadRequest());
        verifyNoInteractions(cu58);
    }

    @Test
    @DisplayName("CU-58 · sin parametros, el caso de uso los recibe VACIOS — no nulos, y no es 400")
    void parametrosAusentesLleganVacios() throws Exception {
        // Hallazgo, fijado a proposito. `parametros` es `required` en el contrato y el
        // modelo generado lo declara `@NotNull`, pero lo inicializa como mapa vacio: la
        // validacion nunca dispara. Quien rechaza es el caso de uso, contra los
        // `parametros_esperados` de la definicion, y sale 422 en vez de 400.
        //
        // Vale mas fijarlo que taparlo: sin esta prueba, un cambio del generador que
        // empiece a devolver 400 pasaria por mejora silenciosa y romperia a los clientes
        // que hoy reciben 422.
        when(cu58.ejecutar(any(), any()))
                .thenReturn(new CU58EjecutarReporte.SalidaReporte(
                        EJECUCION, "COMPLETADA", 0, "0a".repeat(32), Optional.empty()));

        ejecutar("{\"formato\":\"CSV\"}", "AUDITORIA_LEER").andExpect(status().isCreated());

        var capturada = ArgumentCaptor.forClass(CU58EjecutarReporte.EntradaReporte.class);
        verify(cu58).ejecutar(capturada.capture(), any());
        org.assertj.core.api.Assertions.assertThat(capturada.getValue().parametros())
                .as("llega vacio, no nulo: el caso de uso tiene que poder distinguirlo")
                .isEmpty();
    }

    @Test
    @DisplayName("CU-58 · 204 al descargar: el cuerpo de una descarga no lleva datos")
    void descargaSinCuerpo() throws Exception {
        mvc.perform(post("/reportes/exportaciones/{id}/descargas", EXPORTACION)
                        .with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isNoContent());

        // El tope de descargas y el vencimiento los decide el caso de uso; lo que la
        // pagina no puede hacer es saltearse el registro de que alguien descargó.
        verify(descargas).ejecutar(eq(EXPORTACION), any());
    }
}
