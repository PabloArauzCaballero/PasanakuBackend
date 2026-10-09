package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.LibroNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.SaldoInsuficiente;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio.Instruccion;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Aplica en el libro una instruccion ya persistida.
 *
 * <p>La intencion se guarda ANTES (en la transaccion del caso de uso) y el efecto se pide
 * DESPUES, fuera de toda transaccion. Si el proceso muere entre el efecto y el registro de
 * «aplicada», la instruccion sigue pendiente y se reintenta con la MISMA clave: el libro
 * devuelve lo que ya hizo y no mueve plata dos veces. Es la razon por la que aplicar es
 * seguro de repetir.
 *
 * <p>Tres desenlaces: aplicada; rechazada de forma definitiva por falta de saldo (la unica
 * que cierra la orden); o «no se sabe» (libro caido), que queda pendiente y visible.
 */
@Service
public class AplicadorDeInstrucciones {

    private final Datos datos;
    private final Transaccionar tx;
    private final InstruccionRepositorio instrucciones;
    private final OrdenRepositorio ordenes;
    private final RescateRepositorio rescates;
    private final LibroDelTitular libro;
    private final Outbox outbox;
    private final Reloj reloj;

    public AplicadorDeInstrucciones(
            Datos datos,
            Transaccionar tx,
            InstruccionRepositorio instrucciones,
            OrdenRepositorio ordenes,
            RescateRepositorio rescates,
            LibroDelTitular libro,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.instrucciones = instrucciones;
        this.ordenes = ordenes;
        this.rescates = rescates;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** @return el estado en que quedo la instruccion: APLICADA, PENDIENTE o FALLIDA. */
    public String aplicar(UUID instruccionId, ContextoSesion ctx) {
        record Previo(Instruccion instruccion, Optional<UUID> retencion) {}
        Previo previo = tx.en(() -> datos.conContexto(ctx, dsl -> {
            Instruccion i = instrucciones.porId(dsl, instruccionId).orElseThrow();
            Optional<UUID> retencion = instrucciones
                    .deOrigen(dsl, i.origenId(), "RETENER")
                    .map(InstruccionRepositorio.Instruccion::referenciaLibro);
            return new Previo(i, retencion);
        }));
        Instruccion i = previo.instruccion();
        if (!"PENDIENTE".equals(i.estado())) {
            return i.estado();
        }

        UUID referencia;
        try {
            referencia = pedirAlLibro(i, previo.retencion());
        } catch (SaldoInsuficiente sinSaldo) {
            tx.en(() -> datos.conContexto(ctx, dsl -> {
                instrucciones.fallida(dsl, i.id(), "SALDO_INSUFICIENTE");
                if (ordenes.rechazar(dsl, i.origenId(), List.of("CREADA"), "SALDO_INSUFICIENTE", ahora())) {
                    evento(dsl, "inversiones.orden_rechazada", "orden_inversion", i.origenId(), ctx);
                }
                return null;
            }));
            return "FALLIDA";
        } catch (LibroNoDisponible noSeSabe) {
            tx.en(() -> datos.conContexto(ctx, dsl -> {
                instrucciones.registrarIntento(dsl, i.id(), "LIBRO_NO_DISPONIBLE");
                return null;
            }));
            return "PENDIENTE";
        }

        tx.en(() -> datos.conContexto(ctx, dsl -> {
            if (instrucciones.aplicada(dsl, i.id(), referencia, ahora())) {
                alAplicar(dsl, i, ctx);
            }
            return null;
        }));
        return "APLICADA";
    }

    /** Reintenta las pendientes; lo corre un trabajo programado y tambien las pruebas. */
    public int reintentarPendientes(int limite, ContextoSesion ctx) {
        List<Instruccion> pendientes =
                tx.en(() -> datos.conContexto(ctx, dsl -> instrucciones.pendientes(dsl, limite)));
        int aplicadas = 0;
        for (Instruccion i : pendientes) {
            if ("APLICADA".equals(aplicar(i.id(), ctx))) {
                aplicadas++;
            }
        }
        return aplicadas;
    }

    private UUID pedirAlLibro(Instruccion i, Optional<UUID> retencion) {
        Dinero monto = Dinero.de(i.monto(), Moneda.valueOf(i.moneda()));
        return switch (i.tipo()) {
            case "RETENER" -> libro.retener(i.cuentaId(), monto, i.clave(), i.origenId());
            case "LIBERAR" -> {
                UUID r = retencion.orElseThrow(() -> new LibroNoDisponible("No hay retencion que liberar"));
                libro.liberar(r, i.clave());
                yield r;
            }
            case "DEBITAR" ->
                libro.debitarInversion(
                        retencion.orElseThrow(() -> new LibroNoDisponible("No hay retencion que debitar")),
                        monto,
                        i.clave());
            case "ACREDITAR" -> libro.acreditarRescate(i.cuentaId(), monto, i.clave(), i.origenId());
            default -> throw new IllegalStateException("Tipo de instruccion desconocido: " + i.tipo());
        };
    }

    private void alAplicar(org.jooq.DSLContext dsl, Instruccion i, ContextoSesion ctx) {
        switch (i.tipo()) {
            case "RETENER" -> {
                if (ordenes.pasar(dsl, i.origenId(), List.of("CREADA"), "RETENIDA", ahora())) {
                    evento(dsl, "inversiones.saldo_reservado", "orden_inversion", i.origenId(), ctx);
                }
            }
            case "ACREDITAR" -> {
                if (rescates.pasar(
                        dsl, i.origenId(), List.of("POR_ACREDITAR"), "LIQUIDADO", null, null, null, null, ahora())) {
                    evento(dsl, "inversiones.rescate_liquidado", "rescate_inversion", i.origenId(), ctx);
                }
            }
            default -> evento(dsl, "inversiones.instruccion_aplicada", "instruccion_libro", i.id(), ctx);
        }
    }

    private void evento(org.jooq.DSLContext dsl, String tipo, String agregado, UUID id, ContextoSesion ctx) {
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        agregado,
                        id,
                        Map.of("id", id.toString()),
                        UUID.fromString(ctx.traza().id())));
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}
