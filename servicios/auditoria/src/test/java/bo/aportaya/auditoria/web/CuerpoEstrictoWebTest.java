package bo.aportaya.auditoria.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.CU07EjercerDerechos;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H13.S1.M2 · El contrato dice {@code additionalProperties: false}: un campo que no declara se rechaza, no se
 * ignora en silencio (96.2.2, mass assignment). Sin {@code spring.jackson.deserialization.fail-on-unknown-properties}
 * el servidor aceptaba el cuerpo con el campo de mas y contestaba como si nada.
 */
@PruebaWeb(AuditoriaController.class)
class CuerpoEstrictoWebTest {

    private static final String CLAVE = "a0000000-0000-4000-8000-0000000000fd";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU07EjercerDerechos cu07;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos catalogosCasoDeUso;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.ConsultarBitacora bitacoraCasoDeUso;

    @Test
    @DisplayName("rechaza: un campo que el contrato no declara devuelve 400 y el caso de uso ni se entera")
    void rechazaElCampoDeMas() throws Exception {
        mvc.perform(
                        post("/auditoria/derechos")
                                .with(Sesiones.como("ANALISTA_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                                .header("Idempotency-Key", CLAVE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"usuarioId":"a0000000-0000-4000-8000-000000000001","tipo":"ACCESO",
                                 "descripcion":"Solicito el acceso a mis datos personales",
                                 "ultimaActividad":"2026-01-15","esAdministrador":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-02"));

        verifyNoInteractions(cu07);
    }
}
