package bo.aportaya.tarifas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-132 · qué versión rige en un momento y qué falta. Datos sintéticos. */
class VigenciaDeParametrosTest {

    private static final UUID P = UUID.fromString("c1320000-0000-4000-8000-0000000000a1");
    private static final UUID Q = UUID.fromString("c1320000-0000-4000-8000-0000000000a2");
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-11-21T12:00:00Z");

    private static VersionDelParametro v(UUID parametro, String nivel, String estado, OffsetDateTime desde) {
        return new VersionDelParametro(
                UUID.randomUUID(),
                parametro,
                "COD",
                Optional.ofNullable(nivel),
                BigDecimal.ONE,
                Optional.empty(),
                desde,
                UUID.randomUUID(),
                Optional.empty(),
                estado);
    }

    @Test
    @DisplayName("Correcto · rige la aprobada más reciente ya en vigor, una por parámetro y nivel")
    void laMasRecienteEnVigor() {
        var vieja = v(P, null, "APROBADA", INICIO.minusDays(10));
        var nueva = v(P, null, "APROBADA", INICIO.minusDays(1));
        var nivelA = v(Q, "A", "APROBADA", INICIO.minusDays(5));
        var nivelB = v(Q, "B", "APROBADA", INICIO.minusDays(5));

        assertThat(VigenciaDeParametros.vigentesAl(List.of(vieja, nivelA, nueva, nivelB), INICIO))
                .containsExactlyInAnyOrder(nueva, nivelA, nivelB);
    }

    @Test
    @DisplayName("Límite · la que entra en vigor justo en el momento ya rige; un instante después no")
    void bordeDelMomento() {
        var justo = v(P, null, "APROBADA", INICIO);
        var despues = v(P, null, "APROBADA", INICIO.plusNanos(1_000));

        assertThat(VigenciaDeParametros.vigentesAl(List.of(despues, justo), INICIO))
                .containsExactly(justo);
    }

    @Test
    @DisplayName("Inválido · una propuesta o una rechazada nunca rigen, aunque sean anteriores")
    void soloAprobadas() {
        assertThat(VigenciaDeParametros.vigentesAl(
                        List.of(
                                v(P, null, "PROPUESTA", INICIO.minusDays(1)),
                                v(P, null, "RECHAZADA", INICIO.minusDays(1))),
                        INICIO))
                .isEmpty();
    }

    @Test
    @DisplayName("Faltantes · los códigos del catálogo sin valor, ordenados")
    void faltantes() {
        var catalogo = List.of(
                new ParametroDelCatalogo(P, "p", new PropuestaDeParametro.Catalogo("ZETA", Optional.of("DIAS"), false)),
                new ParametroDelCatalogo(Q, "q", new PropuestaDeParametro.Catalogo("ALFA", Optional.empty(), false)));

        assertThat(VigenciaDeParametros.faltantes(catalogo, Set.of(P))).containsExactly("ALFA");
        assertThat(VigenciaDeParametros.faltantes(catalogo, Set.of())).containsExactly("ALFA", "ZETA");
        assertThat(VigenciaDeParametros.faltantes(catalogo, Set.of(P, Q))).isEmpty();
    }
}
