package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CambiarClaveDeCuenta;
import bo.aportaya.identidad.aplicacion.InvitacionesDeCuenta;
import bo.aportaya.identidad.aplicacion.RecuperacionesDeAcceso;
import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import bo.aportaya.identidad.web.generado.modelo.SalidaRecuperaciones;
import bo.aportaya.identidad.web.generado.modelo.SolicitudDeRecuperacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * El contrato HTTP de {@code /cuenta/*} (CU-09 §10-§11): lo publico responde igual ante el error y no pide
 * sesion; lo que decide sobre el acceso de otra persona exige su permiso; ningun cuerpo nombra a otro usuario.
 * Los casos de uso son dobles declarados: la logica se prueba contra la base en {@code CU09CuentaDeOperadorTest}.
 */
@PruebaWeb(value = CuentaController.class)
@Import(CookieDeRefresco.class)
class CuentaControllerWebTest {

    private static final UUID TITULAR = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID SOLICITUD = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final String TOKEN_SINTETICO = "t".repeat(43);
    private static final String PERMISO = "SEGURIDAD_ACCESO_RESTABLECER";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InvitacionesDeCuenta invitaciones;

    @MockitoBean
    private RecuperacionesDeAcceso recuperaciones;

    @MockitoBean
    private CambiarClaveDeCuenta cambioDeClave;

    private static RequestPostProcessor token(UUID usuario, String rol, String... permisos) {
        return jwt().jwt(t -> t.subject(usuario.toString())
                .claim("rol", rol)
                .claim("permisos", List.of(permisos))
                .claim("sesion_id", UUID.randomUUID().toString()));
    }

    private static SolicitudDeRecuperacion solicitud() {
        SolicitudDeRecuperacion s = new SolicitudDeRecuperacion();
        s.setId(SOLICITUD);
        s.setUsuarioId(TITULAR);
        s.setNombres("Operadora");
        s.setApellidos("Demo");
        s.setAlcance("CLAVE");
        s.setEstado("APROBADA");
        s.setSolicitadaEn(OffsetDateTime.now(ZoneOffset.UTC));
        s.setMotivo("Pedido del propio operador");
        return s;
    }

    @Test
    @DisplayName("correcto: consultar la invitacion no pide sesion, devuelve el alcance y no se cachea")
    void consultarSinSesion() throws Exception {
        when(invitaciones.consultar(any(), any()))
                .thenReturn(new InvitacionesDeCuenta.Consulta(
                        AlcanceDeCuenta.ALTA,
                        "Operadora",
                        OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)));

        mvc.perform(post("/cuenta/invitacion/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN_SINTETICO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.alcance").value("ALTA"))
                .andExpect(jsonPath("$.requiereFactor").value(true))
                .andExpect(jsonPath("$.requiereClaveActual").value(false));
    }

    @Test
    @DisplayName("invalido: un token que no sirve responde 422 AP-CU09-04, el mismo mensaje en todos los casos")
    void tokenInvalido() throws Exception {
        when(invitaciones.consultar(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(9, 4), "Ese enlace ya no sirve."));

        mvc.perform(post("/cuenta/invitacion/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN_SINTETICO + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("invalido: completar con un rechazo del caso de uso se traduce a 422 despues del commit")
    void completarRechazado() throws Exception {
        when(invitaciones.completar(any(), any()))
                .thenReturn(new InvitacionesDeCuenta.Resultado(
                        null, 0, new ErrorDeNegocio(CodigoError.de(4, 4), "Ese codigo no coincide.")));

        mvc.perform(post("/cuenta/invitacion/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN_SINTETICO + "\",\"codigo\":\"123456\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("correcto: completar devuelve el alcance y las sesiones cerradas")
    void completarCorrecto() throws Exception {
        when(invitaciones.completar(any(), any()))
                .thenReturn(new InvitacionesDeCuenta.Resultado(AlcanceDeCuenta.CLAVE, 2, null));

        mvc.perform(post("/cuenta/invitacion/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN_SINTETICO + "\",\"claveNueva\":\"una-clave-larga-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcance").value("CLAVE"))
                .andExpect(jsonPath("$.sesionesCerradas").value(2));
    }

    @Test
    @DisplayName("invalido: un cuerpo con un campo no declarado se rechaza (400), nunca se ignora")
    void campoNoDeclarado() throws Exception {
        mvc.perform(post("/cuenta/invitacion/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN_SINTETICO + "\",\"usuarioId\":\"" + TITULAR + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(invitaciones);
    }

    @Test
    @DisplayName("correcto: pedir la recuperacion responde 202 con el mismo mensaje y sin sesion")
    void pedirRecuperacion() throws Exception {
        mvc.perform(post("/cuenta/recuperaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"operadora@ejemplo.test\",\"alcance\":\"CLAVE\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        verify(recuperaciones).pedir(eq("operadora@ejemplo.test"), eq("CLAVE"), any(), any());
    }

    @Test
    @DisplayName("invalido: un alcance que no es CLAVE ni FACTOR se rechaza (400)")
    void alcanceInvalido() throws Exception {
        mvc.perform(post("/cuenta/recuperaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"operadora@ejemplo.test\",\"alcance\":\"TODO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recuperaciones);
    }

    @Test
    @DisplayName("invalido: la bandeja sin token es 401 y con un rol sin el permiso es 403")
    void bandejaSinPermiso() throws Exception {
        mvc.perform(get("/cuenta/recuperaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/cuenta/recuperaciones").with(token(TITULAR, "BACKOFFICE", "OTRO_PERMISO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recuperaciones);
    }

    @Test
    @DisplayName("correcto: con el permiso la bandeja responde y el actor es el del token")
    void bandejaConPermiso() throws Exception {
        SalidaRecuperaciones salida = new SalidaRecuperaciones();
        salida.setItems(List.of(solicitud()));
        when(recuperaciones.listar(any(), any(), any(), any(), any())).thenReturn(salida);

        mvc.perform(get("/cuenta/recuperaciones")
                        .param("estado", "PENDIENTE")
                        .with(token(TITULAR, "BACKOFFICE", PERMISO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(SOLICITUD.toString()));
        verify(recuperaciones)
                .listar(
                        eq("PENDIENTE"),
                        any(),
                        any(),
                        any(),
                        argThat((ContextoSesion c) -> TITULAR.equals(c.usuarioId())));
    }

    @Test
    @DisplayName("invalido: aprobar o rechazar sin el permiso es 403 y no toca el caso de uso")
    void decidirSinPermiso() throws Exception {
        String cuerpo = "{\"motivo\":\"Verificado en persona\",\"evidenciaMfa\":\"" + "e".repeat(30) + "\"}";
        mvc.perform(post("/cuenta/recuperaciones/{id}/aprobacion", SOLICITUD)
                        .with(token(TITULAR, "BACKOFFICE", "OTRO_PERMISO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cuenta/recuperaciones/{id}/rechazo", SOLICITUD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(recuperaciones);
    }

    @Test
    @DisplayName("correcto: aprobar pasa el motivo, la evidencia y el actor del token")
    void aprobar() throws Exception {
        when(recuperaciones.aprobar(any(), any(), any(), any())).thenReturn(solicitud());

        mvc.perform(post("/cuenta/recuperaciones/{id}/aprobacion", SOLICITUD)
                        .with(token(TITULAR, "BACKOFFICE", PERMISO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Verificado en persona\",\"evidenciaMfa\":\"" + "e".repeat(30) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
        verify(recuperaciones)
                .aprobar(
                        eq(SOLICITUD),
                        eq("Verificado en persona"),
                        eq("e".repeat(30)),
                        argThat((ContextoSesion c) -> TITULAR.equals(c.usuarioId())));
    }

    @Test
    @DisplayName("invalido: cambiar la clave sin token es 401, y el cuerpo no puede nombrar a otro usuario")
    void cambiarClaveSinToken() throws Exception {
        String cuerpo = "{\"claveActual\":\"la-actual\",\"claveNueva\":\"una-clave-larga-1\"}";
        mvc.perform(post("/cuenta/clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/cuenta/clave")
                        .with(token(TITULAR, "BACKOFFICE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"la-actual\",\"claveNueva\":\"una-clave-larga-1\",\"usuarioId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cambioDeClave);
    }

    @Test
    @DisplayName("correcto: cambiar la clave usa el usuario del token y avisa si cierra la sesion actual")
    void cambiarClave() throws Exception {
        when(cambioDeClave.cambiar(any(), any(), any(), any())).thenReturn(new CambiarClaveDeCuenta.Salida(3, true));

        mvc.perform(post("/cuenta/clave")
                        .with(token(TITULAR, "BACKOFFICE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"la-actual\",\"claveNueva\":\"una-clave-larga-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cierraLaSesionActual").value(true))
                .andExpect(jsonPath("$.sesionesCerradas").value(3));
        verify(cambioDeClave)
                .cambiar(
                        eq("la-actual"),
                        eq("una-clave-larga-1"),
                        any(),
                        argThat((ContextoSesion c) -> TITULAR.equals(c.usuarioId())));
    }
}
