package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CalendarioDelPeriodo;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H8.S1.M1 · Lo invalido del contrato con `grupos` y de la base: no se genera nada. */
class CU21CalendarioRechazosTest extends BaseDeCalendario {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName("rechaza por AP-CU21-08 si grupos no contesta: no se genera nada y no se inventa el titular")
    void rechazaSinRespuestaDeGrupos() {
        UUID periodo = UUID.randomUUID();
        grupos.respuesta = Optional.empty();

        assertThatThrownBy(() -> generar(periodo)).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU21-08"));
        assertThat(contar("SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ?", periodo))
                .isZero();
    }

    @Test
    @DisplayName("rechaza por AP-CU21-08 si grupos contesta por OTRO periodo, un cupo repetido o un importe en cero")
    void rechazaRespuestaInutilizable() {
        var p = fixtura.periodoAbierto(2, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        var buena = afirma(p, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));

        // Contesta por otro periodo.
        grupos.respuesta = Optional.of(new CalendarioDelPeriodo.Periodo(
                UUID.randomUUID(),
                buena.grupoId(),
                1,
                "ABIERTO",
                buena.fechaLimitePago(),
                buena.fechaFinGracia(),
                buena.cupos()));
        assertThatThrownBy(() -> generar(p.periodoId())).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU21-08"));

        // Cupo repetido.
        var repetido = List.of(buena.cupos().get(0), buena.cupos().get(0));
        grupos.respuesta = Optional.of(new CalendarioDelPeriodo.Periodo(
                p.periodoId(),
                buena.grupoId(),
                1,
                "ABIERTO",
                buena.fechaLimitePago(),
                buena.fechaFinGracia(),
                repetido));
        assertThatThrownBy(() -> generar(p.periodoId())).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU21-08"));

        // Importe cero.
        var cero = List.of(new CalendarioDelPeriodo.Cupo(
                buena.cupos().get(0).cupoId(), buena.cupos().get(0).participanteId(), bob("0.00")));
        grupos.respuesta = Optional.of(new CalendarioDelPeriodo.Periodo(
                p.periodoId(), buena.grupoId(), 1, "ABIERTO", buena.fechaLimitePago(), buena.fechaFinGracia(), cero));
        assertThatThrownBy(() -> generar(p.periodoId())).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU21-08"));

        // Nada de lo anterior escribio.
        assertThat(contar("SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ?", p.periodoId()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza por AP-CU21-04 un periodo que no esta ABIERTO")
    void rechazaPeriodoCerrado() {
        var p = fixtura.periodoAbierto(1, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        var buena = afirma(p, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        grupos.respuesta = Optional.of(new CalendarioDelPeriodo.Periodo(
                p.periodoId(),
                buena.grupoId(),
                1,
                "CERRADO",
                buena.fechaLimitePago(),
                buena.fechaFinGracia(),
                buena.cupos()));

        assertThatThrownBy(() -> generar(p.periodoId())).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU21-04"));
        assertThat(contar("SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ?", p.periodoId()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza por AP-CU21-09 un vencimiento en un anio sin calendario de feriados: no se cuenta corrido")
    void rechazaSinCalendario() {
        // 2031 no tiene un solo dia cargado (ver CU59Test): un anio sin feriados no es un anio sin calendario.
        var p = fixtura.periodoAbierto(1, LocalDate.of(2031, 3, 12), LocalDate.of(2031, 3, 15));
        afirma(p, LocalDate.of(2031, 3, 12), LocalDate.of(2031, 3, 15));

        assertThatThrownBy(() -> generar(p.periodoId())).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU21-09"));
        assertThat(contar("SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ?", p.periodoId()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza por R-GRP-03 una segunda obligacion periodica para el mismo cupo en la base")
    void rechazaDuplicadoEnLaBase() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(1, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        afirma(p, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        generar(p.periodoId());
        var cupo = p.cupos().get(0);

        assertThat(rechazaLaBase(
                        """
                        INSERT INTO aportes.obligacion_aporte
                            (id, grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda,
                             estado, fecha_vencimiento, fecha_fin_gracia, dias_mora, version)
                        VALUES (gen_random_uuid(), '%s', '%s', '%s', '%s', 'APORTE_PERIODICO', 500.00, 'BOB',
                                'PENDIENTE', DATE '2026-05-12', DATE '2026-05-15', 0, 0)
                        """
                                .formatted(p.grupoId(), p.periodoId(), cupo.cupoId(), cupo.participanteId())))
                .contains("uq_obligacion_periodo_cupo");
    }
}
