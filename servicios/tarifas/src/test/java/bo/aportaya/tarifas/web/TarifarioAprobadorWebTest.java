package bo.aportaya.tarifas.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * CU-34 · F1.L5.M28: el aprobador de la publicacion es el actor de la sesion, nunca un id del cuerpo.
 *
 * <p>Antes, {@code aprobadoPor} llegaba en el cuerpo y quedaba registrado como quien aprobo el tarifario: cualquiera
 * con el permiso de publicar podia atribuirle la aprobacion a otra persona.
 */
@PruebaWeb(TarifasController.class)
class TarifarioAprobadorWebTest {

    private static final UUID TARIFARIO = UUID.fromString("b7100000-0000-4000-8000-000000000001");
    private static final String BASE =
            """
            {"tarifarioBaseId":"b7100000-0000-4000-8000-000000000001","nombre":"Tarifario sintetico",
             "tipoCambio":"INCREMENTO","diasPreaviso":30,%s
             "actaComite":"ACTA-SINT-001","urlPublicacion":"https://ejemplo.invalid/tarifas",
             "hashDocumento":"%s","canalAviso":"CORREO","permiteRescisionSinCosto":true}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private bo.aportaya.tarifas.aplicacion.ConsultarCostosDeProveedor consultarCostos;

    @MockitoBean
    private ConsultarTarifarioVigente vigentes;

    @MockitoBean
    private bo.aportaya.tarifas.aplicacion.ConsultarTarifarios historial;

    @MockitoBean
    private CU34PublicarTarifario cu34;

    @MockitoBean
    private CU35CerrarLiquidacion cu35;

    @MockitoBean
    private CU36ResolverPrecio cu36;

    @BeforeEach
    void publicacionAceptada() {
        when(cu34.publicar(any(), any()))
                .thenReturn(new CU34PublicarTarifario.SalidaPublicacion(
                        TARIFARIO, UUID.randomUUID(), 2, "EN_PREAVISO", null, true));
    }

    private static String cuerpo(String aprobador) {
        String campo = aprobador == null ? "" : "\"aprobadoPor\":\"" + aprobador + "\",";
        return BASE.formatted(campo, "ab".repeat(32));
    }

    private ResultActions publicar(RequestPostProcessor quien, String cuerpo) throws Exception {
        return mvc.perform(post("/tarifas/tarifarios")
                .with(quien)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private UUID aprobadorQueRecibeElCasoDeUso() {
        var entrada = ArgumentCaptor.forClass(CU34PublicarTarifario.EntradaPublicacion.class);
        verify(cu34).publicar(entrada.capture(), any());
        return entrada.getValue().aprobadoPor();
    }

    @Test
    @DisplayName("Correcto · sin aprobador en el cuerpo, el aprobador registrado es el actor de la sesión")
    void elAprobadorEsLaSesion() throws Exception {
        publicar(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR"), cuerpo(null))
                .andExpect(status().isCreated());
        assertThat(aprobadorQueRecibeElCasoDeUso()).isEqualTo(Sesiones.USUARIO);
    }

    @Test
    @DisplayName("Límite · el campo deprecado con el MISMO actor se acepta (clientes viejos)")
    void elMismoActorSeAcepta() throws Exception {
        publicar(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR"), cuerpo(Sesiones.USUARIO.toString()))
                .andExpect(status().isCreated());
        assertThat(aprobadorQueRecibeElCasoDeUso()).isEqualTo(Sesiones.USUARIO);
    }

    @Test
    @DisplayName("Inválido · un aprobador inventado en el cuerpo es 422 AP-CU34-06 y no se publica nada")
    void aprobadorInventadoSeRechaza() throws Exception {
        publicar(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR"), cuerpo(Sesiones.OTRO_USUARIO.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU34-06"));
        verifyNoInteractions(cu34);
    }

    @Test
    @DisplayName("Inválido · rol insuficiente 403 y sin token 401: no se publica nada")
    void sinPermisoNiToken() throws Exception {
        publicar(Sesiones.sinPermisos(), cuerpo(null)).andExpect(status().isForbidden());
        mvc.perform(post("/tarifas/tarifarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(null)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cu34);
    }
}
