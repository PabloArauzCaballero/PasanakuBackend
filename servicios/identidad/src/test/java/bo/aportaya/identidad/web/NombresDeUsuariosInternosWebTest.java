package bo.aportaya.identidad.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos;
import bo.aportaya.identidad.web.generado.modelo.NombreDeUsuarioInterno;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@PruebaWeb(UsuariosInternosController.class)
class NombresDeUsuariosInternosWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarUsuariosInternos casoDeUso;

    @MockitoBean
    private ResolverNombresDeUsuariosInternos nombres;

    // --- POST /usuarios-internos/nombres (CU-132: quien propuso y quien aprobo) ---

    private static final UUID OPERADOR = UUID.fromString("c1320000-0000-4000-8000-00000000a001");
    private static final UUID CLIENTE = UUID.fromString("c1320000-0000-4000-8000-00000000c003");

    private static String lote(UUID... ids) {
        return "{\"usuarioIds\":["
                + java.util.Arrays.stream(ids).map(id -> "\"" + id + "\"").collect(Collectors.joining(","))
                + "]}";
    }

    @Test
    @DisplayName("nombres: 200 con usuarioId y nombre, sin correo, telefono, documento ni roles")
    void nombresDevuelveSoloElNombre() throws Exception {
        when(nombres.ejecutar(eq(List.of(OPERADOR, CLIENTE)), any()))
                .thenReturn(List.of(new NombreDeUsuarioInterno(OPERADOR, "Carla Gutiérrez Rojas")));

        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(OPERADOR, CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuarioId").value(OPERADOR.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Carla Gutiérrez Rojas"))
                .andExpect(jsonPath("$[0].correo").doesNotExist())
                .andExpect(jsonPath("$[0].telefono").doesNotExist())
                .andExpect(jsonPath("$[0].numeroDocumento").doesNotExist())
                .andExpect(jsonPath("$[0].roles").doesNotExist());
    }

    @Test
    @DisplayName("nombres: quien ya lee el personal interno tambien puede resolverlos")
    void nombresConPermisoAlternativo() throws Exception {
        when(nombres.ejecutar(any(), any())).thenReturn(List.of());

        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(OPERADOR)))
                .andExpect(status().isOk());
        verify(nombres).ejecutar(eq(List.of(OPERADOR)), any());
    }

    @Test
    @DisplayName("nombres: 401 sin sesion, sin llegar al caso de uso")
    void nombresSinSesion401() throws Exception {
        mvc.perform(post("/usuarios-internos/nombres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(OPERADOR)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(nombres);
    }

    @Test
    @DisplayName("nombres: 403 para un participante (cliente), sin llegar al caso de uso")
    void nombresParticipante403() throws Exception {
        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(OPERADOR)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.sinPermisos())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(OPERADOR)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(nombres);
    }

    @Test
    @DisplayName("nombres: 400 con mas de 50 ids, por contrato, sin llegar al caso de uso")
    void nombresLoteDeMasDe50() throws Exception {
        UUID[] ids = IntStream.range(0, 51).mapToObj(i -> UUID.randomUUID()).toArray(UUID[]::new);

        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(ids)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-01"));
        verifyNoInteractions(nombres);
    }

    @Test
    @DisplayName("nombres: 400 si el cuerpo trae un campo no declarado o viene vacio")
    void nombresCampoNoDeclarado() throws Exception {
        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioIds\":[\"" + OPERADOR + "\"],\"incluirCorreo\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioIds\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(nombres);
    }

    @Test
    @DisplayName("nombres: 422 AP-CU08-07 si la lista repite un id")
    void nombresRepetidos422() throws Exception {
        when(nombres.ejecutar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(8, 7), "La lista repite un identificador de operador."));

        mvc.perform(post("/usuarios-internos/nombres")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_ADMINISTRAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote(OPERADOR, OPERADOR)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU08-07"));
    }
}
