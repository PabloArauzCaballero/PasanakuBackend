package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.nucleofinanciero.dominio.puertos.SimuladorDePagoDeRecarga;
import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El doble de {@code local}/{@code test} de {@link ProveedorDeRecargas}: emite un QR dinamico <b>de
 * prueba</b> mientras no exista el adaptador del banco real.
 *
 * <p>El QR de prueba no se puede pagar desde ningun banco: su contenido empieza con
 * {@value #PREFIJO} y viaja con origen {@code PRUEBA}, que la app muestra como sello. El pago lo
 * simula {@link #marcarPagado}, y desde ahi la acreditacion recorre el camino real (misma
 * confirmacion, mismo asiento, misma idempotencia por orden).
 *
 * <p>Mismo patron que {@link ProveedorDeRetiroLocal}: {@code @Profile({"local","test"})}, estado en
 * memoria. Un reinicio olvida las referencias: la orden queda pendiente ({@link #consultar} contesta
 * «no confirmado», como el adaptador real ante un 404) y {@link #marcarPagado} la vuelve a registrar.
 * Se activa con {@code aportaya.recarga.qr.modo=prueba}; con cualquier otro valor el bean es
 * {@link RecargasProveedorSimulado}.
 */
@Component
@Profile({"local", "test"})
@ConditionalOnProperty(name = "aportaya.recarga.qr.modo", havingValue = "prueba")
public class RecargasProveedorLocal implements ProveedorDeRecargas, SimuladorDePagoDeRecarga {

    static final String PREFIJO = "PASANAKU-PRUEBA";

    private final ConcurrentHashMap<UUID, Confirmacion> operaciones = new ConcurrentHashMap<>();

    @Override
    public Optional<CobroQr> solicitar(UUID referencia, Dinero monto) {
        Objects.requireNonNull(referencia, "referencia");
        exigirPositivo(monto);
        operaciones.putIfAbsent(referencia, new Confirmacion(referencia, null, monto, "PENDIENTE", null));
        return Optional.of(
                new CobroQr(contenido(referencia, operaciones.get(referencia).monto()), "PRUEBA"));
    }

    @Override
    public Confirmacion consultar(UUID referencia) {
        var operacion = operaciones.get(referencia);
        if (operacion == null) {
            throw ClienteProveedorSimulado.noConfirmado();
        }
        return operacion;
    }

    /** Un segundo pago de la misma referencia devuelve el MISMO recibo: no hay dos transacciones. */
    @Override
    public void marcarPagado(UUID referencia, Dinero monto) {
        Objects.requireNonNull(referencia, "referencia");
        exigirPositivo(monto);
        operaciones.compute(
                referencia,
                (ref, actual) -> actual != null && "CONFIRMADO".equals(actual.estado())
                        ? actual
                        : new Confirmacion(
                                ref, UUID.randomUUID(), monto, "CONFIRMADO", OffsetDateTime.now(ZoneOffset.UTC)));
    }

    /** Texto plano, sin datos de la persona: la referencia de la orden y el importe que cobra. */
    static String contenido(UUID referencia, Dinero monto) {
        return PREFIJO + "|NO-PAGABLE|ref=" + referencia + "|monto="
                + monto.monto().toPlainString() + "|" + monto.moneda().name();
    }

    private static void exigirPositivo(Dinero monto) {
        if (monto == null || monto.monto().signum() <= 0) {
            throw new IllegalArgumentException("El monto de una recarga tiene que ser positivo.");
        }
    }
}
