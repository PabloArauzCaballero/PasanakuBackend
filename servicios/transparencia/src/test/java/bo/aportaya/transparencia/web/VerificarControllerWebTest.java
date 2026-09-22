package bo.aportaya.transparencia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.transparencia.aplicacion.CU75EmitirCertificado;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-75 · {@code GET /verificar/{codigo}}, la ruta que hace que un certificado sirva.
 *
 * <p>Un certificado que solo puede comprobar quien lo emitió no es un certificado. Por
 * eso esta ruta es pública, y por eso lo que <b>no</b> devuelve importa tanto como lo
 * que devuelve: el código es lo único que hace falta para preguntar, así que quien
 * tenga uno ajeno no puede además averiguar de quién es ni qué dice.
 */
@PruebaWeb(VerificarController.class)
class VerificarControllerWebTest {

    private static final String CODIGO = "APY-CERT-2026-000123";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU75EmitirCertificado cu75;

    @Test
    @DisplayName("CU-75 · 200 SIN sesión: vigente, con emisión y vencimiento")
    void certificadoVigente() throws Exception {
        when(cu75.verificarPublico(any(), any(), any()))
                .thenReturn(new CU75EmitirCertificado.SalidaVerificacion(
                        true,
                        "VIGENTE",
                        OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2027, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC)));

        // Sin sesion: un certificado que solo puede verificar el emisor no certifica.
        mvc.perform(get("/verificar/{codigo}", CODIGO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.emitidoEn").exists())
                .andExpect(jsonPath("$.expiraEn").exists());
    }

    @ParameterizedTest(name = "CU-75 · {0} responde 200 y valido=false")
    @CsvSource({"VENCIDO", "REVOCADO", "NO_VALIDO"})
    @DisplayName("CU-75 · un certificado que no sirve lo dice con su estado, y sigue siendo 200")
    void certificadoQueNoSirve(String estado) throws Exception {
        when(cu75.verificarPublico(any(), any(), any()))
                .thenReturn(new CU75EmitirCertificado.SalidaVerificacion(false, estado, null, null));

        // 200 y no 404: la pregunta se pudo contestar. Un 404 ademas diria que el
        // codigo no existe, y eso permite averiguar cuales si existen a fuerza de
        // probar.
        mvc.perform(get("/verificar/{codigo}", CODIGO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.estado").value(estado));
    }

    @Test
    @DisplayName("CU-75 · la respuesta pública no dice DE QUIÉN es el certificado ni qué dice")
    void noRevelaAlTitular() throws Exception {
        when(cu75.verificarPublico(any(), any(), any()))
                .thenReturn(new CU75EmitirCertificado.SalidaVerificacion(
                        true,
                        "VIGENTE",
                        OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2027, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC)));

        String cuerpo = mvc.perform(get("/verificar/{codigo}", CODIGO))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Cuatro campos y ninguno del titular. Con el codigo alcanza para preguntar, y
        // el codigo puede andar circulando en una foto.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("usuario")
                .doesNotContain("nombre")
                .doesNotContain("documento")
                .doesNotContain("contenido")
                .doesNotContain("puntaje");
    }

    @Test
    @DisplayName("CU-75 · el código de la ruta llega tal cual al caso de uso")
    void elCodigoLlegaEntero() throws Exception {
        when(cu75.verificarPublico(any(), any(), any()))
                .thenReturn(new CU75EmitirCertificado.SalidaVerificacion(true, "VIGENTE", null, null));

        mvc.perform(get("/verificar/{codigo}", CODIGO)).andExpect(status().isOk());

        verify(cu75).verificarPublico(eq(CODIGO), any(), any());
    }
}
