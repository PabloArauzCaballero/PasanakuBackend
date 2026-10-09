package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.AsientoPropuesto;
import bo.aportaya.garantia.dominio.RespaldoEmpresarial;
import bo.aportaya.garantia.infraestructura.RespaldoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-23 (respaldo empresarial) · Reservar y liberar capacidad por ciclo.
 *
 * <p>Es la mitad de «el pozo se entrega completo»: antes de activar un grupo la empresa
 * aparta, de una capacidad finita, lo que podria tener que poner. Dos grupos que activan
 * a la vez compiten por la <b>misma fila de capacidad</b>: se bloquea primero, se mira el
 * disponible con el candado puesto y recien ahi se reserva. La base lo vuelve a exigir
 * ({@code ck_capacidad_respaldo_comprometido}) por si alguien llega sin pasar por aca.
 *
 * <p>No hay capacidad por omision: sin una fila cargada por gobierno, se deniega. El tope
 * es un dato (ambiguedad A2), no una constante.
 */
@Service
public class CU23ReservarRespaldo {

    private static final int LARGO_CLAVE = 80;

    /** El unico pozo de capacidad hoy. Si algun dia hay mas de uno, esto pasa a ser dato. */
    static final String AMBITO = "GENERAL";

    private final Datos datos;
    private final RespaldoRepositorio respaldo;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU23ReservarRespaldo(Datos datos, RespaldoRepositorio respaldo, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.respaldo = respaldo;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record EntradaReserva(UUID grupoId, int ciclo, Dinero monto, UUID responsableId, ClaveIdempotencia clave) {}

    public record SalidaReserva(UUID reservaId, Dinero reservado, Dinero capacidadDisponible, boolean esNueva) {}

    public record SalidaLiberacion(UUID reservaId, Dinero liberado, boolean esNueva) {}

    @Transactional
    public SalidaReserva reservar(EntradaReserva e, ContextoSesion ctx) {
        validar(e);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            // El candado va PRIMERO: la comparacion y la escritura quedan en la misma fila.
            var capacidad = respaldo.bloquearCapacidad(dsl, AMBITO, e.monto().moneda())
                    .orElseThrow(() -> new ErrorDeNegocio(
                            CodigoError.de(23, 5), "No hay capacidad empresarial cargada: se deniega por omision."));

            var previa = respaldo.porClave(dsl, e.clave().valor());
            if (previa.isPresent()) {
                var p = previa.get();
                if (!p.grupoId().equals(e.grupoId())
                        || p.ciclo() != e.ciclo()
                        || !p.cifras().reservado().equals(e.monto())) {
                    throw new ErrorDeNegocio(CodigoError.de(23, 6), "Esa clave ya se uso para otra reserva distinta.");
                }
                return new SalidaReserva(p.id(), p.cifras().reservado(), capacidad.disponible(), false);
            }

            if (e.monto().esMayorQue(capacidad.disponible())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(23, 5),
                        "La capacidad empresarial no alcanza para respaldar este ciclo.",
                        Map.of(
                                "solicitado",
                                e.monto().toString(),
                                "disponible",
                                capacidad.disponible().toString()));
            }

            UUID reservaId = respaldo.crear(
                    dsl,
                    capacidad.id(),
                    e.grupoId(),
                    e.ciclo(),
                    e.monto(),
                    e.responsableId(),
                    e.clave().valor(),
                    ahora);
            var despues = new RespaldoEmpresarial.Reserva(
                    e.monto(),
                    Dinero.cero(e.monto().moneda()),
                    Dinero.cero(e.monto().moneda()),
                    Dinero.cero(e.monto().moneda()));
            respaldo.movimiento(
                    dsl,
                    reservaId,
                    "RESERVA",
                    e.monto(),
                    despues,
                    "RESERVA",
                    reservaId,
                    "mov:" + e.clave().valor(),
                    ctx.usuarioId(),
                    e.responsableId(),
                    ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "garantia.respaldo_reservado",
                            "reserva_respaldo",
                            reservaId,
                            Map.of(
                                    "grupoId", e.grupoId().toString(),
                                    "ciclo", e.ciclo(),
                                    "monto", e.monto().toString(),
                                    "moneda", e.monto().moneda().name(),
                                    "responsableId", e.responsableId().toString(),
                                    "asiento",
                                            AsientoPropuesto.reserva(reservaId, e.monto())
                                                    .comoCarga()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaReserva(
                    reservaId, e.monto(), capacidad.disponible().menos(e.monto()), true);
        });
    }

    public record SalidaAmpliacion(
            UUID reservaId, Dinero reservadoDespues, Dinero capacidadDisponible, boolean esNueva) {}

