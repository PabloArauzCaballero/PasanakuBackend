package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.ConsultarContactoEnGrupo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Perfil del integrante (2026-10-21): el telefono de alguien, solo para quien comparte su grupo. */
@PruebaWeb(VerificacionesController.class)
class ContactoEnGrupoWebTest {

    private static final UUID GRUPO = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("e0000000-0000-4000-8000-000000000002");
    private static final String RUTA = "/identidad/contactos/grupos/{g}/integrantes/{u}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarContactoEnGrupo contactos;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CU02RevisarExpediente revision;

    @Test
    @DisplayName("contacto · 200: quien comparte el grupo ve el telefono, y nada mas de la cuenta")
    void compartenVenElTelefono() throws Exception {
        when(contactos.ejecutar(eq(GRUPO), eq(USUARIO), any())).thenReturn("+59171234567");
        mvc.perform(get(RUTA, GRUPO, USUARIO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefonoE164").value("+59171234567"))
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andExpect(jsonPath("$.estado").doesNotExist());
    }

    @Test
    @DisplayName("contacto · 403: quien no comparte el grupo (o no se pudo comprobar) no ve nada")
    void noCompartenEs403() throws Exception {
        when(contactos.ejecutar(eq(GRUPO), eq(USUARIO), any())).thenThrow(new AccessDeniedException("no comparte"));
        mvc.perform(get(RUTA, GRUPO, USUARIO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.telefonoE164").doesNotExist());
    }

    @Test
    @DisplayName("contacto · 401 sin sesion, 403 sin el permiso de participante, 400 con un id que no es UUID")
    void sinSesionNiPermiso() throws Exception {
        mvc.perform(get(RUTA, GRUPO, USUARIO)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA, GRUPO, USUARIO).with(Sesiones.como("OTRO_PERMISO")))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA, GRUPO, "no-es-uuid").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(contactos);
    }
}
