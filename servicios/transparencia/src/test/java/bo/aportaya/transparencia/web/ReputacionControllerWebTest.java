package bo.aportaya.transparencia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.transparencia.aplicacion.CU70RegistrarEventoReputacion;
import bo.aportaya.transparencia.aplicacion.CU71RecalcularPuntaje;
import bo.aportaya.transparencia.aplicacion.CU72SellarBloque;
import bo.aportaya.transparencia.aplicacion.CU74EvaluarInsignias;
import bo.aportaya.transparencia.aplicacion.CU75EmitirCertificado;
import bo.aportaya.transparencia.aplicacion.CU76PublicarResena;
import bo.aportaya.transparencia.aplicacion.CU97EvaluarRiesgo;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /reputacion}.
 *
 * <p>Catorce operaciones y cinco permisos distintos. Lo que se fija acá son las tres
 * cosas que la respuesta decide y ningún otro nivel ve.
 *
 * <p><b>El puntaje viaja como cadena decimal.</b> Es un número con el que se toman
 * decisiones sobre personas; en coma flotante, dos servidores pueden mostrar dos
 * valores distintos del mismo puntaje.
 *
 * <p><b>El certificado publica lo que el titular eligió, y su hash.</b> Lo que no eligió
 * no entra al contenido ni al hash — si entrara al hash, el verificador vería que hay
 * algo escondido.
 *
 * <p><b>Sellar el bloque es de auditoría, no de soporte.</b> Quien registra eventos de
 * reputación no puede además sellar la cadena que los vuelve inmodificables.
 */