    /**
     * Contingencia (falta extraordinaria de caja): la reserva de un ciclo en curso se
     * amplia, siempre contra la misma capacidad y con su propio movimiento en el libro.
     * Quien autoriza la ampliacion es el responsable de la reserva (ambiguedad A2).
     */
    @Transactional
    public SalidaAmpliacion ampliar(UUID reservaId, Dinero monto, ClaveIdempotencia clave, ContextoSesion ctx) {
        if (monto.esNegativo() || monto.esCero() || clave.valor().length() > LARGO_CLAVE) {
            throw new ErrorDeNegocio(
                    CodigoError.de(23, 5), "Una ampliacion es por un importe positivo y con clave valida.");
        }
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var sinCandado = respaldo.bloquear(dsl, reservaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(23, 7), "Esa reserva no existe."));
            // Se bloquea la capacidad DESPUES de leer la reserva; el orden es siempre el mismo
            // (reserva y luego capacidad) en todo el servicio, para que no haya abrazo mortal.
            var capacidad = respaldo.bloquearCapacidad(dsl, AMBITO, monto.moneda())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(23, 5), "No hay capacidad cargada."));
            var movimientoPrevio = respaldo.movimientoPorClave(dsl, "mov:" + clave.valor());
            if (movimientoPrevio) {
                return new SalidaAmpliacion(reservaId, sinCandado.cifras().reservado(), capacidad.disponible(), false);
            }
            if (!"VIGENTE".equals(sinCandado.estado())) {
                throw new ErrorDeNegocio(CodigoError.de(23, 7), "La reserva ya fue liberada: no se amplia.");
            }
            if (monto.esMayorQue(capacidad.disponible())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(23, 5),
                        "La capacidad empresarial no alcanza para ampliar la reserva.",
                        Map.of(
                                "solicitado",
                                monto.toString(),
                                "disponible",
                                capacidad.disponible().toString()));
            }
            var antes = sinCandado.cifras();
            var despues = new RespaldoEmpresarial.Reserva(
                    antes.reservado().mas(monto), antes.aplicado(), antes.recuperado(), antes.liberado());
            respaldo.movimiento(
                    dsl,
                    reservaId,
                    "AMPLIACION",
                    monto,
                    despues,
                    "RESERVA",
                    reservaId,
                    "mov:" + clave.valor(),
                    ctx.usuarioId(),
                    sinCandado.responsableId(),
                    ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "garantia.respaldo_ampliado",
                            "reserva_respaldo",
                            reservaId,
                            Map.of(
                                    "monto", monto.toString(),
                                    "moneda", monto.moneda().name(),
                                    "asiento",
                                            AsientoPropuesto.reserva(reservaId, monto)
                                                    .comoCarga()),
                            UUID.fromString(ctx.traza().id())));
            return new SalidaAmpliacion(
                    reservaId, despues.reservado(), capacidad.disponible().menos(monto), true);
        });
    }

    /** Al cerrar el ciclo lo no usado vuelve a la caja y a la capacidad. Una sola vez. */
    @Transactional
    public SalidaLiberacion liberar(UUID reservaId, ClaveIdempotencia clave, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var reserva = respaldo.bloquear(dsl, reservaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(23, 7), "Esa reserva no existe."));
            Dinero liberable = RespaldoEmpresarial.liberable(reserva.cifras());
            if ("LIBERADA".equals(reserva.estado())) {
                return new SalidaLiberacion(reservaId, reserva.cifras().liberado(), false);
            }
            if (liberable.esCero()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(23, 7), "La reserva esta toda aplicada: no hay nada que liberar.");
            }
            var despues = new RespaldoEmpresarial.Reserva(
                    reserva.cifras().reservado(),
                    reserva.cifras().aplicado(),
                    reserva.cifras().recuperado(),
                    reserva.cifras().liberado().mas(liberable));
            respaldo.movimiento(
                    dsl,
                    reservaId,
                    "LIBERACION",
                    liberable,
                    despues,
                    "RESERVA",
                    reservaId,
                    "mov:" + clave.valor(),
                    ctx.usuarioId(),
                    reserva.responsableId(),
                    ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "garantia.respaldo_liberado",
                            "reserva_respaldo",
                            reservaId,
                            Map.of(
                                    "monto", liberable.toString(),
                                    "moneda", liberable.moneda().name(),
                                    "asiento",
                                            AsientoPropuesto.liberacion(reservaId, liberable)
                                                    .comoCarga()),
                            UUID.fromString(ctx.traza().id())));
            return new SalidaLiberacion(reservaId, liberable, true);
        });
    }

    private void validar(EntradaReserva e) {
        if (e.monto().esNegativo() || e.monto().esCero()) {
            throw new ErrorDeNegocio(CodigoError.de(23, 5), "Una reserva se hace por un importe positivo.");
        }
        if (e.ciclo() < 1) {
            throw new ErrorDeNegocio(CodigoError.de(23, 5), "El ciclo empieza en 1.");
        }
        if (e.clave().valor().length() > LARGO_CLAVE) {
            throw new ErrorDeNegocio(CodigoError.de(23, 6), "La clave de idempotencia supera " + LARGO_CLAVE + ".");
        }
    }
}
