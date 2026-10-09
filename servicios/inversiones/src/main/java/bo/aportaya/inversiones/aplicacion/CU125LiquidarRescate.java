package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.Descomposicion;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoRechazo;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.OperacionDelAliado;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio.Rescate;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * CU-125 · Liquidar un rescate: del «el aliado dijo que si» al dinero acreditado.
 *
 * <p>Nada de lo que informa el aliado se acepta por venir firmado: el interes de un DPF y
 * su retencion se RECALCULAN con la formula propia y se concilian ({@code conciliacion_interes});
 * el importe de un rescate de cuotas se compara contra cuotas x valor. Si no coinciden, el
 * rescate NO se acredita y queda a la vista como discrepancia.
 *
 * <p>La liquidacion y la intencion de acreditar se guardan en UNA transaccion; recien
 * despues se le pide al libro el credito, con clave determinista (repetirlo no acredita dos
 * veces). Si el libro no responde, el rescate queda POR_ACREDITAR: liquidado, descontado de
 * la posicion y visible, pero todavia no disponible.
 *
 * <p>Redondeo: montos a centavos half-even. El costo base de un rescate parcial se prorratea
 * y el residuo de redondeo cae en el ultimo rescate (nunca se pierde).
 */
@Service
public class CU125LiquidarRescate {

    private enum Resultado {
        LIQUIDADO,
        DISCREPANCIA,
        YA_RESUELTO
    }

    private static final List<String> EN_CURSO = List.of("SOLICITADO", "PENDIENTE", "INCIERTO");

