package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.aplicacion.SustituirAdministrador;
import bo.aportaya.grupos.dominio.DecisionDeIngreso;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaDecisionIngreso;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Etapa minima en la admision humana (2026-10-30): proponer y resolver ACEPTAR terminan reservando un cupo, asi que la
 * etapa del solicitante se mira antes, fuera de la transaccion. Rechazar no pide nada. Dobles declarados: el caso de
 * uso, las consultas y transparencia ({@link EtapasDePrueba}, catalogo SINTETICO).
 */
class EtapaMinimaAdmisionTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID SOLICITANTE = UUID.randomUUID();

    private final CU68AceptarIngreso casoDeUso = mock(CU68AceptarIngreso.class);
    private final Consultas consultas = mock(Consultas.class);
    private final HechosDeOtrosServicios afuera = mock(HechosDeOtrosServicios.class);
    private final AdmisionDelGrupo admision =
            new AdmisionDelGrupo(casoDeUso, mock(SustituirAdministrador.class), afuera, consultas);
    private final ContextoSesion ctx = ContextoSesion.de(
            UUID.randomUUID(), "ORGANIZADOR", new Traza(UUID.randomUUID().toString()));

    @BeforeEach
    void solicitudEnUnGrupoQuePideMedia() {
        when(casoDeUso.exigeEtapaAlAceptar(eq(SOLICITUD), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(true);
        when(consultas.solicitanteConCondiciones(eq(SOLICITUD), any()))
                .thenReturn(Optional.of(new Consultas.Solicitante(
                        SOLICITANTE,
                        EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.MEDIA, 10, "500.00")
                                .orElseThrow())));
    }

    private static EntradaDecisionIngreso decision(EntradaDecisionIngreso.DecisionEnum d) {
        return new EntradaDecisionIngreso(d, "Revisado por una persona.", 0);
    }

    @Test
    void proponerYResolverAceptarBajoLaMinimaSeRechazanSinTocarElExpediente() {
        when(afuera.etapaDe(SOLICITANTE)).thenReturn(EtapasDePrueba.con("499.99"));

        for (boolean resolver : new boolean[] {false, true}) {
            assertThatThrownBy(() -> admision.decidir(
                            SOLICITUD,
                            UUID.randomUUID(),
                            decision(EntradaDecisionIngreso.DecisionEnum.ACEPTAR),
                            ctx,
                            resolver))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .satisfies(e -> {
                        assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU68-03");
                        assertThat(((ErrorDeNegocio) e).detalle()).containsEntry("etapaMinima", "MEDIA");
                    });
        }
        verify(casoDeUso, never()).proponer(any(), any());
        verify(casoDeUso, never()).resolver(any(), any());
    }

    @Test
    void aceptarEnElPisoSigueAlCasoDeUso() {
        when(afuera.etapaDe(SOLICITANTE)).thenReturn(EtapasDePrueba.con("500.00"));
        when(casoDeUso.proponer(any(), any())).thenReturn(unaDecision());

        assertThat(admision.decidir(
                                SOLICITUD,
                                UUID.randomUUID(),
                                decision(EntradaDecisionIngreso.DecisionEnum.ACEPTAR),
                                ctx,
                                false)
                        .getSolicitudId())
                .isEqualTo(SOLICITUD);
    }

    @Test
    void rechazarNoPreguntaLaEtapa() {
        when(casoDeUso.resolver(any(), any())).thenReturn(unaDecision());

        admision.decidir(
                SOLICITUD, UUID.randomUUID(), decision(EntradaDecisionIngreso.DecisionEnum.RECHAZAR), ctx, true);

        verify(afuera, never()).etapaDe(any());
        verify(consultas, never()).solicitanteConCondiciones(any(), any());
    }

    @Test
    void sinTransparenciaNoSeAcepta() {
        when(afuera.etapaDe(SOLICITANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> admision.decidir(
                        SOLICITUD, UUID.randomUUID(), decision(EntradaDecisionIngreso.DecisionEnum.ACEPTAR), ctx, true))
                .isInstanceOf(bo.aportaya.plataforma.dominio.ErrorDeDominio.class)
                .hasMessageContaining("etapa");
        verify(casoDeUso, never()).proponer(any(), any());
        verify(casoDeUso, never()).resolver(any(), any());
    }

    @Test
    void quienNoAdministraElGrupoNoLlegaAPreguntarLaEtapa() {
        // El organizador de OTRO grupo: el caso de uso lo rechaza ANTES de que se mire la etapa del solicitante.
        when(casoDeUso.exigeEtapaAlAceptar(eq(SOLICITUD), any(), any(), eq(false)))
                .thenThrow(new ErrorDeNegocio(
                        bo.aportaya.plataforma.dominio.CodigoError.de(68, 6),
                        "Solo el administrador de este grupo puede proponer la admisión."));
        when(afuera.etapaDe(SOLICITANTE)).thenReturn(EtapasDePrueba.con("100.00"));

        assertThatThrownBy(() -> admision.decidir(
                        SOLICITUD,
                        UUID.randomUUID(),
                        decision(EntradaDecisionIngreso.DecisionEnum.ACEPTAR),
                        ctx,
                        false))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> {
                    assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU68-06");
                    assertThat(((ErrorDeNegocio) e).detalle()).doesNotContainKey("etapaMinima");
                });
        verify(afuera, never()).etapaDe(any());
        verify(consultas, never()).solicitanteConCondiciones(any(), any());
    }

    @Test
    void elReintentoConLaMismaClaveNoPreguntaLaEtapa() {
        when(casoDeUso.exigeEtapaAlAceptar(eq(SOLICITUD), any(), any(), eq(false)))
                .thenReturn(false);
        when(afuera.etapaDe(SOLICITANTE)).thenReturn(java.util.Optional.empty());
        var original = unaDecision();
        when(casoDeUso.proponer(any(), any())).thenReturn(original);

        assertThat(admision.decidir(
                                SOLICITUD,
                                UUID.randomUUID(),
                                decision(EntradaDecisionIngreso.DecisionEnum.ACEPTAR),
                                ctx,
                                false)
                        .getId())
                .isEqualTo(original.id());
        verify(afuera, never()).etapaDe(any());
    }

    private static DecisionDeIngreso unaDecision() {
        return new DecisionDeIngreso(
                UUID.randomUUID(),
                SOLICITUD,
                UUID.randomUUID(),
                "PROPUESTA",
                "ACEPTAR",
                UUID.randomUUID(),
                "Revisado por una persona.",
                null,
                null,
                1,
                "{}",
                "v1",
                null,
                false,
                java.time.OffsetDateTime.now(),
                UUID.randomUUID());
    }
}
