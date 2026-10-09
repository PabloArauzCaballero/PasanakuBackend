package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.CesionRepositorio;
import bo.aportaya.entregas.infraestructura.CesionRepositorio.Cesion;
import bo.aportaya.entregas.infraestructura.FondeoRepositorio;
import bo.aportaya.entregas.infraestructura.OfertaRepositorio;
import bo.aportaya.entregas.infraestructura.OfertaRepositorio.Oferta;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cada paso de la cesion, cada uno en SU transaccion local, con su evento.
 *
 * <p>La cesion es una saga: VALIDADA, FONDOS_RETENIDOS, TITULO_ASIGNADO, LIQUIDADA (o FALLIDA).
 * Entre un paso y el siguiente hay una llamada a {@code nucleo-financiero} que va FUERA de la
 * transaccion (invariante 6); por eso cada paso es una escritura corta y reanudable: el estado
 * persistido dice por donde iba, y repetir un paso ya dado no hace nada. Toda transicion lleva
 * su precondicion en la escritura, asi que dos compradores, una cancelacion y un vencimiento
 * simultaneos tienen UN ganador y respuestas coherentes para el resto.
 */
@Service
public class TransicionesDeCesion {

    private final Datos datos;
    private final OfertaRepositorio ofertas;
    private final CesionRepositorio cesiones;
    private final FondeoRepositorio candados;
    private final Outbox outbox;
    private final Reloj reloj;

