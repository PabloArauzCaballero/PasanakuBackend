package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CU-64 por subasta · las tres rutas que usa {@code entregas}: quien puede llamarlas y que contestan. */
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
            bo.aportaya.grupos.aplicacion.CanjearInvitacion.class,
            CU20CrearGrupo.class,
            CU59CalcularPlazo.class,
            CU64TraspasarCupo.class,
            CU65Retirarse.class,
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
class GruposSubastaWebTest {

    private static final UUID GRUPO = UUID.fromString("c6400000-0000-4000-8000-000000000001");
    private static final UUID CUPO = UUID.fromString("c6400000-0000-4000-8000-000000000002");
    private static final UUID SALIENTE = UUID.fromString("c6400000-0000-4000-8000-000000000003");
    private static final UUID ENTRANTE = UUID.fromString("c6400000-0000-4000-8000-000000000004");
    private static final UUID TRASPASO = UUID.fromString("c6400000-0000-4000-8000-000000000005");
    private static final String CLAVE = "c6400000-0000-4000-8000-0000000000ff";
    private static final String TRASPASAR = "/grupos/" + GRUPO + "/cupos/" + CUPO + "/traspasos-por-subasta";
    private static final String CUERPO = "{\"subastaId\":\"" + UUID.randomUUID() + "\",\"ganadorUsuarioId\":\""
            + UUID.randomUUID() + "\",\"salienteParticipanteId\":\"" + SALIENTE + "\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU64TraspasoPorSubasta porSubasta;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @Test
    @DisplayName("Un participante cualquiera no puede pasar un cupo por subasta: 403 y el caso de uso ni se toca")
    void unParticipanteNoTraspasa() throws Exception {
        mvc.perform(post(TRASPASAR)
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(porSubasta);
    }

    @Test
    @DisplayName("Sin sesion: 401")
    void sinSesion() throws Exception {
        mvc.perform(post(TRASPASAR)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(porSubasta);
    }

    @Test
    @DisplayName("Con la identidad de servicio de entregas: 200 con el traspaso y el participante que entra")
    void elServicioDeEntregasTraspasa() throws Exception {
        when(porSubasta.traspasar(eq(GRUPO), eq(CUPO), any(), any(), eq(SALIENTE), any()))
                .thenReturn(new CU64TraspasoPorSubasta.Traspaso(TRASPASO, ENTRANTE));
        mvc.perform(post(TRASPASAR)
                        .with(Sesiones.como("SERVICIO_ENTREGAS", "SERVICIO_ENTREGAS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traspasoId").value(TRASPASO.toString()))
                .andExpect(jsonPath("$.participanteEntranteId").value(ENTRANTE.toString()));
    }

    @Test
    @DisplayName("Un campo de mas en el cuerpo se rechaza (lista blanca): 400 y no se traspasa nada")
    void campoDeMas() throws Exception {
        mvc.perform(post(TRASPASAR)
                        .with(Sesiones.como("SERVICIO_ENTREGAS", "SERVICIO_ENTREGAS"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("{", "{\"precio\":\"1.00\",")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(porSubasta);
    }

    @Test
    @DisplayName("El titular ve si su cupo es subastable, con el aporte como cadena decimal y su moneda")
    void elTitularVeLaSubastabilidad() throws Exception {
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(SALIENTE));
        when(afuera.estadoDePagos(SALIENTE))
                .thenReturn(new HechosDeOtrosServicios.EstadoDePagos(
                        true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "BOB"));
        when(porSubasta.subastabilidad(eq(GRUPO), eq(CUPO), eq(Optional.of(SALIENTE)), eq(true), any()))
                .thenReturn(new CU64TraspasoPorSubasta.Subastabilidad(
                        GRUPO,
                        CUPO,
                        SALIENTE,
                        Optional.empty(),
                        new BigDecimal("500"),
                        "BOB",
                        "Pasanaku del barrio",
                        "MENSUAL"));
        mvc.perform(get("/grupos/" + GRUPO + "/cupos/" + CUPO + "/subastabilidad")
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subastable").value(true))
                .andExpect(jsonPath("$.montoAporte.monto").value("500.00"))
                .andExpect(jsonPath("$.montoAporte.moneda").value("BOB"))
                .andExpect(jsonPath("$.nombreDelGrupo").value("Pasanaku del barrio"))
                .andExpect(jsonPath("$.periodicidad").value("MENSUAL"));
    }

    @Test
    @DisplayName("La elegibilidad dice el motivo cuando no alcanza el KYC del grupo")
    void elegibilidadConMotivo() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("COMPLETO", 0, BigDecimal.ONE, 0)));
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.BAJA, 5, "100.00"));
        when(afuera.etapaDe(any())).thenReturn(EtapasDePrueba.con("520.00"));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
        when(porSubasta.elegibilidad(eq(GRUPO), eq(false), eq(false), anyBoolean(), any()))
                .thenReturn(Optional.of(CU64TraspasoPorSubasta.NoElegible.KYC_INSUFICIENTE));
        mvc.perform(get("/grupos/" + GRUPO + "/elegibilidad-para-subasta")
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegible").value(false))
                .andExpect(jsonPath("$.motivo").value("KYC_INSUFICIENTE"));
    }

    @Test
    @DisplayName("Pujar exige la etapa minima: quien no la alcanza no es elegible (antes de retener la plata)")
    void pujarExigeLaEtapaMinima() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 0, BigDecimal.ONE, 0)));
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.ALTA, 5, "100.00"));
        when(afuera.etapaDe(any())).thenReturn(EtapasDePrueba.con("520.00"));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
        when(porSubasta.elegibilidad(eq(GRUPO), eq(false), eq(true), eq(false), any()))
                .thenReturn(Optional.of(CU64TraspasoPorSubasta.NoElegible.REPUTACION_INSUFICIENTE));
        mvc.perform(get("/grupos/" + GRUPO + "/elegibilidad-para-subasta")
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegible").value(false))
                .andExpect(jsonPath("$.motivo").value("REPUTACION_INSUFICIENTE"));
    }

    @Test
    @DisplayName("Pujar con la etapa minima alcanzada pasa el control de etapa")
    void pujarConLaEtapaAlcanzada() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 0, BigDecimal.ONE, 0)));
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.MEDIA, 5, "100.00"));
        when(afuera.etapaDe(any())).thenReturn(EtapasDePrueba.con("600.00"));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
        when(porSubasta.elegibilidad(eq(GRUPO), eq(false), eq(true), eq(true), any()))
                .thenReturn(Optional.empty());
        mvc.perform(get("/grupos/" + GRUPO + "/elegibilidad-para-subasta")
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegible").value(true));
    }

    @Test
    @DisplayName("Sin respuesta de transparencia no se declara elegible a nadie (falla cerrado)")
    void pujarSinEtapaFallaCerrado() throws Exception {
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 0, BigDecimal.ONE, 0)));
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.MEDIA, 5, "100.00"));
        when(afuera.etapaDe(any())).thenReturn(Optional.empty());
        mvc.perform(get("/grupos/" + GRUPO + "/elegibilidad-para-subasta")
                        .with(Sesiones.como("PARTICIPANTE", "PARTICIPANTE")))
                .andExpect(status().is4xxClientError());
        org.mockito.Mockito.verifyNoInteractions(porSubasta);
    }
}
