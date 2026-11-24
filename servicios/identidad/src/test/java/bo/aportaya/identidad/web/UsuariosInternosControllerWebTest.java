package bo.aportaya.identidad.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.NombreDeUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.List;
import java.util.Optional;
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
class UsuariosInternosControllerWebTest {

    private static final UUID USUARIO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID PERFIL_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarUsuariosInternos casoDeUso;

    @MockitoBean
    private ResolverNombresDeUsuariosInternos nombres;

    @Test
    @DisplayName("listar: 200 con lista de usuarios internos")
    void listarUsuariosInternosOk() throws Exception {
        UsuarioInternoResumen u = new UsuarioInternoResumen();
        u.setId(PERFIL_ID);
        u.setUsuarioId(USUARIO_ID);
        u.setNombres("Carlos");
        u.setApellidos("Mamani");
        u.setCorreo("carlos.mamani@aportaya.bo");
        u.setDepartamento("RIESGOS");
        u.setCargo("Analista de Riesgo");
        u.setEstado("ACTIVO");

        when(casoDeUso.listar(any(), any(), any(), any())).thenReturn(List.of(u));

        mvc.perform(get("/usuarios-internos").with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PERFIL_ID.toString()))
                .andExpect(jsonPath("$[0].correo").value("carlos.mamani@aportaya.bo"))
                .andExpect(jsonPath("$[0].departamento").value("RIESGOS"));
    }

    @Test
    @DisplayName("consultar: 200 con detalle completo de usuario interno")
    void consultarUsuarioInternoOk() throws Exception {
        UsuarioInternoDetalle d = new UsuarioInternoDetalle();
        d.setId(PERFIL_ID);
        d.setUsuarioId(USUARIO_ID);
        d.setNombres("Carlos");
        d.setApellidos("Mamani");
        d.setCorreo("carlos.mamani@aportaya.bo");
        d.setDepartamento("RIESGOS");
        d.setCargo("Analista de Riesgo");
        d.setEstado("ACTIVO");
        d.setRoles(List.of("RISK_ANALYST"));
        d.setPermisosEfectivos(List.of("ALERTAS_RIESGO_LEER", "SCORE_CONSULTAR"));

        when(casoDeUso.consultar(any(), any())).thenReturn(Optional.of(d));

        mvc.perform(get("/usuarios-internos/{id}", PERFIL_ID)
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PERFIL_ID.toString()))
                .andExpect(jsonPath("$.roles[0]").value("RISK_ANALYST"))
                .andExpect(jsonPath("$.permisosEfectivos[0]").value("ALERTAS_RIESGO_LEER"));
    }

    @Test
    @DisplayName("consultar: 404 si el usuario interno no existe")
    void consultarUsuarioInternoNotFound() throws Exception {
        when(casoDeUso.consultar(any(), any())).thenReturn(Optional.empty());

        mvc.perform(get("/usuarios-internos/{id}", PERFIL_ID)
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("crear: 201 con Idempotency-Key y datos válidos")
    void crearUsuarioInternoOk() throws Exception {
        UsuarioInternoDetalle creado = new UsuarioInternoDetalle();
        creado.setId(PERFIL_ID);
        creado.setUsuarioId(USUARIO_ID);
        creado.setCorreo("nuevo.operador@aportaya.bo");
        creado.setDepartamento("OPERACIONES");

        when(casoDeUso.crear(any(EntradaAltaUsuarioInterno.class), any())).thenReturn(creado);

        String json =
                """
                {
                  "telefono": "+59178901234",
                  "nombres": "Laura",
                  "apellidos": "Rojas",
                  "correo": "nuevo.operador@aportaya.bo",
                  "departamento": "OPERACIONES",
                  "cargo": "Especialista de Conciliacion",
                  "rolCodigo": "OPERATIONS_ANALYST"
                }
                """;

        mvc.perform(post("/usuarios-internos")
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_GESTIONAR"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PERFIL_ID.toString()))
                .andExpect(jsonPath("$.correo").value("nuevo.operador@aportaya.bo"));
    }

    @Test
    @DisplayName("actualizar: 200 al modificar departamento, cargo o estado con motivo")
    void actualizarUsuarioInternoOk() throws Exception {
        UsuarioInternoDetalle modificado = new UsuarioInternoDetalle();
        modificado.setId(PERFIL_ID);
        modificado.setCargo("Líder de Riesgo");

        when(casoDeUso.actualizar(any(), any(EntradaActualizarUsuarioInterno.class), any()))
                .thenReturn(modificado);

        String json =
                """
                {
                  "cargo": "Líder de Riesgo",
                  "motivo": "Promocion interna aprobada en comite"
                }
                """;

        mvc.perform(patch("/usuarios-internos/{id}", PERFIL_ID)
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_GESTIONAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("Líder de Riesgo"));
    }

    @Test
    @DisplayName("asignarRoles: 200 con lista de roles y motivo CU-08")
    void asignarRolesOk() throws Exception {
        UsuarioInternoDetalle actualizado = new UsuarioInternoDetalle();
        actualizado.setId(PERFIL_ID);
        actualizado.setRoles(List.of("RISK_ANALYST", "FRAUD_ANALYST"));

        when(casoDeUso.asignarRoles(any(), any(EntradaAsignarRolesUsuarioInterno.class), any()))
                .thenReturn(actualizado);

        String json =
                """
                {
                  "roles": ["RISK_ANALYST", "FRAUD_ANALYST"],
                  "motivo": "Asignacion de funciones de monitoreo antifraude"
                }
                """;

        mvc.perform(put("/usuarios-internos/{id}/roles", PERFIL_ID)
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_GESTIONAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[1]").value("FRAUD_ANALYST"));
    }

    @Test
    @DisplayName("desbloquear: 200 con motivo formal")
    void desbloquearOk() throws Exception {
        UsuarioInternoDetalle desbloqueado = new UsuarioInternoDetalle();
        desbloqueado.setId(PERFIL_ID);
        desbloqueado.setBloqueadoHasta(null);

        when(casoDeUso.desbloquear(any(), any(), any())).thenReturn(desbloqueado);

        String json =
                """
                {
                  "motivo": "Verificacion de identidad por canal seguro completada"
                }
                """;

        mvc.perform(post("/usuarios-internos/{id}/desbloquear", PERFIL_ID)
                        .with(Sesiones.como("SISTEMAS_ADMIN", "USUARIOS_INTERNOS_GESTIONAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("seguridad: 401 si no hay sesion autenticada")
    void sinSesionRechaza401() throws Exception {
        mvc.perform(get("/usuarios-internos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(casoDeUso);
    }

    @Test
    @DisplayName("seguridad: 403 si el rol no tiene el permiso necesario")
    void rolInsuficienteRechaza403() throws Exception {
        mvc.perform(get("/usuarios-internos").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/usuarios-internos")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(casoDeUso);
    }

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
