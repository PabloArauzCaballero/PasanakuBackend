package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo.EntradaCreacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.infraestructura.CalendarioDeAportesRepositorio;
import bo.aportaya.grupos.infraestructura.ConsultasRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-20 · La etapa minima queda guardada al crear y se lee de vuelta para exigirla en cada ingreso (2026-10-30). Sobre
 * PostgreSQL real; datos sinteticos. La etapa la resuelve la API con {@code transparencia} ({@code EtapaMinimaWebTest});
 * aca se prueba la persistencia: que el {@code desde} llega a {@code reputacion_minima} y que quien todavia NO integra el
 * grupo lo lee igual (con rol de sistema), porque una fila invisible no puede leerse como «no pide nada».
 */
class CU20EtapaMinimaTest extends BaseDeCU20 {

    private final Consultas consultas =
            new Consultas(new Datos(dsl), new ConsultasRepositorio(), new CalendarioDeAportesRepositorio());

    private EntradaCreacion conMinima(UUID tarifario, BigDecimal minima) {
        var e = entrada(tarifario, true, true);
        return new EntradaCreacion(
                e.datos(),
                e.organizador(),
                e.organizadorHabilitado(),
                e.tarifarioVigenteId(),
                e.licenciaHabilitaElServicio(),
                e.permitePermutaDeTurnos(),
                e.claveIdempotencia(),
                e.limiteDeGrupos(),
                e.nombreDelCreador(),
                minima);
    }

