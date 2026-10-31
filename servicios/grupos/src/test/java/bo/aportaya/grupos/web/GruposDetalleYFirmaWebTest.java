package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
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
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CU-69 · el detalle del grupo y la firma del reglamento en el borde (2026-10-20). Datos sinteticos. */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
            "aportaya.grupos.inicio.porcentaje=75",
        })
@Import(IntegrantesDelGrupo.class)
@MockitoBean(
        types = {
            CanjearInvitacion.class,
            CU20CrearGrupo.class,
            CU59CalcularPlazo.class,
            CU64TraspasarCupo.class,
            CU65Retirarse.class,
            CU68Postular.class,
            CU68AceptarIngreso.class,
            InvitacionesWeb.class,
            AdmisionDelGrupo.class,
            CU69Invitar.class,
            RespuestasAOtrosServicios.class,
            SorteoDelGrupo.class,
            CU69EnlaceDeGrupo.class,
            Consultas.class,
            GruposDeBackoffice.class,
            bo.aportaya.grupos.aplicacion.CU69IniciarGrupo.class
        })
class GruposDetalleYFirmaWebTest {

    private static final UUID GRUPO = UUID.fromString("c6900000-0000-4000-8000-0000000000f1");
    private static final UUID CUPO = UUID.fromString("c6900000-0000-4000-8000-0000000000f2");
    private static final UUID PARTICIPANTE = UUID.fromString("c6900000-0000-4000-8000-0000000000f3");
    private static final String HASH = "c".repeat(64);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarDetalleDelGrupo detalle;

    @MockitoBean
    private CU69FirmarReglamento firma;

    /** La etapa minima se nombra con el catalogo de transparencia (doble, catalogo SINTETICO). */
    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @MockitoBean
    private bo.aportaya.grupos.aplicacion.ConsultarMisLimites limites;

    private static ConsultarDetalleDelGrupo.Detalle unDetalle() {
        return new ConsultarDetalleDelGrupo.Detalle(
                GRUPO,
                "Pasanaku del barrio",
                "ABIERTO_A_INSCRIPCION",
                new BigDecimal("500.00"),
                "BOB",
                "MENSUAL",
                2,
                2,
                false,
                true,
                false,
                "Reglamento de prueba.",
                HASH,
                null,
                null,
                List.of(
                        new ConsultarDetalleDelGrupo.Integrante(1, CUPO, "Rosa Quispe", "FIRMADO", true, false, null),
                        new ConsultarDetalleDelGrupo.Integrante(
                                2, UUID.randomUUID(), "Juan Mamani", "PENDIENTE_FIRMA", false, true, null)));
    }

    @Test
    @DisplayName("CU-69 · 200: el detalle trae una fila por cupo con nombre y estado, sin telefono ni correo")
    void detalleDelGrupo() throws Exception {
        when(detalle.consultar(eq(GRUPO), any())).thenReturn(unDetalle());
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoAporte").value("500.00"))
                .andExpect(jsonPath("$.debeFirmar").value(true))
                .andExpect(jsonPath("$.reglamento.hash").value(HASH))
                .andExpect(jsonPath("$.integrantes.length()").value(2))
                .andExpect(jsonPath("$.integrantes[0].nombre").value("Rosa Quispe"))
                .andExpect(jsonPath("$.integrantes[0].estado").value("FIRMADO"))
                .andExpect(jsonPath("$.integrantes[1].soyYo").value(true))
                .andExpect(jsonPath("$.integrantes[0].telefono").doesNotExist())
                .andExpect(jsonPath("$.integrantes[0].correo").doesNotExist());
    }

    /** El organizador de un grupo de 10 que pide MEDIA, con {@code firmados} OCUPADO y {@code reservados} sin firmar. */
    private static ConsultarDetalleDelGrupo.Detalle paraIniciar(int firmados, int reservados) {
        var d = unDetalle();
        return new ConsultarDetalleDelGrupo.Detalle(
                d.grupoId(),
                d.nombre(),
                d.estado(),
                d.montoAporte(),
                d.moneda(),
                d.periodicidad(),
                10,
                firmados + reservados,
                true,
                false,
                false,
                d.reglamentoTexto(),
                d.reglamentoHash(),
                null,
                null,
                d.integrantes(),
                new ConsultarDetalleDelGrupo.Inicio(new BigDecimal("500.00"), firmados, reservados));
    }

