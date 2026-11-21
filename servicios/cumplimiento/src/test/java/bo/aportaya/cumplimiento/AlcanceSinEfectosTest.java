package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bo.aportaya.cumplimiento.aplicacion.CU46VerificarAlcance;
import bo.aportaya.cumplimiento.aplicacion.CU46VerificarAlcance.EntradaAlcance;
import bo.aportaya.cumplimiento.infraestructura.LicenciaRepositorio;
import bo.aportaya.cumplimiento.infraestructura.SandboxRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L4.M25 · {@code GET /licencia/alcance} es una consulta: no escribe y no recibe el usuario.
 *
 * <p>Antes cada rechazo emitia un evento al outbox, asi que cualquier participante hacia crecer la
 * tabla con un GET. El rechazo se registra solo en la operacion real ({@code exigirHabilitado}), con
 * el actor del token. Los dobles son de los repositorios y del outbox; la regla de habilitacion es
 * la real. La version contra la base esta en {@code CU46Test} (Testcontainers).
 */
class AlcanceSinEfectosTest {

    private static final UUID ACTOR = UUID.fromString("c0000000-0000-4000-8000-0000000000a1");

    private final Outbox outbox = mock(Outbox.class);
    private CU46VerificarAlcance cu46;
    private ContextoSesion ctx;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        var licencias = mock(LicenciaRepositorio.class);
        when(licencias.vigente(any(), any())).thenReturn(Optional.empty()); // sin licencia: deniega
        var sandboxes = mock(SandboxRepositorio.class);
        when(sandboxes.activoPara(any(), any(), any(), any(Integer.class))).thenReturn(Optional.empty());
        cu46 = new CU46VerificarAlcance(
                datos,
                licencias,
                sandboxes,
                outbox,
                Reloj.fijo(Instant.parse("2026-11-21T12:00:00Z")),
                Set.of("RETIRO"));
        ctx = mock(ContextoSesion.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(ctx.usuarioId()).thenReturn(ACTOR);
        when(ctx.traza().id()).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("diez consultas rechazadas no escriben nada en el outbox")
    void diezConsultasNoEscriben() {
        for (int i = 0; i < 10; i++) {
            assertThat(cu46.ejecutar(EntradaAlcance.de("BILLETERA"), ctx).habilitado())
                    .isFalse();
        }
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("la operacion real rechazada registra el rechazo una vez, con el actor del token")
    void laOperacionRealRegistraConElActorDelToken() {
        assertThatThrownBy(() -> cu46.exigirHabilitado(EntradaAlcance.de("BILLETERA"), ctx))
                .isInstanceOf(ErrorDeNegocio.class);

        verify(outbox, times(1))
                .emitir(
                        any(),
                        argThat(e -> e.tipo().equals("cumplimiento.servicio_rechazado_por_alcance")
                                && e.agregadoId().equals(ACTOR)));
    }
}
