package bo.aportaya.plataforma.web.errores;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lo que rechaza la base por jOOQ sale catalogado, nunca como 500.
 *
 * <p>Vivia anidado en {@code ManejadorGlobalDeErroresWebTest}; se separo porque ese
 * archivo pasaba las 300 lineas del barrido. Mismo arnes, mismo controlador de ensayo.
 */
@PruebaWeb(ControladorDeEnsayo.class)
class RechazosDeJooqWebTest {

    private static final String CON_PERMISO_DE_LECTURA = "BILLETERA_VER";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("409: la excepcion de jOOQ con una restriccion del catalogo sale con su R-XXX-nn")
    void restriccionConReglaPorJooq() throws Exception {
        // Regresion (2026-10-15): el manejador atrapaba solo la excepcion de Spring y
        // los servicios lanzan la de jOOQ. Todo rechazo real de la base era un 500.
        mvc.perform(get("/ensayo/jooq/23505")
                        .param("restriccion", "uq_envio_idempotencia")
                        .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("R-NOT-01"));
    }

    @Test
    @DisplayName("409 AP-BD-01: un unico sin regla —uq_usuario_correo— es un duplicado, no una caida")
    void unicoSinRegla() throws Exception {
        String cuerpo = mvc.perform(get("/ensayo/jooq/23505")
                        .param("restriccion", "uq_usuario_correo")
                        .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AP-BD-01"))
                .andExpect(jsonPath("$.trazaId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        // Ni el nombre de la restriccion, ni la tabla, ni el valor que la violo.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("uq_usuario_correo")
                .doesNotContain("tabla_secreta")
                .doesNotContain("persona@ejemplo.test");
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "SQLSTATE {0} -> {1} {2}")
    @org.junit.jupiter.params.provider.CsvSource({
        "23503, 409, AP-BD-02",
        "23514, 422, AP-BD-03",
        "23502, 422, AP-BD-03",
        "22001, 422, AP-BD-03",
        "23P01, 409, AP-BD-04",
        "40001, 409, AP-BD-05",
        "40P01, 409, AP-BD-05",
        "57014, 503, AP-BD-06",
    })
    @DisplayName("cada clase de falla de la base tiene su codigo estable")
    void cadaClaseTieneCodigo(String sqlState, int estado, String codigo) throws Exception {
        mvc.perform(get("/ensayo/jooq/{s}", sqlState).with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                .andExpect(status().is(estado))
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    @DisplayName("500: sin SQLSTATE no hay como saber que paso, y eso sigue siendo un fallo")
    void sinSqlState() throws Exception {
        mvc.perform(get("/ensayo/jooq-sin-sqlstate").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("AP-INT-01"));
    }
}
