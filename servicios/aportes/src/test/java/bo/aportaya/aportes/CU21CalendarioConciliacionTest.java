package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.ConciliarLoteTripartito;
import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDelPeriodo;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.LineaDelProveedor;
import bo.aportaya.aportes.infraestructura.ConciliacionRepositorio;
import bo.aportaya.aportes.infraestructura.RecaudoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M1 · De punta a punta dentro de `aportes`: calendario generado -> cobros acreditados -> recaudo
 * del periodo -> conciliacion del lote contra el extracto. El recaudo, lo conciliado y las diferencias
 * tienen que cerrar al centavo, y la conciliacion no fabrica ni mueve ningun pago.
 */
class CU21CalendarioConciliacionTest extends BaseDeCalendario {

    @AfterEach
    void limpiar() {
        dslFixtura.execute("DELETE FROM aportes.excepcion_conciliacion");
        dslFixtura.execute("DELETE FROM aportes.conciliacion");
        dslFixtura.execute("DELETE FROM aportes.movimiento_bancario");
        dslFixtura.execute("DELETE FROM aportes.extracto_bancario");
        fixtura.limpiar();
    }

    private void cobrar(UUID obligacion, UUID usuario, String clave) {
        transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        clave,
                        obligacion,
                        bob("500.00"),
                        bob("0.00"),
                        "QR_INTEROPERABLE",
                        "ref-" + clave,
                        Optional.empty(),
                        false,
                        true),
                contextoDe(usuario)));
    }

    private UUID extractoConMovimientos(UUID usuario, String[][] movimientos) {
        UUID proveedor = fixtura.proveedor("CAL-" + UUID.randomUUID().toString().substring(0, 8), true, 1);
        UUID extracto = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO aportes.extracto_bancario
                    (id, proveedor_id, cuenta, fecha_desde, fecha_hasta, saldo_inicial, saldo_final,
                     archivo_url, importado_en, importado_por)
                VALUES (?, ?, 'CTA-PRUEBA', current_date - 1, current_date + 1, 0, 0,
                        'archivo://sintetico', now(), ?)
                """,
                extracto,
                proveedor,
                usuario);
        for (String[] m : movimientos) {
            dslFixtura.execute(
                    """
                    INSERT INTO aportes.movimiento_bancario
                        (id, extracto_id, fecha_movimiento, monto, moneda, glosa, referencia_banco, conciliado)
                    VALUES (gen_random_uuid(), ?, current_date, ?, 'BOB', 'Movimiento sintetico', ?, false)
                    """,
                    extracto,
                    new BigDecimal(m[1]),
                    m[0]);
        }
        return extracto;
    }

    private BigDecimal suma(String consulta, Object... parametros) {
        return dsl.fetchOne(consulta, parametros).get(0, BigDecimal.class);
    }

    @Test
    @DisplayName(
            "Dadas las obligaciones generadas de un período con pagos acreditados y un extracto del banco · Cuando se concilia el lote · Entonces lo conciliado coincide con el recaudo confirmado del período · Y las diferencias quedan con su causa y su responsable, sin crear ni mover ningún pago")
    void criterio9() {
        fixtura.feriado(LocalDate.of(2026, 5, 1), "Dia del Trabajo - feriado nacional");
        var p = fixtura.periodoAbierto(3, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        afirma(p, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        var calendario = generar(p.periodoId());
        var o = calendario.calendario();
        UUID titular = p.cupos().get(0).usuarioId();

        // Dos pagan (R1 y R2); el tercero queda pendiente. El banco trae R1 bien y R2 por Bs 490.
        cobrar(o.get(0).obligacionId(), p.cupos().get(0).usuarioId(), "c1");
        cobrar(o.get(1).obligacionId(), p.cupos().get(1).usuarioId(), "c2");
        UUID extracto = extractoConMovimientos(titular, new String[][] {{"ref-c1", "500.00"}, {"ref-c2", "490.00"}});
        var reporte =
                List.of(new LineaDelProveedor("ref-c1", bob("500.00")), new LineaDelProveedor("ref-c2", bob("490.00")));

        var recaudo = transaccion.execute(
                t -> new ConsultarRecaudoDelPeriodo(new Datos(dsl), new RecaudoRepositorio(), Reloj.delSistema())
                        .ejecutar(p.periodoId(), contextoDe(titular)));
        int pagosAntes = contar("SELECT count(*)::int FROM aportes.pago");
        BigDecimal montoAntes = suma("SELECT COALESCE(sum(monto), 0) FROM aportes.pago");

        var informe = transaccion.execute(t -> new ConciliarLoteTripartito(
                        new Datos(dsl), new ConciliacionRepositorio(), new Outbox("aportes"), Reloj.delSistema())
                .ejecutar(
                        extracto,
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1),
                        reporte,
                        contextoDe(titular)));

        // El recaudo sale de las obligaciones del calendario: 3 x 500, 1.000 confirmados, 500 por cobrar.
        assertThat(recaudo.recaudo().pozo()).isEqualTo(bob("1500.00"));
        assertThat(recaudo.recaudo().confirmado()).isEqualTo(bob("1000.00"));
        assertThat(recaudo.recaudo().faltante()).isEqualTo(bob("500.00"));
        assertThat(recaudo.recaudo().pendientes())
                .extracting(x -> x.obligacionId())
                .containsExactly(o.get(2).obligacionId());

        // Lo conciliado automaticamente (R1) + lo que quedo en excepcion (R2) = lo confirmado: nada se pierde.
        BigDecimal conciliado = suma(
                """
                SELECT COALESCE(sum(p.monto), 0) FROM aportes.pago p
                  JOIN aportes.conciliacion c ON c.pago_id = p.id
                 WHERE c.estado = 'CONCILIADO_AUTOMATICO'
                """);
        BigDecimal enExcepcion = suma(
                """
                SELECT COALESCE(sum(p.monto), 0) FROM aportes.pago p
                  JOIN aportes.conciliacion c ON c.pago_id = p.id
                 WHERE c.estado = 'EN_EXCEPCION'
                """);
        assertThat(conciliado).isEqualByComparingTo("500.00");
        assertThat(enExcepcion).isEqualByComparingTo("500.00");
        assertThat(conciliado.add(enExcepcion))
                .isEqualByComparingTo(recaudo.recaudo().confirmado().monto());
        assertThat(informe.excepcionesNuevas()).isEqualTo(1);
        assertThat(
                        contar(
                                "SELECT count(*)::int FROM aportes.excepcion_conciliacion WHERE tipo = 'MONTO_DISTINTO' AND estado = 'ABIERTA' AND descripcion LIKE '%responsable%'"))
                .isEqualTo(1);

        // Sin fabricar abonos: ni un pago nuevo, ni un importe movido, ni una obligacion tocada.
        assertThat(contar("SELECT count(*)::int FROM aportes.pago")).isEqualTo(pagosAntes);
        assertThat(suma("SELECT COALESCE(sum(monto), 0) FROM aportes.pago")).isEqualByComparingTo(montoAntes);
        assertThat(suma(
                        "SELECT COALESCE(sum(monto_pagado), 0) FROM aportes.obligacion_aporte WHERE periodo_id = ?",
                        p.periodoId()))
                .isEqualByComparingTo("1000.00");
        // Y repetir el lote no duplica nada (la identidad de la conciliacion la decide la base).
        var otraVez = transaccion.execute(t -> new ConciliarLoteTripartito(
                        new Datos(dsl), new ConciliacionRepositorio(), new Outbox("aportes"), Reloj.delSistema())
                .ejecutar(
                        extracto,
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1),
                        reporte,
                        contextoDe(titular)));
        assertThat(otraVez.conciliacionesNuevas()).isZero();
        assertThat(contar("SELECT count(*)::int FROM aportes.conciliacion")).isEqualTo(2);
    }
}
