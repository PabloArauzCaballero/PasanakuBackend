package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.SustituirAdministrador;
import bo.aportaya.grupos.dominio.SustitucionDeAdministrador;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaSustitucionAdministrador;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Frontera de la sustitución de administrador: que el entrante esté habilitado como organizador lo contesta
 * quien lo sabe (otro servicio) ANTES de la transacción. Doble del puerto en sus tres niveles: afirma, niega y falla.
 */
class AdmisionDelGrupoSustitucionTest {
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID NUEVO = UUID.randomUUID();
    private static final UUID CLAVE = UUID.randomUUID();

    private SustituirAdministrador sustitucion;
    private HechosDeOtrosServicios afuera;
    private AdmisionDelGrupo adaptador;
    private ContextoSesion backoffice;
    private EntradaSustitucionAdministrador cuerpo;

    @BeforeEach
    void armar() {
        sustitucion = mock(SustituirAdministrador.class);
        afuera = mock(HechosDeOtrosServicios.class);
        adaptador = new AdmisionDelGrupo(mock(CU68AceptarIngreso.class), sustitucion, afuera);
        backoffice = ContextoSesion.de(
                UUID.randomUUID(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
        cuerpo = new EntradaSustitucionAdministrador();
        cuerpo.setNuevoAdministradorId(NUEVO);
        cuerpo.setMotivo("Habilitación revocada");
    }

    @Test
    void entranteHabilitadoDelegaYDevuelveElRegistro() {
        when(afuera.organizadorHabilitadoDelUsuario(NUEVO)).thenReturn(Optional.of(UUID.randomUUID()));
        var registro = new SustitucionDeAdministrador(
                UUID.randomUUID(),
                GRUPO,
                UUID.randomUUID(),
                UUID.randomUUID(),
                CLAVE,
                "Habilitación revocada",
                backoffice.usuarioId(),
                "participante=x;cupos=1:ASIGNADO",
                OffsetDateTime.now(),
                UUID.randomUUID());
        when(sustitucion.ejecutar(any(), any())).thenReturn(registro);

        var salida = adaptador.sustituir(GRUPO, CLAVE, cuerpo, backoffice);

        assertThat(salida.getId()).isEqualTo(registro.id());
        assertThat(salida.getObligacionesConservadas()).isEqualTo("participante=x;cupos=1:ASIGNADO");
        verify(sustitucion).ejecutar(any(), any());
    }

    @Test
    void entranteNoHabilitadoSeRechazaSinTocarElGrupo() {
        when(afuera.organizadorHabilitadoDelUsuario(NUEVO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adaptador.sustituir(GRUPO, CLAVE, cuerpo, backoffice))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("habilitado como organizador");
        verifyNoInteractions(sustitucion);
    }

    @Test
    void siElServicioDeHabilitacionFallaNoSeSustituyeNiSeTragaElError() {
        when(afuera.organizadorHabilitadoDelUsuario(NUEVO)).thenThrow(new IllegalStateException("sin respuesta"));

        assertThatThrownBy(() -> adaptador.sustituir(GRUPO, CLAVE, cuerpo, backoffice))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(sustitucion);
    }
}
