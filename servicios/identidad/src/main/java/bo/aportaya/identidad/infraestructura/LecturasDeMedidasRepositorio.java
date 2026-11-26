package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.APELACION_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EVIDENCIA_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EXPEDIENTE_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.HISTORIAL_ESTADO_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.POLITICA_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import bo.aportaya.identidad.dominio.PlazosDeMedida;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Apelacion;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Expediente;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Hito;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio.PoliticaVigente;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

/**
 * CU-134 · las consultas del expediente de una medida sobre la cuenta. {@link MedidasDeCuentaRepositorio}
 * las hereda y agrega las escrituras: quien usa el repositorio ve una sola API.
 */
public abstract class LecturasDeMedidasRepositorio {

    public Optional<PoliticaVigente> politicaVigente(DSLContext dsl, OffsetDateTime ahora) {
        return dsl.selectFrom(POLITICA_MEDIDA_CUENTA)
                .where(POLITICA_MEDIDA_CUENTA.VIGENTE_DESDE.le(ahora))
                .orderBy(POLITICA_MEDIDA_CUENTA.VIGENTE_DESDE.desc())
                .limit(1)
                .fetchOptional()
                .map(LecturasDeMedidasRepositorio::politica);
    }

    public PoliticaVigente politica(DSLContext dsl, UUID politicaId) {
        return politica(dsl.selectFrom(POLITICA_MEDIDA_CUENTA)
                .where(POLITICA_MEDIDA_CUENTA.ID.eq(politicaId))
                .fetchSingle());
    }

    private static PoliticaVigente politica(Record r) {
        return new PoliticaVigente(
                r.get(POLITICA_MEDIDA_CUENTA.ID),
                new PlazosDeMedida.Politica(
                        r.get(POLITICA_MEDIDA_CUENTA.PLAZO_DESCARGO_DIAS_HABILES),
                        r.get(POLITICA_MEDIDA_CUENTA.PLAZO_APELACION_DIAS_HABILES),
                        r.get(POLITICA_MEDIDA_CUENTA.PRESCRIBE_EN_MESES),
                        r.get(POLITICA_MEDIDA_CUENTA.DURACION_MAXIMA_SUSPENSION_DIAS)),
                Optional.ofNullable(r.get(POLITICA_MEDIDA_CUENTA.PLAZO_RESOLUCION_APELACION_DIAS_HABILES))
                        .map(Short::intValue));
    }

    public Optional<UUID> porClave(DSLContext dsl, String clave) {
        return dsl.select(EXPEDIENTE_MEDIDA_CUENTA.ID)
                .from(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.CLAVE_IDEMPOTENCIA.eq(clave))
                .fetchOptional(EXPEDIENTE_MEDIDA_CUENTA.ID);
    }

    public boolean hayAbierto(DSLContext dsl, UUID usuarioId, String causal) {
        return dsl.fetchExists(
                EXPEDIENTE_MEDIDA_CUENTA,
                EXPEDIENTE_MEDIDA_CUENTA
                        .USUARIO_ID
                        .eq(usuarioId)
                        .and(EXPEDIENTE_MEDIDA_CUENTA.CAUSAL.eq(causal))
                        .and(EXPEDIENTE_MEDIDA_CUENTA.CERRADA_EN.isNull()));
    }

    public boolean tieneDescargo(DSLContext dsl, UUID expedienteId) {
        return dsl.fetchExists(
                EVIDENCIA_MEDIDA_CUENTA,
                EVIDENCIA_MEDIDA_CUENTA
                        .EXPEDIENTE_MEDIDA_CUENTA_ID
                        .eq(expedienteId)
                        .and(EVIDENCIA_MEDIDA_CUENTA.TIPO.eq("DESCARGO")));
    }

    public Optional<Expediente> consultar(DSLContext dsl, UUID id) {
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .fetchOptional()
                .map(LecturasDeMedidasRepositorio::expediente);
    }

