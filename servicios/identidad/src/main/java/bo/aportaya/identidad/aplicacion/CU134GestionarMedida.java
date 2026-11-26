package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.CausalDeMedida;
import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.dominio.PlazosDeMedida;
import bo.aportaya.identidad.dominio.RegistroDeMedida;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Apelacion;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Expediente;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-134 · suspension o bloqueo de la cuenta del participante con debido proceso (regla 91.4).
 *
 * <p>Causal cerrada (D-1) → notificacion probada que hace correr el plazo (R-GAR-01 como criterio) →
 * descargo → decision motivada de OTRA persona → apelacion unica resuelta por una TERCERA (R-SEG-13) →
 * firmeza, prescripcion o reversion. La base repite la segregacion y la unicidad (R-SEG-13..15); la
 * evidencia y el historial son append-only (R-AUD-01). Cada cambio emite su evento en la misma
 * transaccion (R-AUD-04). La medida nunca mueve dinero: la retencion de la entrega la hace
 * {@code entregas} consultando la medida vigente (D-11).
 */
@Service
public class CU134GestionarMedida {

    /** Estados de la cuenta que una medida puede afectar: los que admite {@code estado_cuenta_previo}. */
    private static final Set<String> AFECTABLES =
            Set.of("ACTIVO", "INCOMPLETO_KYC", "PENDIENTE_VERIFICACION", "PRE_REGISTRO");

    private final Datos datos;
    private final MedidasDeCuentaRepositorio medidas;
    private final AccesosRepositorio accesos;
    private final EfectosDeMedida efectos;
    private final Reloj reloj;

    public CU134GestionarMedida(
            Datos datos,
            MedidasDeCuentaRepositorio medidas,
            AccesosRepositorio accesos,
            EvidenciaDeStepUp evidencias,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.medidas = medidas;
        this.accesos = accesos;
        this.efectos = new EfectosDeMedida(medidas, accesos, evidencias, outbox);
        this.reloj = reloj;
    }

    public record Evidencia(String descripcion, UUID referenciaId) {}

    public record Propuesta(
            String causal,
            String medida,
            boolean cautelar,
            String motivo,
            List<Evidencia> evidencias,
            Optional<OffsetDateTime> vigenteHasta) {}

    public record Vista(
            Expediente expediente,
            List<RegistroDeMedida.Hito> lineaDeTiempo,
            Optional<Apelacion> apelacion,
            String estadoCuenta) {}