    private final Datos datos;
    private final Transaccionar tx;
    private final RescateRepositorio rescates;
    private final PosicionRepositorio posiciones;
    private final CatalogoRepositorio catalogo;
    private final ComprobanteRepositorio comprobantes;
    private final InstruccionRepositorio instrucciones;
    private final AplicadorDeInstrucciones aplicador;
    private final AliadoDeInversion aliado;
    private final CalculoDeLiquidacion calculo;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU125LiquidarRescate(
            Datos datos,
            Transaccionar tx,
            RescateRepositorio rescates,
            PosicionRepositorio posiciones,
            CatalogoRepositorio catalogo,
            ComprobanteRepositorio comprobantes,
            InstruccionRepositorio instrucciones,
            AplicadorDeInstrucciones aplicador,
            AliadoDeInversion aliado,
            CalculoDeLiquidacion calculo,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.rescates = rescates;
        this.posiciones = posiciones;
        this.catalogo = catalogo;
        this.comprobantes = comprobantes;
        this.instrucciones = instrucciones;
        this.aplicador = aplicador;
        this.aliado = aliado;
        this.calculo = calculo;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** Consulta al aliado por la referencia y avanza: liquida, rechaza o deja pendiente. */
    public VistaRescate sincronizar(UUID rescateId, ContextoSesion ctx) {
        Rescate r = propio(rescateId, ctx);
        if (EN_CURSO.contains(r.estado())) {
            try {
                Optional<OperacionDelAliado> op = aliado.consultar(rescateId);
                if (op.isPresent()) {
                    resolver(r, op.get(), ctx);
                } else {
                    reenviar(r, ctx);
                }
            } catch (AliadoNoDisponible noSeSabe) {
                tx.en(() -> datos.conContexto(
                        ctx,
                        dsl -> rescates.pasar(
                                dsl,
                                rescateId,
                                List.of("SOLICITADO", "PENDIENTE"),
                                "INCIERTO",
                                null,
                                null,
                                null,
                                null,
                                ahora())));
            }
        }
        acreditar(rescateId, ctx);
        return ver(rescateId, ctx);
    }

    public VistaRescate ver(UUID rescateId, ContextoSesion ctx) {
        Rescate r = propio(rescateId, ctx);
        Optional<BigDecimal> neto = tx.en(() -> datos.conContexto(ctx, dsl -> comprobantes.deOrigen(dsl, rescateId)))
                .map(c -> c.d().neto());
        return VistaRescate.de(r, neto.orElse(null));
    }

    /** Lo usa tambien CU-124 justo despues de pedir el rescate. */
    void resolver(Rescate r, OperacionDelAliado op, ContextoSesion ctx) {
        switch (op.estado()) {
            case CONFIRMADO -> {
                Resultado res = tx.en(() -> datos.conContexto(ctx, dsl -> liquidar(dsl, r.id(), op, ctx)));
                if (res == Resultado.DISCREPANCIA) {
                    throw Errores.de(
                            125,
                            1,
                            "El aliado informo cifras que no coinciden con el calculo propio: el rescate queda en revision y no se acredita.");
                }
            }
            case RECHAZADO -> rechazar(r.id(), "RECHAZADO_POR_EL_ALIADO", ctx);
            case PENDIENTE ->
                tx.en(() -> datos.conContexto(
                        ctx,
                        dsl -> rescates.pasar(
                                dsl,
                                r.id(),
                                List.of("SOLICITADO", "INCIERTO"),
                                "PENDIENTE",
                                op.fechaValor().orElse(null),
                                op.liquidaEn()
                                        .map(i -> i.atOffset(ZoneOffset.UTC))
                                        .orElse(null),
                                null,
                                null,
                                ahora())));
        }
    }

    void rechazar(UUID rescateId, String motivo, ContextoSesion ctx) {
        tx.en(() -> datos.conContexto(ctx, dsl -> {
            if (rescates.pasar(dsl, rescateId, EN_CURSO, "RECHAZADO", null, null, null, motivo, ahora())) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "inversiones.rescate_rechazado",
                                "rescate_inversion",
                                rescateId,
                                Map.of("motivo", motivo),
                                UUID.fromString(ctx.traza().id())));
            }
            return null;
        }));
    }

    void acreditar(UUID rescateId, ContextoSesion ctx) {
        Optional<InstruccionRepositorio.Instruccion> i =
                tx.en(() -> datos.conContexto(ctx, dsl -> instrucciones.deOrigen(dsl, rescateId, "ACREDITAR")));
        i.filter(x -> "PENDIENTE".equals(x.estado())).ifPresent(x -> aplicador.aplicar(x.id(), ctx));
    }

    private void reenviar(Rescate r, ContextoSesion ctx) {
        // El aliado no conoce la referencia: nunca la recibio. Reenviar con la misma clave es seguro.
        Posicion p = tx.en(() -> datos.conContexto(
                ctx, dsl -> posiciones.porId(dsl, r.posicionId()).orElseThrow()));
        try {
            resolver(r, aliado.rescatar(r.id(), p.posicionExterna(), Optional.ofNullable(r.cuotas())), ctx);
        } catch (AliadoRechazo rechazo) {
            rechazar(r.id(), rechazo.codigo(), ctx);
        }
    }

    // ------------------------------------------------------------------ liquidacion
    private Resultado liquidar(DSLContext dsl, UUID rescateId, OperacionDelAliado op, ContextoSesion ctx) {
        Rescate r = rescates.porIdBloqueando(dsl, rescateId).orElseThrow();
        if (!EN_CURSO.contains(r.estado())) {
            return Resultado.YA_RESUELTO;
        }
        Posicion p = posiciones.porIdBloqueando(dsl, r.posicionId()).orElseThrow();
        Condiciones c = catalogo.version(dsl, p.versionId()).orElseThrow();
        OffsetDateTime ahora = ahora();

        Descomposicion d;
        if (p.tipo() == TipoProducto.DPF) {
            d = calculo.dpf(dsl, r, p, c, op, ahora);
            if (d == null) {
                return Resultado.DISCREPANCIA;
            }
            posiciones.cerrar(dsl, p.id(), ahora);
        } else {
            d = calculo.fondo(dsl, r, p, c, op, ahora);
            posiciones.descontarCuotas(dsl, p.id(), p.cuotas().subtract(r.cuotas()), ahora);
        }

        comprobantes.emitir(dsl, p.id(), p.usuarioId(), "LIQUIDACION", r.id(), d, p.moneda(), c.origen(), ahora);
        UUID cuenta = cuentaDeLaOrden(dsl, p);
        instrucciones.crear(dsl, "RESCATE", r.id(), "ACREDITAR", p.usuarioId(), cuenta, d.neto(), p.moneda(), ahora);
        rescates.pasar(
                dsl,
                r.id(),
                EN_CURSO,
                "POR_ACREDITAR",
                op.fechaValor().orElse(null),
                op.liquidaEn().map(i -> i.atOffset(ZoneOffset.UTC)).orElse(null),
                op.transaccion(),
                null,
                ahora);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "inversiones.rescate_confirmado",
                        "rescate_inversion",
                        r.id(),
                        Map.of("posicionId", p.id().toString()),
                        UUID.fromString(ctx.traza().id())));
        return Resultado.LIQUIDADO;
    }
    /** La cuenta donde se acredita es la de la orden que creo la posicion. */
    private UUID cuentaDeLaOrden(DSLContext dsl, Posicion p) {
        return instrucciones.deOrigen(dsl, p.ordenId(), "DEBITAR").orElseThrow().cuentaId();
    }

    private Rescate propio(UUID id, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> rescates.porId(dsl, id)))
                .filter(x -> ctx.esSistema() || x.usuarioId().equals(ctx.usuarioId()))
                .orElseThrow(() -> Errores.de(124, 7, "Ese rescate no existe."));
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}
