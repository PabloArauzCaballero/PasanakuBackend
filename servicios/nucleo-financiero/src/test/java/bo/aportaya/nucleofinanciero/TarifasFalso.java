package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Un tarifas de bolsillo: comision 4.00 + impuesto 1.00 por cotizacion, valida 10 minutos. */
final class TarifasFalso implements CotizadorDeComision {
    final Map<String, Cotizacion> guardadas = new HashMap<>();
    final Set<UUID> aceptadas = new HashSet<>();
    final AtomicReference<Instant> reloj;
    boolean caido;
    boolean gratuito;
    boolean rechazaLaAceptacion;

    TarifasFalso(AtomicReference<Instant> reloj) {
        this.reloj = reloj;
    }

    @Override
    public Optional<Dinero> costoDe(String hecho, UUID ref, Dinero monto, String clave) {
        return cotizar(hecho, ref, monto, clave).map(Cotizacion::total);
    }

    @Override
    public Optional<Cotizacion> cotizar(String hecho, UUID ref, Dinero monto, String clave) {
        if (caido) {
            return Optional.empty();
        }
        if (gratuito) {
            var cero = Dinero.cero(Moneda.BOB);
            return Optional.of(new Cotizacion(Optional.empty(), monto, cero, cero, cero, Optional.empty(), true));
        }
        return Optional.of(guardadas.computeIfAbsent(
                hecho + "|" + ref + "|" + clave,
                k -> new Cotizacion(
                        Optional.of(UUID.randomUUID()),
                        monto,
                        Dinero.de("4.00", Moneda.BOB),
                        Dinero.de("1.00", Moneda.BOB),
                        Dinero.de("5.00", Moneda.BOB),
                        Optional.of(reloj.get().plus(Duration.ofMinutes(10)).atOffset(ZoneOffset.UTC)),
                        false)));
    }

    @Override
    public boolean aceptar(UUID id) {
        if (rechazaLaAceptacion) {
            return false;
        }
        aceptadas.add(id);
        return true;
    }
}
