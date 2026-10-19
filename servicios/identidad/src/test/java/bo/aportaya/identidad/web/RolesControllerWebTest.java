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
    @DisplayName("seguridad: 401 si no hay sesion")
    void sinSesionRechaza401() throws Exception {
        mvc.perform(get("/roles")).andExpect(status().isUnauthorized());
        verifyNoInteractions(casoDeUso);
    }
}
