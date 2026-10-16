package bo.aportaya.organizador.aplicacion;

import bo.aportaya.organizador.dominio.DecisionDeHabilitacion;
import bo.aportaya.organizador.dominio.ExpedienteDeHabilitacion;
import bo.aportaya.organizador.dominio.NivelDeOrganizador;
import bo.aportaya.organizador.dominio.RequisitosDeHabilitacion;
import bo.aportaya.organizador.infraestructura.HabilitacionRepositorio;
import bo.aportaya.organizador.infraestructura.OrganizadorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-90 · Bandeja del backoffice y resolución humana de la habilitación de un administrador.
 *
 * <p>Manda el backoffice y manda una persona: el sistema evalúa los requisitos y lo dice, pero no
 * rechaza solo. Aprobar exige que los requisitos obligatorios consten; rechazar es siempre una
 * decisión humana con motivo, que se conserva (no es una excepción que revierta la transacción).
 * Reintentar con la misma clave devuelve la decisión original; con otro contenido, se rechaza.
 * Revisar una resolución es agregar otra (fase REVISION): lo resuelto nunca se reescribe, y revocar
 * la habilitación no toca los grupos que el organizador ya administra, para que sus obligaciones
 * sigan identificadas (la continuidad la gobierna la sustitución de administrador de {@code grupos}).
 */
@Service
public class CU90ResolverHabilitacion {
    private final Datos datos;
    private final OrganizadorRepositorio organizadores;
    private final HabilitacionRepositorio habilitaciones;
    private final Outbox outbox;
    private final Reloj reloj;
    private final int limiteInicialDeGrupos;
    private final BigDecimal limiteInicialDeMonto;

