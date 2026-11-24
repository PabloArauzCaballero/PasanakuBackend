package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.ListarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** GU-17 · el listado de personal interno paginado: lo que la ruta entrega al caso de uso y lo que rechaza. */
@PruebaWeb(UsuariosInternosController.class)
class UsuariosInternosListadoWebTest {

    private static final UUID PERFIL_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-11-20T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarUsuariosInternos casoDeUso;

    @MockitoBean
    private ListarUsuariosInternos listado;

    @MockitoBean
    private ResolverNombresDeUsuariosInternos nombres;

    @Test
    @DisplayName("listar: 200 entrega limite y cursor al caso de uso y devuelve creadoEn para la pagina siguiente")
    void entregaElCursor() throws Exception {
        UsuarioInternoResumen u = new UsuarioInternoResumen();
        u.setId(PERFIL_ID);
        u.setCreadoEn(CREADO);
        when(listado.listar(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(u));

        mvc.perform(get("/usuarios-internos")
                        .param("rol", "TESORERIA")
                        .param("limite", "20")
                        .param("antesDe", "2026-11-21T00:00:00Z")
                        .param("antesDeId", PERFIL_ID.toString())
                        .with(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PERFIL_ID.toString()))
                .andExpect(jsonPath("$[0].creadoEn").exists());

        verify(listado)
                .listar(
                        isNull(),
                        eq("TESORERIA"),
                        isNull(),
                        eq(20),
                        eq(OffsetDateTime.parse("2026-11-21T00:00:00Z")),
                        eq(PERFIL_ID),
                        any());
    }

    @Test
    @DisplayName("listar: 400 si el limite pasa de 100; no llega al caso de uso")
    void limiteFueraDeRango() throws Exception {
        mvc.perform(get("/usuarios-internos")
                        .param("limite", "101")
                        .with(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listado);
    }

    @Test
    @DisplayName("seguridad: 401 sin sesion y 403 sin USUARIOS_INTERNOS_LEER ni ACCESOS_ADMINISTRAR")
    void autorizacionNegativa() throws Exception {
        mvc.perform(get("/usuarios-internos").param("limite", "5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios-internos").param("limite", "5").with(Sesiones.como("SOPORTE", "RECLAMO_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(listado);
    }
}
