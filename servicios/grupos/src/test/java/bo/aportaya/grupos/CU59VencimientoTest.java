package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo.DiaSalteado;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo.EntradaVencimiento;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo.SalidaVencimiento;
import bo.aportaya.grupos.dominio.CalendarioVacio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-59 · H7.S1.M3 · el vencimiento con sus tres fechas (UTC, contractual, de liquidacion) contra el
 * calendario REAL de la base y la zona America/La_Paz (UTC-4, sin horario de verano).
 *
 * <p>Los feriados que cada prueba necesita los siembra la propia prueba con la fuente en la
 * descripcion; ninguno se da por existente. Las fechas son las del calendario oficial boliviano
 * ya sembrado (1 de mayo, Viernes Santo 2027) y no se mezclan con las de {@link CU59Test}.
 */
class CU59VencimientoTest extends BaseDeCU59 {

    private SalidaVencimiento vencimiento(String instante, String alcance, Optional<UUID> ref, Optional<String> zona) {
        return transaccion.execute(e -> calcularPlazo.vencimientoLocal(
                new EntradaVencimiento(Instant.parse(instante), alcance, ref, zona), contexto()));
    }

    @Test
    @DisplayName(
            "Dado un vencimiento a las 22:00 del 1 de mayo de 2026 hora de La Paz (02:00 UTC del 2 de mayo) y el 1 de mayo feriado nacional · Cuando se calcula el vencimiento local en America/La_Paz · Entonces la fecha contractual es el 1 de mayo y la de liquidación el lunes 4 de mayo · Y la respuesta muestra también la fecha en UTC, el 2 de mayo, y los días que no contaron")
    void criterio5() {
        feriado(LocalDate.of(2026, 5, 1), "NACIONAL", "Dia del Trabajo - feriado nacional");

        var v = vencimiento("2026-05-02T02:00:00Z", "NACIONAL", Optional.empty(), Optional.empty());

        assertThat(v.zonaHoraria()).isEqualTo("America/La_Paz");
        assertThat(v.fechaEnUtc()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(v.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(v.corridaPorNoHabil()).isTrue();
        assertThat(v.diasSalteados())
                .extracting(DiaSalteado::fecha)
                .containsExactly(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3));
        assertThat(v.diasSalteados().get(0).descripcion()).containsIgnoringCase("trabajo");
        assertThat(v.diasSalteados().get(1).descripcion()).isEqualTo("Sabado");
        assertThat(v.diasSalteados().get(2).descripcion()).isEqualTo("Domingo");
    }

    @Test
    @DisplayName(
            "Dado el mismo instante calculado en la zona UTC · Cuando se calcula el vencimiento local · Entonces la fecha contractual es el 2 de mayo y la de liquidación el lunes 4 de mayo · Y las dos zonas dan fechas contractuales distintas para el mismo instante")
    void criterio6() {
        feriado(LocalDate.of(2026, 5, 1), "NACIONAL", "Dia del Trabajo - feriado nacional");

        var enUtc = vencimiento("2026-05-02T02:00:00Z", "NACIONAL", Optional.empty(), Optional.of("UTC"));
        var enLaPaz = vencimiento("2026-05-02T02:00:00Z", "NACIONAL", Optional.empty(), Optional.empty());

        assertThat(enUtc.zonaHoraria()).isEqualTo("UTC");
        assertThat(enUtc.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(enUtc.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(enUtc.fechaContractual()).isNotEqualTo(enLaPaz.fechaContractual());
        // Aun asi, las dos terminan en el mismo lunes: la zona cambia la fecha del contrato, no el calendario.
        assertThat(enUtc.fechaDeLiquidacion()).isEqualTo(enLaPaz.fechaDeLiquidacion());
    }

    @Test
    @DisplayName(
            "Dado un vencimiento a las 23:30 del 31 de diciembre de 2026 hora de La Paz (03:30 UTC del 1 de enero de 2027, feriado) · Cuando se calcula el vencimiento local · Entonces la fecha contractual y la de liquidación son el 31 de diciembre · Y el feriado del 1 de enero en UTC no corre nada")
    void criterio7() {
        feriado(LocalDate.of(2026, 12, 25), "NACIONAL", "Navidad - feriado nacional");
        feriado(LocalDate.of(2027, 1, 1), "NACIONAL", "Ano Nuevo - feriado nacional");

        var v = vencimiento("2027-01-01T03:30:00Z", "NACIONAL", Optional.empty(), Optional.empty());

        assertThat(v.fechaEnUtc()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(v.fechaContractual()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(v.corridaPorNoHabil()).isFalse();
        assertThat(v.diasSalteados()).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado un vencimiento el Viernes Santo de 2027, feriado nacional · Cuando se calcula el vencimiento local · Entonces la liquidación es el lunes siguiente · Y diasSalteados enumera el feriado, el sábado y el domingo")
    void criterio8() {
        feriado(LocalDate.of(2027, 3, 26), "NACIONAL", "Viernes Santo - feriado movil (Pascua 2027-03-28)");

        var v = vencimiento("2027-03-26T15:00:00Z", "NACIONAL", Optional.empty(), Optional.empty());

        assertThat(v.fechaContractual()).isEqualTo(LocalDate.of(2027, 3, 26));
        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2027, 3, 29));
        assertThat(v.fechaDeLiquidacion().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(v.diasSalteados())
                .extracting(DiaSalteado::fecha)
                .containsExactly(LocalDate.of(2027, 3, 26), LocalDate.of(2027, 3, 27), LocalDate.of(2027, 3, 28));
    }

    @Test
    @DisplayName(
            "Dado un feriado local de un grupo sobre un vencimiento de ese grupo · Cuando se calcula el vencimiento local con el alcance GRUPO · Entonces la liquidación se corre al día hábil siguiente · Y el mismo instante con alcance NACIONAL no se corre")
    void criterio9() {
        // Miercoles 14 de octubre de 2026: dia no habil SOLO de este grupo (fecha sintetica de prueba).
        feriado(LocalDate.of(2026, 12, 25), "NACIONAL", "Navidad - feriado nacional");
        UUID grupo = fixtura.grupoConformado(3);
        feriadoDeGrupo(grupo, LocalDate.of(2026, 10, 14), "[SINTETICO] asueto del grupo de prueba");

        var delGrupo = vencimiento("2026-10-14T15:00:00Z", "GRUPO", Optional.of(grupo), Optional.empty());
        var nacional = vencimiento("2026-10-14T15:00:00Z", "NACIONAL", Optional.empty(), Optional.empty());
        var deOtroGrupo =
                vencimiento("2026-10-14T15:00:00Z", "GRUPO", Optional.of(UUID.randomUUID()), Optional.empty());

        assertThat(delGrupo.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(delGrupo.diasSalteados()).extracting(DiaSalteado::fecha).containsExactly(LocalDate.of(2026, 10, 14));
        assertThat(nacional.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(nacional.corridaPorNoHabil()).isFalse();
        // Un feriado de grupo no se filtra a otro grupo.
        assertThat(deOtroGrupo.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 10, 14));
    }

    @Test
    @DisplayName(
            "Dado un vencimiento en un año sin calendario cargado · Cuando se calcula el vencimiento local · Entonces se rechaza con CALENDARIO_VACIO")
    void criterio10() {
        assertThatThrownBy(() -> vencimiento("2031-01-05T15:00:00Z", "NACIONAL", Optional.empty(), Optional.empty()))
                .isInstanceOf(CalendarioVacio.class)
                .hasMessageContaining("2031");
    }

    @Test
    @DisplayName("rechaza por AP-CU59-06 una zona horaria que no existe, sin calcular nada")
    void rechazaZonaInexistente() {
        assertThatThrownBy(() -> vencimiento(
                        "2026-05-02T02:00:00Z", "NACIONAL", Optional.empty(), Optional.of("America/Atlantida")))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU59-06"));
    }

    @Test
    @DisplayName("rechaza por AP-CU59-02 un vencimiento de alcance GRUPO sin grupo")
    void rechazaGrupoSinReferencia() {
        assertThatThrownBy(() -> vencimiento("2026-05-02T02:00:00Z", "GRUPO", Optional.empty(), Optional.empty()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU59-02"));
    }

    @Test
    @DisplayName("reintento: el mismo instante calculado dos veces da las mismas tres fechas y no escribe nada")
    void reintentoDelVencimiento() {
        feriado(LocalDate.of(2026, 12, 25), "NACIONAL", "Navidad - feriado nacional");
        int antes = dsl.fetchCount(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("catalogo", "dia_no_habil")));

        var primera = vencimiento("2026-12-26T01:00:00Z", "NACIONAL", Optional.empty(), Optional.empty());
        var segunda = vencimiento("2026-12-26T01:00:00Z", "NACIONAL", Optional.empty(), Optional.empty());

        // 21:00 del viernes 25 en La Paz (Navidad): contractual 25, liquidacion lunes 28.
        assertThat(primera.fechaContractual()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(primera.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(segunda).isEqualTo(primera);
        assertThat(dsl.fetchCount(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("catalogo", "dia_no_habil"))))
                .isEqualTo(antes);
    }
}
