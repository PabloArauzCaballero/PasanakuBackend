package bo.aportaya.aportes.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conciliar tres fuentes por la referencia del proveedor: el libro (lo que este servicio acredito),
 * el proveedor (lo que dice haber cobrado) y el banco (lo que de verdad entro).
 *
 * <p>Regla de oro: <b>una diferencia se EXPLICA, no se tapa</b>. Este calculo nunca decide
 * acreditar ni descontar nada: devuelve cada hallazgo con su causa, su responsable y lo que
 * corresponde hacer, y la sugerencia dice «investigar», jamas «acreditar» (regla 91.3: un pago
 * no es un pago hasta que el proveedor lo confirma). Un movimiento que entro al banco sin pago
 * asociado es un movimiento huerfano, no un abono pendiente de fabricar.
 *
 * <p>Conservacion: cada REFERENCIA de entrada aparece EXACTAMENTE una vez en el resultado, ya sea
 * conciliada o en un hallazgo. Si esa cuenta no cierra, el calculo esta mal.
 */
public final class ConciliacionTripartita {

    private ConciliacionTripartita() {}

    public record PagoDelLibro(UUID pagoId, String referencia, Dinero monto) {}

    public record LineaDelProveedor(String referencia, Dinero monto) {}

    public record MovimientoDelBanco(UUID movimientoId, String referencia, Dinero monto) {}

    public enum Causa {
        PAGO_DUPLICADO,
        MONEDA_DISTINTA,
        MONTO_DISTINTO,
        SIN_CONFIRMACION_DEL_PROVEEDOR,
        SIN_MOVIMIENTO_BANCARIO,
        MOVIMIENTO_SIN_PAGO,
        CONFIRMACION_SIN_PAGO
    }

    /** Quien tiene que mirar el caso. Es un rol, no una persona: la asignacion nominal la hace operaciones. */
    public enum Responsable {
        OPERACIONES_DE_PAGOS,
        PROVEEDOR_DE_PAGOS,
        BANCO,
        CONTABILIDAD
    }

    public record Hallazgo(
            Causa causa,
            Responsable responsable,
            String referencia,
            List<UUID> pagos,
            UUID movimientoId,
            Dinero diferencia,
            String que) {}

    public record Conciliado(String referencia, UUID pagoId, UUID movimientoId) {}

    public record Resultado(List<Conciliado> conciliados, List<Hallazgo> hallazgos) {}

    public static Resultado conciliar(
            List<PagoDelLibro> libro, List<LineaDelProveedor> proveedor, List<MovimientoDelBanco> banco) {
        Map<String, List<PagoDelLibro>> l = agrupar(libro, PagoDelLibro::referencia);
        Map<String, List<LineaDelProveedor>> p = agrupar(proveedor, LineaDelProveedor::referencia);
        Map<String, List<MovimientoDelBanco>> b = agrupar(banco, MovimientoDelBanco::referencia);

        Set<String> referencias = new LinkedHashSet<>();
        referencias.addAll(l.keySet());
        referencias.addAll(p.keySet());
        referencias.addAll(b.keySet());

        List<Conciliado> conciliados = new ArrayList<>();
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (String ref : referencias.stream().sorted().toList()) {
            var ls = l.getOrDefault(ref, List.of());
            var ps = p.getOrDefault(ref, List.of());
            var bs = b.getOrDefault(ref, List.of());
            clasificar(ref, ls, ps, bs, conciliados, hallazgos);
        }
        return new Resultado(List.copyOf(conciliados), List.copyOf(hallazgos));
    }

