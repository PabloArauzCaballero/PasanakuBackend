package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.dominio.OfertaVisible;
import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.entregas.infraestructura.CesionRepositorio;
import bo.aportaya.entregas.infraestructura.FondeoRepositorio;
import bo.aportaya.entregas.infraestructura.OfertaRepositorio;
import bo.aportaya.entregas.infraestructura.OfertaRepositorio.Oferta;
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
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La mitad LOCAL de las ofertas: publicar, cancelar, vencer y listar, cada una en una transaccion.
 *
 * <p>Las preguntas a {@code grupos} (quien es el titular, de que grupos es miembro quien mira)
 * ya se hicieron afuera. El turno se serializa con el MISMO candado que el fondeo del pozo:
 * publicar una oferta y fondear la entrega del mismo turno no se cruzan.
 */
@Service
public class RegistroDeOfertas {

    private final Datos datos;
    private final OfertaRepositorio ofertas;
    private final CesionRepositorio cesiones;
    private final FondeoRepositorio candados;
    private final Outbox outbox;
    private final Reloj reloj;

    public RegistroDeOfertas(
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

    public record Publicacion(
            HechosDeGrupos.Turno turno, Dinero precio, Dinero cargos, OffsetDateTime vigenteHasta, String clave) {}

    public record Publicada(UUID ofertaId, boolean esNueva) {}

    @Transactional
    public Publicada publicar(Publicacion p, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            candados.serializarTurno(dsl, p.turno().turnoId());

            var previa = ofertas.ofertaPorClave(dsl, p.clave());
            if (previa.isPresent()) {
                var o = previa.get();
                if (!o.turnoId().equals(p.turno().turnoId())
                        || !o.precio().equals(p.precio())
                        || !o.cargos().equals(p.cargos())
                        || !o.vendedorUsuarioId().equals(ctx.usuarioId())) {
                    throw new ErrorDeNegocio(CodigoError.de(130, 4), "Esa clave ya se uso para otra oferta distinta.");
                }
                return new Publicada(o.id(), false);
            }
            if (cesiones.hayEntrega(dsl, p.turno().turnoId())
                    || cesiones.vivaDelTurno(dsl, p.turno().turnoId()).isPresent()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(130, 2),
                        "Ese derecho ya se esta entregando o ya se cedio: no se vuelve a ofrecer.");
            }
            if (ofertas.activaDelTurno(dsl, p.turno().turnoId()).isPresent()) {
                throw new ErrorDeNegocio(CodigoError.de(130, 4), "Ese turno ya tiene una oferta activa.");
            }

            var t = p.turno();
            UUID id = ofertas.crearOferta(
                    dsl,
                    new Oferta(
                            null,
                            t.grupoId(),
                            t.turnoId(),
                            t.cupoId(),
                            t.participanteId(),
                            ctx.usuarioId(),
                            t.montoDelDerecho(),
                            p.precio(),
                            p.cargos(),
                            "PUBLICADA",
                            p.vigenteHasta(),
                            0),
                    p.clave(),
                    ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "entregas.oferta_publicada",
                            "oferta_turno",
                            id,
                            Map.of(
                                    "turnoId", t.turnoId().toString(),
                                    "precio", p.precio().toString(),
                                    "cargos", p.cargos().toString(),
                                    "moneda", p.precio().moneda().name()),
                            UUID.fromString(ctx.traza().id())));
            return new Publicada(id, true);
        });
    }

    /** Solo el dueño de la oferta, y solo mientras nadie la reservo. Cualquier otro caso: «no disponible». */
    @Transactional
    public boolean cancelar(UUID ofertaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var sinCandado = ofertas.ver(dsl, ofertaId);
            if (sinCandado.isEmpty() || !sinCandado.get().vendedorUsuarioId().equals(ctx.usuarioId())) {
                // No se distingue «no existe» de «no es tuya»: confirmarlo es regalar datos.
                throw new ErrorDeNegocio(CodigoError.de(130, 5), "Esa oferta no esta disponible.");
            }
            candados.serializarTurno(dsl, sinCandado.get().turnoId());
            var o = ofertas.bloquear(dsl, ofertaId).orElseThrow();
            if ("CANCELADA".equals(o.estado())) {
                return false;
            }
            if (!ofertas.mover(dsl, o, List.of("PUBLICADA"), "CANCELADA")) {
                throw new ErrorDeNegocio(
                        CodigoError.de(130, 5), "La oferta ya no se puede cancelar: esta " + o.estado() + ".");
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "entregas.oferta_cancelada",
                            "oferta_turno",
                            ofertaId,
                            Map.of("turnoId", o.turnoId().toString()),
                            UUID.fromString(ctx.traza().id())));
            return true;
        });
    }

    /** Las ofertas publicadas cuya vigencia paso. Una compra en curso no se vence. */
    @Transactional
    public int vencerLasVencidas(ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> ofertas.vencer(dsl, ahora));
    }

    @Transactional(readOnly = true)
    public List<OfertaVisible> listar(Set<UUID> gruposDelComprador, ContextoSesion ctx, int limite, int pagina) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        int tamano = Math.max(1, Math.min(limite, FILAS_POR_PAGINA));
        int desplazamiento = Math.max(0, pagina) * tamano;
        return datos.conContexto(
                ctx, dsl -> ofertas.listar(dsl, gruposDelComprador, ctx.usuarioId(), ahora, tamano, desplazamiento));
    }

    /** El listado siempre tiene techo (regla 96.6). */
    static final int FILAS_POR_PAGINA = 50;
}