    public CU90ResolverHabilitacion(
            Datos datos,
            OrganizadorRepositorio organizadores,
            HabilitacionRepositorio habilitaciones,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.organizador.limite-inicial-de-grupos}") int limiteInicialDeGrupos,
            @Value("${aportaya.organizador.limite-inicial-de-monto}") BigDecimal limiteInicialDeMonto) {
        this.datos = datos;
        this.organizadores = organizadores;
        this.habilitaciones = habilitaciones;
        this.outbox = outbox;
        this.reloj = reloj;
        this.limiteInicialDeGrupos = limiteInicialDeGrupos;
        this.limiteInicialDeMonto = limiteInicialDeMonto;
    }

    /** Primera resolución humana: aprobar o rechazar con motivo. */
    @Transactional
    public Resultado resolver(Entrada entrada, ContextoSesion ctx) {
        Backoffice.exigir(ctx);
        validar(entrada, List.of("APROBAR", "RECHAZAR"));
        return datos.conContexto(ctx, dsl -> {
            var solicitud = habilitaciones
                    .bloquear(dsl, entrada.solicitudId())
                    .orElseThrow(() -> error(3, "Esa solicitud no existe."));
            var repetida = repetida(dsl, entrada, "RESOLUCION", ctx);
            if (repetida != null) return repetida;
            if (solicitud.usuarioId().equals(ctx.usuarioId())) {
                throw error(4, "Quien aprueba una postulacion no puede ser quien la presento.");
            }
            if (!List.of("PENDIENTE", "EN_REVISION").contains(solicitud.estado())) {
                throw error(3, "Esa solicitud ya fue resuelta; usa la revision.");
            }
            if (solicitud.revision() != entrada.revisionEsperada()) {
                throw error(8, "La solicitud cambio; actualiza el expediente.");
            }
            var ahora = reloj.ahora().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
            var veredicto = RequisitosDeHabilitacion.evaluar(
                    organizadores.requisitosDe(dsl, NivelDeOrganizador.APRENDIZ.name()), entrada.medidos());
            UUID organizadorId = null;
            if ("APROBAR".equals(entrada.decision())) {
                // Sin dato o por debajo del minimo no se aprueba: el sistema no habilita por omision. Tampoco
                // rechaza por su cuenta: la persona del backoffice decide rechazar, con motivo, si corresponde.
                if (!veredicto.habilitable()) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(90, 5),
                            "No cumple los requisitos de habilitacion.",
                            Map.of("faltantes", veredicto.faltantes().toString()));
                }
                if (!habilitaciones.cerrar(dsl, solicitud.solicitudId(), "APROBADA", ctx.usuarioId(), null, ahora)) {
                    throw error(8, "La solicitud cambio; actualiza el expediente.");
                }
                organizadorId = organizadores.crear(
                        dsl,
                        solicitud.usuarioId(),
                        "CAPACITACION_PENDIENTE",
                        NivelDeOrganizador.APRENDIZ.name(),
                        limiteInicialDeGrupos,
                        limiteInicialDeMonto,
                        ahora);
            } else {
                if (!habilitaciones.cerrar(
                        dsl, solicitud.solicitudId(), "RECHAZADA", ctx.usuarioId(), entrada.motivo(), ahora)) {
                    throw error(8, "La solicitud cambio; actualiza el expediente.");
                }
            }
            var decision = nueva(entrada, "RESOLUCION", solicitud.revision() + 1, organizadorId, veredicto, ahora, ctx);
            habilitaciones.guardar(dsl, decision);
            emitir(dsl, "organizador.habilitacion_resuelta", solicitud, decision, ctx);
            if (organizadorId != null) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "organizador.postulacion_aprobada",
                                "organizador",
                                organizadorId,
                                Map.of(
                                        "usuarioId", solicitud.usuarioId().toString(),
                                        "estado", "CAPACITACION_PENDIENTE",
                                        "nivel", NivelDeOrganizador.APRENDIZ.name()),
                                UUID.fromString(ctx.traza().id())));
            }
            return new Resultado(decision, true);
        });
    }

    /**
     * Revisión posterior de una resolución aprobada: confirmarla o revocarla. Revocar suspende al
     * organizador (no crea grupos nuevos) y avisa para que se sustituya al administrador de los grupos
     * vigentes; no borra ni reescribe nada.
     */
    @Transactional
    public Resultado revisar(Entrada entrada, ContextoSesion ctx) {
        Backoffice.exigir(ctx);
        validar(entrada, List.of("CONFIRMAR", "REVOCAR"));
        return datos.conContexto(ctx, dsl -> {
            var solicitud = habilitaciones
                    .bloquear(dsl, entrada.solicitudId())
                    .orElseThrow(() -> error(3, "Esa solicitud no existe."));
            var repetida = repetida(dsl, entrada, "REVISION", ctx);
            if (repetida != null) return repetida;
            if (!"APROBADA".equals(solicitud.estado())) {
                throw error(3, "Solo se revisa una habilitacion aprobada.");
            }
            if (solicitud.revision() != entrada.revisionEsperada()) {
                throw error(8, "La solicitud cambio; actualiza el expediente.");
            }
            var organizador = organizadores
                    .porUsuario(dsl, solicitud.usuarioId())
                    .flatMap(o -> organizadores.bloquear(dsl, o.id()))
                    .orElseThrow(() -> error(3, "No hay organizador para esa solicitud."));
            var ahora = reloj.ahora().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
            if ("REVOCAR".equals(entrada.decision())
                    && !"SUSPENDIDO".equals(organizador.estado())
                    && !organizadores.cambiarEstado(
                            dsl, organizador.id(), "SUSPENDIDO", organizador.version(), ahora)) {
                throw error(8, "Otro cambio movio al organizador primero: reintenta.");
            }
            var decision = nueva(
                    entrada,
                    "REVISION",
                    solicitud.revision() + 1,
                    organizador.id(),
                    new RequisitosDeHabilitacion.Veredicto(true, List.of()),
                    ahora,
                    ctx);
            habilitaciones.guardar(dsl, decision);
            emitir(
                    dsl,
                    "REVOCAR".equals(entrada.decision())
                            ? "organizador.habilitacion_revocada"
                            : "organizador.habilitacion_confirmada",
                    solicitud,
                    decision,
                    ctx);
            return new Resultado(decision, true);
        });
    }

    /** Reintento con la misma clave: la decisión original si el contenido coincide; error si es otro. */
    private Resultado repetida(org.jooq.DSLContext dsl, Entrada entrada, String fase, ContextoSesion ctx) {
        var previa = habilitaciones.porClave(dsl, entrada.clave());
        if (previa.isEmpty()) return null;
        var d = previa.get();
        if (!d.solicitudId().equals(entrada.solicitudId())
                || !d.actorId().equals(ctx.usuarioId())
                || !d.fase().equals(fase)
                || !d.decision().equals(entrada.decision())
                || !d.motivo().equals(entrada.motivo())
                || d.revision() != entrada.revisionEsperada() + 1) {
            throw error(8, "La clave de idempotencia pertenece a otra decision.");
        }
        return new Resultado(d, false);
    }

    private DecisionDeHabilitacion nueva(
            Entrada entrada,
            String fase,
            int revision,
            UUID organizadorId,
            RequisitosDeHabilitacion.Veredicto veredicto,
            OffsetDateTime ahora,
            ContextoSesion ctx) {
        String evidencia = "habilitable=" + veredicto.habilitable() + ";faltantes="
                + veredicto.faltantes().stream()
                        .map(f -> f.codigo() + ":" + f.motivo())
                        .collect(Collectors.joining("|"));
        return new DecisionDeHabilitacion(
                UUID.randomUUID(),
                entrada.solicitudId(),
                entrada.clave(),
                fase,
                entrada.decision(),
                ctx.usuarioId(),
                entrada.motivo(),
                revision,
                organizadorId,
                evidencia,
                ahora,
                UUID.fromString(ctx.traza().id()));
    }

    private void emitir(
            org.jooq.DSLContext dsl,
            String tipo,
            ExpedienteDeHabilitacion solicitud,
            DecisionDeHabilitacion decision,
            ContextoSesion ctx) {
        var carga = new java.util.LinkedHashMap<String, Object>();
        carga.put("decisionId", decision.id().toString());
        carga.put("usuarioId", solicitud.usuarioId().toString());
        carga.put("resultado", decision.decision());
        if (decision.organizadorId() != null)
            carga.put("organizadorId", decision.organizadorId().toString());
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo, "solicitud_organizador", solicitud.solicitudId(), carga, decision.correlacionId()));
    }

    private void validar(Entrada entrada, List<String> permitidas) {
        if (entrada.clave() == null
                || entrada.solicitudId() == null
                || entrada.revisionEsperada() < 0
                || entrada.motivo() == null
                || entrada.motivo().isBlank()
                || entrada.motivo().length() > 1000
                || !permitidas.contains(entrada.decision())
                || entrada.medidos() == null) {
            throw error(7, "La decision requiere clave, revision, resultado y motivo explicitos.");
        }
    }

    private static ErrorDeNegocio error(int numero, String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(90, numero), mensaje);
    }

    public record Entrada(
            UUID solicitudId,
            UUID clave,
            String decision,
            String motivo,
            int revisionEsperada,
            Map<String, BigDecimal> medidos) {}

    /** {@code nueva} falso significa que fue un reintento y se devolvió la decisión original. */
    public record Resultado(DecisionDeHabilitacion decision, boolean nueva) {}
}