    /** Bloquea la fila para la transicion: dos decisiones simultaneas no pasan las dos. */
    public Optional<Expediente> paraActualizar(DSLContext dsl, UUID id) {
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .forUpdate()
                .fetchOptional()
                .map(LecturasDeMedidasRepositorio::expediente);
    }

    public List<Expediente> deUsuario(DSLContext dsl, UUID usuarioId) {
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.USUARIO_ID.eq(usuarioId))
                .orderBy(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_EN.desc())
                .limit(100)
                .fetch(LecturasDeMedidasRepositorio::expediente);
    }

    /** Toma solo filas con una transición disponible, sin esperar el lote de otra réplica. */
    public List<Expediente> abiertosConPlazoVencido(DSLContext dsl, OffsetDateTime ahora) {
        var apelacion = DSL.selectOne()
                .from(APELACION_MEDIDA_CUENTA)
                .where(APELACION_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID.eq(EXPEDIENTE_MEDIDA_CUENTA.ID));
        var apelacionVencida = DSL.exists(dsl.selectOne()
                .from(APELACION_MEDIDA_CUENTA)
                .where(APELACION_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID.eq(EXPEDIENTE_MEDIDA_CUENTA.ID))
                .and(APELACION_MEDIDA_CUENTA.ESTADO.eq("PRESENTADA"))
                .and(APELACION_MEDIDA_CUENTA.FECHA_LIMITE_RESOLUCION.lt(ahora)));
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.CERRADA_EN.isNull())
                .and(EXPEDIENTE_MEDIDA_CUENTA
                        .RESULTADO_DECISION
                        .isNull()
                        .and(EXPEDIENTE_MEDIDA_CUENTA.PRESCRIBE_EN.le(ahora))
                        .or(EXPEDIENTE_MEDIDA_CUENTA
                                .ESTADO
                                .eq("DECIDIDA")
                                .and(EXPEDIENTE_MEDIDA_CUENTA.FECHA_LIMITE_APELACION.lt(ahora))
                                .and(DSL.notExists(apelacion)))
                        .or(EXPEDIENTE_MEDIDA_CUENTA
                                .ESTADO
                                .eq("FIRME")
                                .and(EXPEDIENTE_MEDIDA_CUENTA.VIGENTE_HASTA.le(ahora)))
                        .or(EXPEDIENTE_MEDIDA_CUENTA.ESTADO.eq("EN_APELACION").and(apelacionVencida)))
                .orderBy(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_EN, EXPEDIENTE_MEDIDA_CUENTA.ID)
                .limit(500)
                .forUpdate()
                .skipLocked()
                .fetch(LecturasDeMedidasRepositorio::expediente);
    }

    public List<Hito> lineaDeTiempo(DSLContext dsl, UUID expedienteId) {
        return dsl.selectFrom(HISTORIAL_ESTADO_MEDIDA_CUENTA)
                .where(HISTORIAL_ESTADO_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID.eq(expedienteId))
                .orderBy(HISTORIAL_ESTADO_MEDIDA_CUENTA.OCURRIDO_EN, HISTORIAL_ESTADO_MEDIDA_CUENTA.ID)
                .limit(100)
                .fetch(r -> new Hito(r.getEstadoAnterior(), r.getEstadoNuevo(), r.getMotivo(), r.getOcurridoEn()));
    }

    /** La medida vigente que retiene la entrega (D-11): abierta y con la cuenta afectada. */
    public Optional<UUID> vigenteQueRetiene(DSLContext dsl, UUID usuarioId, OffsetDateTime ahora) {
        return dsl.select(EXPEDIENTE_MEDIDA_CUENTA.ID)
                .from(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.USUARIO_ID.eq(usuarioId))
                .and(enCurso(ahora).or(firmeSinLiberar()))
                .orderBy(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_EN)
                .limit(1)
                .fetchOptional(EXPEDIENTE_MEDIDA_CUENTA.ID);
    }

    /** Abierta, vigente y aplicada: firme, o cautelar mientras corre el debido proceso. */
    private static Condition enCurso(OffsetDateTime ahora) {
        return EXPEDIENTE_MEDIDA_CUENTA
                .CERRADA_EN
                .isNull()
                .and(EXPEDIENTE_MEDIDA_CUENTA
                        .VIGENTE_HASTA
                        .isNull()
                        .or(EXPEDIENTE_MEDIDA_CUENTA.VIGENTE_HASTA.gt(ahora)))
                .and(EXPEDIENTE_MEDIDA_CUENTA
                        .ESTADO
                        .eq("FIRME")
                        .or(EXPEDIENTE_MEDIDA_CUENTA
                                .CAUTELAR
                                .isTrue()
                                .and(EXPEDIENTE_MEDIDA_CUENTA.ESTADO.in(
                                        "CAUTELAR",
                                        "NOTIFICACION_PENDIENTE",
                                        "EN_DESCARGO",
                                        "DECIDIDA",
                                        "EN_APELACION"))));
    }

    /** A-1 (Pablo, 2026-11-26): lo que llegó a firme retiene aunque venza o se cierre, hasta la liberación humana. */
    private static Condition firmeSinLiberar() {
        return EXPEDIENTE_MEDIDA_CUENTA.FIRME_EN.isNotNull().and(EXPEDIENTE_MEDIDA_CUENTA.ENTREGA_LIBERADA_EN.isNull());
    }

    /** Si la entrega retenida por este expediente ya se liberó (A-1). */
    public boolean entregaLiberada(DSLContext dsl, UUID expedienteId) {
        return dsl.fetchExists(
                EXPEDIENTE_MEDIDA_CUENTA,
                EXPEDIENTE_MEDIDA_CUENTA
                        .ID
                        .eq(expedienteId)
                        .and(EXPEDIENTE_MEDIDA_CUENTA.ENTREGA_LIBERADA_EN.isNotNull()));
    }

    public Optional<Apelacion> apelacion(DSLContext dsl, UUID expedienteId) {
        return dsl.selectFrom(APELACION_MEDIDA_CUENTA)
                .where(APELACION_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID.eq(expedienteId))
                .fetchOptional()
                .map(r -> new Apelacion(r.getId(), r.getEstado(), r.getFechaLimiteResolucion()));
    }

    public Optional<String> estadoDeCuenta(DSLContext dsl, UUID usuarioId) {
        return dsl.select(USUARIO.ESTADO)
                .from(USUARIO)
                .where(USUARIO.ID.eq(usuarioId))
                .fetchOptional(USUARIO.ESTADO);
    }

    /** Dias no habiles nacionales y de plataforma entre dos fechas ({@code catalogo.dia_no_habil}). */
    public Set<LocalDate> noHabiles(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        var fecha = DSL.field(DSL.name("fecha"), LocalDate.class);
        return new HashSet<>(dsl.select(fecha)
                .from(DSL.table(DSL.name("catalogo", "dia_no_habil")))
                .where(fecha.between(desde, hasta))
                .and(DSL.field(DSL.name("alcance"), String.class).in("NACIONAL", "PLATAFORMA"))
                .and(DSL.field(DSL.name("grupo_id"), UUID.class).isNull())
                .fetch(fecha));
    }

    private static Expediente expediente(Record r) {
        var e = r.into(EXPEDIENTE_MEDIDA_CUENTA);
        return new Expediente(
                e.getId(),
                e.getUsuarioId(),
                e.getPoliticaMedidaCuentaId(),
                e.getCausal(),
                e.getMedida(),
                Boolean.TRUE.equals(e.getCautelar()),
                e.getMotivo(),
                e.getEstado(),
                e.getEstadoCuentaPrevio(),
                e.getPropuestaPor(),
                e.getPropuestaEn(),
                e.getNotificadaEn(),
                e.getFechaLimiteDescargo(),
                e.getDecididaPor(),
                e.getResultadoDecision(),
                e.getFundamentoDecision(),
                e.getFechaLimiteApelacion(),
                e.getVigenteHasta(),
                e.getPrescribeEn(),
                e.getFirmeEn(),
                e.getCerradaEn());
    }
}
