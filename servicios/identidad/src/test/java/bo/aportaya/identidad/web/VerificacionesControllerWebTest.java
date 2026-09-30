package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU02RevisarExpediente;
import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El portal de riesgo: la cola de expedientes, la foto del carnet y la decision.
 *
 * <p>Lo que se fija aca y en ningun otro lado: quien mira una cedula ajena. Son datos
 * personales de alguien que todavia no es cliente, y el unico permiso que los abre es
 * {@code VERIFICACION_RESOLVER}. Un operador autenticado sin ese permiso tiene que
 * recibir 403 <b>antes</b> de que el caso de uso toque nada — una prueba de integracion
 * llama al caso de uso directamente y nunca pasa por la guardia.
 *
 * <p>Y la foto no viaja en la respuesta: lo que vuelve es un enlace con vencimiento
 * (ADR-034). Si algun dia alguien devuelve la imagen o una URL permanente, esta prueba
 * se rompe.
 */
@PruebaWeb(VerificacionesController.class)
class VerificacionesControllerWebTest {

    private static final UUID VERIFICACION = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("dddddddd-0000-4000-8000-000000000002");
    private static final UUID CLAVE = UUID.fromString("dddddddd-0000-4000-8000-000000000003");
    private static final OffsetDateTime CUANDO = OffsetDateTime.of(2026, 3, 15, 14, 0, 0, 0, ZoneOffset.UTC);

    private static final ExpedienteDeIdentidad EN_REVISION = new ExpedienteDeIdentidad(
            VERIFICACION,
            USUARIO,
            "Marisol Quispe",
            "CI 1234567 LP",
            "EN_REVISION",
            CUANDO,
            null,
            null,
            List.of("ANVERSO", "REVERSO", "SELFIE"));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU02RevisarExpediente revision;

    @Nested
    @DisplayName("GET /identidad/verificaciones — la cola del operador")
    class Cola {

        @Test
        @DisplayName("CU-02 · con VERIFICACION_RESOLVER devuelve la cola, sin la imagen adentro")
        void colaConPermiso() throws Exception {
            when(revision.cola(eq("EN_REVISION"), any())).thenReturn(List.of(EN_REVISION));

            mvc.perform(get("/identidad/verificaciones")
                            .param("estado", "EN_REVISION")
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].verificacionId").value(VERIFICACION.toString()))
                    .andExpect(jsonPath("$[0].estado").value("EN_REVISION"))
                    .andExpect(jsonPath("$[0].fotos[0]").value("ANVERSO"))
                    // Las caras se nombran; la imagen no viaja en la lista.
                    .andExpect(jsonPath("$[0].url").doesNotExist());
        }

        @Test
        @DisplayName("CU-02 · un operador SIN el permiso no ve un solo expediente")
        void colaSinPermiso() throws Exception {
            mvc.perform(get("/identidad/verificaciones").with(Sesiones.como("BACKOFFICE", "AUDITORIA_LEER")))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(revision);
        }

        @Test
        @DisplayName("CU-02 · sin sesion no se llega ni a la guardia de permiso")
        void colaSinSesion() throws Exception {
            mvc.perform(get("/identidad/verificaciones")).andExpect(status().isUnauthorized());
            verifyNoInteractions(revision);
        }
    }

    @Nested
    @DisplayName("GET /identidad/verificaciones/{id}/fotos/{cara} — la cedula")
    class Foto {

