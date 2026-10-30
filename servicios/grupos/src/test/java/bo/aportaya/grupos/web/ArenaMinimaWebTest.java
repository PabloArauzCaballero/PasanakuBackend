package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.InvitacionRegistrada;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
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

/**
 * Arena minima y desbloqueos en el borde, por TODOS los caminos que pasan por esta pagina (2026-10-30): postular,
 * canjear una invitacion, pedir por el enlace del grupo, que el organizador acepte, traspasar un cupo y crear un grupo.
 * Aceptar por el enlace de invitacion lo cubre {@code InvitacionesWebTest}; proponer y resolver la admision,
 * {@code ArenaMinimaAdmisionTest}. La arena la dice {@code transparencia}: aca es un doble con un catalogo SINTETICO
 * ({@link ArenasDePrueba}). En cada rechazo se comprueba que el caso de uso no llego a correr.
 */
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
            CU59CalcularPlazo.class, CU65Retirarse.class, InvitacionesWeb.class, AdmisionDelGrupo.class,
            RespuestasAOtrosServicios.class, SorteoDelGrupo.class, IntegrantesDelGrupo.class, GruposDeBackoffice.class
        })
class ArenaMinimaWebTest {

    private static final UUID GRUPO = UUID.fromString("a7e00000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("a7e00000-0000-4000-8000-000000000002");
    private static final UUID INVITACION = UUID.fromString("a7e00000-0000-4000-8000-000000000003");
    private static final UUID CUPO = UUID.fromString("a7e00000-0000-4000-8000-000000000004");
    private static final UUID ENTRANTE = UUID.fromString("a7e00000-0000-4000-8000-000000000005");
    private static final String CLAVE = "a7e00000-0000-4000-8000-0000000000ff";
    private static final String CODIGO = UUID.randomUUID() + "." + "ab".repeat(32);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private CU68AceptarIngreso cu68Decision;

    @MockitoBean
    private CU69Invitar cu69;

    @MockitoBean
    private CanjearInvitacion canjes;

    @MockitoBean
    private CU69EnlaceDeGrupo enlacesDeGrupo;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @BeforeEach
    void grupoQuePideMedia() {
        // 10 cupos de 500.00 BOB: justo lo que desbloquea MEDIA.
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(ArenasDePrueba.grupo(GRUPO, ArenasDePrueba.MEDIA, 10, "500.00"));
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 500, new BigDecimal("0.600"), 1)));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.reputacion(any())).thenReturn(new HechosDeOtrosServicios.Reputacion(true, new BigDecimal("400")));
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
    }

    private ResultActions postular() throws Exception {
        return mvc.perform(post("/grupos/{g}/postulaciones", GRUPO)
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuposSolicitados\":1}"));
    }

    private static void rechazoPorArena(ResultActions r, String codigo) throws Exception {
        r.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.detalle.arenaMinima").value("MEDIA"));
    }

    @Test
    @DisplayName("postular: 499.99 (BAJA) en un grupo que pide MEDIA → 422 AP-CU68-03 y no se postula")
    void postularBajoLaMinima() throws Exception {
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("499.99"));
        rechazoPorArena(postular(), "AP-CU68-03");
        verifyNoInteractions(cu68);
    }

    @Test
    @DisplayName("postular: 500.00, el piso exacto de MEDIA, entra")
    void postularEnElPiso() throws Exception {
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("500.00"));
        when(cu68.postular(any(), any()))
                .thenReturn(
                        new CU68Postular.SalidaPostulacion(SOLICITUD, new BigDecimal("70.00"), java.util.List.of()));
        postular().andExpect(status().isCreated());
    }

    @Test
    @DisplayName("canjear: bajo la minima → 422 AP-CU68-03 ANTES de consumir el enlace en identidad")
    void canjeBajoLaMinima() throws Exception {
        when(cu69.consultar(eq(INVITACION), any()))
                .thenReturn(new InvitacionRegistrada(
                        "ENVIADA", (short) 1, GRUPO, OffsetDateTime.now().plusDays(1), UUID.randomUUID(), null));
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("100.00"));
        rechazoPorArena(
                mvc.perform(post("/grupos/{g}/invitaciones/{i}/canje", GRUPO, INVITACION)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + "a".repeat(64) + "\",\"cuposSolicitados\":1}")),
                "AP-CU68-03");
        verify(afuera, never()).consumirInvitacion(any(), any(), any(), any());
        verify(canjes, never()).ejecutar(any(), any(), any(), any());
    }

    private ResultActions canjear() throws Exception {
        return mvc.perform(post("/grupos/{g}/invitaciones/{i}/canje", GRUPO, INVITACION)
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + "a".repeat(64) + "\",\"cuposSolicitados\":1}"));
    }

    @Test
    @DisplayName("canjear de nuevo con la misma clave un canje completo: no se pregunta la arena y vuelve la misma"
            + " solicitud, aunque transparencia no conteste")
    void reintentoDelCanjeNoPreguntaLaArena() throws Exception {
        when(cu69.consultar(eq(INVITACION), any()))
                .thenReturn(new InvitacionRegistrada(
                        "ACEPTADA", (short) 1, GRUPO, OffsetDateTime.now().plusDays(1), UUID.randomUUID(), null));
        when(canjes.comprobarAntesDeConsumir(eq(INVITACION), any(), any())).thenReturn(true);
        when(afuera.arenaDe(any())).thenReturn(Optional.empty());
        var recibo = new HechosDeOtrosServicios.ConsumoInvitacion(
                UUID.randomUUID(), GRUPO, UUID.randomUUID(), UUID.fromString(CLAVE), OffsetDateTime.now());
        when(afuera.consumirInvitacion(any(), any(), any(), any())).thenReturn(recibo);
        when(canjes.ejecutar(any(), any(), any(), any())).thenReturn(SOLICITUD);

        canjear().andExpect(status().isOk()).andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()));
        verify(afuera, never()).arenaDe(any());
    }

    @Test
    @DisplayName("pedir de nuevo por el enlace del grupo con la solicitud PENDIENTE: no se pregunta la arena y vuelve"
            + " la misma")
    void reintentoDelEnlaceNoPreguntaLaArena() throws Exception {
        when(enlacesDeGrupo.grupoDe(eq(CODIGO), any())).thenReturn(GRUPO);
        when(enlacesDeGrupo.pendienteDe(eq(GRUPO), any())).thenReturn(Optional.of(SOLICITUD));
        when(afuera.arenaDe(any())).thenReturn(Optional.empty());
        when(enlacesDeGrupo.solicitar(eq(CODIGO), any(), any(), any()))
                .thenReturn(new CU69EnlaceDeGrupo.Solicitada(SOLICITUD, GRUPO));
        mvc.perform(post("/grupos/enlace-del-grupo/solicitudes")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()));
        verify(afuera, never()).arenaDe(any());
    }

    @Test
    @DisplayName("el organizador repite ACEPTAR sobre una solicitud ya APROBADA: no se pregunta la arena y vuelve lo"
            + " resuelto")
    void reintentoDeLaDecisionNoPreguntaLaArena() throws Exception {
        when(cu68Decision.solicitante(eq(SOLICITUD), any())).thenReturn(ENTRANTE);
        when(consultas.solicitanteConCondiciones(eq(SOLICITUD), any()))
                .thenReturn(Optional.of(new Consultas.Solicitante(
                        ENTRANTE,
                        ArenasDePrueba.grupo(GRUPO, ArenasDePrueba.MEDIA, 10, "500.00")
                                .orElseThrow(),
                        "APROBADA")));
        when(afuera.arenaDe(any())).thenReturn(Optional.empty());
        UUID cupo = UUID.randomUUID();
        when(cu68Decision.decidir(eq(SOLICITUD), eq(true), any(), any(), any()))
                .thenReturn(new CU68AceptarIngreso.Resultado(SOLICITUD, "APROBADA", ENTRANTE, cupo));
        decidir("ACEPTAR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupoId").value(cupo.toString()));
        verify(afuera, never()).arenaDe(any());
    }

    @Test
    @DisplayName("el organizador de OTRO grupo no llega a preguntar la arena: la autorizacion va primero")
    void quienNoOrganizaNoPreguntaLaArena() throws Exception {
        when(cu68Decision.solicitante(eq(SOLICITUD), any()))
                .thenThrow(new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                        bo.aportaya.plataforma.dominio.CodigoError.de(68, 8), "Esa solicitud no existe."));
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("100.00"));
        decidir("ACEPTAR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU68-08"))
                .andExpect(jsonPath("$.detalle.arenaMinima").doesNotExist());
        verify(afuera, never()).arenaDe(any());
        verify(consultas, never()).solicitanteConCondiciones(any(), any());
    }

    @Test
    @DisplayName("enlace del grupo: bajo la minima → 422 AP-CU68-03 y no queda solicitud")
    void enlaceDelGrupoBajoLaMinima() throws Exception {
        when(enlacesDeGrupo.grupoDe(eq(CODIGO), any())).thenReturn(GRUPO);
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("0.00"));
        rechazoPorArena(
                mvc.perform(post("/grupos/enlace-del-grupo/solicitudes")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}")),
                "AP-CU68-03");
        verify(enlacesDeGrupo, never()).solicitar(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "el organizador acepta: el puntaje del solicitante bajo desde que pidio → 422 AP-CU68-03; rechazar no mira")
    void organizadorAceptaBajoLaMinima() throws Exception {
        when(cu68Decision.solicitante(eq(SOLICITUD), any())).thenReturn(ENTRANTE);
        when(consultas.solicitanteConCondiciones(eq(SOLICITUD), any()))
                .thenReturn(Optional.of(new Consultas.Solicitante(
                        ENTRANTE,
                        ArenasDePrueba.grupo(GRUPO, ArenasDePrueba.MEDIA, 10, "500.00")
                                .orElseThrow())));
        when(afuera.arenaDe(ENTRANTE)).thenReturn(ArenasDePrueba.con("499.00"));
        rechazoPorArena(decidir("ACEPTAR"), "AP-CU68-03");
        verify(cu68Decision, never()).decidir(any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any());

        when(cu68Decision.decidir(eq(SOLICITUD), eq(false), any(), any(), any()))
                .thenReturn(new CU68AceptarIngreso.Resultado(SOLICITUD, "RECHAZADA", null, null));
        mvc.perform(post("/grupos/solicitudes/{s}/decision", SOLICITUD)
                        .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RECHAZAR\",\"motivo\":\"No lo conozco.\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions decidir(String decision) throws Exception {
        return mvc.perform(post("/grupos/solicitudes/{s}/decision", SOLICITUD)
                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\"}"));
    }

    @Test
    @DisplayName("traspaso: quien entra bajo la minima → 422 AP-CU64-04 (el codigo de CU-64) y no se traspasa")
    void traspasoBajoLaMinima() throws Exception {
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(consultas.participanteDeOtro(eq(GRUPO), eq(ENTRANTE), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(afuera.arenaDe(ENTRANTE)).thenReturn(ArenasDePrueba.con("10.00"));
        rechazoPorArena(traspasar(), "AP-CU64-04");
        verify(cu64, never()).ejecutar(any(), any());
    }

    @Test
    @DisplayName("traspaso: quien no integra el grupo no llega a preguntar la arena de nadie (no es un oraculo)")
    void traspasoDeQuienNoIntegraNoPreguntaLaArena() throws Exception {
        when(afuera.arenaDe(ENTRANTE)).thenReturn(ArenasDePrueba.con("10.00"));
        traspasar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalle.arenaMinima").doesNotExist());
        verify(afuera, never()).arenaDe(any());

        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        traspasar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU64-01"))
                .andExpect(jsonPath("$.detalle.arenaMinima").doesNotExist());
        verify(afuera, never()).arenaDe(any());
    }

    @Test
    @DisplayName("traspaso repetido con el mismo acuerdo: no se pregunta la arena y vuelve el traspaso original")
    void reintentoDelTraspasoNoPreguntaLaArena() throws Exception {
        UUID participanteEntrante = UUID.randomUUID();
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(consultas.participanteDeOtro(eq(GRUPO), eq(ENTRANTE), any()))
                .thenReturn(Optional.of(participanteEntrante));
        when(cu64.yaTraspasado(eq(CUPO), any(), eq(participanteEntrante), any()))
                .thenReturn(true);
        when(afuera.arenaDe(any())).thenReturn(Optional.empty());
        when(afuera.estadoDePagos(any()))
                .thenReturn(new HechosDeOtrosServicios.EstadoDePagos(
                        true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "BOB"));
        UUID traspaso = UUID.randomUUID();
        when(cu64.ejecutar(any(), any())).thenReturn(traspaso);

        traspasar()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.traspasoId").value(traspaso.toString()));
        verify(afuera, never()).arenaDe(any());
    }

    @Test
    @DisplayName("traspaso: sin historial cuenta el piso de su arena, no un cero (CU-64 recibe 500, no 0)")
    void traspasoSinHistorialNoEsCero() throws Exception {
        when(afuera.arenaDe(ENTRANTE)).thenReturn(ArenasDePrueba.con("500.00"));
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(consultas.participanteDeOtro(eq(GRUPO), eq(ENTRANTE), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(afuera.estadoDePagos(any()))
                .thenReturn(new HechosDeOtrosServicios.EstadoDePagos(
                        true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "BOB"));
        when(cu64.ejecutar(any(), any())).thenReturn(UUID.randomUUID());

        traspasar().andExpect(status().isCreated());

        var entrada = ArgumentCaptor.forClass(CU64TraspasarCupo.EntradaTraspaso.class);
        verify(cu64).ejecutar(entrada.capture(), any());
        assertThat(entrada.getValue().reputacionDelEntrante()).isEqualTo(500);
    }

    private ResultActions traspasar() throws Exception {
        return mvc.perform(post("/grupos/{g}/cupos/{c}/traspasos", GRUPO, CUPO)
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioEntranteId\":\"" + ENTRANTE + "\",\"motivo\":\"Me mudo de ciudad.\"}"));
    }

    @Test
    @DisplayName("desbloqueo: MEDIA llega hasta 10 cupos; en uno de 12 → 422 AP-CU69-18 con lo que si desbloquea")
    void desbloqueoExcedidoAlEntrar() throws Exception {
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(ArenasDePrueba.grupo(GRUPO, ArenasDePrueba.MEDIA, 12, "500.00"));
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("650.00"));
        postular()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-18"))
                .andExpect(jsonPath("$.detalle.cuposMax").value(10))
                .andExpect(jsonPath("$.detalle.aporteMax").value("500.00"))
                .andExpect(jsonPath("$.detalle.moneda").value("BOB"));
        verifyNoInteractions(cu68);
    }

    @Test
    @DisplayName("desbloqueo PROPUESTO no limita: ALTA (propuesto, 3 cupos) entra a un grupo de 10")
    void desbloqueoPropuestoNoLimita() throws Exception {
        when(afuera.arenaDe(any())).thenReturn(ArenasDePrueba.con("900.00"));
        when(cu68.postular(any(), any()))
                .thenReturn(
                        new CU68Postular.SalidaPostulacion(SOLICITUD, new BigDecimal("70.00"), java.util.List.of()));
        postular().andExpect(status().isCreated());
    }

    @Test
    @DisplayName("transparencia no contesta: no se entra (falla cerrado) y el caso de uso no corre")
    void sinTransparenciaNoSeEntra() throws Exception {
        when(afuera.arenaDe(any())).thenReturn(Optional.empty());
        postular()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-DOM-01"));
        verifyNoInteractions(cu68);
    }

    @Test
    @DisplayName("grupo ya iniciado: la arena no se pregunta; el caso de uso responde lo suyo (AP-CU69-17)")
    void grupoCerradoNoPreguntaLaArena() throws Exception {
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(Optional.of(new bo.aportaya.grupos.dominio.CondicionesDeIngreso(
                        GRUPO, "CONFORMADO", new BigDecimal("500.00"), 8, new BigDecimal("500.00"), "BOB")));
        when(cu68.postular(any(), any()))
                .thenThrow(new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                        bo.aportaya.plataforma.dominio.CodigoError.de(69, 17), "Este grupo ya inicio."));
        postular()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-17"));
        verify(afuera, never()).arenaDe(any());
    }
}