    @Test
    @DisplayName("CU-69 · 200 (2026-10-30): minimoParaIniciar, puedoIniciar y la etapa minima por su codigo")
    void detalleConInicioYEtapa() throws Exception {
        org.mockito.Mockito.when(afuera.catalogoDeEtapas()).thenReturn(java.util.Optional.of(EtapasDePrueba.CATALOGO));
        when(detalle.consultar(eq(GRUPO), any())).thenReturn(paraIniciar(8, 0));
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoParaIniciar").value(8))
                .andExpect(jsonPath("$.puedoIniciar").value(true))
                .andExpect(jsonPath("$.etapaMinima").value("MEDIA"));

        when(detalle.consultar(eq(GRUPO), any())).thenReturn(paraIniciar(7, 0));
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(jsonPath("$.puedoIniciar").value(false));
        when(detalle.consultar(eq(GRUPO), any())).thenReturn(paraIniciar(8, 1));
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(jsonPath("$.puedoIniciar").value(false));
    }

    @Test
    @DisplayName("CU-69 · 200: sin respuesta de transparencia el detalle sale igual, sin etapaMinima")
    void detalleSinTransparencia() throws Exception {
        org.mockito.Mockito.when(afuera.catalogoDeEtapas()).thenReturn(java.util.Optional.empty());
        when(detalle.consultar(eq(GRUPO), any())).thenReturn(paraIniciar(8, 0));
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoParaIniciar").value(8))
                .andExpect(jsonPath("$.etapaMinima").doesNotExist());
    }

    @Test
    @DisplayName("CU-69 · 422: quien no integra el grupo no ve su detalle ni hace preguntar la etapa de nadie")
    void ajenoNoVeElDetalle() throws Exception {
        when(detalle.consultar(eq(GRUPO), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(69, 14), "No sos integrante de este grupo."));
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-14"))
                .andExpect(jsonPath("$.integrantes").doesNotExist());
        verifyNoInteractions(afuera);
    }

    /** Un grupo con dos ocupantes y un cupo libre, para la etapa por integrante (2026-10-30). */
    private static ConsultarDetalleDelGrupo.Detalle conOcupantes(UUID rosa, UUID juan, boolean pendiente) {
        var d = unDetalle();
        return new ConsultarDetalleDelGrupo.Detalle(
                d.grupoId(),
                d.nombre(),
                "CONFORMADO",
                d.montoAporte(),
                d.moneda(),
                d.periodicidad(),
                3,
                2,
                false,
                false,
                false,
                d.reglamentoTexto(),
                d.reglamentoHash(),
                null,
                null,
                List.of(
                        new ConsultarDetalleDelGrupo.Integrante(
                                1, CUPO, rosa, "Rosa Quispe", "FIRMADO", true, false, 1),
                        new ConsultarDetalleDelGrupo.Integrante(
                                2, UUID.randomUUID(), juan, "Juan Mamani", "FIRMADO", false, true, 2),
                        new ConsultarDetalleDelGrupo.Integrante(
                                3, UUID.randomUUID(), null, null, "LIBRE", false, false, null)),
                null,
                2,
                pendiente);
    }

    @Test
    @DisplayName("CU-69 · 200 (2026-10-30): cada ocupante con su etapa (codigo y nombre, nunca el puntaje), "
            + "en UNA consulta; el cupo libre sin etapa")
    void etapaDeCadaIntegrante() throws Exception {
        UUID rosa = UUID.randomUUID();
        UUID juan = UUID.randomUUID();
        when(detalle.consultar(eq(GRUPO), any())).thenReturn(conOcupantes(rosa, juan, false));
        when(afuera.etapasDe(any()))
                .thenReturn(java.util.Optional.of(java.util.Map.of(
                        rosa, new bo.aportaya.grupos.dominio.Etapas.Visible("MEDIA", "Etapa 1 · Media"),
                        juan, new bo.aportaya.grupos.dominio.Etapas.Visible("BAJA", "En recuperacion"))));
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrantes[0].etapa.codigo").value("MEDIA"))
                .andExpect(jsonPath("$.integrantes[0].etapa.nombre").value("Etapa 1 · Media"))
                .andExpect(jsonPath("$.integrantes[0].etapa.puntaje").doesNotExist())
                .andExpect(jsonPath("$.integrantes[0].etapa.desde").doesNotExist())
                .andExpect(jsonPath("$.integrantes[1].etapa.codigo").value("BAJA"))
                .andExpect(jsonPath("$.integrantes[2].etapa").doesNotExist());
        org.mockito.Mockito.verify(afuera, org.mockito.Mockito.times(1))
                .etapasDe(org.mockito.ArgumentMatchers.argThat(
                        pedidos -> List.copyOf(pedidos).equals(List.of(rosa, juan))));
        org.mockito.Mockito.verify(afuera, org.mockito.Mockito.never()).etapaDe(any());
        org.mockito.Mockito.verify(afuera, org.mockito.Mockito.never()).reputacion(any());
    }

