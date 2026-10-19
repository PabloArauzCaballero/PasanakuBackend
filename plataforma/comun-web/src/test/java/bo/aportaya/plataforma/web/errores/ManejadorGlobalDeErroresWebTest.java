package bo.aportaya.plataforma.web.errores;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato de error de los catorce servicios, comprobado una vez.
 *
 * <p>Cada codigo HTTP de aca es una promesa distinta para quien consume la API, y
 * confundir dos es un defecto aunque «devuelva error»: el {@code 400} dice «mandaste
 * mal el formulario» y el {@code 422} dice «tu formulario esta bien y la regla te lo
 * niega». Un cliente que no puede distinguirlos no puede decidir si reintentar.
 *
 * <p>Se prueba en {@code comun-web} y no en cada servicio porque el manejador es uno
 * solo: probarlo catorce veces seria mantener catorce copias de la misma verdad.
 */
@PruebaWeb(ControladorDeEnsayo.class)
class ManejadorGlobalDeErroresWebTest {

    private static final String CON_PERMISO_DE_LECTURA = "BILLETERA_VER";

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("lo que el cliente mando mal es 4xx, y nunca 500")
    class EntradaDelCliente {

        @Test
        @DisplayName("400: JSON corrupto — el parser no puede ser un fallo del servidor")
        void jsonCorrupto() throws Exception {
            mvc.perform(post("/ensayo/recursos")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ ESTO NO ES JSON }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("400: cuerpo vacio")
        void cuerpoVacio() throws Exception {
            mvc.perform(post("/ensayo/recursos")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("400: un identificador de ruta que no es un identificador")
        void variableDeRutaConTipoEquivocado() throws Exception {
            mvc.perform(get("/ensayo/recursos/{id}", "no-soy-un-uuid")
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("400: falta un parametro obligatorio")
        void parametroQueFalta() throws Exception {
            mvc.perform(get("/ensayo/con-parametro").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("400: el contrato rechaza los campos invalidos Y los nombra")
        void validacionDelContrato() throws Exception {
            String cuerpo =
                    """
                    {"nombre":"  ","correo":"NO-ES-UN-CORREO","clave":"1234567","monto":{"monto":"1.00","moneda":"BOB"}}
                    """;
            mvc.perform(post("/ensayo/recursos")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-01"))
                    // Nombrar el campo es la mitad del valor de un 400: sin esto el
                    // cliente sabe que algo esta mal y no cual.
                    .andExpect(jsonPath("$.detalle.nombre").exists())
                    .andExpect(jsonPath("$.detalle.correo").exists())
                    .andExpect(jsonPath("$.detalle.clave").exists());
        }

        @Test
        @DisplayName("400: falta la cabecera obligatoria — Idempotency-Key olvidada NO es un 500")
        void cabeceraObligatoriaQueFalta() throws Exception {
            // Regresion. Toda operacion con efecto exige `Idempotency-Key`, y olvidarla
            // es el error mas facil de cometer contra esta API. Devolvia 500: un fallo
            // del servidor por algo que el cliente arregla solo, y un ERROR en la
            // bitacora por cada peticion mal armada.
            mvc.perform(post("/ensayo/con-cabecera")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("400: la cabecera obligatoria con un valor que no es del tipo declarado")
        void cabeceraConTipoEquivocado() throws Exception {
            mvc.perform(post("/ensayo/con-cabecera")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .header("Idempotency-Key", "no-soy-un-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("400: un parametro de consulta que viola su restriccion NO es un 500")
        void parametroQueViolaSuRestriccion() throws Exception {
            // Regresion. La violacion de un `pattern` o un `minimum` de un parametro NO
            // llega como excepcion de Spring MVC: la lanza el proxy de `@Validated` de
            // la interfaz generada, como `ConstraintViolationException` de Jakarta. Sin
            // atraparla, todo endpoint con parametros restringidos devolvia 500 por un
            // parametro mal escrito.
            mvc.perform(get("/ensayo/con-parametro-restringido")
                            .param("periodo", "febrero-2026")
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));
        }

        @Test
        @DisplayName("el parametro que SI cumple la restriccion pasa")
        void parametroQueCumple() throws Exception {
            mvc.perform(get("/ensayo/con-parametro-restringido")
                            .param("periodo", "2026-02")
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodo").value("2026-02"));
        }

        @Test
        @DisplayName("405: el verbo no aplica a esa ruta — no es 404 ni 500")
        void verboEquivocado() throws Exception {
            mvc.perform(post("/ensayo/recursos/{id}", Sesiones.USUARIO)
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-03"));
        }

        @Test
        @DisplayName("415: el cuerpo tiene que ser application/json")
        void tipoDeContenidoQueNoServimos() throws Exception {
            mvc.perform(post("/ensayo/recursos")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("nombre=Pablo"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.codigo").value("AP-VAL-04"));
        }
    }

    @Nested
    @DisplayName("lo que decide el dominio")
    class ReglasDelDominio {

        @Test
        @DisplayName("422: una regla de negocio sale con su AP-CU<NN>-<nn> y su detalle accionable")
        void reglaDeNegocio() throws Exception {
            mvc.perform(get("/ensayo/regla-de-negocio").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.codigo").value("AP-CU21-03"))
                    .andExpect(jsonPath("$.mensaje").value("El aporte no cubre la obligacion."))
                    .andExpect(jsonPath("$.detalle.faltan").value("35.00"))
                    .andExpect(jsonPath("$.trazaId").exists());
        }

        @Test
        @DisplayName("422: un invariante del dominio sale como AP-DOM-01")
        void reglaDelDominio() throws Exception {
            mvc.perform(get("/ensayo/regla-del-dominio").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-DOM-01"));
        }

        @Test
        @DisplayName("409: una restriccion del catalogo sale traducida a su R-XXX-nn")
        void restriccionConocida() throws Exception {
            mvc.perform(get("/ensayo/restriccion-conocida").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("R-NOT-01"));
        }

        @Test
        @DisplayName(
                "409: misma clave de idempotencia con otro cuerpo — IdempotenciaConflicto, no 422 ni 200 (H1.S1.M4)")
        void idempotenciaConflicto() throws Exception {
            mvc.perform(get("/ensayo/idempotencia-conflicto")
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("AP-CU00-01"))
                    .andExpect(jsonPath("$.trazaId").exists());
        }

        @Test
        @DisplayName("409: reserva de idempotencia sin respuesta final todavia — IdempotenciaEnProceso")
        void idempotenciaEnProceso() throws Exception {
            mvc.perform(get("/ensayo/idempotencia-en-proceso")
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("AP-CU00-02"))
                    .andExpect(jsonPath("$.trazaId").exists());
        }

        @Test
        @DisplayName("500: una restriccion que el catalogo NO conoce no se disfraza de mensaje amable")
        void restriccionSinTraduccion() throws Exception {
            // Improvisar aca un 409 con un texto generico esconderia un caso que nadie
            // previo. Sale como 500 —con alerta— y sin el nombre de la tabla.
            mvc.perform(get("/ensayo/restriccion-desconocida")
                            .with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.codigo").value("AP-INT-01"))
                    .andExpect(content()
                            .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("tabla_secreta"))));
        }
    }

    @Test
    @DisplayName("500: el fallo no previsto sale con la traza y NADA mas")
    void elErrorNoEnsenaLaArquitectura() throws Exception {
        String cuerpo = mvc.perform(
                        get("/ensayo/fallo-no-previsto").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("AP-INT-01"))
                .andExpect(jsonPath("$.trazaId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("jdbc:")
                .doesNotContain("password")
                .doesNotContain("supersecreto")
                .doesNotContain("IllegalStateException")
                .doesNotContain("java.lang")
                .doesNotContain("at bo.aportaya");
    }

    @Test
    @DisplayName("404: una ruta que no existe responde con el mismo cuerpo que todo lo demas")
    void rutaInexistente() throws Exception {
        mvc.perform(get("/ensayo/esta-ruta-no-existe").with(Sesiones.como("PARTICIPANTE", CON_PERMISO_DE_LECTURA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-05"))
                // Un 404 con la pagina de Tomcat regala la version del servidor.
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Tomcat"))));
    }
}
