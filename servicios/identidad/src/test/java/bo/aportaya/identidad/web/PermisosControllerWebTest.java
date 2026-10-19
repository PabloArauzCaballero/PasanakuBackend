package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.web.generado.modelo.PermisoCatalogo;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@PruebaWeb(PermisosController.class)
class PermisosControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarUsuariosInternos casoDeUso;

    @Test
    @DisplayName("catalogo permisos: 200 con lista de permisos")
    void listarPermisosOk() throws Exception {
        PermisoCatalogo pc = new PermisoCatalogo();
        pc.setId(UUID.randomUUID());
        pc.setCodigo("ALERTAS_RIESGO_LEER");
        pc.setRecurso("ALERTAS_RIESGO");
        pc.setAccion("LEER");
        pc.setNivelRiesgo(PermisoCatalogo.NivelRiesgoEnum.MEDIO);

        when(casoDeUso.listarPermisos(any())).thenReturn(List.of(pc));

        mvc.perform(get("/permisos").with(Sesiones.como("SISTEMAS_ADMIN", "PERMISOS_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ALERTAS_RIESGO_LEER"))
                .andExpect(jsonPath("$[0].nivelRiesgo").value("MEDIO"));
    }

    @Test
    @DisplayName("seguridad: 401 si no hay sesion")
    void sinSesionRechaza401() throws Exception {
        mvc.perform(get("/permisos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(casoDeUso);
    }
}
