package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.OperacionDelAliado;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio.Orden;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Lo que el aliado confirma tiene que coincidir con lo pedido; nunca se acepta algo «porque vino firmado». */
final class ConfirmacionDeSuscripcion {

    private ConfirmacionDeSuscripcion() {}

    /**
     * Lo que el aliado confirma tiene que coincidir con lo que se pidio, y las cuotas con
     * una cuenta propia: nunca se acepta una confirmacion «porque vino firmada».
     */
    static void exigirQueCoincida(Orden o, OperacionDelAliado op, Condiciones c) {
        boolean ok = op.estado() == AliadoDeInversion.Estado.CONFIRMADO
                && op.monto().map(m -> m.compareTo(o.monto()) == 0).orElse(false)
                && op.posicionExterna().isPresent();
        if (ok && c.tipo() == TipoProducto.FONDO) {
            BigDecimal valor = new BigDecimal(op.detalle().getOrDefault("valorCuota", "0"));
            ok = valor.signum() > 0
                    && op.cuotas().isPresent()
                    && o.monto()
                                    .divide(valor, 6, RoundingMode.DOWN)
                                    .compareTo(op.cuotas().get())
                            == 0;
        }
        if (ok && c.tipo() == TipoProducto.DPF) {
            ok = op.detalle().containsKey("vencimiento");
        }
        if (!ok) {
            throw Errores.de(
                    122, 2, "La confirmacion del aliado no coincide con la orden: no se registra la posicion.");
        }
    }
}
