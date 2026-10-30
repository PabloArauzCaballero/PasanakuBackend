package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.PujaRepositorio;
import bo.aportaya.entregas.infraestructura.PujaRepositorio.Puja;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio.Subasta;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los pasos locales de la subasta del cupo: cada uno es una escritura corta con la subasta BLOQUEADA, y nunca llama a
 * otro servicio (invariante 6). Las llamadas a {@code grupos} y a la billetera las hace {@link SubastaDelCupo}, entre
 * paso y paso. Los eventos salen por el outbox, en la misma transaccion que el cambio.
 */
@Service
public class TransicionesDeSubasta {

    private final Datos datos;
    private final SubastaRepositorio subastas;
    private final PujaRepositorio pujas;
    private final Outbox outbox;
    private final Reloj reloj;

    public TransicionesDeSubasta(
            Datos datos, SubastaRepositorio subastas, PujaRepositorio pujas, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.subastas = subastas;
        this.pujas = pujas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    static ErrorDeNegocio error(int numero, String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(136, numero), mensaje);
    }

    static ErrorDeNegocio noDisponible() {
        return error(5, "Esa subasta no esta disponible.");
    }

    public record Abierta(Subasta subasta, boolean esNueva) {}

    // ------------------------------------------------------------------ lecturas (para la saga, que no abre
    // transaccion)

