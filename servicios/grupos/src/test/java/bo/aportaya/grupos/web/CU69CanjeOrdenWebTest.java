package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.InvitacionRegistrada;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * El orden del canje (carril P, H3): consumir el enlace en {@code identidad} es IRREVERSIBLE, asi que todo lo que se
 * puede saber de antemano se comprueba antes (regla 98.3.5). Hallado contra TEST: una postulacion pendiente hacia que el
 * canje fallara con AP-CU68-06 DESPUES de consumir el enlace, y el emisor ya no podia revocar ni reemitir.
 *
 * <p>Aca se ejercita el orden de las llamadas de la ruta con los casos de uso como dobles; que la comprobacion real
 * rechace lo que tiene que rechazar lo prueba {@code CU69CanjeTest} contra la base.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
@Import(InvitacionesWeb.class)
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class)
class CU69CanjeOrdenWebTest {

    private static final UUID GRUPO = UUID.fromString("c9100000-0000-4000-8000-000000000001");
    private static final UUID INVITACION = UUID.fromString("c9100000-0000-4000-8000-000000000002");
    private static final UUID TOKEN = UUID.fromString("c9100000-0000-4000-8000-000000000003");
    private static final UUID CLAVE = UUID.fromString("c9100000-0000-4000-8000-0000000000ff");
    private static final String CUERPO = "{\"token\":\"" + "ab".repeat(32) + "\",\"cuposSolicitados\":1}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CanjearInvitacion canjes;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU59CalcularPlazo cu59;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private AdmisionDelGrupo admision;

    @MockitoBean
    private CU68AceptarIngreso cu68Decision;

    @MockitoBean
    private CU69Enlace enlaces;

    @MockitoBean
    private CU69Invitar cu69;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @MockitoBean
    private RespuestasAOtrosServicios respuestas;

    @MockitoBean
    private SorteoDelGrupo sorteo;

    @MockitoBean
    private IntegrantesDelGrupo integrantesDelGrupo;

    @MockitoBean
    private GruposDeBackoffice gruposDeBackoffice;

    @BeforeEach
    void escenario() {
        when(cu69.consultar(any(), any()))
                .thenReturn(new InvitacionRegistrada(
                        "ENVIADA", (short) 1, GRUPO, OffsetDateTime.now().plusDays(1), TOKEN, UUID.randomUUID()));
        when(consultas.politicaDelGrupo(any(), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 0, new BigDecimal("0.600"), 1)));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.reputacion(any())).thenReturn(new HechosDeOtrosServicios.Reputacion(false, BigDecimal.ZERO));
        when(afuera.morososDelGrupo(any())).thenReturn(0);
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
    }

    private ResultActions canjear() throws Exception {
        return mvc.perform(post("/grupos/{g}/invitaciones/{i}/canje", GRUPO, INVITACION)
                .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO));
    }

    @Test
    @DisplayName(
            "rechaza el canje por la comprobacion local ANTES de consumir el enlace: identidad no se toca ni se escribe nada")
    void rechazoLocalNoConsumeElEnlace() throws Exception {
        doThrow(new ErrorDeNegocio(CodigoError.de(68, 6), "Ya tenes una postulacion pendiente en este grupo."))
                .when(canjes)
                .comprobarAntesDeConsumir(any(), any(), any());

        canjear()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU68-06"));

        verify(afuera, never()).consumirInvitacion(any(), any(), any(), any());
        verify(canjes, never()).ejecutar(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "compensa el orden irreversible: con todo en regla se comprueba, luego se consume el enlace y luego se canjea localmente")
    void ordenDelCanje() throws Exception {
        UUID solicitud = UUID.randomUUID();
        var recibo = new HechosDeOtrosServicios.ConsumoInvitacion(
                TOKEN, GRUPO, UUID.randomUUID(), CLAVE, OffsetDateTime.now());
        when(afuera.consumirInvitacion(any(), any(), any(), any())).thenReturn(recibo);
        when(canjes.ejecutar(any(), any(), any(), any())).thenReturn(solicitud);

        canjear().andExpect(status().isOk()).andExpect(jsonPath("$.solicitudId").value(solicitud.toString()));

        var orden = inOrder(canjes, afuera);
        orden.verify(canjes).comprobarAntesDeConsumir(any(), any(), any());
        orden.verify(afuera).consumirInvitacion(any(), any(), any(), any());
        orden.verify(canjes).ejecutar(any(), any(), any(), any());
        Mockito.verifyNoMoreInteractions(canjes);
    }

    @Test
    @DisplayName("reintento: la misma clave tras un fallo transitorio del canje local reutiliza el recibo y completa")
    void reintentoTrasFalloTransitorio() throws Exception {
        UUID solicitud = UUID.randomUUID();
        var recibo = new HechosDeOtrosServicios.ConsumoInvitacion(
                TOKEN, GRUPO, UUID.randomUUID(), CLAVE, OffsetDateTime.now());
        // identidad responde el MISMO recibo a la misma clave (consumo idempotente por clave).
        when(afuera.consumirInvitacion(any(), any(), any(), any())).thenReturn(recibo);
        when(canjes.ejecutar(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("la base no respondio"))
                .thenReturn(solicitud);

        canjear().andExpect(status().is5xxServerError());
        canjear().andExpect(status().isOk()).andExpect(jsonPath("$.solicitudId").value(solicitud.toString()));

        verify(afuera, Mockito.times(2)).consumirInvitacion(any(), any(), any(), any());
    }
}
