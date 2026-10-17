package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.aplicacion.IndicadoresDeEntregas;
import bo.aportaya.entregas.infraestructura.IndicadoresDeEntregasRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tableros · H4.S1.M5 — entregas pendientes y tiempo hasta el desembolso, reconstruidos con el historial. */
class CUIndicadoresDeEntregasTest extends BaseDeEntregas {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private final IndicadoresDeEntregasRepositorio repositorio = new IndicadoresDeEntregasRepositorio();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    private <T> T leer(java.util.function.Function<org.jooq.DSLContext, T> consulta) {
        return transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), consulta));
    }

    private static LocalDate diaLejano() {
        return LocalDate.of(2140, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(0, 30_000));
    }

    private UUID entrega(LocalDate programada, String estado) {
        var e = fixtura.escenario(fixtura.usuario());
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO entregas.entrega_fondo
                    (id, grupo_id, periodo_id, turno_id, cupo_id, beneficiario_participante_id,
                     monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar,
                     monto_efectivamente_entregado, moneda, estado, metodo_desembolso, fecha_programada, version)
                VALUES (?, ?, ?, ?, ?, ?, 6000.00, 0, 6000.00, 0, 'BOB', ?, 'BILLETERA_MOVIL', ?, 0)
                """,
                id,
                e.grupoId(),
                e.periodoId(),
                e.turnoId(),
                e.cupoId(),
                e.participanteId(),
                estado,
                programada);
        return id;
    }

    private void entregada(UUID entrega, LocalDate dia) {
        dslFixtura.execute(
                """
                INSERT INTO entregas.historial_estado_entrega
                    (id, entrega_id, estado_anterior, estado_nuevo, motivo, fecha_hora)
                VALUES (gen_random_uuid(), ?, 'EN_PROCESO_DESEMBOLSO', 'ENTREGADA', 'Desembolso confirmado', CAST(? AS timestamptz))
                """,
                entrega,
                dia + " 12:00:00-04");
    }

    @Test
    @DisplayName(
            "cuadre: una entrega programada para D y entregada en D+3 tarda 3 días y deja de estar pendiente en D+3")
    void tiempoYPendiente() {
        LocalDate d = diaLejano();
        long enDAntes = leer(x -> repositorio.pendientes(x, d)).cuantas();
        long enD3Antes = leer(x -> repositorio.pendientes(x, d.plusDays(3))).cuantas();
        UUID e = entrega(d, "PROGRAMADA");
        entregada(e, d.plusDays(3));

        // Pendiente en D (debia salir y no habia salido) y ya no en D+3 (salio ese dia).
        assertThat(leer(x -> repositorio.pendientes(x, d)).cuantas() - enDAntes).isEqualTo(1);
        assertThat(leer(x -> repositorio.pendientes(x, d.plusDays(3))).cuantas() - enD3Antes)
                .isZero();
        var tiempo = leer(x -> repositorio.tiempoHastaDesembolso(x, d.plusDays(3), d.plusDays(3)));
        assertThat(tiempo.dias()).isEqualByComparingTo("3.00");
        assertThat(tiempo.casos()).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: cero pendientes se publica (cero real); el tiempo sin entregas en el período no tiene valor")
    void ceroRealYSinDatos() {
        var calculados =
                leer(x -> new IndicadoresDeEntregas(repositorio).calcular(x, PeriodoDeTablero.de("1990-01-01")));

        assertThat(calculados.get(0).indicador().codigo()).isEqualTo("ENTREGAS_PENDIENTES");
        assertThat(calculados.get(0).indicador().valor()).isEqualByComparingTo("0");
        assertThat(calculados.get(1).indicador().valor()).isNull();
    }

    @Test
    @DisplayName("rechaza contar una entrega ANULADA como pendiente")
    void anuladaNoEsPendiente() {
        LocalDate d = diaLejano();
        long antes = leer(x -> repositorio.pendientes(x, d)).cuantas();

        entrega(d, "ANULADA");

        assertThat(leer(x -> repositorio.pendientes(x, d)).cuantas()).isEqualTo(antes);
    }
}
