package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiros;
import bo.aportaya.plataforma.dominio.Dinero;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** El pago de retiros contra el proveedor ficticio local; mismas guardas que las recargas. */
@Component
public class RetirosProveedorSimulado implements ProveedorDeRetiros {

    private final ClienteProveedorSimulado cliente;

    public RetirosProveedorSimulado(
            @Value("${aportaya.proveedor.modo:deshabilitado}") String modo,
            @Value("${aportaya.ambiente:desarrollo}") String ambiente,
            @Value("${aportaya.proveedor.url:http://127.0.0.1:4020}") URI base,
            @Value("${aportaya.proveedor.api-key:}") String apiKey,
            @Value("${aportaya.proveedor.firma-secreto:}") String secreto,
            @Value("${aportaya.proveedor.timeout:PT5S}") Duration timeout) {
        this.cliente = new ClienteProveedorSimulado("RETIRO", modo, ambiente, base, apiKey, secreto, timeout);
    }

    @Override
    public void instruir(UUID referencia, Dinero neto) {
        cliente.enviar(referencia, neto);
    }

    @Override
    public Optional<Confirmacion> consultar(UUID referencia) {
        return cliente.consultar(referencia)
                .map(r -> new Confirmacion(
                        r.referencia(), r.transaccionProveedor(), r.monto(), r.estado(), r.liquidadaEn()));
    }
}
