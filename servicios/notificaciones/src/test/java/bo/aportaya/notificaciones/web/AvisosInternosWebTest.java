package bo.aportaya.notificaciones.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta;
import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.aplicacion.ConsultarSupresion;
import bo.aportaya.notificaciones.aplicacion.GestionarAvisosInternos;
import bo.aportaya.notificaciones.aplicacion.SolicitarAviso;
import bo.aportaya.notificaciones.web.generado.modelo.AvisoInterno;
import bo.aportaya.notificaciones.web.generado.modelo.EntradaAvisoInterno;
import bo.aportaya.notificaciones.web.generado.modelo.PaginaDeAvisosInternos;
import bo.aportaya.notificaciones.web.generado.modelo.SalidaMarcarTodosLeidos;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas web de los endpoints de avisos internos para operadores (H5.S0).
 */
@PruebaWeb(NotificacionesController.class)
class AvisosInternosWebTest {

    private static final UUID AVISO_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitarAviso avisos;

    @MockitoBean
    private ConsultarSupresion supresiones;

    @MockitoBean
    private CU82ProcesarRespuesta cu82;

    @MockitoBean
    private ConsultarMensajeria mensajeria;

    @MockitoBean
    private GestionarAvisosInternos gestionarAvisosInternos;

    @Test
    @DisplayName("GET /notificaciones/avisos-internos: 200 devuelve lista y total de no leídos")
    void consultarAvisosOk() throws Exception {
        AvisoInterno aviso = new AvisoInterno()
                .id(AVISO_ID)
                .titulo("Alerta crítica")
                .mensaje("Se detectó un pico de mora")
                .tipo(AvisoInterno.TipoEnum.ALERTA)
                .creadoEn(AHORA);

        PaginaDeAvisosInternos pagina = new PaginaDeAvisosInternos()
                .totalNoLeidos(1)
                .elementos(List.of(aviso));

        when(gestionarAvisosInternos.consultar(any(), any(), anyBoolean(), anyInt(), any()))
                .thenReturn(pagina);

        mvc.perform(get("/notificaciones/avisos-internos")
                        .param("soloNoLeidos", "true")
                        .param("limite", "10")
                        .with(Sesiones.como("BACKOFFICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNoLeidos").value(1))
                .andExpect(jsonPath("$.elementos[0].id").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$.elementos[0].titulo").value("Alerta crítica"))
                .andExpect(jsonPath("$.elementos[0].tipo").value("ALERTA"));
    }

    @Test
    @DisplayName("GET /notificaciones/avisos-internos: 401 sin sesión, 403 sin rol de operador")
    void consultarAvisosSeguridad() throws Exception {
        mvc.perform(get("/notificaciones/avisos-internos"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/notificaciones/avisos-internos")
                        .with(Sesiones.sinPermisos()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /notificaciones/avisos-internos: 201 emite aviso interno")
    void emitirAvisoOk() throws Exception {
        AvisoInterno aviso = new AvisoInterno()
                .id(AVISO_ID)
                .titulo("Nuevo aviso")
                .mensaje("Mensaje operativo")
                .tipo(AvisoInterno.TipoEnum.INFO)
                .creadoEn(AHORA);

        when(gestionarAvisosInternos.emitir(any(EntradaAvisoInterno.class), any()))
                .thenReturn(aviso);

        String cuerpo = """
                {
                    "titulo": "Nuevo aviso",
                    "mensaje": "Mensaje operativo",
                    "tipo": "INFO"
                }
                """;

        mvc.perform(post("/notificaciones/avisos-internos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo)
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "ADMIN_PLATAFORMA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$.titulo").value("Nuevo aviso"));
    }

    @Test
    @DisplayName("POST /notificaciones/avisos-internos: 403 si no es ADMIN_PLATAFORMA")
    void emitirAvisoForbidden() throws Exception {
        String cuerpo = """
                {
                    "titulo": "Nuevo aviso",
                    "mensaje": "Mensaje operativo",
                    "tipo": "INFO"
                }
                """;

        mvc.perform(post("/notificaciones/avisos-internos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo)
                        .with(Sesiones.como("SOPORTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /notificaciones/avisos-internos/{id}/leido: 200 marca aviso como leído")
    void marcarAvisoLeidoOk() throws Exception {
        AvisoInterno aviso = new AvisoInterno()
                .id(AVISO_ID)
                .titulo("Aviso leído")
                .mensaje("Mensaje")
                .tipo(AvisoInterno.TipoEnum.INFO)
                .leidoEn(AHORA)
                .creadoEn(AHORA);

        when(gestionarAvisosInternos.marcarLeido(eq(AVISO_ID), any()))
                .thenReturn(Optional.of(aviso));

        mvc.perform(post("/notificaciones/avisos-internos/" + AVISO_ID + "/leido")
                        .with(Sesiones.como("BACKOFFICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$.leidoEn").isNotEmpty());
    }

    @Test
    @DisplayName("POST /notificaciones/avisos-internos/{id}/leido: 404 si no existe")
    void marcarAvisoLeidoNoExiste() throws Exception {
        when(gestionarAvisosInternos.marcarLeido(eq(AVISO_ID), any()))
                .thenReturn(Optional.empty());

        mvc.perform(post("/notificaciones/avisos-internos/" + AVISO_ID + "/leido")
                        .with(Sesiones.como("BACKOFFICE")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /notificaciones/avisos-internos/marcar-todos-leidos: 200 actualiza todos")
    void marcarTodosLeidosOk() throws Exception {
        when(gestionarAvisosInternos.marcarTodosLeidos(any(), any(), any()))
                .thenReturn(new SalidaMarcarTodosLeidos().modificados(5));

        mvc.perform(post("/notificaciones/avisos-internos/marcar-todos-leidos")
                        .with(Sesiones.como("BACKOFFICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modificados").value(5));
    }
}
