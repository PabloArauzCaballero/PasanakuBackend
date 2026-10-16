package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Las consultas sensibles de {@code UsuariosController}: contestan si, o no, nunca el dato.
 *
 * <p>Vivian en {@code UsuariosControllerWebTest}; se separaron por el limite de 300
 * lineas del barrido. Mismo arnes, mismo contexto.
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosControllerConsultasWebTest extends ArnesDeUsuariosController {

    @Nested
    @DisplayName("Las consultas sensibles contestan si, o no — nunca el dato")
    class ConsultasSensibles {

        @Test
        @DisplayName("CU-69 · buscar por telefono devuelve existe + id, y nada mas")
        void buscarPorTelefono() throws Exception {
            when(busqueda.ejecutar(any(), any())).thenReturn(Optional.of(USUARIO));

            String cuerpo = mvc.perform(get("/usuarios/por-telefono")
                            .param("telefono", "+59171234567")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.existe").value(true))
                    .andExpect(jsonPath("$.usuarioId").value(USUARIO.toString()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Dos campos y ningun dato personal: ni nombre, ni documento, ni el telefono
            // que se pregunto.
            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain("nombre")
                    .doesNotContain("documento")
                    .doesNotContain("+59171234567");
        }

        @Test
        @DisplayName("CU-69 · si no existe, contesta que no y no inventa un identificador")
        void telefonoQueNoExiste() throws Exception {
            when(busqueda.ejecutar(any(), any())).thenReturn(Optional.empty());

            mvc.perform(get("/usuarios/por-telefono")
                            .param("telefono", "+59171234567")
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.existe").value(false))
                    .andExpect(jsonPath("$.usuarioId").doesNotExist());
        }

        @Test
        @DisplayName("CU-01 · verificar titularidad contesta si coincide, no que hay guardado")
        void verificarTitularidad() throws Exception {
            when(titularidad.coincide(any(), any(), any(), any())).thenReturn(false);

            String cuerpo = mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                            .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombreCompleto\":\"Pablo Arauz\",\"documento\":\"1234567\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coincide").value(false))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Si contestara «no coincide, el guardado es X», seria un oraculo para
            // adivinar el documento de cualquiera a fuerza de intentos.
            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain("Pablo")
                    .doesNotContain("1234567");
        }

        @Test
        @DisplayName("CU-01 · 400: la titularidad con un documento mas corto que el minimo del contrato")
        void documentoDemasiadoCorto() throws Exception {
            mvc.perform(post("/usuarios/{id}/titularidad/verificacion", USUARIO)
                            .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombreCompleto\":\"Pablo Arauz\",\"documento\":\"12\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(titularidad);
        }

        @Test
        @DisplayName("CU-69 · emitir un token de invitacion exige GRUPO_ADMINISTRAR")
        void tokenDeInvitacionEsUnaOperacionDeOrganizador() throws Exception {
            mvc.perform(post("/usuarios/tokens/invitacion")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"canal\":\"WHATSAPP\",\"destinoEnmascarado\":\"+591*****567\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(tokens);
        }
    }

    /** Que el identificador del contrato exista lo decide `cumplimiento`, no esta ruta. */
    @Test
    @DisplayName("CU-01 · el contrato aceptado viaja como identificador, no como texto")
    void elContratoViajaComoIdentificador() throws Exception {
        registrar(ALTA.replace("\"" + CONTRATO + "\"", "\"acepto todo\"")).andExpect(status().isBadRequest());
        verifyNoInteractions(cu01);
    }
}
