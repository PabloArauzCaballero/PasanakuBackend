package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.MovimientoBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta reciente, de solo lectura, del libro visible para el titular autenticado. */
@Service
public class ConsultarMovimientosBilletera {

    private static final int LIMITE_MAXIMO = 100;

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final MovimientoBilleteraRepositorio movimientos;

    public ConsultarMovimientosBilletera(
            Datos datos, CuentaBilleteraRepositorio cuentas, MovimientoBilleteraRepositorio movimientos) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public Salida ejecutar(UUID cuentaId, Integer limite, ContextoSesion ctx) {
        int cantidad = limite == null ? 5 : limite;
        if (cantidad < 1 || cantidad > LIMITE_MAXIMO) {
            throw new ErrorDeNegocio(CodigoError.de(13, 2), "El límite debe estar entre 1 y 100.");
        }

        return datos.conContexto(ctx, dsl -> {
            var cuenta = cuentas.ver(dsl, cuentaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 1), "Esa cuenta no existe."));
            return new Salida(cuenta.id(), movimientos.recientes(dsl, cuenta.id(), cuenta.moneda(), cantidad));
        });
    }

    public record Salida(UUID cuentaId, List<MovimientoBilleteraRepositorio.Movimiento> movimientos) {}
}
