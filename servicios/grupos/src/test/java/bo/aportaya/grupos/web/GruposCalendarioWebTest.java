package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.CalendarioVacio;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-59 · H7.S1.M3 · {@code GET /grupos/calendario/vencimiento}: la respuesta HTTP muestra las tres
 * fechas (UTC, contractual, de liquidacion), la zona y los dias que no contaron.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo.class)
@MockitoBean(types = bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.class)
class GruposCalendarioWebTest {

    private static final String RUTA = "/grupos/calendario/vencimiento";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private bo.aportaya.grupos.aplicacion.CanjearInvitacion canjearInvitacion;

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
    private bo.aportaya.grupos.aplicacion.CU68AceptarIngreso cu68Decision;

    @MockitoBean
    private InvitacionesWeb invitaciones;

    @MockitoBean
    private AdmisionDelGrupo admision;

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

    private static CU59CalcularPlazo.SalidaVencimiento delPrimeroDeMayo() {
        return new CU59CalcularPlazo.SalidaVencimiento(
                Instant.parse("2026-05-02T02:00:00Z"),
                "America/La_Paz",
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4),
                true,
                List.of(
                        new CU59CalcularPlazo.DiaSalteado(LocalDate.of(2026, 5, 1), "feriado nacional"),
                        new CU59CalcularPlazo.DiaSalteado(LocalDate.of(2026, 5, 2), "Sabado"),
                        new CU59CalcularPlazo.DiaSalteado(LocalDate.of(2026, 5, 3), "Domingo")));
    }

    @Test
    @DisplayName("CU-59 · 200: muestra la fecha UTC, la contractual y la de liquidacion, sin confundirlas")
    void tresFechasExplicitas() throws Exception {
        when(cu59.vencimientoLocal(any(), any())).thenReturn(delPrimeroDeMayo());

        mvc.perform(get(RUTA)
                        .param("instante", "2026-05-02T02:00:00Z")
                        .param("alcance", "NACIONAL")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaHoraria").value("America/La_Paz"))
                .andExpect(jsonPath("$.fechaEnUtc").value("2026-05-02"))
                .andExpect(jsonPath("$.fechaContractual").value("2026-05-01"))
                .andExpect(jsonPath("$.fechaDeLiquidacion").value("2026-05-04"))
                .andExpect(jsonPath("$.corridaPorNoHabil").value(true))
                .andExpect(jsonPath("$.diasSalteados.length()").value(3))
                .andExpect(jsonPath("$.diasSalteados[1].descripcion").value("Sabado"));
    }

    @Test
    @DisplayName("CU-59 · el instante, el alcance y la zona llegan al caso de uso tal como se pidieron")
    void loQueLlegaAlCasoDeUso() throws Exception {
        when(cu59.vencimientoLocal(any(), any())).thenReturn(delPrimeroDeMayo());

        mvc.perform(get(RUTA)
                        .param("instante", "2026-05-01T22:00:00-04:00")
                        .param("alcance", "PLATAFORMA")
                        .param("zona", "UTC")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk());

        var capturada = ArgumentCaptor.forClass(CU59CalcularPlazo.EntradaVencimiento.class);
        verify(cu59).vencimientoLocal(capturada.capture(), any());
        // El mismo instante expresado con otro desfase es el MISMO instante en UTC.
        Assertions.assertThat(capturada.getValue().instante()).isEqualTo(Instant.parse("2026-05-02T02:00:00Z"));
        Assertions.assertThat(capturada.getValue().alcance()).isEqualTo("PLATAFORMA");
        Assertions.assertThat(capturada.getValue().zona()).isEqualTo(Optional.of("UTC"));
        Assertions.assertThat(capturada.getValue().referenciaId()).isEmpty();
    }

    @Test
    @DisplayName("CU-59 · 422 AP-CU59-05: sin calendario del anio no se calcula, y la respuesta lo dice")
    void calendarioVacio() throws Exception {
        when(cu59.vencimientoLocal(any(), any())).thenThrow(new CalendarioVacio(2031));

        mvc.perform(get(RUTA)
                        .param("instante", "2031-01-05T15:00:00Z")
                        .param("alcance", "NACIONAL")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU59-05"));
    }

    @Test
    @DisplayName("CU-59 · 422 AP-CU59-06: una zona horaria inexistente")
    void zonaInvalida() throws Exception {
        when(cu59.vencimientoLocal(any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(59, 6), "La zona horaria no existe."));

        mvc.perform(get(RUTA)
                        .param("instante", "2026-05-02T02:00:00Z")
                        .param("alcance", "NACIONAL")
                        .param("zona", "Marte/Olimpo")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU59-06"));
    }

    @Test
    @DisplayName("CU-59 · 400: sin instante, o con un instante que no es un instante, no se calcula nada")
    void instanteObligatorioYValido() throws Exception {
        mvc.perform(get(RUTA).param("alcance", "NACIONAL").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA)
                        .param("instante", "el primero de mayo")
                        .param("alcance", "NACIONAL")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA)
                        .param("instante", "2026-05-02")
                        .param("alcance", "NACIONAL")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu59);
    }

    @Test
    @DisplayName("CU-59 · 401 sin sesion y 403 sin el rol: la guardia muere antes del caso de uso")
    void sinSesionNiRol() throws Exception {
        mvc.perform(get(RUTA).param("instante", "2026-05-02T02:00:00Z").param("alcance", "NACIONAL"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA)
                        .param("instante", "2026-05-02T02:00:00Z")
                        .param("alcance", "NACIONAL")
                        .with(Sesiones.sinPermisos()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu59);
    }
}
