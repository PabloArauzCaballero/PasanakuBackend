package bo.aportaya.entregas.aplicacion;

import static bo.aportaya.entregas.aplicacion.TransicionesDeSubasta.error;

import bo.aportaya.entregas.infraestructura.PujaRepositorio;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los pasos locales del cierre de la subasta: adjudicar (o dejarla DESIERTA), anotar que {@code grupos} paso el
 * cupo, que la billetera pago, o que fallo. Como {@link TransicionesDeSubasta}: escrituras cortas con la subasta
 * BLOQUEADA, sin llamadas a otros servicios, y el evento en la misma transaccion.
 */
@Service
public class TransicionesDelCierre {

    private final Datos datos;
    private final SubastaRepositorio subastas;
    private final PujaRepositorio pujas;
    private final Outbox outbox;
    private final Reloj reloj;

    public TransicionesDelCierre(
            Datos datos, SubastaRepositorio subastas, PujaRepositorio pujas, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.subastas = subastas;
        this.pujas = pujas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** Al vencer el plazo: la mas alta gana (ADJUDICADA); sin pujas, la subasta queda DESIERTA y nada se mueve. */
    @Transactional
    public void cerrar(UUID subastaId, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        datos.conContexto(ctx, dsl -> {
            var s = subastas.bloquear(dsl, subastaId).orElseThrow();
            if (!"ABIERTA".equals(s.estado()) || s.cierraEn().isAfter(ahora)) {
                return null;
            }
            var vigente = pujas.vigente(dsl, subastaId);
            if (vigente.isEmpty()) {
                subastas.mover(dsl, s, "ABIERTA", "DESIERTA");
                emitir(
                        dsl,
                        "entregas.subasta_desierta",
                        subastaId,
                        Map.of("cupoId", s.cupoId().toString()),
                        ctx);
                return null;
            }
            var ganadora = vigente.get();
            if (!subastas.adjudicar(dsl, s, ganadora)
                    || !pujas.moverPuja(dsl, ganadora, List.of("VIGENTE"), "GANADORA", Optional.empty())) {
                throw error(9, "La subasta cambio mientras se cerraba.");
            }
            emitir(
                    dsl,
                    "entregas.subasta_adjudicada",
                    subastaId,
                    Map.of(
                            "grupoId", s.grupoId().toString(),
                            "cupoId", s.cupoId().toString(),
                            "ganadorUsuarioId", ganadora.postorUsuarioId().toString(),
                            "monto", ganadora.monto().toString(),
                            "moneda", ganadora.monto().moneda().name()),
                    ctx);
            return null;
        });
    }

    @Transactional
    public void traspasada(UUID subastaId, UUID traspasoRef, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var s = subastas.bloquear(dsl, subastaId).orElseThrow();
            if ("ADJUDICADA".equals(s.estado()) && !subastas.traspasada(dsl, s, traspasoRef)) {
                throw error(9, "La subasta cambio mientras se traspasaba el cupo.");
            }
            return null;
        });
    }

    @Transactional
    public void vendida(UUID subastaId, UUID liquidacionRef, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var s = subastas.bloquear(dsl, subastaId).orElseThrow();
            if (!"LIQUIDANDO".equals(s.estado())) {
                return null;
            }
            if (!subastas.vendida(dsl, s, liquidacionRef)) {
                throw error(9, "La subasta cambio mientras se liquidaba.");
            }
            emitir(
                    dsl,
                    "entregas.subasta_vendida",
                    subastaId,
                    Map.of(
                            "grupoId", s.grupoId().toString(),
                            "cupoId", s.cupoId().toString(),
                            "monto", s.ganador().orElseThrow().toString(),
                            "moneda", s.base().moneda().name(),
                            "liquidacionRef", liquidacionRef.toString()),
                    ctx);
            return null;
        });
    }

    /** {@code grupos} no pudo pasar el cupo: la subasta falla y la plata del ganador queda para devolver. */
    @Transactional
    public void fallida(UUID subastaId, String motivo, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var s = subastas.bloquear(dsl, subastaId).orElseThrow();
            if (!"ADJUDICADA".equals(s.estado())) {
                return null;
            }
            var ganadora = pujas.puja(dsl, s.pujaGanadoraId().orElseThrow()).orElseThrow();
            if (!subastas.fallida(dsl, s, motivo)
                    || !pujas.moverPuja(dsl, ganadora, List.of("GANADORA"), "SUPERADA", Optional.empty())) {
                throw error(9, "La subasta cambio mientras se deshacia.");
            }
            emitir(
                    dsl,
                    "entregas.subasta_fallida",
                    subastaId,
                    Map.of("cupoId", s.cupoId().toString()),
                    ctx);
            return null;
        });
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    private void emitir(
            org.jooq.DSLContext dsl, String tipo, UUID subastaId, Map<String, String> datos, ContextoSesion ctx) {
        EventosDeSubasta.emitir(outbox, dsl, tipo, subastaId, datos, ctx);
    }
}
