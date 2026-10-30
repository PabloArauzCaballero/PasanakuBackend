package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.plataforma.dominio.Dinero;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador del contrato ficticio local. Sin configuracion no confirma ningun ingreso.
 *
 * <p>Es el bean salvo que {@code aportaya.recarga.qr.modo=prueba}, que lo reemplaza por
 * {@link RecargasProveedorLocal}.
 */
@Component
@ConditionalOnProperty(name = "aportaya.recarga.qr.modo", havingValue = "proveedor", matchIfMissing = true)
public class RecargasProveedorSimulado implements ProveedorDeRecargas {

    private final ClienteProveedorSimulado cliente;

    public RecargasProveedorSimulado(
            @Value("${aportaya.proveedor.modo:deshabilitado}") String modo,
            @Value("${aportaya.ambiente:desarrollo}") String ambiente,
            @Value("${aportaya.proveedor.url:http://127.0.0.1:4020}") URI base,
            @Value("${aportaya.proveedor.api-key:}") String apiKey,
            @Value("${aportaya.proveedor.firma-secreto:}") String secreto,
            @Value("${aportaya.proveedor.timeout:PT5S}") Duration timeout) {
        this.cliente = new ClienteProveedorSimulado("RECARGA", modo, ambiente, base, apiKey, secreto, timeout);
    }

    @Override
    public Optional<CobroQr> solicitar(UUID referencia, Dinero monto) {
        cliente.enviar(referencia, monto);
        // El contrato ficticio local no emite QR: confirma o rechaza por referencia, nada mas.
        return Optional.empty();
    }

    @Override
    public Confirmacion consultar(UUID referencia) {
        var recibo = cliente.consultar(referencia).orElseThrow(ClienteProveedorSimulado::noConfirmado);
        return new Confirmacion(
                recibo.referencia(),
                recibo.transaccionProveedor(),
                recibo.monto(),
                recibo.estado(),
                recibo.liquidadaEn());
    }
}
