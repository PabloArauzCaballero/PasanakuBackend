package bo.aportaya.inversiones;

import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOBLE del aliado, declarado como tal, con los tres niveles del contrato del puerto:
 *
 * <ul>
 *   <li><b>correcto</b>: confirma y devuelve lo que se espera de una confirmacion;
 *   <li><b>limite</b>: pendiente, misma referencia dos veces, confirmacion con importe justo;
 *   <li><b>invalido</b>: rechazo definitivo, aliado caido, y la respuesta PERDIDA (la operacion
 *       queda hecha del lado del aliado y a nosotros nos llega un corte).
 * </ul>
 *
 * <p>Todos los productos y numeros son SINTETICOS de prueba. Los importes de un DPF
 * (interes, retencion) se cargan a mano desde la prueba, calculados a mano: el doble NO
 * usa la formula del servicio, justamente para que el servicio no se verifique contra si mismo.
 */
public class AliadoDoble implements AliadoDeInversion {

    public enum Modo {
        NORMAL,
        PENDIENTE,
        RECHAZA,
        CAIDO,
        PERDIDA
    }

    private record Posicion(
            String producto, TipoProducto tipo, BigDecimal principal, BigDecimal cuotas, LocalDate constitucion) {}

    private final Map<UUID, OperacionDelAliado> operaciones = new ConcurrentHashMap<>();
    private final Map<String, Posicion> posiciones = new ConcurrentHashMap<>();
    private final Map<String, ProductoDelAliado> productos = new ConcurrentHashMap<>();
    private final Map<String, ValorDeCuotaDelAliado> valores = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> envios = new ConcurrentHashMap<>();
    private volatile Modo modo = Modo.NORMAL;
    private volatile LocalDate hoy = LocalDate.of(2026, 10, 12);
    private volatile BigDecimal valorCuotaAplicado = new BigDecimal("100.000000");
    private volatile String interesDpf = "0.00";
    private volatile String retencionDpf = "0.00";
    private volatile int diasDevengados = 0;
    private volatile boolean rescateFondoInmediato = false;
    private volatile BigDecimal distorsionDeMonto = BigDecimal.ZERO;
    private volatile Optional<LocalDate> fechaValorDeRescate = Optional.empty();

    public AliadoDoble() {
        ProductosDePrueba.todos().forEach(p -> productos.put(p.codigo(), p));
    }
    // ------------------------------------------------------------------ control de la prueba
    public AliadoDoble modo(Modo m) {
        this.modo = m;
        return this;
    }

    public AliadoDoble hoy(LocalDate fecha) {
        this.hoy = fecha;
        return this;
    }

    public AliadoDoble valorCuotaAplicado(String valor) {
        this.valorCuotaAplicado = new BigDecimal(valor);
        return this;
    }

    public AliadoDoble liquidacionDpf(String interes, String retencion, int dias) {
        this.interesDpf = interes;
        this.retencionDpf = retencion;
        this.diasDevengados = dias;
        return this;
    }

    public AliadoDoble rescateFondoInmediato(boolean si) {
        this.rescateFondoInmediato = si;
        return this;
    }

    /** Nivel «invalido»: el aliado confirma un importe distinto del pedido. */
    public AliadoDoble distorsionDeMonto(String diferencia) {
        this.distorsionDeMonto = new BigDecimal(diferencia);
        return this;
    }

    /** Si el aliado informa su propia fecha de valor para un rescate (por omision no informa ninguna). */
    public AliadoDoble fechaValorDeRescate(LocalDate fecha) {
        this.fechaValorDeRescate = Optional.of(fecha);
        return this;
    }

    public AliadoDoble publicar(String producto, LocalDate fecha, String valor) {
        valores.put(
                producto,
                new ValorDeCuotaDelAliado(
                        producto, fecha, new BigDecimal(valor), Instant.parse("2026-10-12T12:00:00Z")));
        return this;
    }

    public AliadoDoble agregarProducto(ProductoDelAliado p) {
        productos.put(p.codigo(), p);
        return this;
    }

    public ProductoDelAliado producto(String codigo) {
        return productos.get(codigo);
    }

    public int envios(UUID referencia) {
        return envios.getOrDefault(referencia, new AtomicInteger()).get();
    }

    public Optional<OperacionDelAliado> operacion(UUID ref) {
        return Optional.ofNullable(operaciones.get(ref));
    }

    /** El aliado confirma lo que tenia pendiente. */
    public synchronized void confirmarPendientes() {
        operaciones.replaceAll((ref, op) -> op.estado() == Estado.PENDIENTE ? confirmada(op) : op);
    }

    // ------------------------------------------------------------------ contrato
    @Override
    public List<ProductoDelAliado> catalogo() {
        caidoONo();
        return List.copyOf(productos.values());
    }

    @Override
    public Optional<ValorDeCuotaDelAliado> valorDeCuota(String codigo) {
        caidoONo();
        return Optional.ofNullable(valores.get(codigo));
    }

