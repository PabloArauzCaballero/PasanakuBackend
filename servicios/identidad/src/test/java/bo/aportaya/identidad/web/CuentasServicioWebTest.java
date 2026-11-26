package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Rotacion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * GU-18 (CU-136) en la capa web: el contrato, la credencial que sale solo en la rotacion y con {@code no-store}, y la
 * matriz de autorizacion negativa. El caso de uso va con doble; la persistencia se prueba en
 * {@code CU136GobernarCuentasDeServicioTest}.
 */
@PruebaWeb(CuentasServicioController.class)
class CuentasServicioWebTest {

    private static final UUID CUENTA = UUID.fromString("33333333-4444-4555-8666-777777777777");
    private static final String EVIDENCIA = "evidencia-step-up-sintetica-de-prueba";
    private static final String CUERPO_MOTIVO =
            """
            {"motivo":"Rotacion programada","evidenciaMfa":"%s"}
            """.formatted(EVIDENCIA);
    private static final String CUERPO_ALTA =
            """
            {"usuarioId":"%s","nombre":"Servicio de entregas","venceEn":"2027-01-15T10:30:00Z",
             "motivo":"Alta del servicio","evidenciaMfa":"%s"}
            """
                    .formatted(UUID.randomUUID(), EVIDENCIA);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU136GobernarCuentasDeServicio casoDeUso;

    @Test
    @DisplayName("rotar: 200 con la credencial en la respuesta, sin cache, y sin que el log del caso de uso la muestre")
    void rotaYEntregaUnaVez() throws Exception {
        Rotacion r = new Rotacion(
                CUENTA,
                "credencial-sintetica-de-un-solo-uso",
                OffsetDateTime.now().plusDays(30),
                1);
        when(casoDeUso.rotar(any(), any(), any())).thenReturn(r);

        mvc.perform(json(post("/cuentas-servicio/{id}/rotacion", CUENTA), gestor(), CUERPO_MOTIVO))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.credencial").value("credencial-sintetica-de-un-solo-uso"))
                .andExpect(jsonPath("$.sesionesCerradas").value(1));
        org.assertj.core.api.Assertions.assertThat(r.toString()).doesNotContain("credencial-sintetica");
    }

    @Test
    @DisplayName("listar: 200 con lectura; la lista no trae el campo credencial ni hash")
    void listaSinCredencial() throws Exception {
        when(casoDeUso.listar(any())).thenReturn(List.of());

        mvc.perform(get("/cuentas-servicio").with(Sesiones.como("AUDITOR_INTERNO", "USUARIOS_INTERNOS_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("revocar una cuenta ya revocada llega como 422 AP-CU136-08")
    void revocarUnaRevocada() throws Exception {
        when(casoDeUso.revocar(any(), any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        CU136GobernarCuentasDeServicio.CUENTA_REVOCADA, "Esa cuenta ya esta revocada."));

        mvc.perform(json(post("/cuentas-servicio/{id}/revocacion", CUENTA), gestor(), CUERPO_MOTIVO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU136-08"));
    }

    @Test
    @DisplayName(
            "autorizacion negativa: sin token 401; sin permiso, solo lectura o de otro dominio 403, sin tocar el caso"
                    + " de uso")
    void matrizDeAutorizacionNegativa() throws Exception {
        mvc.perform(get("/cuentas-servicio")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/cuentas-servicio"), null, CUERPO_ALTA)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/cuentas-servicio/{id}/rotacion", CUENTA), null, CUERPO_MOTIVO))
                .andExpect(status().isUnauthorized());

        RequestPostProcessor soloLectura = Sesiones.como("AUDITOR_INTERNO", "USUARIOS_INTERNOS_LEER");
        RequestPostProcessor otroDominio = Sesiones.como("TESORERIA", "TESORERIA_OPERAR");
        for (RequestPostProcessor sesion : new RequestPostProcessor[] {soloLectura, otroDominio}) {
            mvc.perform(json(post("/cuentas-servicio"), sesion, CUERPO_ALTA)).andExpect(status().isForbidden());
            mvc.perform(json(post("/cuentas-servicio/{id}/rotacion", CUENTA), sesion, CUERPO_MOTIVO))
                    .andExpect(status().isForbidden());
            mvc.perform(json(post("/cuentas-servicio/{id}/revocacion", CUENTA), sesion, CUERPO_MOTIVO))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/cuentas-servicio").with(otroDominio)).andExpect(status().isForbidden());
        verifyNoInteractions(casoDeUso);
    }

    @Test
    @DisplayName("entrada invalida: un campo no declarado es 400, nunca 500")
    void entradaInvalida() throws Exception {
        String conCampoDeMas = CUERPO_MOTIVO.replace("\"motivo\"", "\"credencial\":\"x\",\"motivo\"");

        mvc.perform(json(post("/cuentas-servicio/{id}/rotacion", CUENTA), gestor(), conCampoDeMas))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(casoDeUso);
    }

    private static RequestPostProcessor gestor() {
        return Sesiones.como("INTERNAL_IDENTITY_ADMIN", "USUARIOS_INTERNOS_GESTIONAR");
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder pedido, RequestPostProcessor sesion, String cuerpo) {
        pedido.header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
        return sesion == null ? pedido : pedido.with(sesion);
    }
}