        @Test
        @DisplayName("CU-02 · devuelve un enlace con vencimiento, nunca la imagen")
        void fotoConPermiso() throws Exception {
            when(revision.foto(eq(VERIFICACION), eq("ANVERSO"), any()))
                    .thenReturn(new CU02RevisarExpediente.Enlace("https://almacen/interno/anverso?firma=xyz", CUANDO));

            mvc.perform(get("/identidad/verificaciones/{id}/fotos/{cara}", VERIFICACION, "ANVERSO")
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://almacen/interno/anverso?firma=xyz"))
                    .andExpect(jsonPath("$.vigenteHasta").exists());
        }

        @Test
        @DisplayName("CU-02 · sin el permiso, la cedula de otro no se abre")
        void fotoSinPermiso() throws Exception {
            mvc.perform(get("/identidad/verificaciones/{id}/fotos/{cara}", VERIFICACION, "ANVERSO")
                            .with(Sesiones.como("BACKOFFICE", "AUDITORIA_LEER")))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(revision);
        }

        @Test
        @DisplayName("CU-02 · una cara que no existe se rechaza en el borde, no con un 500")
        void caraFueraDelContrato() throws Exception {
            mvc.perform(get("/identidad/verificaciones/{id}/fotos/{cara}", VERIFICACION, "PERFIL")
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER")))
                    .andExpect(status().isUnprocessableEntity());
            verifyNoInteractions(revision);
        }
    }

    @Nested
    @DisplayName("GET /identidad/verificaciones/{id}/fotos/{cara}/contenido — los bytes")
    class Contenido {

        private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

        @Test
        @DisplayName("CU-02 · los sirve el servicio dueno, con su tipo y sin dejarlos en cache")
        void contenidoConPermiso() throws Exception {
            when(revision.contenido(eq(VERIFICACION), eq("ANVERSO"), any()))
                    .thenReturn(new bo.aportaya.plataforma.archivos.ContenidoAlmacenado(
                            new java.io.ByteArrayInputStream(PNG), PNG.length, "image/png"));

            mvc.perform(get("/identidad/verificaciones/{id}/fotos/{cara}/contenido", VERIFICACION, "ANVERSO")
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER")))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"))
                    // Una cedula no se queda en la cache del navegador ni en la de nadie.
                    .andExpect(header().string("Cache-Control", "no-store"));
        }

        @Test
        @DisplayName("CU-02 · sin el permiso no se leen los bytes, y no se toca el almacen")
        void contenidoSinPermiso() throws Exception {
            mvc.perform(get("/identidad/verificaciones/{id}/fotos/{cara}/contenido", VERIFICACION, "ANVERSO")
                            .with(Sesiones.como("BACKOFFICE", "AUDITORIA_LEER")))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(revision);
        }

        @Test
        @DisplayName("CU-02 · una cara inventada se rechaza en el borde, igual que en el enlace")
        void caraFueraDelContrato() throws Exception {
            mvc.perform(get("/identidad/verificaciones/{id}/fotos/{cara}/contenido", VERIFICACION, "PERFIL")
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER")))
                    .andExpect(status().isUnprocessableEntity());
            verifyNoInteractions(revision);
        }
    }

    @Nested
    @DisplayName("POST /identidad/verificaciones/{id}/decision — aprobar o rechazar")
    class Decision {

        private static final String APRUEBA = """
                {"decision": "APROBAR"}
                """;

        @Test
        @DisplayName("CU-02 · con el permiso y la clave de idempotencia, resuelve y devuelve el expediente")
        void resuelveConPermiso() throws Exception {
            var resuelto = new ExpedienteDeIdentidad(
                    VERIFICACION,
                    USUARIO,
                    "Marisol Quispe",
                    "CI 1234567 LP",
                    "APROBADA",
                    CUANDO,
                    CUANDO,
                    null,
                    List.of("ANVERSO", "REVERSO", "SELFIE"));
            when(revision.cola(eq(null), any())).thenReturn(List.of(resuelto));

            mvc.perform(post("/identidad/verificaciones/{id}/decision", VERIFICACION)
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(APRUEBA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("APROBADA"));

            verify(revision).resolver(eq(VERIFICACION), eq("APROBAR"), eq(null), any());
        }

        @Test
        @DisplayName("CU-02 · sin el permiso no se decide sobre la identidad de nadie")
        void sinPermisoNoDecide() throws Exception {
            mvc.perform(post("/identidad/verificaciones/{id}/decision", VERIFICACION)
                            .with(Sesiones.como("BACKOFFICE", "AUDITORIA_LEER"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(APRUEBA))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(revision);
        }

        @Test
        @DisplayName("CU-02 · una decision fuera del contrato se rechaza antes del caso de uso")
        void decisionFueraDelContrato() throws Exception {
            mvc.perform(post("/identidad/verificaciones/{id}/decision", VERIFICACION)
                            .with(Sesiones.como("BACKOFFICE", "VERIFICACION_RESOLVER"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\": \"MAS_O_MENOS\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(revision);
        }
    }
}