    public TransicionesDeCesion(
            Datos datos,
            OfertaRepositorio ofertas,
            CesionRepositorio cesiones,
            FondeoRepositorio candados,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.ofertas = ofertas;
        this.cesiones = cesiones;
        this.candados = candados;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record Vista(Cesion cesion, Oferta oferta) {}

    public enum Titulo {
        ASIGNADO,
        CONFLICTO_CON_LA_ENTREGA
    }

    public record Reserva(Cesion cesion, boolean esNueva) {}

    @Transactional(readOnly = true)
    public Oferta oferta(UUID ofertaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> ofertas.ver(dsl, ofertaId)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(130, 5), "Esa oferta no esta disponible.")));
    }

    @Transactional(readOnly = true)
    public Vista leer(UUID cesionId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var c = cesiones.cesion(dsl, cesionId, false)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(130, 9), "Esa cesion no existe."));
            return new Vista(c, ofertas.ver(dsl, c.ofertaId()).orElseThrow());
        });
    }

    /** Paso 1: UN comprador gana la oferta. El resto recibe «no disponible» y no retiene nada. */
    @Transactional
    public Reserva reservar(
            UUID ofertaId, UUID compradorUsuario, UUID destinoParticipante, String clave, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var visible = ofertas.ver(dsl, ofertaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(130, 5), "Esa oferta no esta disponible."));
            candados.serializarTurno(dsl, visible.turnoId());

            var previa = cesiones.cesionPorClave(dsl, clave);
            if (previa.isPresent()) {
                var p = previa.get();
                if (!p.ofertaId().equals(ofertaId) || !p.compradorUsuarioId().equals(compradorUsuario)) {
                    throw new ErrorDeNegocio(CodigoError.de(130, 6), "Esa clave ya se uso para otra compra.");
                }
                return new Reserva(p, false);
            }
            var o = ofertas.bloquear(dsl, ofertaId).orElseThrow();
            if (o.vendedorUsuarioId().equals(compradorUsuario)) {
                throw new ErrorDeNegocio(CodigoError.de(130, 6), "No podes comprar tu propia oferta.");
            }
            if (cesiones.hayEntrega(dsl, o.turnoId())) {
                throw new ErrorDeNegocio(CodigoError.de(130, 2), "El pozo de ese turno ya se esta entregando.");
            }
            if (!ofertas.reservar(dsl, o, ahora)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(130, 5),
                        "Esa oferta no esta disponible: otra persona la reservo, se cancelo o vencio.");
            }
            UUID id = cesiones.crearCesion(dsl, o, destinoParticipante, compradorUsuario, clave, ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "entregas.oferta_reservada",
                            "cesion_derecho",
                            id,
                            Map.of(
                                    "ofertaId",
                                    ofertaId.toString(),
                                    "turnoId",
                                    o.turnoId().toString()),
                            UUID.fromString(ctx.traza().id())));
            return new Reserva(cesiones.cesion(dsl, id, false).orElseThrow(), true);
        });
    }

    /** Paso 2 (despues de retener el saldo del comprador). Repetirlo no hace nada. */
    @Transactional
    public void retenida(UUID cesionId, UUID retencionRef, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        datos.conContexto(ctx, dsl -> {
            var c = cesiones.cesion(dsl, cesionId, true).orElseThrow();
            if ("VALIDADA".equals(c.estado())
                    && !cesiones.avanzar(dsl, c, "VALIDADA", "FONDOS_RETENIDOS", retencionRef, null, null, ahora)) {
                throw new ErrorDeNegocio(CodigoError.de(130, 9), "La cesion cambio mientras se retenia.");
            }
            return null;
        });
    }

    /** Paso 3: el titulo cambia de mano, salvo que mientras tanto el pozo ya se haya fondeado. */
    @Transactional
    public Titulo asignarTitulo(UUID cesionId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            var visible = cesiones.cesion(dsl, cesionId, false).orElseThrow();
            candados.serializarTurno(dsl, visible.turnoId());
            var c = cesiones.cesion(dsl, cesionId, true).orElseThrow();
            if ("TITULO_ASIGNADO".equals(c.estado()) || "LIQUIDADA".equals(c.estado())) {
                return Titulo.ASIGNADO;
            }
            if (!"FONDOS_RETENIDOS".equals(c.estado())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(130, 9), "La cesion no esta lista para dar el titulo: " + c.estado() + ".");
            }
            if (cesiones.hayEntrega(dsl, c.turnoId())) {
                return Titulo.CONFLICTO_CON_LA_ENTREGA;
            }
            var o = ofertas.bloquear(dsl, c.ofertaId()).orElseThrow();
            if (!cesiones.avanzar(dsl, c, "FONDOS_RETENIDOS", "TITULO_ASIGNADO", null, null, null, ahora)
                    || !ofertas.mover(dsl, o, List.of("RESERVADA"), "LIQUIDANDO")) {
                throw new ErrorDeNegocio(CodigoError.de(130, 9), "La cesion cambio mientras se daba el titulo.");
            }
            return Titulo.ASIGNADO;
        });
    }

    /** Paso 4, el irreversible: el vendedor cobro. La oferta pasa a VENDIDA y el derecho es del comprador. */
    @Transactional
    public void liquidar(UUID cesionId, UUID liquidacionRef, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        datos.conContexto(ctx, dsl -> {
            var c = cesiones.cesion(dsl, cesionId, true).orElseThrow();
            if ("LIQUIDADA".equals(c.estado())) {
                return null;
            }
            var o = ofertas.bloquear(dsl, c.ofertaId()).orElseThrow();
            if (!cesiones.avanzar(dsl, c, "TITULO_ASIGNADO", "LIQUIDADA", null, liquidacionRef, null, ahora)
                    || !ofertas.mover(dsl, o, List.of("LIQUIDANDO"), "VENDIDA")) {
                throw new ErrorDeNegocio(CodigoError.de(130, 9), "La cesion cambio mientras se liquidaba.");
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "entregas.derecho_cedido",
                            "cesion_derecho",
                            cesionId,
                            Map.of(
                                    "turnoId", c.turnoId().toString(),
                                    "precio", c.precio().toString(),
                                    "moneda", c.precio().moneda().name(),
                                    "liquidacionRef", liquidacionRef.toString()),
                            UUID.fromString(ctx.traza().id())));
            return null;
        });
    }

    /**
     * La compra no prospera. Solo antes de pagarle al vendedor: despues de pagar no se devuelve a
     * ciegas. La oferta vuelve a estar a la venta si sigue vigente; si no, vence.
     */
    @Transactional
    public boolean fallar(UUID cesionId, String motivo, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            var visible = cesiones.cesion(dsl, cesionId, false).orElseThrow();
            candados.serializarTurno(dsl, visible.turnoId());
            var c = cesiones.cesion(dsl, cesionId, true).orElseThrow();
            if ("FALLIDA".equals(c.estado())) {
                return false;
            }
            if ("LIQUIDADA".equals(c.estado()) || c.liquidacionRef() != null) {
                throw new ErrorDeNegocio(
                        CodigoError.de(130, 9), "El vendedor ya cobro: la cesion no se deshace a ciegas.");
            }
            var o = ofertas.bloquear(dsl, c.ofertaId()).orElseThrow();
            if (!cesiones.avanzar(dsl, c, c.estado(), "FALLIDA", null, null, motivo, ahora)) {
                throw new ErrorDeNegocio(CodigoError.de(130, 9), "La cesion cambio mientras se deshacia.");
            }
            ofertas.mover(
                    dsl,
                    o,
                    List.of("RESERVADA", "LIQUIDANDO"),
                    o.vigenteHasta().isAfter(ahora) ? "PUBLICADA" : "VENCIDA");
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "entregas.cesion_fallida",
                            "cesion_derecho",
                            cesionId,
                            Map.of("turnoId", c.turnoId().toString(), "motivo", motivo),
                            UUID.fromString(ctx.traza().id())));
            return true;
        });
    }

    @Transactional(readOnly = true)
    public List<UUID> colgadasAntesDe(OffsetDateTime limite, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> cesiones.colgadasAntesDe(dsl, limite));
    }
}