    @Transactional(readOnly = true)
    public Subasta ver(UUID subastaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> subastas.ver(dsl, subastaId))
                .orElseThrow(TransicionesDeSubasta::noDisponible);
    }

    @Transactional(readOnly = true)
    public Puja puja(UUID pujaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> pujas.puja(dsl, pujaId)).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<Puja> vigente(UUID subastaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> pujas.vigente(dsl, subastaId));
    }

    @Transactional(readOnly = true)
    public List<Puja> porDevolver(Optional<UUID> subastaId, int limite, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> pujas.porDevolver(dsl, subastaId, limite));
    }

    @Transactional(readOnly = true)
    public List<UUID> porAvanzar(int limite, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> subastas.porAvanzar(dsl, ahora(), limite));
    }

    @Transactional
    public Abierta abrir(Subasta nueva, String clave, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            subastas.serializarCupo(dsl, nueva.cupoId());
            var previa = subastas.porClave(dsl, clave);
            if (previa.isPresent()) {
                var p = previa.get();
                if (!p.cupoId().equals(nueva.cupoId())
                        || !p.base().equals(nueva.base())
                        || !p.vendedorUsuarioId().equals(nueva.vendedorUsuarioId())) {
                    throw error(3, "Esa clave ya se uso para otra subasta.");
                }
                return new Abierta(p, false);
            }
            if (subastas.vivaDelCupo(dsl, nueva.cupoId()).isPresent()) {
                throw error(2, "Ese cupo ya esta en subasta.");
            }
            UUID id = subastas.crear(dsl, nueva, clave, ahora);
            emitir(
                    dsl,
                    "entregas.subasta_abierta",
                    id,
                    Map.of(
                            "grupoId", nueva.grupoId().toString(),
                            "cupoId", nueva.cupoId().toString(),
                            "montoBase", nueva.base().toString(),
                            "moneda", nueva.base().moneda().name(),
                            "cierraEn", nueva.cierraEn().toString()),
                    ctx);
            return new Abierta(subastas.ver(dsl, id).orElseThrow(), true);
        });
    }

    /** Solo el saliente, y solo mientras nadie pujo: con pujas, alguien ya tiene su plata retenida por este cupo. */
    @Transactional
    public Subasta cancelar(UUID subastaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var s = subastas.bloquear(dsl, subastaId)
                    .filter(x -> x.vendedorUsuarioId().equals(ctx.usuarioId()))
                    .orElseThrow(TransicionesDeSubasta::noDisponible);
            if ("CANCELADA".equals(s.estado())) {
                return s;
            }
            if (pujas.cantidadDePujas(dsl, subastaId) > 0
                    || pujas.vigente(dsl, subastaId).isPresent()) {
                throw error(2, "Ya hay pujas: la subasta sigue hasta el cierre.");
            }
            if (!subastas.mover(dsl, s, "ABIERTA", "CANCELADA")) {
                throw error(5, "La subasta ya no se puede cancelar: esta " + s.estado() + ".");
            }
            emitir(
                    dsl,
                    "entregas.subasta_cancelada",
                    subastaId,
                    Map.of("cupoId", s.cupoId().toString()),
                    ctx);
            return subastas.ver(dsl, subastaId).orElseThrow();
        });
    }

    /**
     * Anota la puja como PENDIENTE (todavia sin plata retenida). La regla de monto se mira aca con la subasta bloqueada
     * y se vuelve a mirar al fijarla: entre una y otra alguien pudo ofrecer mas.
     */
    @Transactional
    public Puja anotarPuja(UUID subastaId, UUID postor, Dinero monto, String clave, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            var previa = pujas.pujaPorClave(dsl, clave);
            if (previa.isPresent()) {
                var p = previa.get();
                if (!p.subastaId().equals(subastaId)
                        || !p.postorUsuarioId().equals(postor)
                        || !p.monto().equals(monto)) {
                    throw error(3, "Esa clave ya se uso para otra puja.");
                }
                return p;
            }
            var s = subastas.bloquear(dsl, subastaId).orElseThrow(TransicionesDeSubasta::noDisponible);
            if (!"ABIERTA".equals(s.estado()) || !s.cierraEn().isAfter(ahora)) {
                throw error(5, "La subasta ya cerro.");
            }
            if (s.vendedorUsuarioId().equals(postor)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(136, 6),
                        "No podes pujar por tu propio cupo.",
                        Map.of("motivo", "ES_EL_SALIENTE"));
            }
            if (monto.moneda() != s.base().moneda() || monto.esMenorQue(s.base())) {
                throw error(3, "La puja tiene que ser de al menos " + s.base() + ".");
            }
            var vigente = pujas.vigente(dsl, subastaId);
            if (vigente.isPresent() && !monto.esMayorQue(vigente.get().monto())) {
                throw superada(vigente.get().monto());
            }
            UUID id = pujas.crearPuja(dsl, subastaId, postor, monto, clave, ahora);
            return pujas.puja(dsl, id).orElseThrow();
        });
    }

    /**
     * Con la plata ya retenida: si sigue siendo la mas alta y la subasta sigue abierta, queda VIGENTE y la anterior
     * pasa a SUPERADA (para devolverla); si no, la propia queda SUPERADA. Devuelve el estado en que quedo.
     */
    @Transactional
    public String fijarPuja(UUID pujaId, UUID retencionRef, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            var visible = pujas.puja(dsl, pujaId).orElseThrow();
            var s = subastas.bloquear(dsl, visible.subastaId()).orElseThrow();
            var p = pujas.puja(dsl, pujaId).orElseThrow();
            if (!"PENDIENTE".equals(p.estado())) {
                return p.estado();
            }
            var vigente = pujas.vigente(dsl, s.id());
            boolean gana = "ABIERTA".equals(s.estado())
                    && s.cierraEn().isAfter(ahora)
                    && vigente.map(v -> p.monto().esMayorQue(v.monto())).orElse(true);
            if (!gana) {
                pujas.moverPuja(dsl, p, List.of("PENDIENTE"), "SUPERADA", Optional.of(retencionRef));
                return "SUPERADA";
            }
            vigente.ifPresent(v -> {
                pujas.moverPuja(dsl, v, List.of("VIGENTE"), "SUPERADA", Optional.empty());
                emitir(
                        dsl,
                        "entregas.puja_superada",
                        s.id(),
                        Map.of(
                                "pujaId", v.id().toString(),
                                "postorUsuarioId", v.postorUsuarioId().toString(),
                                "grupoId", s.grupoId().toString()),
                        ctx);
            });
            pujas.moverPuja(dsl, p, List.of("PENDIENTE"), "VIGENTE", Optional.of(retencionRef));
            return "VIGENTE";
        });
    }

    /** La billetera no le pudo retener la plata (saldo insuficiente): la puja no existio. */
    @Transactional
    public void rechazarPuja(UUID pujaId, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var p = pujas.puja(dsl, pujaId).orElseThrow();
            pujas.moverPuja(dsl, p, List.of("PENDIENTE"), "RECHAZADA", Optional.empty());
            return null;
        });
    }

    @Transactional
    public void devuelta(UUID pujaId, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            var p = pujas.puja(dsl, pujaId).orElseThrow();
            pujas.moverPuja(dsl, p, List.of("SUPERADA"), "LIBERADA", Optional.empty());
            return null;
        });
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

    static ErrorDeNegocio superada(Dinero masAlto) {
        return new ErrorDeNegocio(
                CodigoError.de(136, 4),
                "Alguien ya ofrecio " + masAlto + ": tu puja tiene que ser mayor.",
                Map.of(
                        "montoMasAlto",
                        masAlto.monto().toPlainString(),
                        "moneda",
                        masAlto.moneda().name()));
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    /** Solo identificadores e importes del cupo, nunca datos de personas (regla 90.2.1). */
    private void emitir(
            org.jooq.DSLContext dsl, String tipo, UUID subastaId, Map<String, String> datos, ContextoSesion ctx) {
        Map<String, String> carga = new HashMap<>(datos);
        carga.put("subastaId", subastaId.toString());
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        "subasta_cupo",
                        subastaId,
                        Map.copyOf(carga),
                        UUID.fromString(ctx.traza().id())));
    }
}
