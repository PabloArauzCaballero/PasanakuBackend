package bo.aportaya.grupos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.dominio.ComputoDeVotacion.Sentido;
import bo.aportaya.grupos.dominio.ComputoDeVotacion.VotoPonderado;
import bo.aportaya.grupos.infraestructura.AcuerdoRepositorio;
import bo.aportaya.grupos.infraestructura.AcuerdoRepositorio.Acuerdo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L3.M07 · un acuerdo se resuelve UNA vez y emite UN solo {@code acuerdo_resuelto}.
 *
 * <p>La precondicion ({@code estado = 'ABIERTO'}) vive en el {@code UPDATE} del repositorio; su efecto con dos
 * conexiones reales esta en la evidencia con {@code psql}. Aca se fija lo que hace el caso de uso con cada
 * respuesta de la escritura: resolvio (emite), otro voto gano la carrera (no emite) o ya estaba resuelto.
 */
class ResolucionDeAcuerdoTest {

    private static final UUID ACUERDO = UUID.fromString("c6300000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("c6300000-0000-4000-8000-000000000002");
    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");

    private AcuerdoRepositorio acuerdos;
    private Outbox outbox;
    private CU63Acordar cu63;
    private final ContextoSesion ctx = ContextoSesion.de(
            UUID.randomUUID(), "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        acuerdos = mock(AcuerdoRepositorio.class);
        outbox = mock(Outbox.class);
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        cu63 = new CU63Acordar(datos, acuerdos, outbox, Reloj.fijo(AHORA));
        // Dos de tres a favor con quorum 0.50: la votacion alcanza.
        when(acuerdos.votosDe(any(), eq(ACUERDO)))
                .thenReturn(List.of(
                        new VotoPonderado(Sentido.A_FAVOR, BigDecimal.ONE),
                        new VotoPonderado(Sentido.A_FAVOR, BigDecimal.ONE)));
        when(acuerdos.pesoTotalDelGrupo(any(), eq(GRUPO))).thenReturn(new BigDecimal("3"));
    }

    private static Acuerdo en(String estado) {
        return new Acuerdo(
                GRUPO,
                "EXPULSION_PARTICIPANTE",
                estado,
                new BigDecimal("0.50"),
                Optional.empty(),
                Optional.empty(),
                OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusDays(3));
    }

    @Test
    @DisplayName("Correcto · el voto que resuelve escribe APROBADO y emite un acuerdo_resuelto")
    void quienResuelveEmite() {
        when(acuerdos.porId(any(), eq(ACUERDO))).thenReturn(Optional.of(en("ABIERTO")));
        when(acuerdos.resolver(any(), eq(ACUERDO), eq("APROBADO"), any(), any()))
                .thenReturn(true);

        assertThat(cu63.resolver(ACUERDO, ctx)).isEqualTo("APROBADO");
        verify(outbox).emitir(any(), any());
    }

    @Test
    @DisplayName("Límite · otro voto lo resolvió entre la lectura y la escritura: no se emite un segundo evento")
    void quienPierdeLaCarreraNoEmite() {
        when(acuerdos.porId(any(), eq(ACUERDO)))
                .thenReturn(Optional.of(en("ABIERTO")))
                .thenReturn(Optional.of(en("APROBADO")));
        when(acuerdos.resolver(any(), eq(ACUERDO), any(), any(), any())).thenReturn(false);

        assertThat(cu63.resolver(ACUERDO, ctx)).isEqualTo("APROBADO");
        verify(outbox, never()).emitir(any(), any());
    }

    @Test
    @DisplayName("Inválido · un acuerdo ya EJECUTADO no se recomputa, no se reescribe ni se reemite")
    void yaResueltoEsIdempotente() {
        when(acuerdos.porId(any(), eq(ACUERDO))).thenReturn(Optional.of(en("EJECUTADO")));

        assertThat(cu63.resolver(ACUERDO, ctx)).isEqualTo("EJECUTADO");
        verify(acuerdos, never()).resolver(any(), any(), any(), any(), any());
        verify(outbox, never()).emitir(any(), any());
    }
}
