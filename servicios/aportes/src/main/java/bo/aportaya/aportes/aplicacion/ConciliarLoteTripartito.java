package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.ConciliacionTripartita;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.Causa;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.Hallazgo;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.LineaDelProveedor;
import bo.aportaya.aportes.infraestructura.ConciliacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * H13.S1.M1 · Conciliar un lote: libro (pagos acreditados), proveedor (su reporte) y banco (extracto).
 *
 * <p>Persiste SOLO lo que el modelo soporta: por cada pago del libro, una conciliacion
 * (conciliado o en excepcion) y, si hay diferencia, una excepcion ABIERTA con causa, responsable y
 * lo que corresponde hacer. <b>No acredita, no descuenta, no crea pagos.</b> Los huerfanos que no
 * tienen pago en el libro (movimientos del banco o lineas del proveedor sin pago) se DEVUELVEN en
 * el informe pero no se persisten: falta la entidad para guardarlos (declarado, ver el reporte del
 * carril C). El reporte del proveedor entra como dato: no hay todavia un importador.
 *
 * <p>Repetir el lote no duplica nada: una conciliacion por pago.
 */
@Service
public class ConciliarLoteTripartito {

    private final Datos datos;
    private final ConciliacionRepositorio repositorio;
    private final Outbox outbox;
    private final Reloj reloj;

    public ConciliarLoteTripartito(Datos datos, ConciliacionRepositorio repositorio, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.repositorio = repositorio;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record Informe(
            int conciliacionesNuevas,
            int excepcionesNuevas,
            int yaProcesadas,
            List<Hallazgo> hallazgos,
            int movimientosHuerfanos,
            int confirmacionesHuerfanas) {}

    @Transactional
    public Informe ejecutar(
            UUID extractoId, LocalDate desde, LocalDate hasta, List<LineaDelProveedor> reporte, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var libro = repositorio.pagosAcreditados(dsl, desde, hasta);
            var banco = repositorio.movimientosDelExtracto(dsl, extractoId);
            var resultado = ConciliacionTripartita.conciliar(libro, reporte, banco);

            int nuevas = 0;
            int excepciones = 0;
            int yaProcesadas = 0;
            for (var c : resultado.conciliados()) {
                var nueva = repositorio.registrar(
                        dsl, c.pagoId(), c.movimientoId(), "CONCILIADO_AUTOMATICO", BigDecimal.ZERO, ahora);
                if (nueva.isPresent()) {
                    nuevas++;
                    repositorio.marcarConciliado(dsl, c.movimientoId());
                } else {
                    yaProcesadas++;
                }
            }
            for (Hallazgo h : resultado.hallazgos()) {
                String tipo = tipoDeExcepcion(h.causa());
                if (tipo == null) {
                    continue; // huerfano sin pago: se informa, no se persiste
                }
                // Con varios pagos (duplicado) no se enlaza el movimiento: uno solo puede apuntarlo.
                UUID movimiento = h.pagos().size() == 1 ? h.movimientoId() : null;
                for (UUID pagoId : h.pagos()) {
                    var nueva = repositorio.registrar(
                            dsl,
                            pagoId,
                            movimiento,
                            "EN_EXCEPCION",
                            h.diferencia().monto(),
                            ahora);
                    if (nueva.isPresent()) {
                        nuevas++;
                        excepciones++;
                        repositorio.abrirExcepcion(
                                dsl,
                                nueva.get(),
                                tipo,
                                h.causa() + " · responsable " + h.responsable() + " · " + h.que(),
                                h.diferencia().monto());
                    } else {
                        yaProcesadas++;
                    }
                }
            }
            int movHuerfanos = (int) resultado.hallazgos().stream()
                    .filter(h -> h.causa() == Causa.MOVIMIENTO_SIN_PAGO)
                    .count();
            int confHuerfanas = (int) resultado.hallazgos().stream()
                    .filter(h -> h.causa() == Causa.CONFIRMACION_SIN_PAGO)
                    .count();

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "aportes.conciliacion_ejecutada",
                            "extracto_bancario",
                            extractoId,
                            Map.of(
                                    "conciliacionesNuevas", nuevas,
                                    "excepcionesNuevas", excepciones,
                                    "hallazgos", resultado.hallazgos().size()),
                            UUID.fromString(ctx.traza().id())));
            return new Informe(nuevas, excepciones, yaProcesadas, resultado.hallazgos(), movHuerfanos, confHuerfanas);
        });
    }

    private static String tipoDeExcepcion(Causa causa) {
        return switch (causa) {
            case PAGO_DUPLICADO -> "PAGO_DUPLICADO";
            case MONEDA_DISTINTA -> "MONEDA_DISTINTA";
            case MONTO_DISTINTO -> "MONTO_DISTINTO";
            case SIN_CONFIRMACION_DEL_PROVEEDOR -> "SIN_CONFIRMACION_PROVEEDOR";
            case SIN_MOVIMIENTO_BANCARIO -> "SIN_MOVIMIENTO_BANCARIO";
            case MOVIMIENTO_SIN_PAGO, CONFIRMACION_SIN_PAGO -> null;
        };
    }
}