    @Transactional
    public Vista proponer(UUID usuarioId, Propuesta p, String clave, ContextoSesion ctx) {
        CU134ConsultarParticipante.noSobreSiMismo(ctx, usuarioId);
        CausalDeMedida causal = CausalDeMedida.del(p.causal())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(134, 4),
                        "Esa causal no esta en el catalogo: la mora, una orden de autoridad o una sospecha de"
                                + " lavado van por su propio caso."));
        boolean bloqueo = "BLOQUEO_CAUTELAR".equals(p.medida());
        if ((p.cautelar() || bloqueo) && !causal.admiteCautelar()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(134, 5), "Esta causal no admite medida inmediata: primero se escucha el descargo.");
        }
        boolean cautelar = p.cautelar() || bloqueo;
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Optional<UUID> repetida = medidas.porClave(dsl, clave);
            if (repetida.isPresent()) {
                return efectos.vista(dsl, repetida.get());
            }
            String previo = medidas.estadoDeCuenta(dsl, usuarioId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(134, 6), "No existe ese participante."));
            if (medidas.hayAbierto(dsl, usuarioId, causal.name()) || !AFECTABLES.contains(previo)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(134, 6),
                        "Ya hay un expediente abierto sobre esta cuenta: agrega la evidencia nueva ahi.");
            }
            var politica = medidas.politicaVigente(dsl, ahora)
                    .orElseThrow(() ->
                            new IllegalStateException("Sin politica_medida_cuenta vigente: se deniega por omision"));
            OffsetDateTime tope = ahora.plusDays(politica.plazos().suspensionMaximaDias());
            OffsetDateTime vigenteHasta = p.vigenteHasta()
                    .filter(h -> PlazosDeMedida.dentroDelMaximo(ahora, h, politica.plazos()))
                    .orElse(tope);
            String estado = cautelar ? "CAUTELAR" : "NOTIFICACION_PENDIENTE";
            UUID id = medidas.abrir(
                    dsl,
                    usuarioId,
                    politica.id(),
                    causal.name(),
                    p.medida(),
                    cautelar,
                    p.motivo(),
                    estado,
                    previo,
                    ctx.usuarioId(),
                    ahora,
                    PlazosDeMedida.prescripcion(ahora, politica.plazos()),
                    vigenteHasta,
                    clave);
            medidas.primerHito(dsl, id, estado, ctx.usuarioId(), ahora);
            for (Evidencia ev : p.evidencias()) {
                medidas.agregarEvidencia(
                        dsl, id, "EVIDENCIA_OPERADOR", ev.descripcion(), ev.referenciaId(), ctx.usuarioId(), ahora);
            }
            if (cautelar) {
                efectos.aplicarMedida(dsl, usuarioId, causal, p.medida(), ahora);
            }
            efectos.emitir(dsl, "identidad.medida_cuenta_propuesta", id, usuarioId, ctx);
            return efectos.vista(dsl, id);
        });
    }

    /** El acuse de entrega hace correr el plazo de descargo, que se guarda y no se recalcula. */
    @Transactional
    public Vista notificar(UUID expedienteId, OffsetDateTime entregadaEn, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Expediente e = efectos.abierto(dsl, expedienteId);
            if (e.notificadaEn() == null) {
                efectos.noPrescrito(e, ahora);
                var plazos = medidas.politica(dsl, e.politicaId()).plazos();
                OffsetDateTime limite =
                        PlazosDeMedida.limiteDescargo(entregadaEn, plazos, efectos.calendario(dsl, entregadaEn));
                medidas.marcarNotificada(dsl, e.id(), entregadaEn, limite);
                medidas.transicion(dsl, e, "EN_DESCARGO", ctx.usuarioId(), "Aviso entregado", ahora);
            }
            return efectos.vista(dsl, expedienteId);
        });
    }

    @Transactional
    public Vista descargo(UUID expedienteId, String argumento, List<Evidencia> aportadas, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Expediente e = efectos.abierto(dsl, expedienteId);
            efectos.soloElTitular(e, ctx);
            efectos.noPrescrito(e, ahora);
            medidas.agregarEvidencia(dsl, e.id(), "DESCARGO", argumento, null, ctx.usuarioId(), ahora);
            for (Evidencia ev : aportadas) {
                medidas.agregarEvidencia(
                        dsl, e.id(), "EVIDENCIA_AFECTADO", ev.descripcion(), ev.referenciaId(), ctx.usuarioId(), ahora);
            }
            return efectos.vista(dsl, expedienteId);
        });
    }

    @Transactional
    public Vista decidir(
            UUID expedienteId, String resultado, String fundamento, String evidenciaMfa, ContextoSesion ctx) {
        efectos.exigirSegundoFactor(ctx, evidenciaMfa);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Expediente e = efectos.abierto(dsl, expedienteId);
            CU134ConsultarParticipante.noSobreSiMismo(ctx, e.usuarioId());
            if (ctx.usuarioId().equals(e.propuestaPor())) {
                throw new ErrorDeNegocio(CodigoError.de(134, 8), "Quien propuso la medida no la decide (R-SEG-13).");
            }
            efectos.noPrescrito(e, ahora);
            if (e.notificadaEn() == null
                    || (ahora.isBefore(e.fechaLimiteDescargo()) && !medidas.tieneDescargo(dsl, e.id()))) {
                throw new ErrorDeNegocio(
                        CodigoError.de(134, 7),
                        "Todavia corre el plazo de descargo: no se decide sin escuchar a la persona.");
            }
            var plazos = medidas.politica(dsl, e.politicaId()).plazos();
            OffsetDateTime limite = PlazosDeMedida.limiteApelacion(ahora, plazos, efectos.calendario(dsl, ahora));
            medidas.decidir(dsl, e.id(), ctx.usuarioId(), resultado, fundamento, ahora, limite);
            if ("DESESTIMADA".equals(resultado)) {
                medidas.transicion(dsl, e, "DESESTIMADA", ctx.usuarioId(), "Decision: desestimada", ahora);
                efectos.revertir(dsl, e, ctx.usuarioId(), ahora);
            } else {
                medidas.transicion(dsl, e, "DECIDIDA", ctx.usuarioId(), "Decision: confirmada", ahora);
            }
            efectos.emitir(dsl, "identidad.medida_cuenta_decidida", e.id(), e.usuarioId(), ctx);
            return efectos.vista(dsl, expedienteId);
        });
    }

    @Transactional
    public Vista apelar(UUID expedienteId, String argumento, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Expediente e = efectos.abierto(dsl, expedienteId);
            efectos.soloElTitular(e, ctx);
            if (medidas.apelacion(dsl, e.id()).isPresent()) {
                throw new ErrorDeNegocio(CodigoError.de(134, 9), "Ya presentaste la apelacion de esta medida.");
            }
            if (!"DECIDIDA".equals(e.estado()) || ahora.isAfter(e.fechaLimiteApelacion())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(134, 10), "El plazo de apelacion de esta medida ya no esta abierto.");
            }
            OffsetDateTime limite = efectos.limiteResolucionApelacion(dsl, e, ahora);
            medidas.apelar(dsl, e.id(), ctx.usuarioId(), argumento, ahora, limite);
            medidas.transicion(dsl, e, "EN_APELACION", ctx.usuarioId(), "Apelacion presentada", ahora);
            efectos.emitir(dsl, "identidad.medida_cuenta_apelada", e.id(), e.usuarioId(), ctx);
            return efectos.vista(dsl, expedienteId);
        });
    }

    @Transactional
    public Vista resolver(
            UUID expedienteId, String resultado, String resolucion, String evidenciaMfa, ContextoSesion ctx) {
        efectos.exigirSegundoFactor(ctx, evidenciaMfa);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Expediente e = efectos.abierto(dsl, expedienteId);
            CU134ConsultarParticipante.noSobreSiMismo(ctx, e.usuarioId());
            if (ctx.usuarioId().equals(e.propuestaPor()) || ctx.usuarioId().equals(e.decididaPor())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(134, 8),
                        "La apelacion la resuelve quien no propuso ni decidio la medida (R-SEG-13).");
            }
            var apelacion = medidas.apelacion(dsl, e.id())
                    .filter(a -> "PRESENTADA".equals(a.estado()))
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(134, 9), "No hay apelacion pendiente."));
            if (efectos.vencioApelacion(apelacion, ahora)) {
                efectos.aceptarPorVencimiento(dsl, e, apelacion, ctx, ahora);
                return efectos.vista(dsl, expedienteId);
            }
            medidas.resolverApelacion(dsl, apelacion.id(), resultado, ctx.usuarioId(), resolucion, ahora);
            if ("ACEPTADA".equals(resultado)) {
                medidas.transicion(dsl, e, "REVOCADA", ctx.usuarioId(), "Apelacion aceptada", ahora);
                efectos.revertir(dsl, e, ctx.usuarioId(), ahora);
                efectos.emitir(dsl, "identidad.medida_cuenta_revocada", e.id(), e.usuarioId(), ctx);
            } else {
                efectos.quedaFirme(dsl, e, ctx, ahora);
            }
            return efectos.vista(dsl, expedienteId);
        });
    }

    /** Operador con permiso de lectura, o el propio titular. */
    @Transactional(readOnly = true)
    public Optional<Vista> consultar(UUID expedienteId, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(
                ctx, dsl -> medidas.consultar(dsl, expedienteId).map(e -> {
                    if (!e.usuarioId().equals(ctx.usuarioId())
                            && !PermisosEfectivos.de(accesos.asignacionesDe(dsl, ctx.usuarioId()), ahora)
                                    .contains("PARTICIPANTES_LEER")) {
                        throw new AccessDeniedException("expediente ajeno");
                    }
                    return efectos.vista(dsl, expedienteId);
                }));
    }

    /** Trabajo diario: aplica las transiciones cuyo plazo venció y devuelve el tamaño del lote procesado. */
    @Transactional
    public int vencer(ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            var expedientes = medidas.abiertosConPlazoVencido(dsl, ahora);
            for (Expediente e : expedientes) {
                efectos.vencerExpediente(dsl, e, ctx, ahora);
            }
            return expedientes.size();
        });
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}
