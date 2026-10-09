package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.aplicacion.TransicionesDeCesion.Titulo;
import bo.aportaya.entregas.dominio.ReglasDeOferta;
import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Comprar el derecho a cobrar un turno: una saga que vende UNA vez y se puede reanudar.
 *
 * <p>Orden, pensado para que lo irreversible vaya al final (regla 98.3.5):
 *
 * <ol>
 *   <li>reservar la oferta (UN comprador gana; el resto no retiene nada);
 *   <li>retener el saldo del comprador en {@code nucleo-financiero} (compensable);
 *   <li>asignar el titulo (compensable mientras el vendedor no cobre);
 *   <li>pagarle al vendedor (lo irreversible) y cerrar.
 * </ol>
 *
 * <p>Cada paso es una escritura local corta ({@link TransicionesDeCesion}) y cada llamada remota
 * lleva una clave que sale de la CESION, no del intento: si el proceso muere en el medio,
 * {@link #reanudar} sigue desde el estado persistido sin retener ni pagar dos veces. Esta clase
 * NO abre transaccion: las llamadas a nucleo y a grupos van afuera (invariante 6).
 */
@Service
public class ComprarOfertaDeTurno {

    private final HechosDeGrupos grupos;
    private final FondosDelComprador fondos;
    private final TransicionesDeCesion pasos;
    private final Reloj reloj;
    private final Duration reservaMaxima;

    public ComprarOfertaDeTurno(
            HechosDeGrupos grupos,
            FondosDelComprador fondos,
            TransicionesDeCesion pasos,
            Reloj reloj,
            @Value("${aportaya.mercado.reserva-maxima}") Duration reservaMaxima) {
        this.grupos = grupos;
        this.fondos = fondos;
        this.pasos = pasos;
        this.reloj = reloj;
        this.reservaMaxima = reservaMaxima;
    }

    /** {@code liquidada} es lo unico que habilita decir «el turno es tuyo»; los demas estados son en curso o fallidos. */
    public record Resultado(UUID cesionId, String estado, boolean liquidada) {}

    public Resultado comprar(UUID ofertaId, String clave, ContextoSesion ctx) {
        var oferta = pasos.oferta(ofertaId, ctx);
        var miembro = grupos.miembro(oferta.grupoId(), ctx.usuarioId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(130, 8), "No se pudo confirmar tu membresia con grupos: no se compra."));
        if (!miembro.activo()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 6), "Solo un miembro activo del grupo puede comprar este derecho.");
        }
        var reserva = pasos.reservar(ofertaId, ctx.usuarioId(), miembro.participanteId(), clave, ctx);
        return avanzar(reserva.cesion().id(), ctx);
    }

    /** Sigue la saga desde donde quedo. Idempotente: llamarla de mas no retiene ni paga otra vez. */
    public Resultado reanudar(UUID cesionId, ContextoSesion ctx) {
        return avanzar(cesionId, ctx);
    }

    private Resultado avanzar(UUID cesionId, ContextoSesion ctx) {
        while (true) {
            var v = pasos.leer(cesionId, ctx);
            var c = v.cesion();
            switch (c.estado()) {
                case "LIQUIDADA":
                    return new Resultado(cesionId, "LIQUIDADA", true);
                case "FALLIDA":
                    return new Resultado(cesionId, "FALLIDA", false);
                case "VALIDADA": {
                    var r = fondos.retener(c.compradorUsuarioId(), c.precio(), "cesion:" + cesionId + ":retener");
                    if (r.resultado() == FondosDelComprador.Resultado.SALDO_INSUFICIENTE) {
                        pasos.fallar(cesionId, "El comprador no tiene saldo para el precio.", ctx);
                        throw new ErrorDeNegocio(CodigoError.de(130, 7), "No tenes saldo disponible para ese precio.");
                    }
                    if (r.resultado() != FondosDelComprador.Resultado.OK) {
                        return new Resultado(cesionId, c.estado(), false);
                    }
                    pasos.retenida(cesionId, r.referencia(), ctx);
                    break;
                }
                case "FONDOS_RETENIDOS": {
                    if (pasos.asignarTitulo(cesionId, ctx) == Titulo.CONFLICTO_CON_LA_ENTREGA) {
                        // El pozo se fondeo mientras tanto: el derecho ya no es de nadie para vender.
                        deshacer(v.cesion().retencionRef(), cesionId, ctx);
                        throw new ErrorDeNegocio(CodigoError.de(130, 2), "El pozo de ese turno ya se esta entregando.");
                    }
                    break;
                }
                case "TITULO_ASIGNADO": {
                    var oferta = v.oferta();
                    var r = fondos.pagarAlVendedor(
                            c.retencionRef(),
                            oferta.vendedorUsuarioId(),
                            ReglasDeOferta.paraElVendedor(oferta.precio(), oferta.cargos()),
                            oferta.cargos(),
                            "cesion:" + cesionId + ":pagar");
                    if (r.resultado() != FondosDelComprador.Resultado.OK) {
                        return new Resultado(cesionId, c.estado(), false);
                    }
                    pasos.liquidar(cesionId, r.referencia(), ctx);
                    break;
                }
                default:
                    throw new ErrorDeNegocio(
                            CodigoError.de(130, 9), "Estado de cesion desconocido: " + c.estado() + ".");
            }
        }
    }

    /** Libera la retencion (idempotente) y recien despues da la cesion por fallida. */
    private void deshacer(UUID retencionRef, UUID cesionId, ContextoSesion ctx) {
        if (retencionRef != null
                && fondos.liberar(retencionRef, "cesion:" + cesionId + ":liberar") != FondosDelComprador.Resultado.OK) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 8), "No se pudo liberar el saldo retenido: la cesion queda para reanudar.");
        }
        pasos.fallar(cesionId, "No se pudo dar el titulo.", ctx);
    }

    /** Cancela una compra que todavia no le pago al vendedor: libera el saldo y reabre la oferta. */
    public Resultado abortar(UUID cesionId, String motivo, ContextoSesion ctx) {
        var c = pasos.leer(cesionId, ctx).cesion();
        if ("FALLIDA".equals(c.estado())) {
            return new Resultado(cesionId, "FALLIDA", false);
        }
        if ("LIQUIDADA".equals(c.estado()) || c.liquidacionRef() != null) {
            throw new ErrorDeNegocio(CodigoError.de(130, 9), "El vendedor ya cobro: la cesion no se deshace a ciegas.");
        }
        if (c.retencionRef() != null
                && fondos.liberar(c.retencionRef(), "cesion:" + cesionId + ":liberar")
                        != FondosDelComprador.Resultado.OK) {
            throw new ErrorDeNegocio(CodigoError.de(130, 8), "No se pudo liberar el saldo retenido.");
        }
        pasos.fallar(cesionId, motivo, ctx);
        return new Resultado(cesionId, "FALLIDA", false);
    }

    /** Lo que quedo colgado mas alla de la reserva maxima se aborta: nadie retiene un derecho para siempre. */
    public int abortarColgadas(ContextoSesion ctx) {
        int abortadas = 0;
        for (UUID id :
                pasos.colgadasAntesDe(reloj.ahora().minus(reservaMaxima).atOffset(java.time.ZoneOffset.UTC), ctx)) {
            try {
                abortar(id, "La reserva vencio sin completarse.", ctx);
                abortadas++;
            } catch (ErrorDeNegocio sigue) {
                // Una que no se puede deshacer hoy (nucleo caido) no frena a las demas: queda para la proxima.
            }
        }
        return abortadas;
    }
}
