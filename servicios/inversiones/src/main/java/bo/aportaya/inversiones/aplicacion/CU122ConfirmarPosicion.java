package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.Descomposicion;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoRechazo;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.OperacionDelAliado;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio.Producto;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio.Orden;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
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
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-122 · Confirmar la posicion SIN duplicar el saldo.
 *
 * <p>Avanza una orden por la saga, un paso idempotente a la vez. El orden de los pasos es
 * el de menor dolor: el importe se RETIENE antes de mandar la orden y el DEBITO definitivo
 * se pide DESPUES de que el aliado confirmo. Si algo se corta en el medio, lo peor que
 * puede pasar es «retenido y sin posicion todavia» —visible, reintentable, y el importe
 * sigue sin estar disponible—; nunca «posicion sin debito», que seria plata duplicada.
 *
 * <p>Una orden ENVIADA o INCIERTA se resuelve CONSULTANDO al aliado por la referencia (el
 * id de la orden): si la conoce, se sigue de ahi; si no la conoce, nunca la recibio y se
 * reenvia con la misma clave. Jamas se libera el saldo mientras no se sepa que paso.
 */
@Service
public class CU122ConfirmarPosicion {

    private static final List<String> ABIERTAS = List.of("CREADA", "RETENIDA", "ENVIADA", "INCIERTA");
    private static final int PASOS_MAXIMOS = 6;

    private final Datos datos;
    private final Transaccionar tx;
    private final OrdenRepositorio ordenes;
    private final CatalogoRepositorio catalogo;
    private final PosicionRepositorio posiciones;
    private final ComprobanteRepositorio comprobantes;
    private final InstruccionRepositorio instrucciones;
    private final AplicadorDeInstrucciones aplicador;
    private final AliadoDeInversion aliado;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU122ConfirmarPosicion(
            Datos datos,
            Transaccionar tx,
            OrdenRepositorio ordenes,
            CatalogoRepositorio catalogo,
            PosicionRepositorio posiciones,
            ComprobanteRepositorio comprobantes,
            InstruccionRepositorio instrucciones,
            AplicadorDeInstrucciones aplicador,
            AliadoDeInversion aliado,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.ordenes = ordenes;
        this.catalogo = catalogo;
        this.posiciones = posiciones;
        this.comprobantes = comprobantes;
        this.instrucciones = instrucciones;
        this.aplicador = aplicador;
        this.aliado = aliado;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** La orden de OTRA persona no existe para esta (objeto propio, no solo rol). */
    public VistaOrden ver(UUID ordenId, ContextoSesion ctx) {
        return vista(propia(ordenId, ctx), ctx);
    }

    /** Consulta al aliado si hace falta y avanza lo que corresponda. */
    public VistaOrden sincronizar(UUID ordenId, ContextoSesion ctx) {
        propia(ordenId, ctx);
        return avanzar(ordenId, ctx);
    }

    VistaOrden avanzar(UUID ordenId, ContextoSesion ctx) {
        String anterior = null;
        for (int paso = 0; paso < PASOS_MAXIMOS; paso++) {
            Orden o = leer(ordenId, ctx);
            if (o.estado().equals(anterior) && !"CONFIRMADA".equals(o.estado())) {
                break; // no hubo progreso: queda pendiente a la vista, no se insiste en bucle
            }
            anterior = o.estado();
            switch (o.estado()) {
                case "CREADA" -> aplicarInstruccion(o, "RETENER", ctx);
                case "RETENIDA" -> enviar(o, ctx);
                case "ENVIADA", "INCIERTA" -> resolverConElAliado(o, ctx);
                case "CONFIRMADA" -> {
                    aplicarInstruccion(o, "DEBITAR", ctx);
                    return vista(leer(ordenId, ctx), ctx);
                }
                default -> {
                    aplicarInstruccion(o, "LIBERAR", ctx);
                    return vista(leer(ordenId, ctx), ctx);
                }
            }
        }
        return vista(leer(ordenId, ctx), ctx);
    }

    // ------------------------------------------------------------------ pasos
    private void aplicarInstruccion(Orden o, String tipo, ContextoSesion ctx) {
        Optional<InstruccionRepositorio.Instruccion> i =
                tx.en(() -> datos.conContexto(ctx, dsl -> instrucciones.deOrigen(dsl, o.id(), tipo)));
        i.filter(x -> "PENDIENTE".equals(x.estado())).ifPresent(x -> aplicador.aplicar(x.id(), ctx));
    }

    private void enviar(Orden o, ContextoSesion ctx) {
        boolean loHiceYo = tx.en(() ->
                datos.conContexto(ctx, dsl -> ordenes.pasar(dsl, o.id(), List.of("RETENIDA"), "ENVIADA", ahora())));
        if (!loHiceYo) {
            return; // otro actor ya la envio
        }
        pedirAlAliado(o, ctx);
    }

    private void pedirAlAliado(Orden o, ContextoSesion ctx) {
        Producto p = tx.en(() -> datos.conContexto(
                ctx, dsl -> catalogo.porId(dsl, o.productoId()).orElseThrow()));
        try {
            resolver(o, aliado.suscribir(o.id(), p.codigo(), o.monto(), o.usuarioId()), ctx);
        } catch (AliadoRechazo r) {
            rechazar(o, r.codigo(), ctx);
        } catch (AliadoNoDisponible noSeSabe) {
            tx.en(() ->
                    datos.conContexto(ctx, dsl -> ordenes.pasar(dsl, o.id(), List.of("ENVIADA"), "INCIERTA", ahora())));
        }
    }

    private void resolverConElAliado(Orden o, ContextoSesion ctx) {
        try {
            Optional<OperacionDelAliado> op = aliado.consultar(o.id());
            if (op.isPresent()) {
                resolver(o, op.get(), ctx);
            } else {
                // El aliado nunca la recibio: reenviar con la misma referencia es seguro.
                tx.en(() -> datos.conContexto(
                        ctx, dsl -> ordenes.pasar(dsl, o.id(), List.of("INCIERTA"), "ENVIADA", ahora())));
                pedirAlAliado(o, ctx);
            }
        } catch (AliadoNoDisponible noSeSabe) {
            tx.en(() ->
                    datos.conContexto(ctx, dsl -> ordenes.pasar(dsl, o.id(), List.of("ENVIADA"), "INCIERTA", ahora())));
        }
    }

    private void resolver(Orden o, OperacionDelAliado op, ContextoSesion ctx) {
        switch (op.estado()) {
            case CONFIRMADO -> confirmar(o, op, ctx);
            case RECHAZADO -> rechazar(o, "RECHAZADA_POR_EL_ALIADO", ctx);
            case PENDIENTE ->
                tx.en(() -> datos.conContexto(
                        ctx, dsl -> ordenes.pasar(dsl, o.id(), List.of("INCIERTA"), "ENVIADA", ahora())));
        }
    }

    private void confirmar(Orden o, OperacionDelAliado op, ContextoSesion ctx) {
        tx.en(() -> datos.conContexto(ctx, dsl -> {
            Producto p = catalogo.porId(dsl, o.productoId()).orElseThrow();
            Condiciones c = catalogo.version(dsl, o.versionId()).orElseThrow();
            ConfirmacionDeSuscripcion.exigirQueCoincida(o, op, c);
            Orden actual = ordenes.porIdBloqueando(dsl, o.id()).orElseThrow();
            if (!ABIERTAS.contains(actual.estado())) {
                return null; // otro actor ya la resolvio
            }
            OffsetDateTime ahora = ahora();
            LocalDate constitucion = op.detalle().containsKey("constitucion")
                    ? LocalDate.parse(op.detalle().get("constitucion"))
                    : reloj.hoy();
            boolean dpf = p.tipo() == TipoProducto.DPF;
            BigDecimal cuotas = dpf ? null : op.cuotas().orElseThrow();
            BigDecimal valorEntrada = dpf ? null : new BigDecimal(op.detalle().get("valorCuota"));
            LocalDate vencimiento = dpf ? LocalDate.parse(op.detalle().get("vencimiento")) : null;

            UUID posicionId = posiciones.crear(
                    dsl,
                    o.usuarioId(),
                    o.productoId(),
                    o.versionId(),
                    o.id(),
                    p.tipo(),
                    o.monto(),
                    o.moneda(),
                    cuotas,
                    valorEntrada,
                    constitucion,
                    vencimiento,
                    op.posicionExterna().orElseThrow(),
                    ahora);
            comprobantes.emitir(
                    dsl,
                    posicionId,
                    o.usuarioId(),
                    "SUSCRIPCION",
                    o.id(),
                    Descomposicion.suscripcion(o.monto()),
                    o.moneda(),
                    c.origen(),
                    ahora);
            // El debito definitivo queda como intencion en la MISMA transaccion que la posicion.
            instrucciones.crear(
                    dsl, "ORDEN", o.id(), "DEBITAR", o.usuarioId(), o.cuentaId(), o.monto(), o.moneda(), ahora);
            ordenes.confirmar(
                    dsl, o.id(), ABIERTAS, op.transaccion(), op.fechaValor().orElse(null), ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "inversiones.posicion_constituida",
                            "posicion_inversion",
                            posicionId,
                            Map.of("ordenId", o.id().toString()),
                            UUID.fromString(ctx.traza().id())));
            return null;
        }));
    }

