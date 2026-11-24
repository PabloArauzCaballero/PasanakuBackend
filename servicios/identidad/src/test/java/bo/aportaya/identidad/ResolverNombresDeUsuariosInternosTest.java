package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos;
import bo.aportaya.identidad.infraestructura.NombresDeUsuariosInternosRepositorio;
import bo.aportaya.identidad.infraestructura.NombresDeUsuariosInternosRepositorio.Nombre;
import bo.aportaya.identidad.web.generado.modelo.NombreDeUsuarioInterno;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El caso de uso que da nombre a quien propuso y quien aprobo (CU-132): formato, orden, omision y repetidos. La
 * consulta contra la base la cubre {@code UsuariosInternosRepositorioTest}.
 */
class ResolverNombresDeUsuariosInternosTest {

    private static final UUID PROPUSO = UUID.fromString("c1320000-0000-4000-8000-00000000a001");
    private static final UUID APROBO = UUID.fromString("c1320000-0000-4000-8000-00000000b002");
    private static final UUID CLIENTE = UUID.fromString("c1320000-0000-4000-8000-00000000c003");

    private Datos datos;
    private NombresDeUsuariosInternosRepositorio repositorio;
    private ResolverNombresDeUsuariosInternos casoDeUso;
    private final ContextoSesion ctx = ContextoSesion.de(
            UUID.randomUUID(), "ADMIN_PLATAFORMA", new Traza(UUID.randomUUID().toString()));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        repositorio = mock(NombresDeUsuariosInternosRepositorio.class);
        casoDeUso = new ResolverNombresDeUsuariosInternos(datos, repositorio);
    }

    @Test
    @DisplayName("devuelve nombres y apellidos en el orden pedido y omite los ids que no son operadores activos")
    void nombresEnOrdenYOmiteLosAjenos() {
        when(repositorio.activos(any(), any()))
                .thenReturn(Map.of(
                        APROBO, new Nombre("Luis  Fernando", "Mamani Choque"),
                        PROPUSO, new Nombre("Carla", "Gutiérrez Rojas")));

        List<NombreDeUsuarioInterno> salida = casoDeUso.ejecutar(List.of(PROPUSO, CLIENTE, APROBO), ctx);

        assertThat(salida)
                .extracting(NombreDeUsuarioInterno::getUsuarioId, NombreDeUsuarioInterno::getNombre)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(PROPUSO, "Carla Gutiérrez Rojas"),
                        org.assertj.core.groups.Tuple.tuple(APROBO, "Luis Fernando Mamani Choque"));
    }

    @Test
    @DisplayName("un operador sin nombre cargado se omite en vez de salir vacio")
    void sinNombreSeOmite() {
        when(repositorio.activos(any(), any())).thenReturn(Map.of(PROPUSO, new Nombre(" ", null)));

        assertThat(casoDeUso.ejecutar(List.of(PROPUSO), ctx)).isEmpty();
    }

    @Test
    @DisplayName("rechaza un lote con ids repetidos con AP-CU08-07, sin consultar la base")
    void repetidosSeRechazan() {
        assertThatThrownBy(() -> casoDeUso.ejecutar(List.of(PROPUSO, PROPUSO), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU08-07"));
        verifyNoInteractions(repositorio);
    }
}