    @Test
    @DisplayName(
            "Dado un tarifario vigente y un organizador que pide una etapa minima cuyo desde es 550,00 · "
                    + "Cuando crea el grupo y alguien que todavia no es integrante lee sus condiciones de ingreso · "
                    + "Entonces reputacion_minima guarda 550,00 y esa persona la lee junto con los cupos, el aporte, la moneda y el estado BORRADOR")
    void guardaYSeLeeLaMinima() {
        UUID tarifario = UUID.randomUUID();
        dobles.publicarTarifario(tarifario);

        var creado = transaccion.execute(
                t -> crearGrupo.ejecutar(conMinima(tarifario, new BigDecimal("550.00")), contexto()));

        assertThat((BigDecimal)
                        dsl.fetchValue("SELECT reputacion_minima FROM grupos.grupo WHERE id = ?", creado.grupoId()))
                .isEqualByComparingTo("550.00");
        UUID ajeno = fixtura.usuario();
        var condiciones = transaccion.execute(t -> consultas.condicionesDeIngreso(
                creado.grupoId(),
                ContextoSesion.de(
                        ajeno, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()))));
        assertThat(condiciones).hasValueSatisfying(c -> {
            assertThat(c.reputacionMinima()).isEqualByComparingTo("550.00");
            assertThat(c.cuposTotales()).isEqualTo(4);
            assertThat(c.montoAporte()).isEqualByComparingTo("500.00");
            assertThat(c.moneda()).isEqualTo("BOB");
            assertThat(c.estado()).isEqualTo("BORRADOR");
            assertThat(c.recibeIntegrantes()).isFalse();
        });
    }

    @Test
    @DisplayName("Dado un organizador que crea un grupo sin etapa minima resuelta (la forma de antes) · "
            + "Cuando se crea el grupo · "
            + "Entonces reputacion_minima queda en 0 y el grupo no pide ninguna etapa")
    void sinMinimaEsCero() {
        UUID tarifario = UUID.randomUUID();
        var creado = crear(tarifario);

        assertThat((BigDecimal)
                        dsl.fetchValue("SELECT reputacion_minima FROM grupos.grupo WHERE id = ?", creado.grupoId()))
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Dado un grupo que no existe · "
            + "Cuando se piden sus condiciones de ingreso · "
            + "Entonces no hay condiciones y el caso de uso responde que no existe")
    void grupoInexistente() {
        var condiciones = transaccion.execute(t -> consultas.condicionesDeIngreso(UUID.randomUUID(), contexto()));
        assertThat(condiciones).isEmpty();
    }

    @Test
    @DisplayName("Dado un grupo creado con una clave pidiendo la etapa MEDIA · "
            + "Cuando se repite la misma clave pidiendo la etapa ALTA · "
            + "Entonces no es un reintento y recibe AP-CU20-02")
    void mismaClaveOtraMinima() {
        UUID tarifario = UUID.randomUUID();
        dobles.publicarTarifario(tarifario);
        var base = conMinima(tarifario, new BigDecimal("550.00"));
        var primera = pedida(base, base.claveIdempotencia(), new BigDecimal("550.00"), "MEDIA");
        transaccion.execute(t -> crearGrupo.ejecutar(primera, contexto()));
        // La huella compara lo PEDIDO (la etapa), no la minima que la API derivo de la etapa de hoy.
        var otra = pedida(primera, primera.claveIdempotencia(), new BigDecimal("600.00"), "ALTA");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transaccion.execute(t -> crearGrupo.ejecutar(otra, contexto())))
                .isInstanceOf(bo.aportaya.plataforma.dominio.ErrorDeNegocio.class)
                .satisfies(e -> assertThat(((bo.aportaya.plataforma.dominio.ErrorDeNegocio) e)
                                .codigo()
                                .valor())
                        .isEqualTo("AP-CU20-02"));
    }

    private static EntradaCreacion pedida(EntradaCreacion e, UUID clave, BigDecimal minima, String etapa) {
        return new EntradaCreacion(
                e.datos(),
                e.organizador(),
                true,
                e.tarifarioVigenteId(),
                true,
                e.permitePermutaDeTurnos(),
                clave,
                e.limiteDeGrupos(),
                e.nombreDelCreador(),
                minima,
                etapa);
    }

    @Test
    @DisplayName("Dado un grupo creado con una clave cuando la minima derivada de la etapa del organizador era 500 · "
            + "Cuando el organizador cambio de etapa y repite la misma clave con la minima en 0 o en 550 · "
            + "Entonces yaCreado lo reconoce antes de preguntar la etapa, vuelve el MISMO grupo y reputacion_minima sigue en 500 · "
            + "Y la misma clave pidiendo otra etapa recibe AP-CU20-02")
    void reintentoConOtraMinimaDerivadaDevuelveElMismoGrupo() {
        UUID tarifario = UUID.randomUUID();
        dobles.publicarTarifario(tarifario);
        var primera = conMinima(tarifario, new BigDecimal("500.00"));
        assertThat(transaccion.<Boolean>execute(t -> crearGrupo.yaCreado(primera.claveIdempotencia(), contexto())))
                .isFalse();
        var creado = transaccion.execute(t -> crearGrupo.ejecutar(primera, contexto()));

        assertThat(transaccion.<Boolean>execute(t -> crearGrupo.yaCreado(primera.claveIdempotencia(), contexto())))
                .isTrue();
        // La API, en el reintento, no resuelve la etapa: la minima llega en 0, o con la etapa de hoy si se resolviera.
        for (String minimaDeHoy : new String[] {"0.00", "550.00"}) {
            var reintento = conMinima(tarifario, new BigDecimal(minimaDeHoy));
            var repetido = transaccion.execute(t -> crearGrupo.ejecutar(
                    new EntradaCreacion(
                            reintento.datos(),
                            reintento.organizador(),
                            reintento.organizadorHabilitado(),
                            reintento.tarifarioVigenteId(),
                            reintento.licenciaHabilitaElServicio(),
                            reintento.permitePermutaDeTurnos(),
                            primera.claveIdempotencia(),
                            reintento.limiteDeGrupos(),
                            reintento.nombreDelCreador(),
                            reintento.reputacionMinima()),
                    contexto()));
            assertThat(repetido.grupoId()).isEqualTo(creado.grupoId());
        }
        assertThat((BigDecimal)
                        dsl.fetchValue("SELECT reputacion_minima FROM grupos.grupo WHERE id = ?", creado.grupoId()))
                .isEqualByComparingTo("500.00");
        // Otra etapa PEDIDA con la misma clave si es otra creacion.
        var otraPedida = new EntradaCreacion(
                primera.datos(),
                primera.organizador(),
                primera.organizadorHabilitado(),
                primera.tarifarioVigenteId(),
                primera.licenciaHabilitaElServicio(),
                primera.permitePermutaDeTurnos(),
                primera.claveIdempotencia(),
                primera.limiteDeGrupos(),
                primera.nombreDelCreador(),
                new BigDecimal("0.00"),
                "BAJA");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transaccion.execute(t -> crearGrupo.ejecutar(otraPedida, contexto())))
                .isInstanceOf(bo.aportaya.plataforma.dominio.ErrorDeNegocio.class)
                .extracting(e -> ((bo.aportaya.plataforma.dominio.ErrorDeNegocio) e)
                        .codigo()
                        .valor())
                .isEqualTo("AP-CU20-02");
    }
}
