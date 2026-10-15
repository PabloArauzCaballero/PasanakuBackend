package bo.aportaya.plataforma.web.errores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H13.S1.M3 · La bitacora de errores lleva identificadores, no contenido (regla 90.2.1).
 *
 * <p>{@code ManejadorGlobalDeErroresWebTest} prueba que la RESPUESTA no filtra; esto prueba lo
 * que esa prueba no miraba: el LOG. Antes, ante una excepcion inesperada se escribia la
 * excepcion entera —con su mensaje—, y el mensaje de un driver trae la cadena de conexion o el
 * valor que violo una unicidad. Los textos de abajo son sinteticos.
 */
@PruebaWeb(ControladorDeEnsayo.class)
class BitacoraDeErroresSinContenidoWebTest {

    private static final String CON_PERMISO = "BILLETERA_VER";

    @Autowired
    private MockMvc mvc;

    private final ListAppender<ILoggingEvent> capturado = new ListAppender<>();
    private Logger bitacora;

    @BeforeEach
    void escuchar() {
        bitacora = (Logger) LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);
        capturado.start();
        bitacora.addAppender(capturado);
    }

    @AfterEach
    void soltar() {
        bitacora.detachAppender(capturado);
    }

    @Test
    @DisplayName("rechaza: un fallo no previsto no escribe en el log la cadena de conexion ni la clave del mensaje")
    void falloNoPrevistoSinMensaje() throws Exception {
        mvc.perform(get("/ensayo/fallo-no-previsto").with(Sesiones.como("PARTICIPANTE", CON_PERMISO)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.trazaId").exists());

        String todo = todoLoEscrito();
        assertThat(todo).doesNotContain("supersecreto").doesNotContain("jdbc:").doesNotContain("password");
        assertThat(todo)
                .as("pero si sirve para diagnosticar: la clase de la excepcion y el trazaId")
                .contains("IllegalStateException")
                .contains("trazaId=");
        assertThat(eventosDeError()).hasSize(1);
    }

    @Test
    @DisplayName(
            "rechaza: una restriccion sin traduccion no escribe el valor repetido; deja el nombre de la restriccion")
    void restriccionConValorSinValor() throws Exception {
        mvc.perform(get("/ensayo/restriccion-con-valor").with(Sesiones.como("PARTICIPANTE", CON_PERMISO)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("AP-INT-01"));

        String todo = todoLoEscrito();
        assertThat(todo).doesNotContain("+59170000000").doesNotContain("Key (telefono)");
        assertThat(todo).contains("uq_sin_catalogo").contains("DataIntegrityViolationException");
    }

    @Test
    @DisplayName(
            "rechaza: el rechazo de jOOQ sin regla (WARN, AP-BD-01) deja la restriccion y el SQLSTATE, nunca el valor")
    void rechazoDeJooqSinValor() throws Exception {
        mvc.perform(get("/ensayo/jooq/23505")
                        .param("restriccion", "uq_usuario_correo")
                        .with(Sesiones.como("PARTICIPANTE", CON_PERMISO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AP-BD-01"));

        String todo = todoLoEscrito();
        assertThat(todo).doesNotContain("persona@ejemplo.test").doesNotContain("Key (correo)");
        assertThat(todo).contains("uq_usuario_correo").contains("sqlstate=23505");
    }

    @Test
    @DisplayName("rechaza: ni el mensaje ni la pila de una excepcion de la base viajan como throwable del evento")
    void sinThrowableAdjunto() throws Exception {
        mvc.perform(get("/ensayo/restriccion-desconocida").with(Sesiones.como("PARTICIPANTE", CON_PERMISO)))
                .andExpect(status().isInternalServerError());

        for (ILoggingEvent evento : eventosDeError()) {
            assertThat(evento.getThrowableProxy())
                    .as("un throwable adjunto vuelve a imprimir getMessage() en la primera linea de la pila")
                    .isNull();
        }
        assertThat(todoLoEscrito()).doesNotContain("tabla_secreta").doesNotContain("columna_que_el_catalogo");
    }

    private List<ILoggingEvent> eventosDeError() {
        return capturado.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
    }

    private String todoLoEscrito() {
        StringBuilder texto = new StringBuilder();
        for (ILoggingEvent evento : capturado.list) {
            texto.append(evento.getFormattedMessage()).append('\n');
            if (evento.getThrowableProxy() instanceof ThrowableProxy proxy) {
                texto.append(proxy.getThrowable()).append('\n');
            }
        }
        return texto.toString();
    }
}