    private void rechazar(Orden o, String motivo, ContextoSesion ctx) {
        tx.en(() -> datos.conContexto(ctx, dsl -> {
            if (ordenes.rechazar(dsl, o.id(), ABIERTAS, motivo, ahora())) {
                boolean habiaRetenido = instrucciones
                        .deOrigen(dsl, o.id(), "RETENER")
                        .filter(i -> "APLICADA".equals(i.estado()))
                        .isPresent();
                if (habiaRetenido) {
                    instrucciones.crear(
                            dsl,
                            "ORDEN",
                            o.id(),
                            "LIBERAR",
                            o.usuarioId(),
                            o.cuentaId(),
                            o.monto(),
                            o.moneda(),
                            ahora());
                }
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "inversiones.orden_rechazada",
                                "orden_inversion",
                                o.id(),
                                Map.of("motivo", motivo),
                                UUID.fromString(ctx.traza().id())));
            }
            return null;
        }));
        aplicarInstruccion(o, "LIBERAR", ctx);
    }

    // ------------------------------------------------------------------ lectura
    private Orden leer(UUID id, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> ordenes.porId(dsl, id).orElseThrow()));
    }

    private Orden propia(UUID id, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> ordenes.porId(dsl, id)))
                .filter(x -> ctx.esSistema() || x.usuarioId().equals(ctx.usuarioId()))
                .orElseThrow(() -> Errores.de(122, 1, "Esa orden no existe."));
    }

    private VistaOrden vista(Orden o, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(
                ctx,
                dsl -> VistaOrden.de(
                        o,
                        catalogo.porId(dsl, o.productoId()).orElseThrow().codigo(),
                        posiciones
                                .porOrden(dsl, o.id())
                                .map(PosicionRepositorio.Posicion::id)
                                .orElse(null))));
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}
