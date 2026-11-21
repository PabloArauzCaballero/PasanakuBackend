package bo.aportaya.tarifas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.tarifas.dominio.ParametroDelCatalogo;
import bo.aportaya.tarifas.dominio.PropuestaDeParametro.Catalogo;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros.ParametroCongelado;
import bo.aportaya.tarifas.infraestructura.CongelamientoRepositorio;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F3.S8.M6 · la consulta por grupo en sus tres respuestas (200, 404, 422), con el repositorio doblado. */
class ParametrosDelGrupoTest {

    private static final UUID GRUPO = UUID.fromString("c1320000-0000-4000-8000-0000000000b1");
    private static final UUID DIAS = UUID.fromString("c1320000-0000-4000-8000-0000000000b2");
    private static final UUID APORTE = UUID.fromString("c1320000-0000-4000-8000-0000000000b3");

    private CongelamientoRepositorio congelamientos;
    private ConsultarParametrosDelGrupo consulta;
    private final ContextoSesion ctx = ContextoSesion.de(
            UUID.randomUUID(), "SERVICIO_GARANTIA", new Traza(UUID.randomUUID().toString()));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        var parametros = mock(ParametroRepositorio.class);
        congelamientos = mock(CongelamientoRepositorio.class);
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        when(parametros.catalogo(any()))
                .thenReturn(List.of(
                        new ParametroDelCatalogo(DIAS, "d", new Catalogo("DIAS_DE_GRACIA", Optional.of("DIAS"), false)),
                        new ParametroDelCatalogo(
                                APORTE, "a", new Catalogo("APORTE_FONDO_GRUPO", Optional.of("PORCENTAJE"), true))));
        consulta = new ConsultarParametrosDelGrupo(datos, parametros, congelamientos);
    }

    private static ParametroCongelado fila(UUID parametro, String codigo, String nivel) {
        return new ParametroCongelado(
                parametro,
                codigo,
                Optional.ofNullable(nivel),
                new BigDecimal("1.000000"),
                Optional.of("DIAS"),
                Optional.empty(),
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-21T12:00:00Z"));
    }

    @Test
    @DisplayName("Correcto · completo (el parámetro por nivel con dos niveles) devuelve todas las filas")
    void completo() {
        var filas = List.of(
                fila(APORTE, "APORTE_FONDO_GRUPO", "A"),
                fila(APORTE, "APORTE_FONDO_GRUPO", "B"),
                fila(DIAS, "DIAS_DE_GRACIA", null));
        when(congelamientos.delGrupo(any(), eq(GRUPO))).thenReturn(filas);

        assertThat(consulta.delGrupo(GRUPO, ctx)).contains(filas);
    }

    @Test
    @DisplayName("Límite · un grupo sin ninguna fila no tiene congelamiento: vacío (404), no 422")
    void sinCongelamiento() {
        when(congelamientos.delGrupo(any(), eq(GRUPO))).thenReturn(List.of());

        assertThat(consulta.delGrupo(GRUPO, ctx)).isEmpty();
    }

    @Test
    @DisplayName("Inválido · incompleto es AP-CU132-06 y nombra el código que falta")
    void incompleto() {
        when(congelamientos.delGrupo(any(), eq(GRUPO))).thenReturn(List.of(fila(DIAS, "DIAS_DE_GRACIA", null)));

        assertThatThrownBy(() -> consulta.delGrupo(GRUPO, ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("APORTE_FONDO_GRUPO");
    }
}
