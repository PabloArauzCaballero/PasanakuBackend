package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CU-69 · el enlace del grupo en el borde: quien puede generarlo, anularlo, verlo y pedir unirse. */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
        })
@MockitoBean(
        types = {
            CanjearInvitacion.class,
            CU20CrearGrupo.class,
            CU59CalcularPlazo.class,
            CU64TraspasarCupo.class,
            bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.class,
            CU65Retirarse.class,
            CU68Postular.class,
            CU68AceptarIngreso.class,
            InvitacionesWeb.class,
            AdmisionDelGrupo.class,
            CU69Invitar.class,
            RespuestasAOtrosServicios.class,
            SorteoDelGrupo.class,
            IntegrantesDelGrupo.class,
            GruposDeBackoffice.class
        })
class GruposEnlaceDelGrupoWebTest {

    private static final UUID GRUPO = UUID.fromString("c6900000-0000-4000-8000-0000000000e1");
    private static final UUID ENLACE = UUID.fromString("c6900000-0000-4000-8000-0000000000e2");
    private static final String CODIGO = ENLACE + "." + "ab".repeat(32);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU69EnlaceDeGrupo enlaces;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @Test
    @DisplayName("CU-69 · 201: el organizador genera el enlace y la respuesta no se guarda en caches")
    void organizadorGenera() throws Exception {
        when(enlaces.generar(eq(GRUPO), any()))
                .thenReturn(new CU69EnlaceDeGrupo.Generado(
                        ENLACE, CODIGO, OffsetDateTime.now().plusDays(7)));
        mvc.perform(post("/grupos/{g}/enlace-del-grupo", GRUPO).with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").value(CODIGO));
    }

    @Test
    @DisplayName("CU-69 · 403: un participante sin GRUPO_ADMINISTRAR no genera ni anula el enlace")
    void participanteNoManejaElEnlace() throws Exception {
        mvc.perform(post("/grupos/{g}/enlace-del-grupo", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/grupos/{g}/enlace-del-grupo/anulacion", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enlaces);
    }

    @Test
    @DisplayName("CU-69 · 401: sin sesión no se genera, no se ve ni se pide nada")
    void sinSesion() throws Exception {
        mvc.perform(post("/grupos/{g}/enlace-del-grupo", GRUPO)).andExpect(status().isUnauthorized());
        mvc.perform(post("/grupos/enlace-del-grupo/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/grupos/enlace-del-grupo/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(enlaces);
    }

    @Test
    @DisplayName("CU-69 · 400: un código con otra forma no llega al caso de uso")
    void codigoMalFormado() throws Exception {
        mvc.perform(post("/grupos/enlace-del-grupo/consultar")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"../../otro\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(enlaces);
    }

    @Test
    @DisplayName("CU-69 · 200: quien abre el enlace ve el grupo, sin datos de los integrantes")
    void consulta() throws Exception {
        when(enlaces.consultar(eq(CODIGO), any()))
                .thenReturn(new CU69EnlaceDeGrupo.Detalle(
                        ENLACE,
                        GRUPO,
                        "Pasanaku del barrio",
                        new BigDecimal("500.00"),
                        "BOB",
                        "MENSUAL",
                        2,
                        OffsetDateTime.now().plusDays(6)));
        mvc.perform(post("/grupos/enlace-del-grupo/consultar")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pasanaku del barrio"))
                .andExpect(jsonPath("$.montoAporte").value("500.00"))
                .andExpect(jsonPath("$.cuposLibres").value(2));
    }

    @Test
    @DisplayName("CU-69 · 201: pedir unirse deja la solicitud PENDIENTE con el nombre visible de identidad")
    void pideUnirse() throws Exception {
        UUID solicitud = UUID.randomUUID();
        when(enlaces.grupoDe(eq(CODIGO), any())).thenReturn(GRUPO);
        when(consultas.politicaDelGrupo(any(), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 0, new BigDecimal("0.600"), 1)));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.reputacion(any())).thenReturn(new HechosDeOtrosServicios.Reputacion(false, BigDecimal.ZERO));
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
        var rosa = new HechosDeOtrosServicios.Contacto("Rosa Quispe", "+59170000001", "rosa@correo.test");
        when(afuera.contactoDeLaSesion()).thenReturn(Optional.of(rosa));
        when(enlaces.solicitar(eq(CODIGO), any(), eq(rosa), any()))
                .thenReturn(new CU69EnlaceDeGrupo.Solicitada(solicitud, GRUPO));
        mvc.perform(post("/grupos/enlace-del-grupo/solicitudes")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitudId").value(solicitud.toString()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }
}
