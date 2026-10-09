package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.infraestructura.RecaudoRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H8.S1.M2 · El faltante al corte sale de las obligaciones reales, contra PostgreSQL real. */
class CU132RecaudoTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private ConsultarRecaudoDelPeriodo consulta() {
        return new ConsultarRecaudoDelPeriodo(
                new bo.aportaya.plataforma.datos.Datos(dsl), new RecaudoRepositorio(), Reloj.delSistema());
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    /** Un periodo con `cupos` obligaciones de Bs 500; devuelve el periodo y los ids en orden. */
    private record Periodo(UUID id, UUID[] obligaciones, ContextoSesion ctx) {}

    private Periodo periodo(int cupos) {
        UUID dueno = fixtura.usuario();
        UUID grupo = fixtura.grupo();
        UUID periodo = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO grupos.periodo
                    (id, grupo_id, numero, fecha_inicio, fecha_limite_pago, fecha_fin_gracia,
                     fecha_entrega_prevista, estado, monto_objetivo, monto_recaudado, cupos_morosos)
                VALUES (?, ?, 1, current_date - 30, current_date - 5, current_date - 2,
                        current_date + 1, 'ABIERTO', ?, 0, 0)
                """,
                periodo,
                grupo,
                new BigDecimal(500 * cupos));
        UUID[] ids = new UUID[cupos];
        for (int i = 0; i < cupos; i++) {
            UUID usuario = i == 0 ? dueno : fixtura.usuario();
            UUID participante = UUID.randomUUID();
            dslFixtura.execute(
                    """
                    INSERT INTO grupos.participante
                        (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso,
                         reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                    VALUES (?, ?, ?, 'ACTIVO', false, now(), 50, 0, 0)
                    """,
                    participante,
                    grupo,
                    usuario);
            UUID cupo = UUID.randomUUID();
            dslFixtura.execute(
                    """
                    INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                    VALUES (?, ?, ?, ?, 'OCUPADO', 1.0, now())
                    """,
                    cupo,
                    grupo,
                    (short) (i + 1),
                    participante);
            ids[i] = UUID.randomUUID();
            dslFixtura.execute(
                    """
                    INSERT INTO aportes.obligacion_aporte
                        (id, grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado,
                         moneda, monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia,
                         estado, fecha_vencimiento, fecha_fin_gracia, dias_mora, version)
                    VALUES (?, ?, ?, ?, ?, 'APORTE_PERIODICO', 500.00, 'BOB', 0, 0, 0, 0,
                            'PENDIENTE', current_date - 5, current_date - 2, 0, 0)
                    """,
                    ids[i],
                    grupo,
                    periodo,
                    cupo,
                    participante);
        }
        return new Periodo(periodo, ids, contextoDe(dueno));
    }

    private void pagar(UUID obligacion, String monto) {
        dslFixtura.execute(
                "UPDATE aportes.obligacion_aporte SET monto_pagado = ?, estado = 'PAGADO' WHERE id = ?",
                new BigDecimal(monto),
                obligacion);
    }

    @Test
    @DisplayName(
            "Dado un pozo de Bs 6.000 (12 cupos) con 10 pagados · Cuando se consulta el recaudo · Entonces confirmado 5.000, faltante 1.000 y las dos obligaciones pendientes")
    void ejemploDelPlan() {
        var p = periodo(12);
        for (int i = 0; i < 10; i++) {
            pagar(p.obligaciones()[i], "500.00");
        }

        var salida = transaccion.execute(t -> consulta().ejecutar(p.id(), p.ctx()));

        assertThat(salida.grupoId()).isNotNull();
        assertThat(salida.recaudo().pozo()).isEqualTo(bob("6000.00"));
        assertThat(salida.recaudo().confirmado()).isEqualTo(bob("5000.00"));
        assertThat(salida.recaudo().faltante()).isEqualTo(bob("1000.00"));
        assertThat(salida.recaudo().pendientes())
                .extracting(x -> x.obligacionId())
                .containsExactlyInAnyOrder(p.obligaciones()[10], p.obligaciones()[11]);
    }

    @Test
    @DisplayName(
            "Dada una obligacion pendiente y otra en pago parcial · Cuando se consulta · Entonces ningun pendiente cuenta como caja")
    void losPendientesNoSonCaja() {
        var p = periodo(2);
        dslFixtura.execute(
                "UPDATE aportes.obligacion_aporte SET monto_pagado = 300.00, estado = 'PAGADO_PARCIAL' WHERE id = ?",
                p.obligaciones()[0]);

        var salida = transaccion.execute(t -> consulta().ejecutar(p.id(), p.ctx()));

        assertThat(salida.recaudo().confirmado()).isEqualTo(bob("300.00"));
        assertThat(salida.recaudo().faltante()).isEqualTo(bob("700.00"));
    }

    @Test
    @DisplayName(
            "Dada una obligacion anulada · Cuando se consulta · Entonces ni entra al pozo ni aparece como pendiente")
    void anuladaNoCuenta() {
        var p = periodo(2);
        dslFixtura.execute("UPDATE aportes.obligacion_aporte SET estado = 'ANULADO' WHERE id = ?", p.obligaciones()[1]);

        var salida = transaccion.execute(t -> consulta().ejecutar(p.id(), p.ctx()));

        assertThat(salida.recaudo().pozo()).isEqualTo(bob("500.00"));
        assertThat(salida.recaudo().pendientes()).hasSize(1);
    }

    @Test
    @DisplayName(
            "Dado el fondo mutual que cubrio Bs 350 de una obligacion · Cuando se consulta · Entonces se cuenta aparte")
    void mutualAparte() {
        var p = periodo(1);
        dslFixtura.execute(
                "UPDATE aportes.obligacion_aporte SET monto_cubierto_garantia = 350.00 WHERE id = ?",
                p.obligaciones()[0]);

        var salida = transaccion.execute(t -> consulta().ejecutar(p.id(), p.ctx()));

        assertThat(salida.recaudo().cubiertoMutual()).isEqualTo(bob("350.00"));
        assertThat(salida.recaudo().faltante()).isEqualTo(bob("150.00"));
    }

    @Test
    @DisplayName(
            "Dado un periodo sin obligaciones · Cuando se consulta · Entonces se rechaza AP-CU21-05: no hay pozo que medir")
    void periodoSinObligaciones() {
        var ctx = contextoDe(fixtura.usuario());

        assertThatThrownBy(() -> transaccion.execute(t -> consulta().ejecutar(UUID.randomUUID(), ctx)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU21-05"));
    }

    @Test
    @DisplayName(
            "Dado el mismo periodo consultado dos veces sin pagos nuevos · Cuando se consulta · Entonces la respuesta es la misma (lectura pura)")
    void lecturaPura() {
        var p = periodo(3);
        pagar(p.obligaciones()[0], "500.00");

        var primera = transaccion.execute(t -> consulta().ejecutar(p.id(), p.ctx()));
        var segunda = transaccion.execute(t -> consulta().ejecutar(p.id(), p.ctx()));

        assertThat(segunda.recaudo()).isEqualTo(primera.recaudo());
        assertThat(contar("SELECT count(*)::int FROM aportes.evento_dominio")).isZero();
    }
}
