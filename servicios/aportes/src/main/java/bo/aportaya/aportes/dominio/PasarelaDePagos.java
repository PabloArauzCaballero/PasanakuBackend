package bo.aportaya.aportes.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;

/**
 * El puerto de salida hacia la pasarela de pagos: lo que aportes le PIDE, con tipos propios y nada del proveedor.
 *
 * <p>Hoy lo cumple un doble ({@code infraestructura.pasarela.simulada}); el adaptador real vive en
 * {@code infraestructura.pasarela.real} y implementa esta misma interfaz sin que dominio ni aplicacion cambien.
 * El otro sentido (lo que la pasarela AVISA) son {@link InterpreteDeWebhook}, {@link VerificadorDeFirmaWebhook} y
 * {@link ProveedorDeSecretoWebhook}.
 *
 * <p>Todo importe viaja como {@link Dinero} (decimal exacto + moneda, regla 91.1). Un pago no es un pago hasta que
 * llega por webhook verificado (regla 91.3.1): {@link #consultarEstado} sirve para conciliar, no para acreditar.
 */
public interface PasarelaDePagos {

    /**
     * Abre una orden de cobro. Idempotente por {@link SolicitudDeCobro#claveIdempotencia()}: repetir la misma
     * solicitud devuelve la misma orden; la misma clave con otro monto es un error, no una segunda orden.
     *
     * @throws IllegalArgumentException si la solicitud es invalida (monto nulo o no positivo, clave vacia)
     * @throws IllegalStateException si la clave ya identifica una orden con otro monto
     */
    OrdenDeCobro crearOrdenDeCobro(SolicitudDeCobro solicitud);

    /** El estado que la pasarela dice tener de la orden; vacio si no la conoce. */
    Optional<CobroConsultado> consultarEstado(String referenciaProveedor);

    /** {@code claveIdempotencia} identifica la intencion de cobrar (no el usuario ni la cuenta: nada de PII). */
    record SolicitudDeCobro(String claveIdempotencia, Dinero monto) {

        public SolicitudDeCobro {
            if (claveIdempotencia == null || claveIdempotencia.isBlank()) {
                throw new IllegalArgumentException("La orden de cobro exige clave de idempotencia.");
            }
            if (monto == null || monto.esCero() || monto.esNegativo()) {
                throw new IllegalArgumentException("El monto de una orden de cobro debe ser positivo.");
            }
        }
    }

    /**
     * La orden abierta. {@code referenciaProveedor} es la que despues vuelve en el webhook y se guarda en
     * {@code pago.referencia_proveedor}; {@code contenidoQr} es lo que se muestra a quien paga; {@code esDoble} es
     * {@code true} cuando la orden la emitio un simulador y NO se puede pagar en ningun banco.
     */
    record OrdenDeCobro(
            String referenciaProveedor, Dinero monto, String contenidoQr, EstadoDeCobro estado, boolean esDoble) {}

    record CobroConsultado(String referenciaProveedor, Dinero monto, EstadoDeCobro estado) {}

    enum EstadoDeCobro {
        PENDIENTE,
        PAGADO
    }
}
