package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.puertos.SimuladorDePagoDeRecarga;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * «Simular pago» de un QR de prueba: el proveedor de prueba pasa a decir que la orden se pago y la
 * acreditacion sigue el camino de siempre ({@link RecargasConProveedor#confirmar}). No hay un segundo
 * camino que mueva saldo: lo unico simulado es la respuesta del proveedor.
 *
 * <p>Solo una orden propia cuyo QR guardado sea de origen {@code PRUEBA}: un QR de banco no se puede
 * dar por pagado desde aca aunque el doble este activo.
 */
@Service
public class PagoDeRecargaDePrueba {

    private final RecargasConProveedor recargas;
    private final CU10RecargarSaldo ordenes;
    private final QrDeLaRecarga qrs;
    private final Optional<SimuladorDePagoDeRecarga> simulador;

    public PagoDeRecargaDePrueba(
            RecargasConProveedor recargas,
            CU10RecargarSaldo ordenes,
            QrDeLaRecarga qrs,
            ObjectProvider<SimuladorDePagoDeRecarga> simulador) {
        this.recargas = recargas;
        this.ordenes = ordenes;
        this.qrs = qrs;
        this.simulador = Optional.ofNullable(simulador.getIfAvailable());
    }

    /** Falso en {@code production}: ahi la operacion no existe. */
    public boolean disponible() {
        return simulador.isPresent();
    }

    public CU10RecargarSaldo.SalidaAcreditacion pagar(UUID ordenId, ContextoSesion ctx) {
        var proveedor = simulador.orElseThrow(() -> new IllegalStateException("Sin proveedor de prueba."));
        var orden = ordenes.consultarPropia(ordenId, ctx);
        boolean dePrueba = qrs.deLaOrden(ordenId, ctx)
                .map(qr -> "PRUEBA".equals(qr.origen()))
                .orElse(false);
        if (!dePrueba) {
            throw new ErrorDeNegocio(CodigoError.de(10, 5), "Esa recarga no tiene un QR de prueba.");
        }
        proveedor.marcarPagado(orden.id(), orden.bruto());
        return recargas.confirmar(orden.id(), ctx);
    }
}