    private static void clasificar(
            String ref,
            List<PagoDelLibro> ls,
            List<LineaDelProveedor> ps,
            List<MovimientoDelBanco> bs,
            List<Conciliado> conciliados,
            List<Hallazgo> hallazgos) {
        if (ls.isEmpty()) {
            if (!bs.isEmpty()) {
                var m = bs.get(0);
                hallazgos.add(
                        new Hallazgo(
                                Causa.MOVIMIENTO_SIN_PAGO,
                                Responsable.OPERACIONES_DE_PAGOS,
                                ref,
                                List.of(),
                                m.movimientoId(),
                                m.monto(),
                                "Entro plata al banco sin pago en el libro. Investigar de quien es; NO acreditar sin confirmacion del proveedor."));
            } else {
                hallazgos.add(
                        new Hallazgo(
                                Causa.CONFIRMACION_SIN_PAGO,
                                Responsable.PROVEEDOR_DE_PAGOS,
                                ref,
                                List.of(),
                                null,
                                ps.get(0).monto(),
                                "El proveedor dice haber cobrado y no hay pago en el libro (webhook perdido?). Consultar su estado; NO acreditar a mano."));
            }
            return;
        }
        var primero = ls.get(0);
        List<UUID> ids = ls.stream().map(PagoDelLibro::pagoId).toList();
        if (ls.size() > 1) {
            hallazgos.add(
                    new Hallazgo(
                            Causa.PAGO_DUPLICADO,
                            Responsable.OPERACIONES_DE_PAGOS,
                            ref,
                            ids,
                            mov(bs),
                            primero.monto(),
                            "La misma referencia esta acreditada " + ls.size()
                                    + " veces. Revisar la idempotencia del cobro; reembolsar solo lo que el proveedor no cobro."));
            return;
        }
        if (ps.size() > 1 || bs.size() > 1) {
            hallazgos.add(new Hallazgo(
                    Causa.PAGO_DUPLICADO,
                    Responsable.PROVEEDOR_DE_PAGOS,
                    ref,
                    ids,
                    mov(bs),
                    primero.monto(),
                    "La referencia aparece " + ps.size() + " veces en el proveedor y " + bs.size()
                            + " en el banco: no se puede conciliar uno a uno. Pedir el detalle al proveedor."));
            return;
        }
        if (ps.isEmpty()) {
            hallazgos.add(
                    new Hallazgo(
                            Causa.SIN_CONFIRMACION_DEL_PROVEEDOR,
                            Responsable.OPERACIONES_DE_PAGOS,
                            ref,
                            ids,
                            mov(bs),
                            primero.monto(),
                            "Acreditado en el libro sin confirmacion del proveedor. Es el caso mas grave: verificar de inmediato."));
            return;
        }
        var prov = ps.get(0);
        if (prov.monto().moneda() != primero.monto().moneda()) {
            hallazgos.add(
                    new Hallazgo(
                            Causa.MONEDA_DISTINTA,
                            Responsable.CONTABILIDAD,
                            ref,
                            ids,
                            mov(bs),
                            primero.monto(),
                            "El proveedor informa otra moneda que la acreditada. Sin tipo de cambio registrado no se compara."));
            return;
        }
        if (!prov.monto().equals(primero.monto())) {
            hallazgos.add(new Hallazgo(
                    Causa.MONTO_DISTINTO,
                    Responsable.PROVEEDOR_DE_PAGOS,
                    ref,
                    ids,
                    mov(bs),
                    primero.monto().menos(prov.monto()),
                    "El libro y el proveedor no coinciden en el importe."));
            return;
        }
        if (bs.isEmpty()) {
            hallazgos.add(
                    new Hallazgo(
                            Causa.SIN_MOVIMIENTO_BANCARIO,
                            Responsable.BANCO,
                            ref,
                            ids,
                            null,
                            primero.monto(),
                            "Confirmado por el proveedor pero todavia no entro al banco. Esperar la liquidacion; si vence el plazo, reclamar al proveedor."));
            return;
        }
        var banco = bs.get(0);
        if (banco.monto().moneda() != primero.monto().moneda() || !banco.monto().equals(primero.monto())) {
            hallazgos.add(
                    new Hallazgo(
                            banco.monto().moneda() != primero.monto().moneda()
                                    ? Causa.MONEDA_DISTINTA
                                    : Causa.MONTO_DISTINTO,
                            Responsable.BANCO,
                            ref,
                            ids,
                            banco.movimientoId(),
                            banco.monto().moneda() != primero.monto().moneda()
                                    ? primero.monto()
                                    : primero.monto().menos(banco.monto()),
                            "Lo que entro al banco no es lo que el libro y el proveedor dicen. Conciliar contra el extracto original."));
            return;
        }
        conciliados.add(new Conciliado(ref, primero.pagoId(), banco.movimientoId()));
    }

    private static UUID mov(List<MovimientoDelBanco> bs) {
        return bs.isEmpty() ? null : bs.get(0).movimientoId();
    }

    private static <T> Map<String, List<T>> agrupar(List<T> elementos, java.util.function.Function<T, String> clave) {
        Map<String, List<T>> m = new LinkedHashMap<>();
        for (T e : elementos) {
            m.computeIfAbsent(clave.apply(e), k -> new ArrayList<>()).add(e);
        }
        return m;
    }
}
