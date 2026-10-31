package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU21AbrirPrimeraCuota;
import bo.aportaya.aportes.aplicacion.CU21AbrirPrimeraCuota.GrupoIniciado;
import bo.aportaya.aportes.aplicacion.CU21AbrirPrimeraCuota.Resultado;
import bo.aportaya.aportes.aplicacion.RegistroDelCalendario;
import bo.aportaya.aportes.infraestructura.CalendarioDeObligacionesRepositorio;
import bo.aportaya.aportes.infraestructura.FeriadosRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * R-GRP-20 · La cuota 1 nace al iniciar el grupo, contra PostgreSQL real y el evento {@code grupos.grupo_iniciado} en
 * los tres niveles de su contrato: correcto (tres cupos → tres obligaciones), limite (el mismo inicio dos veces, con el
 * mismo id y con otro id) e invalido (sin cupos, con un aporte en cero, con un periodo que no es el 1).
 */
class CU21PrimeraCuotaTest extends BaseDeCalendario {

    private static final LocalDate LIMITE = LocalDate.of(2026, 5, 12);
    private static final LocalDate GRACIA = LocalDate.of(2026, 5, 15);

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private CU21AbrirPrimeraCuota abridor() {
        Datos datos = new Datos(dsl);
        return new CU21AbrirPrimeraCuota(
                datos,
                consumidos,
                new RegistroDelCalendario(
                        datos,
                        new FeriadosRepositorio(),
                        new CalendarioDeObligacionesRepositorio(),
                        new ObligacionRepositorio(),
                        new Outbox("aportes")));
    }

    private static GrupoIniciado inicioDe(FixturaDeAportes.PeriodoDePrueba p, UUID eventoId, String monto) {
        return new GrupoIniciado(
                eventoId,
                UUID.randomUUID(),
                p.grupoId(),
                p.periodoId(),
                1,
                LIMITE,
                GRACIA,
                p.cupos().stream()
                        .map(c -> new CU21AbrirPrimeraCuota.Cupo(c.cupoId(), c.participanteId(), bob(monto)))
                        .toList());
    }

    private CU21AbrirPrimeraCuota.Salida procesar(GrupoIniciado e) {
        return transaccion.execute(t -> abridor().procesar(e));
    }

    private int obligacionesDe(UUID periodoId) {
        return contar(
                "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ? AND tipo = 'APORTE_PERIODICO' AND estado = 'PENDIENTE'",
                periodoId);
    }

    @Test
    @DisplayName(
            "Dado un grupo iniciado con tres cupos ocupados y Bs 500 de aporte · Cuando llega grupos.grupo_iniciado · Entonces existe la obligación PENDIENTE de la cuota 1 de cada cupo · Y se emite un solo evento de generación")
    void correcto() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(3, LIMITE, GRACIA);

        var salida = procesar(inicioDe(p, UUID.randomUUID(), "500.00"));

