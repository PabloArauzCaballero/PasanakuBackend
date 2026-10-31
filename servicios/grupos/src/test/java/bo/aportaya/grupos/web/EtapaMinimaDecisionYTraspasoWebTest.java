package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Etapa minima en el borde (2026-10-30), por los caminos de quien organiza y del traspaso: que el organizador acepte
 * (y sus reintentos) y traspasar un cupo. Partida de {@link EtapaMinimaWebTest} (limite de 300 lineas); la etapa la
 * dice un doble de {@code transparencia} con un catalogo SINTETICO ({@link EtapasDePrueba}).
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
            bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.class,
            CU59CalcularPlazo.class,
            CU65Retirarse.class,
            InvitacionesWeb.class,
            AdmisionDelGrupo.class,
            RespuestasAOtrosServicios.class,
            SorteoDelGrupo.class,
            IntegrantesDelGrupo.class,
            GruposDeBackoffice.class
        })
class EtapaMinimaDecisionYTraspasoWebTest extends BaseDeEtapaMinimaWeb {

    @Test
    @DisplayName("el organizador repite ACEPTAR sobre una solicitud ya APROBADA: no se pregunta la etapa y vuelve lo"
            + " resuelto")
    void reintentoDeLaDecisionNoPreguntaLaEtapa() throws Exception {
        when(cu68Decision.solicitante(eq(SOLICITUD), any())).thenReturn(ENTRANTE);
        when(consultas.solicitanteConCondiciones(eq(SOLICITUD), any()))
                .thenReturn(Optional.of(new Consultas.Solicitante(
                        ENTRANTE,
                        EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.MEDIA, 10, "500.00")
                                .orElseThrow(),
                        "APROBADA")));
        when(afuera.etapaDe(any())).thenReturn(Optional.empty());
        UUID cupo = UUID.randomUUID();
        when(cu68Decision.decidir(eq(SOLICITUD), eq(true), any(), any(), any()))
                .thenReturn(new CU68AceptarIngreso.Resultado(SOLICITUD, "APROBADA", ENTRANTE, cupo));
        decidir("ACEPTAR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupoId").value(cupo.toString()));
        verify(afuera, never()).etapaDe(any());
    }

    @Test
    @DisplayName("el organizador de OTRO grupo no llega a preguntar la etapa: la autorizacion va primero")
    void quienNoOrganizaNoPreguntaLaEtapa() throws Exception {
        when(cu68Decision.solicitante(eq(SOLICITUD), any()))
                .thenThrow(new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                        bo.aportaya.plataforma.dominio.CodigoError.de(68, 8), "Esa solicitud no existe."));
        when(afuera.etapaDe(any())).thenReturn(EtapasDePrueba.con("100.00"));
        decidir("ACEPTAR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU68-08"))
                .andExpect(jsonPath("$.detalle.etapaMinima").doesNotExist());
        verify(afuera, never()).etapaDe(any());
        verify(consultas, never()).solicitanteConCondiciones(any(), any());
    }

    @Test
    @DisplayName(
            "el organizador acepta: el puntaje del solicitante bajo desde que pidio → 422 AP-CU68-03; rechazar no mira")
    void organizadorAceptaBajoLaMinima() throws Exception {
        when(cu68Decision.solicitante(eq(SOLICITUD), any())).thenReturn(ENTRANTE);
        when(consultas.solicitanteConCondiciones(eq(SOLICITUD), any()))
                .thenReturn(Optional.of(new Consultas.Solicitante(
                        ENTRANTE,
                        EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.MEDIA, 10, "500.00")
                                .orElseThrow())));
        when(afuera.etapaDe(ENTRANTE)).thenReturn(EtapasDePrueba.con("499.00"));
        rechazoPorEtapa(decidir("ACEPTAR"), "AP-CU68-03");
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

    @Test
    @DisplayName("traspaso: quien entra bajo la minima → 422 AP-CU64-04 (el codigo de CU-64) y no se traspasa")
    void traspasoBajoLaMinima() throws Exception {
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(consultas.participanteDeOtro(eq(GRUPO), eq(ENTRANTE), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(afuera.etapaDe(ENTRANTE)).thenReturn(EtapasDePrueba.con("10.00"));
        rechazoPorEtapa(traspasar(), "AP-CU64-04");
        verify(cu64, never()).ejecutar(any(), any());
    }

    @Test
    @DisplayName("traspaso: quien no integra el grupo no llega a preguntar la etapa de nadie (no es un oraculo)")
    void traspasoDeQuienNoIntegraNoPreguntaLaEtapa() throws Exception {
        when(afuera.etapaDe(ENTRANTE)).thenReturn(EtapasDePrueba.con("10.00"));
        traspasar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalle.etapaMinima").doesNotExist());
        verify(afuera, never()).etapaDe(any());

        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        traspasar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU64-01"))
                .andExpect(jsonPath("$.detalle.etapaMinima").doesNotExist());
        verify(afuera, never()).etapaDe(any());
    }

    @Test
    @DisplayName("traspaso repetido con el mismo acuerdo: no se pregunta la etapa y vuelve el traspaso original")
    void reintentoDelTraspasoNoPreguntaLaEtapa() throws Exception {
        UUID participanteEntrante = UUID.randomUUID();
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(consultas.participanteDeOtro(eq(GRUPO), eq(ENTRANTE), any()))
                .thenReturn(Optional.of(participanteEntrante));
        when(cu64.yaTraspasado(eq(CUPO), any(), eq(participanteEntrante), any()))
                .thenReturn(true);
        when(afuera.etapaDe(any())).thenReturn(Optional.empty());
        when(afuera.estadoDePagos(any()))
                .thenReturn(new HechosDeOtrosServicios.EstadoDePagos(
                        true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "BOB"));
        UUID traspaso = UUID.randomUUID();
        when(cu64.ejecutar(any(), any())).thenReturn(traspaso);

        traspasar()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.traspasoId").value(traspaso.toString()));
        verify(afuera, never()).etapaDe(any());
    }

    @Test
    @DisplayName("traspaso: sin historial cuenta el piso de su etapa, no un cero (CU-64 recibe 500, no 0)")
    void traspasoSinHistorialNoEsCero() throws Exception {
        when(afuera.etapaDe(ENTRANTE)).thenReturn(EtapasDePrueba.con("500.00"));
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
}
