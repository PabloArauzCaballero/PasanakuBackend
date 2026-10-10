package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.PoliticaDeReintento;
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
import java.time.Duration;
import java.time.Instant;
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

    /** Lo que dejo un barrido de reintento: cuantas se aplicaron, cuantas siguen y cuantas el trabajo ya no toca. */
    public record Barrido(
            int aplicadas, int rechazadas, int siguenPendientes, int esperando, int atascadas, boolean cortado) {}

    /** Reintenta TODAS las pendientes ya, sin esperas ni tope de intentos (lo que hace una persona a mano). */
    public int reintentarPendientes(int limite, ContextoSesion ctx) {
        return reintentarPendientes(limite, limite, null, true, Duration.ofDays(1), ctx)
                .aplicadas();
    }

    /**
     * Reintenta las pendientes que le tocan segun la politica; lo corre el trabajo programado.
     * Con {@code forzar} se ignoran la espera y el tope de intentos. Si el barrido supera
     * {@code plazoMaximo} se corta SIN dejar nada a medias: cada instruccion se aplica entera o
     * no se toca, y el resto queda para la proxima vuelta.
     *
     * @param limite cuantas se intentan como mucho en esta vuelta
     * @param lectura cuantas pendientes se leen para elegir entre ellas (las atascadas quedan al final)
     * @param politica espera exponencial y tope de intentos; nula solo con {@code forzar}
     */
    public Barrido reintentarPendientes(
            int limite,
            int lectura,
            PoliticaDeReintento politica,
            boolean forzar,
            Duration plazoMaximo,
            ContextoSesion ctx) {
        List<Instruccion> pendientes =
                tx.en(() -> datos.conContexto(ctx, dsl -> instrucciones.pendientes(dsl, lectura)));
        Instant ahora = reloj.ahora();
        long vence = System.nanoTime() + plazoMaximo.toNanos();
        int aplicadas = 0;
        int rechazadas = 0;
        int siguen = 0;
        int esperando = 0;
        int atascadas = 0;
        boolean cortado = false;
        for (Instruccion i : pendientes) {
            boolean atascada = politica != null && politica.atascada(i.intentos());
            if (atascada && !forzar) {
                atascadas++;
                continue;
            }
            if (!forzar
                    && !politica.leToca(
                            i.intentos(), Duration.between(i.creadaEn().toInstant(), ahora))) {
                esperando++;
                continue;
            }
            if (aplicadas + rechazadas + siguen >= limite) {
                break;
            }
            if (System.nanoTime() > vence) {
                cortado = true;
                break;
            }
            switch (aplicar(i.id(), ctx)) {
                case "APLICADA" -> aplicadas++;
                case "FALLIDA" -> rechazadas++;
                default -> siguen++;
            }
        }
        return new Barrido(aplicadas, rechazadas, siguen, esperando, atascadas, cortado);
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
