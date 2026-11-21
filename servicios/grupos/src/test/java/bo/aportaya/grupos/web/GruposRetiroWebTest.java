package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PosicionAlRetirarse;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
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
 * CU-65 · F1.L3.M04: el retiro lo pide solo su titular, en el grupo de la ruta.
 *
 * <p>Antes, el {@code participanteId} del cuerpo decidía a quién se retiraba, y el creador del grupo podía pedir
 * el retiro de otro integrante. Ahora el participante sale del token y del {@code grupoId} de la ruta. Esta es la
 * matriz negativa: otro usuario, otro participante en el cuerpo, rol insuficiente y sin token.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
@MockitoBean(
        types = {
            bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class,
            bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.class,
            bo.aportaya.grupos.aplicacion.CanjearInvitacion.class,
            CU20CrearGrupo.class,
            CU59CalcularPlazo.class,
            CU64TraspasarCupo.class,
            CU68Postular.class,
            bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.class,
            InvitacionesWeb.class,
            AdmisionDelGrupo.class,
            CU69Invitar.class,
            RespuestasAOtrosServicios.class,
            SorteoDelGrupo.class,
            IntegrantesDelGrupo.class,
            GruposDeBackoffice.class,
        })
class GruposRetiroWebTest {

    private static final UUID GRUPO = UUID.fromString("c6500000-0000-4000-8000-000000000001");
    private static final UUID PROPIO = UUID.fromString("c6500000-0000-4000-8000-000000000002");
    private static final UUID AJENO = UUID.fromString("c6500000-0000-4000-8000-000000000003");
    private static final String CLAVE = "c6500000-0000-4000-8000-0000000000ff";
    private static final String MOTIVO = "\"motivo\":\"Me mudo de ciudad y no puedo seguir\"";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    private ResultActions retirar(RequestPostProcessor quien, String cuerpo) throws Exception {
        return mvc.perform(post("/grupos/{g}/retiros", GRUPO)
                .with(quien)
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private void titularEnElGrupo() {
        when(consultas.participanteDe(eq(GRUPO), eq(Sesiones.USUARIO), any())).thenReturn(Optional.of(PROPIO));
        when(consultas.participanteDe(eq(GRUPO), eq(Sesiones.OTRO_USUARIO), any()))
                .thenReturn(Optional.empty());
        when(afuera.estadoDePagos(PROPIO))
                .thenReturn(new HechosDeOtrosServicios.EstadoDePagos(
                        true, new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), 0, "BOB"));
        when(cu65.solicitar(any(), any()))
                .thenReturn(new CU65Retirarse.SalidaRetiro(
                        UUID.randomUUID(),
                        new PosicionAlRetirarse(
                                PosicionAlRetirarse.Tipo.ACREEDORA,
                                Dinero.de(new BigDecimal("500.00"), Moneda.BOB),
                                PosicionAlRetirarse.Momento.AL_CIERRE_DEL_CICLO)));
    }

    @Test
    @DisplayName("Correcto · el titular se retira sin mandar identidad: el participante sale del token")
    void elTitularSeRetiraASiMismo() throws Exception {
        titularEnElGrupo();

        retirar(Sesiones.como("PARTICIPANTE"), "{" + MOTIVO + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.posicion").value("ACREEDORA"));

        var entrada = ArgumentCaptor.forClass(CU65Retirarse.EntradaRetiro.class);
        verify(cu65).solicitar(entrada.capture(), any());
        assertThat(entrada.getValue().participanteId()).isEqualTo(PROPIO);
        verify(afuera).estadoDePagos(PROPIO);
    }

    @Test
    @DisplayName("Límite · el campo deprecado con su propio participante se acepta (clientes viejos)")
    void elCampoDeprecadoConElPropioSeAcepta() throws Exception {
        titularEnElGrupo();

        retirar(Sesiones.como("PARTICIPANTE"), "{\"participanteId\":\"" + PROPIO + "\"," + MOTIVO + "}")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Inválido · el creador pidiendo el retiro de OTRO integrante recibe 403 y no se crea nada")
    void otroParticipanteEnElCuerpoEs403() throws Exception {
        titularEnElGrupo();

        retirar(Sesiones.como("PARTICIPANTE"), "{\"participanteId\":\"" + AJENO + "\"," + MOTIVO + "}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(cu65, afuera);
    }

    @Test
    @DisplayName("Inválido · otro usuario, que no participa de ese grupo, recibe 403")
    void otroUsuarioFueraDelGrupoEs403() throws Exception {
        titularEnElGrupo();

        retirar(Sesiones.comoOtro("PARTICIPANTE"), "{" + MOTIVO + "}").andExpect(status().isForbidden());
        verifyNoInteractions(cu65, afuera);
    }

    @Test
    @DisplayName("Inválido · rol insuficiente recibe 403")
    void sinPermisoEs403() throws Exception {
        retirar(Sesiones.sinPermisos(), "{" + MOTIVO + "}").andExpect(status().isForbidden());
        verifyNoInteractions(cu65, consultas, afuera);
    }

    @Test
    @DisplayName("Inválido · sin token recibe 401")
    void sinTokenEs401() throws Exception {
        mvc.perform(post("/grupos/{g}/retiros", GRUPO)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + MOTIVO + "}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cu65, consultas, afuera);
    }
}
