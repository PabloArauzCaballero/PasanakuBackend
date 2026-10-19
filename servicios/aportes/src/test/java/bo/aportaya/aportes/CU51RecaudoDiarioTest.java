package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.ConsultarRecaudoDiario;
import bo.aportaya.aportes.infraestructura.RecaudoDiarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El recaudo de un dia que {@code nucleo-financiero} pide para cerrarlo (CU-51, backoffice carril A · H8).
 *
 * <p>Datos sinteticos: pagos, conciliaciones y excepciones insertados como fixture, con instantes elegidos
 * a proposito en los bordes del dia de America/La_Paz (UTC-4). Correcto (totales y corte del dia), limite
 * (borde de medianoche, otra moneda, excepcion resuelta, dia vacio).
 */
class CU51RecaudoDiarioTest extends BaseDeAportes {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);

    @AfterEach
    void limpiar() {
        dslFixtura.execute("DELETE FROM aportes.excepcion_conciliacion");
        dslFixtura.execute("DELETE FROM aportes.conciliacion");
        fixtura.limpiar();
    }

    private static ConsultarRecaudoDiario consulta() {
        return new ConsultarRecaudoDiario(new Datos(dsl), new RecaudoDiarioRepositorio());
    }

    private static ContextoSesion contabilidad() {
        return ContextoSesion.de(
                UUID.randomUUID(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private static OffsetDateTime enLaPaz(LocalDate dia, int hora, int minuto) {
        return dia.atTime(LocalTime.of(hora, minuto)).atZone(LA_PAZ).toOffsetDateTime();
    }

    /** Un pago en {@code estado}, acreditado en {@code cuando}. */
    private UUID pago(String monto, String moneda, String estado, OffsetDateTime cuando) {
        var o = fixtura.obligacion(fixtura.usuario(), monto, 5);
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO aportes.pago
                    (id, obligacion_id, monto, moneda, monto_comision_proveedor, monto_neto_acreditado,
                     canal, estado, fecha_hora_pago, fecha_hora_acreditacion, referencia_proveedor,
                     es_manual, clave_idempotencia)
                VALUES (?, ?, ?, ?, 0, ?, 'QR_INTEROPERABLE', ?, ?::timestamptz, ?::timestamptz, ?, false, ?)
                """,
                id,
                o.id(),
                new BigDecimal(monto),
                moneda,
                new BigDecimal(monto),
                estado,
                cuando,
                cuando,
                "REF-" + id,
                "rd-" + id);
        return id;
    }

    private UUID conciliacion(UUID pago, String estado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO aportes.conciliacion (id, pago_id, estado, metodo, diferencia_monto, fecha_conciliacion)
                VALUES (?, ?, ?, 'REFERENCIA_EXACTA', 0, now())
                """,
                id,
                pago,
                estado);
        return id;
    }

    private void excepcion(UUID conciliacion, String diferencia, String estado) {
        dslFixtura.execute(
                """
                INSERT INTO aportes.excepcion_conciliacion
                    (id, conciliacion_id, tipo, descripcion, monto_diferencia, estado)
                VALUES (gen_random_uuid(), ?, 'MONTO_DISTINTO', 'diferencia de monto sintetica', ?, ?)
                """,
                conciliacion,
                new BigDecimal(diferencia),
                estado);
    }

    @Test
    @DisplayName(
            "Dado un día de La Paz con un pago conciliado de Bs 500 y otro de Bs 300 en excepción por Bs 20 · Cuando nucleo-financiero pide el recaudo del día · Entonces recibe recaudado 800, conciliado 500, excepciones 20 y una excepción abierta")
    void totalesDelDia() {
        UUID conciliado = pago("500.00", "BOB", "ACREDITADO", enLaPaz(DIA, 10, 0));
        conciliacion(conciliado, "CONCILIADO_AUTOMATICO");
        // 23:30 en La Paz es 03:30 UTC del dia siguiente: sigue siendo de este dia.
        UUID enExcepcion = pago("300.00", "BOB", "ACREDITADO", enLaPaz(DIA, 23, 30));
        excepcion(conciliacion(enExcepcion, "EN_EXCEPCION"), "-20.00", "ABIERTA");

        var salida = transaccion.execute(t -> consulta().ejecutar(DIA, contabilidad()));

        assertThat(salida.zonaHoraria()).isEqualTo("America/La_Paz");
        assertThat(salida.excepcionesAbiertas()).isEqualTo(1);
        assertThat(salida.porMoneda()).singleElement().satisfies(m -> {
            assertThat(m.moneda()).isEqualTo("BOB");
            assertThat(m.recaudado()).isEqualByComparingTo("800.00");
            assertThat(m.conciliado()).isEqualByComparingTo("500.00");
            assertThat(m.excepciones()).isEqualByComparingTo("20.00");
            assertThat(m.pagos()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName(
            "Dado un pago acreditado a las 23:00 del día anterior en La Paz · Cuando se pide el recaudo del día · Entonces no se cuenta aunque en UTC ya sea el día pedido")
    void bordeDeMedianoche() {
        // 23:00 del 9 en La Paz = 03:00 UTC del 10: un corte en UTC lo contaria mal.
        pago("100.00", "BOB", "ACREDITADO", enLaPaz(DIA.minusDays(1), 23, 0));
        pago("40.00", "BOB", "ACREDITADO", enLaPaz(DIA, 0, 0));

        var salida = transaccion.execute(t -> consulta().ejecutar(DIA, contabilidad()));

        assertThat(salida.porMoneda()).singleElement().satisfies(m -> {
            assertThat(m.recaudado()).isEqualByComparingTo("40.00");
            assertThat(m.pagos()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dados pagos del día en BOB y en USD · Cuando se pide el recaudo · Entonces vuelve un total por moneda y nunca una suma de las dos")
    void unTotalPorMoneda() {
        pago("700.00", "BOB", "ACREDITADO", enLaPaz(DIA, 9, 0));
        pago("50.00", "USD", "ACREDITADO", enLaPaz(DIA, 9, 30));

        var salida = transaccion.execute(t -> consulta().ejecutar(DIA, contabilidad()));

        assertThat(salida.porMoneda())
                .extracting(ConsultarRecaudoDiario.PorMoneda::moneda)
                .containsExactly("BOB", "USD");
        assertThat(salida.porMoneda().get(1).recaudado()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName(
            "Dados un pago reversado y una excepción ya resuelta · Cuando se pide el recaudo del día · Entonces ninguno de los dos cuenta")
    void reversadoYResueltaNoCuentan() {
        pago("900.00", "BOB", "REVERSADO", enLaPaz(DIA, 11, 0));
        UUID ok = pago("200.00", "BOB", "ACREDITADO", enLaPaz(DIA, 12, 0));
        excepcion(conciliacion(ok, "CONCILIADO_MANUAL"), "5.00", "RESUELTA");

        var salida = transaccion.execute(t -> consulta().ejecutar(DIA, contabilidad()));

        assertThat(salida.excepcionesAbiertas()).isZero();
        assertThat(salida.porMoneda()).singleElement().satisfies(m -> {
            assertThat(m.recaudado()).isEqualByComparingTo("200.00");
            assertThat(m.conciliado()).isEqualByComparingTo("200.00");
            assertThat(m.excepciones()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    @DisplayName(
            "Dado un día sin pagos · Cuando se pide el recaudo · Entonces la lista por moneda va vacía y no hay excepciones, nunca un cero inventado en BOB")
    void diaVacio() {
        var salida = transaccion.execute(t -> consulta().ejecutar(DIA, contabilidad()));

        assertThat(salida.porMoneda()).isEmpty();
        assertThat(salida.excepcionesAbiertas()).isZero();
    }
}