    @Test
    @DisplayName("CU-69 · 200 (2026-10-30): transparencia no contesta → el detalle sale igual, sin etapas")
    void detalleSinEtapasSiTransparenciaNoContesta() throws Exception {
        when(detalle.consultar(eq(GRUPO), any())).thenReturn(conOcupantes(UUID.randomUUID(), UUID.randomUUID(), true));
        when(afuera.etapasDe(any())).thenReturn(java.util.Optional.empty());
        mvc.perform(get("/grupos/{g}/detalle", GRUPO).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrantes.length()").value(3))
                .andExpect(jsonPath("$.integrantes[0].etapa").doesNotExist())
                .andExpect(jsonPath("$.integrantes[1].etapa").doesNotExist())
                // La version vigente y si me falta aceptarla.
                .andExpect(jsonPath("$.reglamentoPendienteDeAceptar").value(true))
                .andExpect(jsonPath("$.reglamento.hash").value(HASH))
                .andExpect(jsonPath("$.reglamento.version").value(2));
    }

    @Test
    @DisplayName("CU-69 · 200: firmar el reglamento devuelve si el grupo quedo conformado")
    void firmar() throws Exception {
        when(firma.firmar(eq(GRUPO), eq(HASH), any(), any()))
                .thenReturn(new CU69FirmarReglamento.Firmado(PARTICIPANTE, "CONFORMADO", true));
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"" + HASH + "\",\"aceptaReglamento\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoConformado").value(true))
                .andExpect(jsonPath("$.estadoDelGrupo").value("CONFORMADO"));
    }

    @Test
    @DisplayName("CU-69 · 422: sin aceptar el reglamento no hay firma, aunque el hash sea el correcto")
    void sinAceptarNoSeFirma() throws Exception {
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"" + HASH + "\",\"aceptaReglamento\":false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-05"));
        verifyNoInteractions(firma);
    }

    @Test
    @DisplayName("CU-69 · 400: un hash con otra forma no llega al caso de uso")
    void hashMalFormado() throws Exception {
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"abc\",\"aceptaReglamento\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(firma);
    }

    @Test
    @DisplayName("CU-69 · 200: mis limites dicen cuantos grupos organizo y en cuantos participo")
    void misLimites() throws Exception {
        when(limites.consultar(any()))
                .thenReturn(new bo.aportaya.grupos.aplicacion.ConsultarMisLimites.Limites(1, 2, 3, 4));
        mvc.perform(get("/grupos/mis-limites").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizados").value(1))
                .andExpect(jsonPath("$.limiteDeOrganizar").value(2))
                .andExpect(jsonPath("$.participaciones").value(3))
                .andExpect(jsonPath("$.limiteDeParticipar").value(4));
        mvc.perform(get("/grupos/mis-limites")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CU-69 · 401: sin sesion no se ve el detalle ni se firma")
    void sinSesion() throws Exception {
        mvc.perform(get("/grupos/{g}/detalle", GRUPO)).andExpect(status().isUnauthorized());
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"" + HASH + "\",\"aceptaReglamento\":true}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(detalle, firma);
    }

    @Test
    @DisplayName("perfil · 200: dice si quien pregunta y el otro comparten el grupo, y nada mas")
    void compartenGrupo() throws Exception {
        when(detalle.comparten(eq(GRUPO), eq(PARTICIPANTE), any())).thenReturn(true);
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE)
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparten").value(true))
                .andExpect(jsonPath("$.estado").doesNotExist())
                .andExpect(jsonPath("$.cupoId").doesNotExist());
    }

    @Test
    @DisplayName("perfil · 200 false: quien no comparte el grupo recibe un no")
    void noCompartenGrupo() throws Exception {
        when(detalle.comparten(eq(GRUPO), eq(PARTICIPANTE), any())).thenReturn(false);
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE)
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparten").value(false));
    }

    @Test
    @DisplayName("perfil · 401 sin sesion, 403 sin el permiso de participante, 400 con un id que no es UUID")
    void compartenSinPermiso() throws Exception {
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE)
                        .with(Sesiones.como("OTRO_PERMISO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, "no-es-uuid")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
    }
}