@PruebaWeb(ReputacionController.class)
class ReputacionControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("e1000000-0000-4000-8000-000000000001");
    private static final UUID SNAPSHOT = UUID.fromString("e1000000-0000-4000-8000-000000000002");
    private static final UUID CERTIFICADO = UUID.fromString("e1000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "e1000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarPuntaje puntajes;

    @MockitoBean
    private CU70RegistrarEventoReputacion cu70;

    @MockitoBean
    private CU71RecalcularPuntaje cu71;

    @MockitoBean
    private CU72SellarBloque cu72;

    @MockitoBean
    private CU74EvaluarInsignias cu74;

    @MockitoBean
    private CU75EmitirCertificado cu75;

    @MockitoBean
    private CU76PublicarResena cu76;

    @MockitoBean
    private CU97EvaluarRiesgo cu97;

    @Nested
    @DisplayName("GET /reputacion/{usuarioId}/puntaje")
    class Puntaje {

        @Test
        @DisplayName("CU-71 · 200 con el puntaje como cadena decimal, nunca como número")
        void puntajeComoCadena() throws Exception {
            when(puntajes.ejecutar(any(), any()))
                    .thenReturn(new ConsultarPuntaje.Puntaje(true, new BigDecimal("87.50"), "ALTO"));

            mvc.perform(get("/reputacion/{id}/puntaje", USUARIO).with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tieneHistorial").value(true))
                    // "87.50" y no 87.5: es un numero con el que se decide sobre
                    // personas, y en coma flotante dos servidores muestran dos valores.
                    .andExpect(jsonPath("$.puntaje").value("87.50"))
                    .andExpect(jsonPath("$.nivelDeConfianza").value("ALTO"));
        }

        @Test
        @DisplayName("CU-71 · quien todavía no tiene historial lo dice, y no aparenta un puntaje ganado")
        void sinHistorial() throws Exception {
            when(puntajes.ejecutar(any(), any()))
                    .thenReturn(new ConsultarPuntaje.Puntaje(false, BigDecimal.ZERO, "SIN_HISTORIAL"));

            mvc.perform(get("/reputacion/{id}/puntaje", USUARIO).with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tieneHistorial").value(false))
                    .andExpect(jsonPath("$.nivelDeConfianza").value("SIN_HISTORIAL"));
        }

        @Test
        @DisplayName("CU-71 · 400: un identificador de usuario que no es un identificador")
        void usuarioQueNoEsUuid() throws Exception {
            mvc.perform(get("/reputacion/{id}/puntaje", "el-vecino").with(Sesiones.como("PARTICIPANTE")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(puntajes);
        }
    }

    @Nested
    @DisplayName("POST /reputacion/certificados — lo que el titular decide mostrar")
    class Certificado {

        private static final String CUERPO =
                """
                {"usuarioId":"e1000000-0000-4000-8000-000000000001",
                 "snapshotId":"e1000000-0000-4000-8000-000000000002",
                 "disponibles":{"puntaje":"87.50","grupos":"4","documento":"1234567"},
                 "incluir":["puntaje","grupos"],
                 "vigenciaDias":90,"identidadVerificada":true}
                """;

        private org.springframework.test.web.servlet.ResultActions emitir(String cuerpo) throws Exception {
            return mvc.perform(post("/reputacion/certificados")
                    .with(Sesiones.como("PARTICIPANTE"))
                    .header("Idempotency-Key", CLAVE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo));
        }

        @Test
        @DisplayName("CU-75 · 201 con el código de verificación, la URL pública y el hash del contenido")
        void caminoFeliz() throws Exception {
            when(cu75.emitir(any(), any()))
                    .thenReturn(new CU75EmitirCertificado.SalidaCertificado(
                            CERTIFICADO,
                            "APY-CERT-2026-000123",
                            "https://aportaya.bo/verificar/APY-CERT-2026-000123",
                            "ab".repeat(32),
                            OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC),
                            OffsetDateTime.of(2027, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC),
                            Map.of("puntaje", "87.50", "grupos", "4")));

            String cuerpo = emitir(CUERPO)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigoVerificacion").value("APY-CERT-2026-000123"))
                    .andExpect(jsonPath("$.urlPublica").exists())
                    // Sin el hash, el certificado no se puede cotejar contra el papel.
                    .andExpect(jsonPath("$.hashContenido").value("ab".repeat(32)))
                    .andExpect(jsonPath("$.contenido.puntaje").value("87.50"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Lo que el titular NO eligio no entra al contenido. Si entrara, un
            // certificado de reputacion serviria para publicar el documento de alguien.
            org.assertj.core.api.Assertions.assertThat(cuerpo).doesNotContain("1234567");
        }

        @Test
        @DisplayName("CU-75 · lo disponible y lo elegido llegan separados al caso de uso")
        void loElegidoLlegaSeparadoDeLoDisponible() throws Exception {
            when(cu75.emitir(any(), any()))
                    .thenReturn(new CU75EmitirCertificado.SalidaCertificado(
                            CERTIFICADO, "APY-1", "url", "ab".repeat(32), null, null, Map.of()));

            emitir(CUERPO).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU75EmitirCertificado.EntradaCertificado.class);
            verify(cu75).emitir(capturada.capture(), any());
            var entrada = capturada.getValue();

            // El caso de uso necesita las dos listas: con `incluir` arma el contenido y
            // el hash; `disponibles` es de donde saca los valores. Confundirlas es
            // publicar lo que nadie eligio.
            org.assertj.core.api.Assertions.assertThat(entrada.incluir())
                    .containsExactlyInAnyOrder("puntaje", "grupos");
            org.assertj.core.api.Assertions.assertThat(entrada.disponibles())
                    .containsKeys("puntaje", "grupos", "documento");
            org.assertj.core.api.Assertions.assertThat(entrada.vigenciaDias()).isEqualTo(90);
            org.assertj.core.api.Assertions.assertThat(entrada.identidadVerificada())
                    .isTrue();
        }

        @Test
        @DisplayName("CU-75 · sin `incluir`, el caso de uso lo recibe VACIO — y un certificado vacío no se emite solo")
        void sinEleccionElCasoDeUsoRecibeVacio() throws Exception {
            // Misma divergencia que en auditoria: el contrato marca `incluir` como
            // `required`, pero el generador inicializa los arreglos vacios y `@NotNull`
            // nunca dispara. Quien rechaza es el caso de uso.
            //
            // Aca importa mas que en otros lados: `incluir` es lo que el titular
            // ELIGIO mostrar. Que llegue vacio y no nulo es la diferencia entre «no
            // eligio nada» y «se perdio la eleccion en el camino», y el caso de uso
            // tiene que poder distinguirlas antes de firmar un certificado.
            when(cu75.emitir(any(), any()))
                    .thenReturn(new CU75EmitirCertificado.SalidaCertificado(
                            CERTIFICADO, "APY-1", "url", "ab".repeat(32), null, null, Map.of()));

            emitir(CUERPO.replace("\"incluir\":[\"puntaje\",\"grupos\"],", "")).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU75EmitirCertificado.EntradaCertificado.class);
            verify(cu75).emitir(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().incluir())
                    .isEmpty();
        }

        @Test
        @DisplayName("CU-75 · 400: un snapshot que no es un identificador")
        void snapshotQueNoEsUuid() throws Exception {
            emitir(CUERPO.replace("e1000000-0000-4000-8000-000000000002", "el-de-enero"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu75);
        }
    }

    @Nested
    @DisplayName("Los permisos que separan reputación de auditoría")
    class SeparacionDePermisos {

        @Test
        @DisplayName("CU-72 · sellar el bloque exige AUDITORIA_LEER: SOPORTE no alcanza")
        void sellarNoEsDeSoporte() throws Exception {
            // Quien registra eventos de reputacion no puede ademas sellar la cadena que
            // los vuelve inmodificables: seria poder escribir y cerrar el acta.
            mvc.perform(post("/reputacion/bloques")
                            .with(Sesiones.como("SOPORTE"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu72);
        }

        @Test
        @DisplayName("CU-76 · moderar una reseña exige MODERADOR_CONTENIDO, no publicarla")
        void moderarNoEsPublicar() throws Exception {
            mvc.perform(post("/reputacion/resenas/{id}/moderacion", USUARIO)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu76);
        }

        @Test
        @DisplayName("CU-97 · evaluar riesgo es de cumplimiento, no de soporte")
        void evaluarRiesgoEsDeCumplimiento() throws Exception {
            mvc.perform(post("/reputacion/riesgo/evaluacion")
                            .with(Sesiones.como("SOPORTE"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu97);
        }
    }
}