        assertThat(salida.resultado()).isEqualTo(Resultado.ABIERTA);
        assertThat(salida.creadas()).isEqualTo(3);
        assertThat(obligacionesDe(p.periodoId())).isEqualTo(3);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE periodo_id = ? AND monto_esperado = 500.00",
                        p.periodoId()))
                .isEqualTo(3);
        assertThat(eventosDeGeneracion(p.periodoId())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el mismo evento entregado dos veces · Cuando se procesa la segunda · Entonces responde DUPLICADO sin crear obligaciones ni emitir nada")
    void limiteMismoEvento() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(3, LIMITE, GRACIA);
        UUID eventoId = UUID.randomUUID();
        procesar(inicioDe(p, eventoId, "500.00"));

        var segunda = procesar(inicioDe(p, eventoId, "500.00"));

        assertThat(segunda.resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(segunda.creadas()).isZero();
        assertThat(obligacionesDe(p.periodoId())).isEqualTo(3);
        assertThat(eventosDeGeneracion(p.periodoId())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un inicio re-emitido con otro id de evento · Cuando se procesa · Entonces la unicidad periodo + cupo deja cero obligaciones nuevas y no se emite otro evento")
    void limiteOtroEventoMismoGrupo() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(2, LIMITE, GRACIA);
        procesar(inicioDe(p, UUID.randomUUID(), "500.00"));

        var reemision = procesar(inicioDe(p, UUID.randomUUID(), "500.00"));

        assertThat(reemision.resultado()).isEqualTo(Resultado.ABIERTA);
        assertThat(reemision.creadas()).isZero();
        assertThat(obligacionesDe(p.periodoId())).isEqualTo(2);
        assertThat(eventosDeGeneracion(p.periodoId())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un grupo con la cuota 1 abierta y un cupo que ya la pagó · Cuando grupos pregunta por la primera cuota · Entonces ese cupo está en pagados y los otros dos en pendientes, con el período de la cuota 1")
    void consultaTrasUnCobro() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(3, LIMITE, GRACIA);
        procesar(inicioDe(p, UUID.randomUUID(), "500.00"));
        UUID pagado = p.cupos().getFirst().cupoId();
        dslFixtura.execute(
                "UPDATE aportes.obligacion_aporte SET estado = 'PAGADO', monto_pagado = monto_esperado WHERE cupo_id = ?",
                pagado);

        var consulta = new bo.aportaya.aportes.aplicacion.ConsultarPrimeraCuota(
                new Datos(dsl), new bo.aportaya.aportes.infraestructura.PrimeraCuotaRepositorio());
        var estado = transaccion.execute(t -> consulta.delGrupo(p.grupoId(), contextoDe(UUID.randomUUID())));

        assertThat(estado.periodoId()).contains(p.periodoId());
        assertThat(estado.cuposPagados()).containsExactly(pagado);
        assertThat(estado.cuposPendientes())
                .containsExactlyInAnyOrderElementsOf(p.cupos().stream()
                        .map(FixturaDeAportes.CupoDePrueba::cupoId)
                        .filter(c -> !c.equals(pagado))
                        .toList());
    }

    @Test
    @DisplayName(
            "Dado un grupo sin obligaciones · Cuando grupos pregunta por la primera cuota · Entonces las dos listas vienen vacías y no hay período")
    void consultaSinObligaciones() {
        var consulta = new bo.aportaya.aportes.aplicacion.ConsultarPrimeraCuota(
                new Datos(dsl), new bo.aportaya.aportes.infraestructura.PrimeraCuotaRepositorio());

        var estado = transaccion.execute(t -> consulta.delGrupo(UUID.randomUUID(), contextoDe(UUID.randomUUID())));

        assertThat(estado.periodoId()).isEmpty();
        assertThat(estado.cuposPagados()).isEmpty();
        assertThat(estado.cuposPendientes()).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado un evento sin cupos · Cuando se procesa · Entonces se rechaza sin tocar la base ni marcarse consumido")
    void invalidoSinCupos() {
        var p = fixtura.periodoAbierto(2, LIMITE, GRACIA);
        UUID eventoId = UUID.randomUUID();
        var sinCupos = new GrupoIniciado(
                eventoId, UUID.randomUUID(), p.grupoId(), p.periodoId(), 1, LIMITE, GRACIA, List.of());

        assertThatThrownBy(() -> procesar(sinCupos)).isInstanceOf(IllegalArgumentException.class);

        assertThat(obligacionesDe(p.periodoId())).isZero();
        assertThat(contar("SELECT count(*)::int FROM aportes.evento_consumido WHERE id_evento = ?", eventoId))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un cupo con aporte en cero · Cuando se procesa · Entonces se rechaza entero: ningún cupo queda con obligación")
    void invalidoMontoCero() {
        var p = fixtura.periodoAbierto(2, LIMITE, GRACIA);

        assertThatThrownBy(() -> procesar(inicioDe(p, UUID.randomUUID(), "0.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(obligacionesDe(p.periodoId())).isZero();
    }

    @Test
    @DisplayName(
            "Dado un evento cuyo período no es el 1 · Cuando se procesa · Entonces se rechaza: el inicio del grupo abre la cuota 1 y ninguna otra")
    void invalidoPeriodoDistintoDeUno() {
        var p = fixtura.periodoAbierto(2, LIMITE, GRACIA);
        var cupos = p.cupos().stream()
                .map(c -> new CU21AbrirPrimeraCuota.Cupo(c.cupoId(), c.participanteId(), bob("500.00")))
                .toList();
        var segundo = new GrupoIniciado(
                UUID.randomUUID(), UUID.randomUUID(), p.grupoId(), p.periodoId(), 2, LIMITE, GRACIA, cupos);

        assertThatThrownBy(() -> procesar(segundo)).isInstanceOf(IllegalArgumentException.class);

        assertThat(obligacionesDe(p.periodoId())).isZero();
    }
}
