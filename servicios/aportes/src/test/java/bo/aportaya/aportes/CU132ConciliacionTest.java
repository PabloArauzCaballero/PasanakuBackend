package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.ConciliarLoteTripartito;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.Causa;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.LineaDelProveedor;
import bo.aportaya.aportes.infraestructura.ConciliacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H13.S1.M1 · Un lote de tres fuentes contra PostgreSQL real, con discrepancias resueltas por evidencia. */
class CU132ConciliacionTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        dslFixtura.execute("DELETE FROM aportes.excepcion_conciliacion");
        dslFixtura.execute("DELETE FROM aportes.conciliacion");
        dslFixtura.execute("DELETE FROM aportes.movimiento_bancario");
        dslFixtura.execute("DELETE FROM aportes.extracto_bancario");
        fixtura.limpiar();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private ConciliarLoteTripartito caso() {
        return new ConciliarLoteTripartito(
                new Datos(dsl), new ConciliacionRepositorio(), new Outbox("aportes"), Reloj.delSistema());
    }

    private UUID pago(UUID usuario, String referencia, String monto) {
        var o = fixtura.obligacion(usuario, monto, 5);
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO aportes.pago
                    (id, obligacion_id, monto, moneda, monto_comision_proveedor, monto_neto_acreditado,
                     canal, estado, fecha_hora_pago, fecha_hora_acreditacion, referencia_proveedor,
                     es_manual, clave_idempotencia)
                VALUES (?, ?, ?, 'BOB', 0, ?, 'QR_INTEROPERABLE', 'ACREDITADO', now(), now(), ?, false, ?)
                """,
                id,
                o.id(),
                new BigDecimal(monto),
                new BigDecimal(monto),
                referencia,
                "conc-" + id);
        return id;
    }

    private UUID extracto(UUID usuario) {
        UUID proveedor =
                fixtura.proveedor("CONC-" + UUID.randomUUID().toString().substring(0, 8), true, 1);
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO aportes.extracto_bancario
                    (id, proveedor_id, cuenta, fecha_desde, fecha_hasta, saldo_inicial, saldo_final,
                     archivo_url, importado_en, importado_por)
                VALUES (?, ?, 'CTA-PRUEBA', current_date - 1, current_date + 1, 0, 0,
                        'archivo://sintetico', now(), ?)
                """,
                id,
                proveedor,
                usuario);
        return id;
    }

    private void movimiento(UUID extracto, String referencia, String monto) {
        dslFixtura.execute(
                """
                INSERT INTO aportes.movimiento_bancario
                    (id, extracto_id, fecha_movimiento, monto, moneda, glosa, referencia_banco, conciliado)
                VALUES (gen_random_uuid(), ?, current_date, ?, 'BOB', 'Movimiento sintetico', ?, false)
                """,
                extracto,
                new BigDecimal(monto),
                referencia);
    }

    private BigDecimal suma(String consulta) {
        return dsl.fetchOne(consulta).get(0, BigDecimal.class);
    }

    @Test
    @DisplayName(
            "Dado un lote con 2 conciliados, 3 discrepancias de pago y 2 huerfanos · Cuando se concilia · Entonces cada una queda anotada con su causa y su responsable, los huerfanos se informan, y NO se crea ni se toca ningun pago")
    void loteDeTresFuentes() {
        UUID usuario = fixtura.usuario();
        pago(usuario, "R1", "500.00");
        pago(usuario, "R2", "500.00");
        pago(usuario, "R3", "500.00");
        pago(usuario, "R4", "500.00");
        pago(usuario, "R5", "500.00");
        UUID extracto = extracto(usuario);
        movimiento(extracto, "R1", "500.00");
        movimiento(extracto, "R2", "500.00");
        movimiento(extracto, "R3", "490.00");
        movimiento(extracto, "R5", "500.00");
        movimiento(extracto, "R8", "800.00");
        var reporte = List.of(
                new LineaDelProveedor("R1", bob("500.00")),
                new LineaDelProveedor("R3", bob("490.00")),
                new LineaDelProveedor("R4", bob("500.00")),
                new LineaDelProveedor("R5", bob("500.00")),
                new LineaDelProveedor("R9", bob("700.00")));
        int pagosAntes = contar("SELECT count(*)::int FROM aportes.pago");
        BigDecimal montoAntes = suma("SELECT COALESCE(sum(monto), 0) FROM aportes.pago");
        BigDecimal pagadoAntes = suma("SELECT COALESCE(sum(monto_pagado), 0) FROM aportes.obligacion_aporte");

        var informe = transaccion.execute(t -> caso().ejecutar(
                        extracto,
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1),
                        reporte,
                        contextoDe(usuario)));

        assertThat(informe.conciliacionesNuevas()).isEqualTo(5);
        assertThat(informe.excepcionesNuevas()).isEqualTo(3);
        assertThat(informe.movimientosHuerfanos()).isEqualTo(1);
        assertThat(informe.confirmacionesHuerfanas()).isEqualTo(1);
        assertThat(informe.hallazgos())
                .extracting(h -> h.causa())
                .contains(
                        Causa.SIN_CONFIRMACION_DEL_PROVEEDOR,
                        Causa.MONTO_DISTINTO,
                        Causa.SIN_MOVIMIENTO_BANCARIO,
                        Causa.MOVIMIENTO_SIN_PAGO,
                        Causa.CONFIRMACION_SIN_PAGO);
        assertThat(contar("SELECT count(*)::int FROM aportes.conciliacion WHERE estado = 'CONCILIADO_AUTOMATICO'"))
                .isEqualTo(2);
        assertThat(contar("SELECT count(*)::int FROM aportes.conciliacion WHERE estado = 'EN_EXCEPCION'"))
                .isEqualTo(3);
        assertThat(
                        contar(
                                "SELECT count(*)::int FROM aportes.excepcion_conciliacion WHERE estado = 'ABIERTA' AND descripcion LIKE '%responsable%'"))
                .isEqualTo(3);
        assertThat(
                        contar(
                                "SELECT count(DISTINCT tipo)::int FROM aportes.excepcion_conciliacion WHERE tipo IN ('SIN_CONFIRMACION_PROVEEDOR','MONTO_DISTINTO','SIN_MOVIMIENTO_BANCARIO')"))
                .isEqualTo(3);
        // Solo se marcan conciliados los movimientos que coinciden con libro y proveedor.
        assertThat(contar("SELECT count(*)::int FROM aportes.movimiento_bancario WHERE conciliado"))
                .isEqualTo(2);
        // Sin fabricar abonos: ni un pago nuevo, ni un importe movido, ni una obligacion tocada.
        assertThat(contar("SELECT count(*)::int FROM aportes.pago")).isEqualTo(pagosAntes);
        assertThat(suma("SELECT COALESCE(sum(monto), 0) FROM aportes.pago")).isEqualByComparingTo(montoAntes);
        assertThat(suma("SELECT COALESCE(sum(monto_pagado), 0) FROM aportes.obligacion_aporte"))
                .isEqualByComparingTo(pagadoAntes);
    }

    @Test
    @DisplayName(
            "IDEMPOTENCIA · Dado el mismo lote corrido dos veces · Cuando se concilia de nuevo · Entonces no se abre ninguna excepcion mas")
    void repetirNoDuplica() {
        UUID usuario = fixtura.usuario();
        pago(usuario, "R1", "500.00");
        pago(usuario, "R2", "500.00");
        UUID extracto = extracto(usuario);
        movimiento(extracto, "R1", "500.00");
        var reporte = List.of(new LineaDelProveedor("R1", bob("500.00")));
        ContextoSesion ctx = contextoDe(usuario);

        var primera = transaccion.execute(t -> caso().ejecutar(
                        extracto, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), reporte, ctx));
        int excepciones = contar("SELECT count(*)::int FROM aportes.excepcion_conciliacion");
        var segunda = transaccion.execute(t -> caso().ejecutar(
                        extracto, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), reporte, ctx));

        assertThat(primera.conciliacionesNuevas()).isEqualTo(2);
        assertThat(segunda.conciliacionesNuevas()).isZero();
        assertThat(segunda.yaProcesadas()).isEqualTo(2);
        assertThat(contar("SELECT count(*)::int FROM aportes.excepcion_conciliacion"))
                .isEqualTo(excepciones);
        assertThat(contar("SELECT count(*)::int FROM aportes.conciliacion")).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "LIMITE · Dado un periodo sin pagos ni movimientos · Cuando se concilia · Entonces no hay nada que anotar")
    void sinNada() {
        UUID usuario = fixtura.usuario();
        UUID extracto = extracto(usuario);

        var informe = transaccion.execute(t -> caso().ejecutar(
                        extracto,
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(11),
                        List.of(),
                        contextoDe(usuario)));

        assertThat(informe.conciliacionesNuevas()).isZero();
        assertThat(informe.hallazgos()).isEmpty();
    }
}
