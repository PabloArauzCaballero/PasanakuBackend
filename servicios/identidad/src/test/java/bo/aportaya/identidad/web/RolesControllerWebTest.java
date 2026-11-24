package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@PruebaWeb(RolesController.class)
class RolesControllerWebTest {

    private static final UUID ROL_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarUsuariosInternos casoDeUso;

    @Test
    @DisplayName("catalogo roles: 200 con lista de roles")
    void listarRolesOk() throws Exception {
        RolCatalogo rc = new RolCatalogo();
        rc.setId(ROL_ID);
        rc.setCodigo("RISK_ANALYST");
        rc.setNombre("Analista de Riesgo");
        rc.setDepartamento("RIESGOS");
        rc.setPermisos(List.of("ALERTAS_RIESGO_LEER"));

        when(casoDeUso.listarRoles(any())).thenReturn(List.of(rc));

        mvc.perform(get("/roles").with(Sesiones.como("SISTEMAS_ADMIN", "ROLES_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("RISK_ANALYST"))
                .andExpect(jsonPath("$[0].departamento").value("RIESGOS"));
    }

    @Test
    @DisplayName("GU-13 · con asignablesAPersonalInterno=true no vienen los de participante, grupo, anunciante ni"
            + " servicio")
    void soloLosAsignablesAPersonalInterno() throws Exception {
        when(casoDeUso.listarRoles(any())).thenReturn(catalogoMezclado());

        mvc.perform(get("/roles")
                        .param("asignablesAPersonalInterno", "true")
                        .with(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ACCESOS_ADMINISTRAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("TESORERIA"))
                .andExpect(jsonPath("$[1].codigo").value("SUPERVISOR_CUMPLIMIENTO"));
    }

    @Test
    @DisplayName("GU-13 · sin el parametro el catalogo viene completo, como antes (otros consumidores intactos)")
    void sinParametroVieneCompleto() throws Exception {
        when(casoDeUso.listarRoles(any())).thenReturn(catalogoMezclado());

        mvc.perform(get("/roles").with(Sesiones.como("INTERNAL_IDENTITY_ADMIN", "ROLES_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    @DisplayName("seguridad: 403 si la sesion no tiene ROLES_INTERNOS_LEER ni ACCESOS_ADMINISTRAR")
    void sinPermisoRechaza403() throws Exception {
        mvc.perform(get("/roles").param("asignablesAPersonalInterno", "true").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(casoDeUso);
    }

    private static List<RolCatalogo> catalogoMezclado() {
        return List.of(
                rol("TESORERIA", "GLOBAL"),
                rol("PARTICIPANTE", "GRUPO"),
                rol("ORGANIZADOR", "GRUPO"),
                rol("ANUNCIANTE", "GLOBAL"),
                rol("SERVICIO_ENTREGAS", "GLOBAL"),
                rol("SUPERVISOR_CUMPLIMIENTO", "GLOBAL"),
                rol("COMITE_DE_GRUPO", "GRUPO"));
    }

    private static RolCatalogo rol(String codigo, String ambito) {
        RolCatalogo rc = new RolCatalogo();
        rc.setId(UUID.randomUUID());
        rc.setCodigo(codigo);
        rc.setNombre(codigo);
        rc.setAmbito(ambito);
        rc.setPermisos(List.of());
        return rc;
    }

    @Test
    @DisplayName("seguridad: 401 si no hay sesion")
    void sinSesionRechaza401() throws Exception {
        mvc.perform(get("/roles")).andExpect(status().isUnauthorized());
        verifyNoInteractions(casoDeUso);
    }
}
