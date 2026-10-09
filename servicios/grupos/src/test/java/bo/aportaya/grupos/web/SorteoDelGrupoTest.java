package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.aplicacion.CU60Sortear;
import bo.aportaya.grupos.aplicacion.CU60Sortear.Revelacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.web.generado.modelo.EntradaCompromiso;
import bo.aportaya.grupos.web.generado.modelo.EntradaRevelacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El adaptador del sorteo: autorización por grupo, pertenencia del sorteo a la ruta, semilla que ningún cliente
 * aporta y reintento que devuelve el original. Casos de uso y consultas son dobles declarados (la integración
 * real está en CU60Test/CU60SnapshotTest); lo que se prueba aquí es la guardia del adaptador.
 */
class SorteoDelGrupoTest {
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID OTRO_GRUPO = UUID.randomUUID();
    private static final UUID SORTEO = UUID.randomUUID();

    private CU60Sortear cu60;
    private Consultas consultas;
    private SorteoDelGrupo adaptador;
    private ContextoSesion ctx;

    @BeforeEach
    void preparar() {
        cu60 = mock(CU60Sortear.class);
        consultas = mock(Consultas.class);
        adaptador = new SorteoDelGrupo(cu60, consultas);
        ctx = ContextoSesion.de(
                UUID.randomUUID(), "ORGANIZADOR", new Traza(UUID.randomUUID().toString()));
    }

    private EntradaRevelacion entrada(String semillaDelCliente) {
        var e = new EntradaRevelacion();
        e.setSorteoId(SORTEO);
        e.setSemilla(semillaDelCliente);
        return e;
    }

    @Test
    @DisplayName("Quien no administra el grupo no compromete ni revela su sorteo")
    void soloElAdministradorOperaElSorteo() {
        when(consultas.puedeAdministrar(GRUPO, ctx)).thenReturn(false);

        assertThatThrownBy(() -> adaptador.comprometer(GRUPO, new EntradaCompromiso(), ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU60-10"));
        assertThatThrownBy(() -> adaptador.revelar(GRUPO, entrada(null), ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU60-10"));
        verify(cu60, never()).comprometer(any(), any(), any(), any());
        verify(cu60, never()).revelar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "Un sorteo de OTRO grupo no se revela por la ruta de este: es el mismo 'no existe' que un sorteo inexistente")
    void sorteoAjenoNoSeToca() {
        when(consultas.puedeAdministrar(GRUPO, ctx)).thenReturn(true);
        when(cu60.grupoDelSorteo(SORTEO, ctx)).thenReturn(Optional.of(OTRO_GRUPO));

        assertThatThrownBy(() -> adaptador.revelar(GRUPO, entrada(null), ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU60-02"));
        when(cu60.grupoDelSorteo(SORTEO, ctx)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adaptador.revelar(GRUPO, entrada(null), ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU60-02"));
        verify(cu60, never()).revelar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("La semilla que mande un cliente se ignora: el caso de uso recibe semilla nula (la sellada)")
    void laSemillaDelClienteNoLlega() {
        when(consultas.puedeAdministrar(GRUPO, ctx)).thenReturn(true);
        when(cu60.grupoDelSorteo(SORTEO, ctx)).thenReturn(Optional.of(GRUPO));
        when(cu60.resultadoVigente(SORTEO, ctx)).thenReturn(Optional.empty());
        when(cu60.revelar(any(), isNull(), any(), any(), any(), any(), any()))
                .thenReturn(new Revelacion(SORTEO, List.of(UUID.randomUUID()), true, "semilla-publicada"));

        var respuesta = adaptador.revelar(GRUPO, entrada("semilla-que-prueba-un-cliente"), ctx);

        assertThat(respuesta.getBody().getVerificado()).isTrue();
        assertThat(respuesta.getBody().getSemilla()).isEqualTo("semilla-publicada");
        verify(cu60).revelar(any(), isNull(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si ya hay un resultado vigente se devuelve el original y no se vuelve a sortear")
    void reintentoDevuelveElOriginal() {
        var original = new Revelacion(SORTEO, List.of(UUID.randomUUID(), UUID.randomUUID()), true, "semilla");
        when(consultas.puedeAdministrar(GRUPO, ctx)).thenReturn(true);
        when(cu60.grupoDelSorteo(SORTEO, ctx)).thenReturn(Optional.of(GRUPO));
        when(cu60.resultadoVigente(SORTEO, ctx)).thenReturn(Optional.of(original));

        var respuesta = adaptador.revelar(GRUPO, entrada(null), ctx);

        assertThat(respuesta.getBody().getCuposEnOrden()).isEqualTo(original.cuposEnOrden());
        verify(cu60, never()).revelar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "El que pierde la carrera (AP-CU60-07) recibe el resultado del que gano; cualquier otro error se propaga")
    void perderLaCarreraDevuelveElOriginal() {
        var original = new Revelacion(SORTEO, List.of(UUID.randomUUID()), true, "semilla");
        when(consultas.puedeAdministrar(GRUPO, ctx)).thenReturn(true);
        when(cu60.grupoDelSorteo(SORTEO, ctx)).thenReturn(Optional.of(GRUPO));
        when(cu60.resultadoVigente(SORTEO, ctx)).thenReturn(Optional.empty()).thenReturn(Optional.of(original));
        when(cu60.revelar(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(60, 7), "ya revelado"));

        assertThat(adaptador.revelar(GRUPO, entrada(null), ctx).getBody().getCuposEnOrden())
                .isEqualTo(original.cuposEnOrden());

        doThrow(new ErrorDeNegocio(CodigoError.de(60, 6), "plantel cambio"))
                .when(cu60)
                .revelar(any(), any(), any(), any(), any(), any(), any());
        when(cu60.resultadoVigente(SORTEO, ctx)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adaptador.revelar(GRUPO, entrada(null), ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU60-06"));
    }
}
