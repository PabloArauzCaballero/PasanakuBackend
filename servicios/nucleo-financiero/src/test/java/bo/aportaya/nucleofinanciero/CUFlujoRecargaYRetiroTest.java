package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.EntradaSolicitud;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.DatosSinteticos;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Catálogo de flujos · {@code docs/catalogo-de-flujos/recarga-y-retiro.yaml} (H8.S3.M3). La cadena entera con los
 * casos de uso reales contra Postgres: recargar, que el proveedor confirme, retirar, instruir y pagar. Los importes
 * los elige Datafaker con semilla fija, así cada corrida usa los mismos y el cuadre no depende de un número cómodo.
 */
class CUFlujoRecargaYRetiroTest extends BaseDeBilletera {

    private static final Dinero COSTO = Dinero.de("5.00", Moneda.BOB);

    private UUID cuenta;
    private UUID instrumento;
    private ContextoSesion ctx;

    @BeforeEach
    void billeteraVacia() {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        fixtura.limite("RETIRO", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        ctx = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero recargar(Dinero monto, String clave) {
        var orden = transaccion.execute(t -> recargaCU.solicitar(
                new EntradaSolicitud(clave, cuenta, monto, Dinero.cero(Moneda.BOB), "QR", Optional.empty()), ctx));
        var acreditada = transaccion.execute(t -> recargaCU.acreditar(
                orden.ordenRecargaId(), ConfirmacionesDePrueba.para(dsl, orden.ordenRecargaId()), ctx));
        assertThat(patasSumanCero(acreditada.transaccionId())).isTrue();
        return acreditada.saldoDespues();
    }

    private Dinero retirarYPagar(Dinero monto, String clave) {
        var solicitud = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(clave, cuenta, monto, COSTO, instrumento, true, true, false), ctx));
        assertThat(solicitud.estado()).isEqualTo("AUTORIZADA");
        transaccion.execute(t -> retiroCU.instruirPago(solicitud.ordenRetiroId(), ctx));
        var pago = transaccion.execute(t -> retiroCU.confirmarPago(solicitud.ordenRetiroId(), ctx));
        assertThat(patasSumanCero(pago.transaccionId())).isTrue();
        return pago.saldoDespues();
    }

    private boolean patasSumanCero(UUID transaccionId) {
        return contar(
                        """
                        SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)::int
                          FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?
                        """,
                        transaccionId)
                == 0;
    }

    private BigDecimal retenido() {
        return dsl.fetchOne("SELECT saldo_retenido FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta)
                .get(0, BigDecimal.class);
    }

    @Test
    @DisplayName(
            "cuadre: recargar, confirmar, retirar y pagar deja recarga − retiro, cada transacción con patas que suman cero")
    void cadenaCompleta() {
        var datos = DatosSinteticos.porDefecto();
        Dinero recarga = datos.monto("800.00", "1500.00", Moneda.BOB);
        Dinero retiro = datos.monto("100.00", "700.00", Moneda.BOB);

        assertThat(recargar(recarga, "flujo-recarga").monto()).isEqualByComparingTo(recarga.monto());
        Dinero saldo = retirarYPagar(retiro, "flujo-retiro");

        assertThat(saldo.monto()).isEqualByComparingTo(recarga.monto().subtract(retiro.monto()));
        assertThat(retenido()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("límite: retirar EXACTAMENTE todo el disponible se autoriza y deja la billetera en 0.00")
    void retirarTodo() {
        Dinero recarga = DatosSinteticos.conSemilla(7L).monto("300.00", "900.00", Moneda.BOB);
        recargar(recarga, "flujo-recarga-todo");

        assertThat(retirarYPagar(recarga, "flujo-retiro-todo").monto()).isEqualByComparingTo("0.00");
        assertThat(retenido()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza retirar un centavo más que el disponible (AP-CU11-01): no retiene nada ni crea la orden")
    void unCentavoDeMas() {
        Dinero recarga = DatosSinteticos.conSemilla(11L).monto("300.00", "900.00", Moneda.BOB);
        recargar(recarga, "flujo-recarga-centavo");
        Dinero deMas = Dinero.de(recarga.monto().add(new BigDecimal("0.01")).toPlainString(), Moneda.BOB);

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.solicitar(
                        new EntradaRetiro("flujo-retiro-centavo", cuenta, deMas, COSTO, instrumento, true, true, false),
                        ctx)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU11-01"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE cuenta_billetera_id = ?",
                        cuenta))
                .isZero();
        assertThat(retenido()).isEqualByComparingTo("0.00");
        assertThat(dsl.fetchOne("SELECT saldo_disponible FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo(recarga.monto());
    }
}
