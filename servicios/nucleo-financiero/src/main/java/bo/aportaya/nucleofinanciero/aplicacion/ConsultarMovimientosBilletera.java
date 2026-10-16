package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.MovimientoBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta reciente, de solo lectura, del libro visible para el titular autenticado. */
@Service
public class ConsultarMovimientosBilletera {

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final MovimientoBilleteraRepositorio movimientos;
    private final int topeDeMovimientos;

    public ConsultarMovimientosBilletera(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            MovimientoBilleteraRepositorio movimientos,
            @Value("${aportaya.billetera.tope-de-movimientos}") int topeDeMovimientos) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.topeDeMovimientos = topeDeMovimientos;
    }

    @Transactional(readOnly = true)
    public Salida ejecutar(UUID cuentaId, Integer limite, ContextoSesion ctx) {
        int cantidad = limite == null ? 5 : limite;
        if (cantidad < 1 || cantidad > topeDeMovimientos) {
            throw new ErrorDeNegocio(
                    CodigoError.de(13, 2), "El límite debe estar entre 1 y " + topeDeMovimientos + ".");
        }

        return datos.conContexto(ctx, dsl -> {
            var cuenta = cuentas.ver(dsl, cuentaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 1), "Esa cuenta no existe."));
            return new Salida(
                    cuenta.id(),
                    movimientos.recientes(dsl, cuenta.id(), cuenta.moneda(), cantidad).stream()
                            .map(m -> new Movimiento(
                                    m.id(),
                                    m.sentido(),
                                    m.monto(),
                                    m.saldoDisponiblePosterior(),
                                    m.saldoRetenidoPosterior(),
                                    m.glosa(),
                                    m.registradoEn()))
                            .toList());
        });
    }

    public record Salida(UUID cuentaId, List<Movimiento> movimientos) {}

    /** Una linea del libro, en terminos del caso de uso: la web no lee el registro del repositorio. */
    public record Movimiento(
            UUID id,
            String sentido,
            Dinero monto,
            Dinero saldoDisponiblePosterior,
            Dinero saldoRetenidoPosterior,
            String glosa,
            OffsetDateTime registradoEn) {}
}
