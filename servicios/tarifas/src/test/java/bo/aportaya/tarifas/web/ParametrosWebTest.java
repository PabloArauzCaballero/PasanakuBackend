package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarCostosDeProveedor;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarios;
import bo.aportaya.tarifas.dominio.VersionDelParametro;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * CU-132 en el borde: quien propone y quien aprueba salen de su token, la segunda firma es de otra persona y el
 * contrato no acepta identidades ni valores fuera del patrón decimal. Matriz negativa: misma persona, rol insuficiente, sin
 * token, campo no declarado. Datos sintéticos.
 */
@PruebaWeb(TarifasController.class)
@Import(ParametrosWeb.class)
@MockitoBean(
        types = {
            ConsultarCostosDeProveedor.class,
            ConsultarTarifarioVigente.class,
            ConsultarTarifarios.class,
            CU34PublicarTarifario.class,
            CU35CerrarLiquidacion.class,
            CU36ResolverPrecio.class,
            bo.aportaya.tarifas.aplicacion.ConsultarParametrosDelGrupo.class,
        })
class ParametrosWebTest {

    private static final UUID VERSION = UUID.fromString("c1320000-0000-4000-8000-000000000002");
    private static final String CLAVE = "c1320000-0000-4000-8000-0000000000ff";
    private static final String PERMISO = "TARIFARIO_ADMINISTRAR";
    private static final String CUERPO =
            """
            {"valor":"5","vigenteDesde":"2026-12-01T00:00:00Z",
             "procedencia":{"fuente":"Acta sintética","referencia":"REF-PRUEBA-1","fecha":"2026-11-20"}}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU132RegistroDeParametros cu132;

    private static VersionDelParametro version(String estado) {
        return new VersionDelParametro(
                VERSION,
                UUID.randomUUID(),
                "DIAS_DE_GRACIA",
                Optional.empty(),
                new BigDecimal("5.000000"),
                Optional.empty(),
                OffsetDateTime.parse("2026-12-01T00:00:00Z"),
                Sesiones.USUARIO,
                Optional.empty(),
                estado);
    }

    private ResultActions proponer(RequestPostProcessor quien, String cuerpo) throws Exception {
        return mvc.perform(post("/tarifas/parametros/{c}/versiones", "DIAS_DE_GRACIA")
                .with(quien)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions aprobar(RequestPostProcessor quien) throws Exception {
        return mvc.perform(post("/tarifas/parametros/versiones/{v}/aprobacion", VERSION)
                .with(quien)
                .header("Idempotency-Key", CLAVE));
    }

    @Test
    @DisplayName("Correcto · propone con su token: 201, valor como cadena decimal y sin identidades en la respuesta")
    void propone() throws Exception {
        when(cu132.proponer(eq("DIAS_DE_GRACIA"), any(), eq(UUID.fromString(CLAVE)), any()))
                .thenReturn(version("PROPUESTA"));

        proponer(Sesiones.enSesion(Sesiones.USUARIO, UUID.randomUUID(), "ADMIN_PLATAFORMA", PERMISO), CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value("5.000000"))
                .andExpect(jsonPath("$.estado").value("PROPUESTA"))
                .andExpect(jsonPath("$.propuestoPor").doesNotExist());
    }

    @Test
    @DisplayName("Inválido · un proponente en el cuerpo es un campo no declarado (400) y no se registra nada")
    void identidadEnElCuerpo() throws Exception {
        proponer(
                        Sesiones.enSesion(Sesiones.USUARIO, UUID.randomUUID(), "ADMIN_PLATAFORMA", PERMISO),
                        CUERPO.replace("{\"valor\"", "{\"propuestoPor\":\"" + Sesiones.OTRO_USUARIO + "\",\"valor\""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu132);
    }

    @Test
    @DisplayName("Inválido · el valor con más de 6 decimales o negativo se rechaza en el borde (400)")
    void valorNoDecimalExacto() throws Exception {
        var quien = Sesiones.enSesion(Sesiones.USUARIO, UUID.randomUUID(), "ADMIN_PLATAFORMA", PERMISO);
        proponer(quien, CUERPO.replace("\"5\"", "\"0.1234567\"")).andExpect(status().isBadRequest());
        proponer(quien, CUERPO.replace("\"5\"", "\"-1\"")).andExpect(status().isBadRequest());
        verifyNoInteractions(cu132);
    }

    @Test
    @DisplayName("Correcto · otra persona, con su token, aprueba (200)")
    void otraPersonaAprueba() throws Exception {
        when(cu132.propuestaPor(eq(VERSION), any())).thenReturn(Sesiones.USUARIO);
        when(cu132.aprobar(eq(VERSION), any())).thenReturn(version("APROBADA"));

        aprobar(Sesiones.enSesion(Sesiones.OTRO_USUARIO, UUID.randomUUID(), "ADMIN_PLATAFORMA", PERMISO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.aprobadoPor").doesNotExist());
    }

    @Test
    @DisplayName("Inválido · quien propuso no aprueba lo suyo, ni desde otra sesión (422 AP-CU132-03)")
    void mismaPersonaNoAprueba() throws Exception {
        when(cu132.propuestaPor(eq(VERSION), any())).thenReturn(Sesiones.USUARIO);

        aprobar(Sesiones.enSesion(Sesiones.USUARIO, UUID.randomUUID(), "ADMIN_PLATAFORMA", PERMISO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU132-03"));
        verify(cu132, never()).aprobar(any(), any());
    }

    @Test
    @DisplayName("Inválido · un token sin sesión no firma ninguno de los dos actos (401)")
    void tokenSinSesion() throws Exception {
        proponer(Sesiones.como("ADMIN_PLATAFORMA", PERMISO), CUERPO).andExpect(status().isUnauthorized());
        verify(cu132, never()).proponer(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Inválido · rol insuficiente recibe 403 en las tres rutas")
    void sinPermiso() throws Exception {
        mvc.perform(get("/tarifas/parametros").with(Sesiones.sinPermisos())).andExpect(status().isForbidden());
        proponer(Sesiones.sinPermisos(), CUERPO).andExpect(status().isForbidden());
        aprobar(Sesiones.comoOtro("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR")).andExpect(status().isForbidden());
        verifyNoInteractions(cu132);
    }

    @Test
    @DisplayName("Inválido · sin token recibe 401")
    void sinToken() throws Exception {
        mvc.perform(get("/tarifas/parametros")).andExpect(status().isUnauthorized());
        mvc.perform(post("/tarifas/parametros/versiones/{v}/aprobacion", VERSION)
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cu132);
    }
}
