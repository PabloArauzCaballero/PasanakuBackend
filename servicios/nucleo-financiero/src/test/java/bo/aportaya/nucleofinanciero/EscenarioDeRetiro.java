package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.UUID;

/** Una billetera con saldo, su destino habilitado y la custodia en regla: lo que un retiro necesita. */
record EscenarioDeRetiro(UUID usuario, UUID cuenta, UUID instrumento, ContextoSesion ctx) {

    /** El catalogo que un retiro y una transferencia necesitan; una sola vez por prueba (los limites son unicos). */
    static void catalogo() {
        var base = BaseDeBilletera.fixtura;
        base.tipoDeCambioDeHoy();
        BaseDeBilletera.custodia.cumpleEncaje();
        base.limite("RETIRO", "ESTANDAR", "MES", new BigDecimal("100000.00"), null);
        base.limite("TRANSFERENCIA", "ESTANDAR", "MES", new BigDecimal("100000.00"), null);
    }

    /** Una billetera nueva con saldo y su destino habilitado, sobre un catalogo ya cargado. */
    static EscenarioDeRetiro nuevo(String saldo) {
        var base = BaseDeBilletera.fixtura;
        UUID usuario = base.usuario();
        UUID cuenta = base.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        base.acreditar(cuenta, new BigDecimal(saldo));
        UUID instrumento = BaseDeBilletera.custodia.instrumentoDestino(usuario, true, true, null);
        var ctx = ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        return new EscenarioDeRetiro(usuario, cuenta, instrumento, ctx);
    }

    static EscenarioDeRetiro con(String saldo) {
        catalogo();
        return nuevo(saldo);
    }

    static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    EntradaRetiro pedido(String clave, String monto, String costo) {
        return new EntradaRetiro(clave, cuenta, bob(monto), bob(costo), instrumento, true, false);
    }

    SalidaRetiro solicitar(String clave, String monto, String costo) {
        return BaseDeBilletera.transaccion.execute(
                t -> BaseDeBilletera.retiroCU.solicitar(pedido(clave, monto, costo), ctx));
    }
}
