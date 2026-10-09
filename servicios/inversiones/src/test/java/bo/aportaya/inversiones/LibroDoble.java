package bo.aportaya.inversiones;

import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOBLE de {@code nucleo-financiero}, declarado como tal: cumple el contrato que pide el
 * puerto {@link LibroDelTitular} y lo ejercita en tres niveles.
 *
 * <ul>
 *   <li><b>correcto</b>: responde y mueve;
 *   <li><b>limite</b>: saldo justo, retencion exacta, repeticion de la misma clave;
 *   <li><b>invalido</b>: saldo insuficiente (definitivo) y libro caido (no se sabe).
 * </ul>
 *
 * <p>Lleva un mayor de dos patas (debe = haber) para poder pegar los asientos de una
 * operacion con su suma. Es el mayor DEL DOBLE: no prueba el de {@code nucleo-financiero}.
 */
public class LibroDoble implements LibroDelTitular {

    public record Asiento(String concepto, String cuentaDebe, String cuentaHaber, BigDecimal monto) {}

    private record Retencion(UUID cuenta, BigDecimal monto, boolean abierta) {}

    public static final String CUSTODIA = "custodia-inversiones";

    private final Map<UUID, UUID> titulares = new ConcurrentHashMap<>();
    private final Map<UUID, BigDecimal> libro = new ConcurrentHashMap<>();
    private final Map<UUID, BigDecimal> enPozos = new ConcurrentHashMap<>();
    private final Map<UUID, Retencion> retenciones = new ConcurrentHashMap<>();
    private final Map<String, UUID> porClave = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> llamadasPorClave = new ConcurrentHashMap<>();
    private final List<Asiento> asientos = new CopyOnWriteArrayList<>();
    private volatile boolean caido;
    private final java.util.Set<String> fallan = ConcurrentHashMap.newKeySet();

    public void abrirCuenta(UUID cuenta, UUID titular, String saldo) {
        titulares.put(cuenta, titular);
        libro.put(cuenta, new BigDecimal(saldo));
    }

    public void comprometerEnPozos(UUID cuenta, String monto) {
        enPozos.put(cuenta, new BigDecimal(monto));
    }

    public void caido(boolean valor) {
        this.caido = valor;
    }

    /** Nivel «invalido» por operacion: solo esa operacion falla («no se sabe»), el resto responde. */
    public void fallarEn(String operacion, boolean falla) {
        if (falla) {
            fallan.add(operacion);
        } else {
            fallan.remove(operacion);
        }
    }

    public List<Asiento> asientos() {
        return List.copyOf(asientos);
    }

    public int veces(String clave) {
        return llamadasPorClave.getOrDefault(clave, new AtomicInteger()).get();
    }

    /** Saldo derivado del mayor, nunca de un campo: lo que hay menos lo retenido abierto. */
    public BigDecimal total(UUID cuenta) {
        BigDecimal total = libro.get(cuenta);
        if (total == null) {
            throw new LibroNoDisponible("cuenta desconocida para este libro (doble)");
        }
        return total;
    }

    public BigDecimal retenido(UUID cuenta) {
        return retenciones.values().stream()
                .filter(r -> r.abierta() && r.cuenta().equals(cuenta))
                .map(Retencion::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal disponible(UUID cuenta) {
        return total(cuenta).subtract(retenido(cuenta));
    }

    public BigDecimal sumaDeDebe() {
        return asientos.stream().map(Asiento::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public synchronized SaldoDelTitular saldo(UUID cuenta) {
        exigirDisponible();
        Dinero libre = bob(disponible(cuenta));
        return new SaldoDelTitular(
                titulares.get(cuenta),
                libre,
                bob(retenido(cuenta)),
                bob(enPozos.getOrDefault(cuenta, BigDecimal.ZERO)));
    }

    @Override
    public synchronized UUID retener(UUID cuenta, Dinero monto, String clave, UUID referencia) {
        contar(clave);
        exigirDisponible();
        exigir("retener");
        if (porClave.containsKey(clave)) {
            return porClave.get(clave); // misma clave: devuelve lo que ya hizo
        }
        if (monto.monto().compareTo(disponible(cuenta)) > 0) {
            throw new SaldoInsuficiente();
        }
        UUID id = UUID.randomUUID();
        retenciones.put(id, new Retencion(cuenta, monto.monto(), true));
        porClave.put(clave, id);
        return id;
    }

    @Override
    public synchronized void liberar(UUID retencionId, String clave) {
        contar(clave);
        exigirDisponible();
        exigir("liberar");
        Retencion r = retenciones.get(retencionId);
        if (r != null && r.abierta()) {
            retenciones.put(retencionId, new Retencion(r.cuenta(), r.monto(), false));
        }
    }

    @Override
    public synchronized UUID debitarInversion(UUID retencionId, Dinero monto, String clave) {
        contar(clave);
        exigirDisponible();
        exigir("debitar");
        if (porClave.containsKey(clave)) {
            return porClave.get(clave);
        }
        Retencion r = retenciones.get(retencionId);
        if (r == null || !r.abierta() || r.monto().compareTo(monto.monto()) != 0) {
            throw new LibroNoDisponible("La retencion no esta abierta o no coincide");
        }
        retenciones.put(retencionId, new Retencion(r.cuenta(), r.monto(), false));
        libro.merge(r.cuenta(), monto.monto().negate(), BigDecimal::add);
        asientos.add(new Asiento("DEBITO_INVERSION", CUSTODIA, "billetera:" + r.cuenta(), monto.monto()));
        UUID tx = UUID.randomUUID();
        porClave.put(clave, tx);
        return tx;
    }

    @Override
    public synchronized UUID acreditarRescate(UUID cuenta, Dinero monto, String clave, UUID referencia) {
        contar(clave);
        exigirDisponible();
        exigir("acreditar");
        if (porClave.containsKey(clave)) {
            return porClave.get(clave);
        }
        libro.merge(cuenta, monto.monto(), BigDecimal::add);
        asientos.add(new Asiento("CREDITO_RESCATE", "billetera:" + cuenta, CUSTODIA, monto.monto()));
        UUID tx = UUID.randomUUID();
        porClave.put(clave, tx);
        return tx;
    }

    private void contar(String clave) {
        llamadasPorClave.computeIfAbsent(clave, k -> new AtomicInteger()).incrementAndGet();
    }

    private void exigir(String operacion) {
        if (fallan.contains(operacion)) {
            throw new LibroNoDisponible(operacion + " no responde (doble)");
        }
    }

    private void exigirDisponible() {
        if (caido) {
            throw new LibroNoDisponible("libro caido (doble)");
        }
    }

    private static Dinero bob(BigDecimal x) {
        return Dinero.de(x, Moneda.BOB);
    }

    /** Las claves aplicadas, para afirmar «una sola vez». */
    public List<String> clavesAplicadas() {
        return new ArrayList<>(new HashMap<>(porClave).keySet());
    }
}