    @Override
    public synchronized OperacionDelAliado suscribir(UUID ref, String codigo, BigDecimal monto, UUID titular) {
        contar(ref);
        caidoONo();
        if (operaciones.containsKey(ref)) {
            return operaciones.get(ref);
        }
        if (modo == Modo.RECHAZA) {
            throw new AliadoRechazo("RECHAZO_DE_PRUEBA");
        }
        ProductoDelAliado p = productos.get(codigo);
        String posicion = "POS-" + ref;
        Map<String, String> detalle = new HashMap<>();
        detalle.put("constitucion", hoy.toString());
        BigDecimal cuotas = null;
        if (p.tipo() == TipoProducto.DPF) {
            detalle.put("vencimiento", hoy.plusDays(p.plazoDias().orElseThrow()).toString());
        } else {
            cuotas = monto.divide(valorCuotaAplicado, 6, RoundingMode.DOWN);
            detalle.put("valorCuota", valorCuotaAplicado.toPlainString());
        }
        posiciones.put(posicion, new Posicion(codigo, p.tipo(), monto, cuotas, hoy));
        var op = new OperacionDelAliado(
                ref,
                "SUSCRIPCION",
                modo == Modo.PENDIENTE ? Estado.PENDIENTE : Estado.CONFIRMADO,
                Optional.of(monto.add(distorsionDeMonto)),
                Optional.ofNullable(cuotas),
                Optional.of(posicion),
                "TX-" + ref,
                Optional.of(hoy),
                Optional.empty(),
                detalle);
        operaciones.put(ref, op);
        perdidaONo();
        return op;
    }

    @Override
    public synchronized OperacionDelAliado rescatar(UUID ref, String posicionExterna, Optional<BigDecimal> cuotas) {
        contar(ref);
        caidoONo();
        if (operaciones.containsKey(ref)) {
            return operaciones.get(ref);
        }
        if (modo == Modo.RECHAZA) {
            throw new AliadoRechazo("RECHAZO_DE_PRUEBA");
        }
        Posicion pos = posiciones.get(posicionExterna);
        Map<String, String> detalle = new HashMap<>();
        BigDecimal monto;
        Optional<BigDecimal> cuotasOp = cuotas;
        Estado estado;
        if (pos.tipo() == TipoProducto.DPF) {
            BigDecimal bruto = new BigDecimal(interesDpf);
            BigDecimal ret = new BigDecimal(retencionDpf);
            monto = pos.principal().add(bruto).subtract(ret);
            detalle.put("interesBruto", interesDpf);
            detalle.put("retencion", retencionDpf);
            detalle.put("diasDevengados", Integer.toString(diasDevengados));
            estado = modo == Modo.PENDIENTE ? Estado.PENDIENTE : Estado.CONFIRMADO;
        } else {
            monto = cuotas.orElseThrow().multiply(valorCuotaAplicado).setScale(2, RoundingMode.HALF_EVEN);
            detalle.put("valorCuota", valorCuotaAplicado.toPlainString());
            estado = rescateFondoInmediato && modo != Modo.PENDIENTE ? Estado.CONFIRMADO : Estado.PENDIENTE;
        }
        var op = new OperacionDelAliado(
                ref,
                "RESCATE",
                estado,
                Optional.of(monto.add(distorsionDeMonto)),
                cuotasOp,
                Optional.of(posicionExterna),
                "TX-" + ref,
                fechaValorDeRescate,
                Optional.of(Instant.parse("2026-10-14T13:00:00Z")),
                detalle);
        operaciones.put(ref, op);
        perdidaONo();
        return op;
    }

    @Override
    public Optional<OperacionDelAliado> consultar(UUID ref) {
        caidoONo();
        if (modo == Modo.PERDIDA) {
            throw new AliadoNoDisponible("el enlace sigue cortado (doble)");
        }
        return Optional.ofNullable(operaciones.get(ref));
    }

    // ------------------------------------------------------------------ mecanica
    private OperacionDelAliado confirmada(OperacionDelAliado op) {
        return new OperacionDelAliado(
                op.referencia(),
                op.tipo(),
                Estado.CONFIRMADO,
                op.monto(),
                op.cuotas(),
                op.posicionExterna(),
                op.transaccion(),
                op.fechaValor(),
                op.liquidaEn(),
                op.detalle());
    }

    private void contar(UUID ref) {
        envios.computeIfAbsent(ref, k -> new AtomicInteger()).incrementAndGet();
    }

    private void caidoONo() {
        if (modo == Modo.CAIDO) {
            throw new AliadoNoDisponible("aliado caido (doble)");
        }
    }

    /** La operacion quedo hecha en el aliado, pero la respuesta no llego. */
    private void perdidaONo() {
        if (modo == Modo.PERDIDA) {
            throw new AliadoNoDisponible("respuesta perdida (doble)");
        }
    }
}
