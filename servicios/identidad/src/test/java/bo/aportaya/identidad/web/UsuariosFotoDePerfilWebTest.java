package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02ActualizarFechaExpiracion;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.CU07FotoDePerfil;
import bo.aportaya.identidad.aplicacion.ConfirmarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ConsultarNivelKyc;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.plataforma.archivos.ContenidoAlmacenado;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-07 · la foto de perfil propia: {@code PUT/GET /usuarios/yo/foto-de-perfil}. Siempre de la sesion,
 * nunca de un id del cliente; los tres niveles del contrato (correcto, limite, invalido).
 */
@PruebaWeb(
        value = UsuariosController.class,
        properties = {"aportaya.seguridad.pimienta=pimienta-de-prueba-no-es-la-de-produccion"})
class UsuariosFotoDePerfilWebTest {

    private static final String RUTA = "/usuarios/yo/foto-de-perfil";
    /** Cabecera JPEG (FF D8 FF): sintetica, no es la cara de nadie. */
    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'
    };

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU07FotoDePerfil fotoDePerfil;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsumirInvitacion consumirInvitacion;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion validacionDeInvitacion;

    @MockitoBean
    private CU02GuardarFotoDelExpediente guardarFoto;

    @MockitoBean
    private CU02ConsultarEstadoDeVerificacion estadoDeVerificacion;

    @MockitoBean
    private CU02ActualizarFechaExpiracion actualizarFechaExpiracion;

    @MockitoBean
    private CU01RegistrarUsuario cu01;

    @MockitoBean
    private BuscarPorTelefono busqueda;

    @MockitoBean
    private EmitirTokenDeInvitacion tokens;

    @MockitoBean
    private VerificarTitularidad titularidad;

    @MockitoBean
    private SolicitarVerificacionCorreo solicitarCorreo;

    @MockitoBean
    private ConfirmarVerificacionCorreo confirmarCorreo;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ActivarRolOrganizador activarRolOrganizador;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.GuardarBorradorOnboarding guardarBorrador;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.ConsultarMiPerfil consultarMiPerfil;

    @MockitoBean
    private bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion iniciarVerificacion;

    @MockitoBean
    private ConsultarNivelKyc nivelesKyc;

    @Test
    @DisplayName("Sin sesion: 401 al subir y al ver, y no se toca el almacen")
    void sinSesion() throws Exception {
        mvc.perform(multipart(RUTA)
                        .file(new MockMultipartFile("archivo", "yo.jpg", "image/jpeg", FOTO))
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        }))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(fotoDePerfil);
    }

    @Test
    @DisplayName("Correcto: sube con la sesion de quien llama y responde 204")
    void subeLaPropia() throws Exception {
        mvc.perform(multipart(RUTA)
                        .file(new MockMultipartFile("archivo", "yo.jpg", "image/jpeg", FOTO))
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isNoContent());
        verify(fotoDePerfil)
                .cambiar(
                        argThat(ctx -> Sesiones.USUARIO.equals(ctx.usuarioId())),
                        any(),
                        eq((long) FOTO.length),
                        eq("yo.jpg"));
    }

    @Test
    @DisplayName("Invalido: un tipo o tamano que el ambito rechaza es 422 del contrato, no 500")
    void tipoRechazado() throws Exception {
        doThrow(new ErrorDeDominio("Ese tipo de archivo no se admite aca: application/pdf"))
                .when(fotoDePerfil)
                .cambiar(any(), any(), anyLong(), any());
        mvc.perform(multipart(RUTA)
                        .file(new MockMultipartFile("archivo", "yo.pdf", "application/pdf", FOTO))
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-DOM-01"));
    }

    @Test
    @DisplayName("Limite: la cuenta sin foto responde 404 sin cuerpo")
    void sinFoto() throws Exception {
        when(fotoDePerfil.ver(any())).thenReturn(Optional.empty());
        mvc.perform(get(RUTA).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Correcto: devuelve los bytes con su tipo y sin cache compartida")
    void verLaPropia() throws Exception {
        when(fotoDePerfil.ver(argThat(ctx -> Sesiones.USUARIO.equals(ctx.usuarioId()))))
                .thenReturn(Optional.of(
                        new ContenidoAlmacenado(new ByteArrayInputStream(FOTO), FOTO.length, "image/jpeg")));
        mvc.perform(get(RUTA).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(content().bytes(FOTO));
    }
    /** Movida desde UsuariosAutorizacionNegativaWebTest (limite de 300 lineas), mas la asercion de la foto. */
    @Test
    @DisplayName("CU-07 · mi perfil: sin sesión 401; con sesión devuelve el correo de la cuenta de ESA sesión")
    void miPerfilSaleDeLaSesion() throws Exception {
        var ruta = "/usuarios/yo/perfil";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ruta))
                .andExpect(status().isUnauthorized());
        var yo = UUID.fromString("b8000000-0000-4000-8000-000000000077");
        when(consultarMiPerfil.ejecutar(any()))
                .thenReturn(new bo.aportaya.identidad.aplicacion.ConsultarMiPerfil.Perfil(
                        yo, "ana@correo.test", true, "ACTIVO", "Ana M.", "Ana Maria Mendoza", "+59170000077", true));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ruta)
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.test"))
                .andExpect(jsonPath("$.correoVerificado").value(true))
                .andExpect(jsonPath("$.estadoDeCuenta").value("ACTIVO"))
                .andExpect(jsonPath("$.nombreVisible").value("Ana M."))
                .andExpect(jsonPath("$.nombreCompleto").value("Ana Maria Mendoza"))
                .andExpect(jsonPath("$.tieneFotoDePerfil").value(true))
                .andExpect(jsonPath("$.telefono").value("+59170000077"));
    }
}
