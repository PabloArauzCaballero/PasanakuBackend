package bo.aportaya.nucleofinanciero.aplicacion;

import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.SERVICIO_INVERSIONES;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.bloquearEnOrden;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.comoOperacion;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.exigirEjecutable;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.exigirServicio;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.iniciador;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.sinCuentaDeSistema;

import bo.aportaya.nucleofinanciero.dominio.VigenciaDeRetencion.Motivo;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio.Cuenta;
import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio.Retencion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-13 · Ejecutar una retencion CON DESTINO: cierra la retencion y escribe el movimiento que la motivo en
 * <b>la misma transaccion</b> (el saldo se deriva del libro: cerrarla sin movimiento devolveria la plata).
 * {@link #debitarInversion} (llama {@code inversiones}) es una operacion de SERVICIO, no de usuario final (ver
 * {@link ReglasDeEjecucion}); idempotente por clave (R-BIL-06) y por hecho (R-BIL-24). El pago de una compra del
 * mercado de turnos se retiro con la cesion (D-L2-1).
 */
@Service
public class CU13EjecutarConDestino {

    private static final String ORIGEN_INVERSION = "ORDEN_INVERSION";

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final RetencionRepositorio retenciones;
    private final CuentasDeSistemaRepositorio sistema;
    private final CU13RetenerSaldo cu13;
    private final LibroDeBilletera libro;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU13EjecutarConDestino(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            RetencionRepositorio retenciones,
            CuentasDeSistemaRepositorio sistema,
            CU13RetenerSaldo cu13,
            LibroDeBilletera libro,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.retenciones = retenciones;
        this.sistema = sistema;
        this.cu13 = cu13;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ inversion

    @Transactional
    public SalidaEjecucion debitarInversion(EntradaDebito e, ContextoSesion llamador) {
        exigirServicio(llamador, SERVICIO_INVERSIONES, CodigoError.de(13, 9));
        ContextoSesion ctx = comoOperacion(llamador);
        return datos.conContexto(ctx, dsl -> {
            libro.bloquearIdempotencia(dsl, iniciador(ctx), ORIGEN_INVERSION, e.claveIdempotencia());
            Retencion retencion = retencionExistente(dsl, e.retencionId());
            Cuenta duena = cuentas.ver(dsl, retencion.cuentaId()).orElseThrow();
            // El servicio llamador identifica a la persona que ya valido: el libro lo verifica, no lo supone.
            e.titularId().ifPresent(titular -> {
                if (!titular.equals(duena.usuarioId())) {
                    throw new ErrorDeNegocio(CodigoError.de(13, 1), "Esa retencion no es de esa persona.");
                }
            });
            Optional<UUID> previa =
                    libro.porClaveIdempotencia(dsl, iniciador(ctx), ORIGEN_INVERSION, e.claveIdempotencia());
            if (previa.isPresent()) {
                return repetida(dsl, previa.get(), retencion, e.monto(), duena);
            }
            exigirEjecutable(retencion, Motivo.INVERSION_VOLUNTARIA, e.monto());
            UUID custodia = sistema.custodiaDeInversion(dsl, duena.moneda())
                    .orElseThrow(() -> sinCuentaDeSistema("la custodia de inversion", duena.moneda()));

            bloquearEnOrden(dsl, cuentas, List.of(duena.id(), custodia));
            cu13.ejecutarDentroDe(dsl, retencion.id(), ctx);
            UUID origenId = retencion.referenciaId().orElse(retencion.id());
            UUID transaccionId = libro.registrar(
                    dsl,
                    "INVERSION",
                    ORIGEN_INVERSION,
                    origenId,
                    "API",
                    retencion.monto(),
                    e.claveIdempotencia(),
                    iniciador(ctx),
                    List.of(
                            Pata.debito(duena.id(), retencion.monto(), "Inversion voluntaria"),
                            Pata.credito(custodia, retencion.monto(), "Custodia de inversion")),
                    ahora());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.inversion_debitada",
                            "transaccion_billetera",
                            transaccionId,
                            Map.of(
                                    "cuentaBilleteraId", duena.id().toString(),
                                    "retencionId", retencion.id().toString(),
                                    "ordenId", origenId.toString(),
                                    "monto", retencion.monto().toString(),
                                    "moneda", retencion.monto().moneda().name(),
                                    "servicio", llamador.rol()),
                            UUID.fromString(ctx.traza().id())));
            return salida(dsl, transaccionId, duena.id(), false);
        });
    }

    // ------------------------------------------------------------------ compartido

    private Retencion retencionExistente(DSLContext dsl, UUID retencionId) {
        return retenciones
                .ver(dsl, retencionId)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 3), "Esa retencion no existe."));
    }

    /**
     * Repetir la clave es repetir el MISMO hecho: misma retencion (ya ejecutada), mismo importe, y la
     * transaccion nacio de esa orden u oferta. Si algo cambia, la clave ya identifica otra cosa.
     */
    private SalidaEjecucion repetida(
            DSLContext dsl, UUID transaccionId, Retencion retencion, Dinero monto, Cuenta duena) {
        var previa = libro.resumen(dsl, transaccionId).orElseThrow();
        boolean mismo = "EJECUTADA".equals(retencion.estado())
                && previa.origenId().equals(retencion.referenciaId().orElse(retencion.id()))
                && previa.monto().equals(monto)
                && retencion.monto().equals(monto);
        if (!mismo) {
            throw new ErrorDeNegocio(CodigoError.de(13, 4), "La clave de idempotencia ya identifica otra operacion.");
        }
        return salida(dsl, transaccionId, duena.id(), true);
    }

    private SalidaEjecucion salida(DSLContext dsl, UUID transaccionId, UUID cuentaId, boolean repetida) {
        Cuenta despues = cuentas.ver(dsl, cuentaId).orElseThrow();
        return new SalidaEjecucion(transaccionId, despues.disponible(), despues.retenido(), repetida);
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    public record EntradaDebito(String claveIdempotencia, UUID retencionId, Dinero monto, Optional<UUID> titularId) {}

    /** Los hechos de la oferta los trajo el servidor de {@code entregas}; el cliente no dicta ninguno. */

    /** {@code repetida} distingue el reintento del primer intento sin cambiar el codigo HTTP del contrato. */
    public record SalidaEjecucion(UUID transaccionId, Dinero saldoDisponible, Dinero saldoRetenido, boolean repetida) {}
}
