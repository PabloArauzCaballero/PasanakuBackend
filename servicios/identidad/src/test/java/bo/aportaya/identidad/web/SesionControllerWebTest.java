package bo.aportaya.identidad.web;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** ADR-010 · {@code POST /sesion/refrescar}: publico por token de acceso, autenticado por la cookie. */
@PruebaWeb(value = SesionController.class)
@Import(CookieDeRefresco.class)
class SesionControllerWebTest {

    private static final UUID OPERADOR = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    private static final String VIEJO = "b".repeat(64);
    private static final String NUEVO = "c".repeat(64);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RenovarSesion renovarSesion;

    @MockitoBean
    private EmitirAcceso emitirAcceso;

    @Test
    @DisplayName("con un refresh valido: 200 con el acceso nuevo, el rol, los permisos y la cookie ROTADA")
    void renueva() throws Exception {
        when(renovarSesion.renovar(eq(VIEJO), any(), any(), any()))
                .thenReturn(new RenovarSesion.Renovacion(
                        Optional.of(OPERADOR),
                        true,
                        Optional.of(new RenovarSesion.Emitido(
                                NUEVO, OffsetDateTime.now(ZoneOffset.UTC).plusHours(12)))));
        when(emitirAcceso.ejecutar(eq(OPERADOR), eq("BACKOFFICE"), any(), isNull()))
                .thenReturn(new EmitirAcceso.Acceso(
                        "acceso.nuevo", Instant.now().plusSeconds(900), List.of("ver:cumplimiento")));

        mvc.perform(post("/sesion/refrescar").cookie(new Cookie("aportaya_refresco", VIEJO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceso").value("acceso.nuevo"))
                .andExpect(jsonPath("$.rol").value("BACKOFFICE"))
                .andExpect(jsonPath("$.permisos[0]").value("ver:cumplimiento"))
                .andExpect(header().string(
                                "Set-Cookie",
                                allOf(
                                        startsWith("aportaya_refresco=" + NUEVO),
                                        containsString("HttpOnly"),
                                        containsString("Secure"),
                                        containsString("SameSite=Strict"),
                                        containsString("Path=/api/v1/sesion"))));
    }

    @Test
    @DisplayName("sin cookie: 401 AP-SES-01, se borra la cookie y no se emite acceso")
    void sinCookie() throws Exception {
        when(renovarSesion.renovar(isNull(), any(), any(), any()))
                .thenReturn(new RenovarSesion.Renovacion(Optional.empty(), false, Optional.empty()));

        mvc.perform(post("/sesion/refrescar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AP-SES-01"))
                .andExpect(header().string(
                                "Set-Cookie", allOf(startsWith("aportaya_refresco=;"), containsString("Max-Age=0"))));
        verify(emitirAcceso, never()).ejecutar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("refresh rechazado (vencido, revocado o reusado): 401 y la cookie se borra")
    void rechazado() throws Exception {
        when(renovarSesion.renovar(eq(VIEJO), any(), any(), any()))
                .thenReturn(new RenovarSesion.Renovacion(Optional.empty(), false, Optional.empty()));

        mvc.perform(post("/sesion/refrescar").cookie(new Cookie("aportaya_refresco", VIEJO)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        verify(emitirAcceso, never()).ejecutar(any(), any(), any(), any());
    }
}
