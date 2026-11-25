package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos;
import bo.aportaya.identidad.dominio.InformeDeAccesos;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El informe de accesos de CU-08 (GU-08) en la capa web: paginado, sin datos de contacto, 400/401/403. */
@PruebaWeb(UsuariosInternosController.class)
@MockitoBean(
        types = {
            bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos.class,
            bo.aportaya.identidad.aplicacion.ListarUsuariosInternos.class,
            bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos.class,
            bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.class,
            bo.aportaya.identidad.aplicacion.CU08DarDeBaja.class
        })
class RutasDeAccesosInformeWebTest {

    private static final UUID OPERADOR = UUID.fromString("33333333-4444-4555-8666-777777777777");
    private static final UUID ROL = UUID.fromString("44444444-5555-4666-8777-888888888888");
    private static final UUID ASIGNACION = UUID.fromString("55555555-6666-4777-8888-999999999999");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU08InformeDeAccesos informe;

    @Test
    @DisplayName("informe: 200 paginado con USUARIOS_INTERNOS_LEER, sin correo ni telefono")
    void informe() throws Exception {
        var fila = new InformeDeAccesos.Fila(
                ASIGNACION,
                OPERADOR,
                "Ana Quispe",
                "ACTIVO",
                "SOPORTE",
                "GLOBAL",
                Optional.empty(),
                ROL,
                OffsetDateTime.parse("2026-11-01T12:00:00Z"),
                Optional.of(OffsetDateTime.parse("2026-11-28T12:00:00Z")),
                false);
        when(informe.ejecutar(eq(InformeDeAccesos.Seccion.POR_VENCER), eq(2), eq(10), any()))
                .thenReturn(new CU08InformeDeAccesos.Pagina(
                        InformeDeAccesos.Seccion.POR_VENCER,
                        OffsetDateTime.parse("2026-11-24T12:00:00Z"),
                        List.of(fila),
                        2,
                        10,
                        11,
                        2));

        mvc.perform(get("/usuarios-internos/accesos/informe")
                        .param("seccion", "POR_VENCER")
                        .param("pagina", "2")
                        .param("limite", "10")
                        .with(Sesiones.como("SOPORTE", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seccion").value("POR_VENCER"))
                .andExpect(jsonPath("$.total").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.items[0].nombre").value("Ana Quispe"))
                .andExpect(jsonPath("$.items[0].totpConfirmado").value(false))
                .andExpect(jsonPath("$.items[0].correo").doesNotExist())
                .andExpect(jsonPath("$.items[0].telefono").doesNotExist());
    }

    @Test
    @DisplayName("informe: 400 con seccion, pagina o limite fuera de dominio; 401 y 403")
    void informeNegativos() throws Exception {
        var leer = Sesiones.como("SOPORTE", "USUARIOS_INTERNOS_LEER");
        mvc.perform(get("/usuarios-internos/accesos/informe").with(leer)).andExpect(status().isBadRequest());
        mvc.perform(get("/usuarios-internos/accesos/informe")
                        .param("seccion", "TODOS")
                        .with(leer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/usuarios-internos/accesos/informe")
                        .param("seccion", "VIVOS")
                        .param("limite", "101")
                        .with(leer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/usuarios-internos/accesos/informe")
                        .param("seccion", "VIVOS")
                        .param("pagina", "0")
                        .with(leer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/usuarios-internos/accesos/informe").param("seccion", "VIVOS"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios-internos/accesos/informe")
                        .param("seccion", "VIVOS")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verify(informe, never()).ejecutar(any(), anyInt(), anyInt(), any());
    }
}
