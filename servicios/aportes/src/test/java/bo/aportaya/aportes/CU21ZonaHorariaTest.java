package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H7.S1.M3 · El dia de una obligacion es el de La Paz (UTC-4), no el de UTC.
 *
 * <p>El ultimo dia de gracia es el 7 de mayo de 2026. A las 21:00 de La Paz de ese dia, UTC ya marca
 * el 8 (01:00Z): contar el dia en UTC dejaria en mora a quien paga a tiempo. La medianoche de La Paz
 * (04:00Z del 8) es el primer instante en que si esta vencida. Reloj fijo, sin sorpresas.
 */
class CU21ZonaHorariaTest extends BaseDeAportes {

    private static final Instant ULTIMO_DIA_A_LAS_21_EN_LA_PAZ = Instant.parse("2026-05-08T01:00:00Z");
    private static final Instant MEDIANOCHE_EN_LA_PAZ = Instant.parse("2026-05-08T04:00:00Z");
    private static final Instant UN_SEGUNDO_ANTES = Instant.parse("2026-05-08T03:59:59Z");

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private CU21CobrarAporte conReloj(Instant instante) {
        return new CU21CobrarAporte(
                new Datos(dsl),
                new ObligacionRepositorio(),
                new PagoRepositorio(),
                new Outbox("aportes"),
                Reloj.fijo(instante));
    }

    /** Una obligacion de Bs 500 cuyo ultimo dia de gracia es el 7 de mayo de 2026. */
    private FixturaDeAportes.Obligacion obligacionConGraciaHastaElSiete(UUID usuario) {
        var o = fixtura.obligacion(usuario, "500.00", 10);
        dslFixtura.execute(
                "UPDATE aportes.obligacion_aporte SET fecha_vencimiento = DATE '2026-05-05', fecha_fin_gracia = DATE '2026-05-07' WHERE id = ?",
                o.id());
        return o;
    }

    private String pagarParcial(CU21CobrarAporte caso, UUID obligacion, UUID usuario, String clave) {
        return transaccion
                .execute(t -> caso.acreditar(
                        new EntradaCobro(
                                clave,
                                obligacion,
                                Dinero.de("100.00", Moneda.BOB),
                                Dinero.de("0.00", Moneda.BOB),
                                "BILLETERA_MOVIL",
                                "ref-" + clave,
                                Optional.empty(),
                                false,
                                true),
                        contextoDe(usuario)))
                .estadoObligacion();
    }

    @Test
    @DisplayName(
            "Dado un pago a las 21:00 de La Paz del último día de gracia, que en UTC ya es el día siguiente · Cuando se acredita el pago parcial · Entonces la obligación no está vencida · Y a la medianoche de La Paz sí lo está")
    void criterio8() {
        UUID usuario = fixtura.usuario();
        var aTiempo = obligacionConGraciaHastaElSiete(usuario);
        var alBorde = obligacionConGraciaHastaElSiete(usuario);
        var tarde = obligacionConGraciaHastaElSiete(usuario);

        String a21 = pagarParcial(conReloj(ULTIMO_DIA_A_LAS_21_EN_LA_PAZ), aTiempo.id(), usuario, "a-las-21");
        String unSegundoAntes = pagarParcial(conReloj(UN_SEGUNDO_ANTES), alBorde.id(), usuario, "un-segundo-antes");
        String enLaMedianoche = pagarParcial(conReloj(MEDIANOCHE_EN_LA_PAZ), tarde.id(), usuario, "medianoche");

        assertThat(a21).isEqualTo("PAGADO_PARCIAL");
        assertThat(unSegundoAntes).isEqualTo("PAGADO_PARCIAL");
        assertThat(enLaMedianoche).isEqualTo("EN_MORA");
    }

    @Test
    @DisplayName(
            "cuadre: el recargo de mora no sale mientras en La Paz todavia es el ultimo dia de gracia, y sale una sola vez al pasar")
    void elRecargoEsperaALaMedianocheDeLaPaz() {
        UUID usuario = fixtura.usuario();
        var o = obligacionConGraciaHastaElSiete(usuario);
        fixtura.politicaDeMora(o.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");

        var antes =
                transaccion.execute(t -> conReloj(ULTIMO_DIA_A_LAS_21_EN_LA_PAZ).generarRecargos(contextoDe(usuario)));
        var despues = transaccion.execute(t -> conReloj(MEDIANOCHE_EN_LA_PAZ).generarRecargos(contextoDe(usuario)));
        var otraVez = transaccion.execute(t -> conReloj(MEDIANOCHE_EN_LA_PAZ).generarRecargos(contextoDe(usuario)));

        assertThat(antes.generados()).isZero();
        assertThat(despues.generados()).isEqualTo(1);
        assertThat(otraVez.generados()).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE tipo = 'RECARGO_MORA' AND obligacion_origen_id = ?",
                        o.id()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: una obligacion parcialmente pagada que vence tambien recibe su recargo, una sola vez")
    void elPagoParcialVencidoTambienGeneraRecargo() {
        UUID usuario = fixtura.usuario();
        var o = obligacionConGraciaHastaElSiete(usuario);
        fixtura.politicaDeMora(o.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");
        // Pago parcial A TIEMPO: queda PAGADO_PARCIAL y despues se vence sin que nadie la toque.
        assertThat(pagarParcial(conReloj(ULTIMO_DIA_A_LAS_21_EN_LA_PAZ), o.id(), usuario, "parcial"))
                .isEqualTo("PAGADO_PARCIAL");

        var generados = transaccion.execute(t -> conReloj(MEDIANOCHE_EN_LA_PAZ).generarRecargos(contextoDe(usuario)));
        var repetido = transaccion.execute(t -> conReloj(MEDIANOCHE_EN_LA_PAZ).generarRecargos(contextoDe(usuario)));

        assertThat(generados.generados()).isEqualTo(1);
        assertThat(repetido.generados()).isZero();
        // El recargo es sobre lo que falta (400), no sobre el esperado: 5 % de 400.
        assertThat(contar(
                        "SELECT monto_esperado::int FROM aportes.obligacion_aporte WHERE obligacion_origen_id = ?",
                        o.id()))
                .isEqualTo(20);
    }
}
