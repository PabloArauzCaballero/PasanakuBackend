package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.APELACION_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.BLOQUEO_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EVIDENCIA_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EXPEDIENTE_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.HISTORIAL_ESTADO_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.POLITICA_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import bo.aportaya.identidad.dominio.PlazosDeMedida;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Persistencia del expediente de una medida sobre la cuenta (CU-134). Evidencia e historial son
 * append-only (R-AUD-01): aca solo se insertan. La segregacion la repite la base (R-SEG-13..15).
 */
@Component
public class MedidasDeCuentaRepositorio {

    public record PoliticaVigente(
            UUID id, PlazosDeMedida.Politica plazos, Optional<Integer> resolucionApelacionDiasHabiles) {}

    public record Expediente(
            UUID id,
            UUID usuarioId,
            UUID politicaId,
            String causal,
            String medida,
            boolean cautelar,
            String motivo,
            String estado,
            String estadoCuentaPrevio,
            UUID propuestaPor,
            OffsetDateTime propuestaEn,
            OffsetDateTime notificadaEn,
            OffsetDateTime fechaLimiteDescargo,
            UUID decididaPor,
            String resultadoDecision,
            String fundamentoDecision,
            OffsetDateTime fechaLimiteApelacion,
            OffsetDateTime vigenteHasta,
            OffsetDateTime prescribeEn,
            OffsetDateTime firmeEn,
            OffsetDateTime cerradaEn) {}

    public record Hito(String estadoAnterior, String estado, String motivo, OffsetDateTime ocurridoEn) {}

    public record Apelacion(UUID id, String estado, OffsetDateTime fechaLimiteResolucion) {}

    public Optional<PoliticaVigente> politicaVigente(DSLContext dsl, OffsetDateTime ahora) {
        return dsl.selectFrom(POLITICA_MEDIDA_CUENTA)
                .where(POLITICA_MEDIDA_CUENTA.VIGENTE_DESDE.le(ahora))
                .orderBy(POLITICA_MEDIDA_CUENTA.VIGENTE_DESDE.desc())
                .limit(1)
                .fetchOptional()
                .map(MedidasDeCuentaRepositorio::politica);
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

    public UUID abrir(
            DSLContext dsl,
            UUID usuarioId,
            UUID politicaId,
            String causal,
            String medida,
            boolean cautelar,
            String motivo,
            String estado,
            String estadoCuentaPrevio,
            UUID propuestaPor,
            OffsetDateTime ahora,
            OffsetDateTime prescribeEn,
            OffsetDateTime vigenteHasta,
            String clave) {
        return dsl.insertInto(EXPEDIENTE_MEDIDA_CUENTA)
                .set(EXPEDIENTE_MEDIDA_CUENTA.USUARIO_ID, usuarioId)
                .set(EXPEDIENTE_MEDIDA_CUENTA.POLITICA_MEDIDA_CUENTA_ID, politicaId)
                .set(EXPEDIENTE_MEDIDA_CUENTA.CAUSAL, causal)
                .set(EXPEDIENTE_MEDIDA_CUENTA.MEDIDA, medida)
                .set(EXPEDIENTE_MEDIDA_CUENTA.CAUTELAR, cautelar)
                .set(EXPEDIENTE_MEDIDA_CUENTA.MOTIVO, motivo)
                .set(EXPEDIENTE_MEDIDA_CUENTA.ESTADO, estado)
                .set(EXPEDIENTE_MEDIDA_CUENTA.ESTADO_CUENTA_PREVIO, estadoCuentaPrevio)
                .set(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_POR, propuestaPor)
                .set(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_EN, ahora)
                .set(EXPEDIENTE_MEDIDA_CUENTA.PRESCRIBE_EN, prescribeEn)
                .set(EXPEDIENTE_MEDIDA_CUENTA.VIGENTE_HASTA, vigenteHasta)
                .set(EXPEDIENTE_MEDIDA_CUENTA.CLAVE_IDEMPOTENCIA, clave)
                .returning(EXPEDIENTE_MEDIDA_CUENTA.ID)
                .fetchSingle()
                .getId();
    }

    public void agregarEvidencia(
            DSLContext dsl,
            UUID expedienteId,
            String tipo,
            String contenido,
            UUID referenciaId,
            UUID por,
            OffsetDateTime ahora) {
        dsl.insertInto(EVIDENCIA_MEDIDA_CUENTA)
                .set(EVIDENCIA_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID, expedienteId)
                .set(EVIDENCIA_MEDIDA_CUENTA.TIPO, tipo)
                .set(EVIDENCIA_MEDIDA_CUENTA.CONTENIDO, contenido)
                .set(EVIDENCIA_MEDIDA_CUENTA.REFERENCIA_ID, referenciaId)
                .set(EVIDENCIA_MEDIDA_CUENTA.APORTADA_POR, por)
                .set(EVIDENCIA_MEDIDA_CUENTA.REGISTRADA_EN, ahora)
                .execute();
    }

    public boolean tieneDescargo(DSLContext dsl, UUID expedienteId) {
        return dsl.fetchExists(
                EVIDENCIA_MEDIDA_CUENTA,
                EVIDENCIA_MEDIDA_CUENTA
                        .EXPEDIENTE_MEDIDA_CUENTA_ID
                        .eq(expedienteId)
                        .and(EVIDENCIA_MEDIDA_CUENTA.TIPO.eq("DESCARGO")));
    }

    /** Cambia el estado y deja la fila del historial en la misma transaccion. */
    public void transicion(DSLContext dsl, Expediente e, String nuevo, UUID por, String motivo, OffsetDateTime ahora) {
        dsl.update(EXPEDIENTE_MEDIDA_CUENTA)
                .set(EXPEDIENTE_MEDIDA_CUENTA.ESTADO, nuevo)
                .set(EXPEDIENTE_MEDIDA_CUENTA.VERSION, EXPEDIENTE_MEDIDA_CUENTA.VERSION.plus(1))
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(e.id()))
                .execute();
        dsl.insertInto(HISTORIAL_ESTADO_MEDIDA_CUENTA)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID, e.id())
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.ESTADO_ANTERIOR, e.estado())
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.ESTADO_NUEVO, nuevo)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.EJECUTADO_POR, por)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.MOTIVO, motivo == null ? null : recortar(motivo, 500))
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.OCURRIDO_EN, ahora)
                .execute();
    }

    public void primerHito(DSLContext dsl, UUID expedienteId, String estado, UUID por, OffsetDateTime ahora) {
        dsl.insertInto(HISTORIAL_ESTADO_MEDIDA_CUENTA)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID, expedienteId)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.ESTADO_NUEVO, estado)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.EJECUTADO_POR, por)
                .set(HISTORIAL_ESTADO_MEDIDA_CUENTA.OCURRIDO_EN, ahora)
                .execute();
    }

    public void marcarNotificada(DSLContext dsl, UUID id, OffsetDateTime entregadaEn, OffsetDateTime limite) {
        dsl.update(EXPEDIENTE_MEDIDA_CUENTA)
                .set(EXPEDIENTE_MEDIDA_CUENTA.NOTIFICADA_EN, entregadaEn)
                .set(EXPEDIENTE_MEDIDA_CUENTA.FECHA_LIMITE_DESCARGO, limite)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .and(EXPEDIENTE_MEDIDA_CUENTA.NOTIFICADA_EN.isNull())
                .execute();
    }

    public void decidir(
            DSLContext dsl,
            UUID id,
            UUID por,
            String resultado,
            String fundamento,
            OffsetDateTime ahora,
            OffsetDateTime limiteApelacion) {
        dsl.update(EXPEDIENTE_MEDIDA_CUENTA)
                .set(EXPEDIENTE_MEDIDA_CUENTA.DECIDIDA_POR, por)
                .set(EXPEDIENTE_MEDIDA_CUENTA.DECIDIDA_EN, ahora)
                .set(EXPEDIENTE_MEDIDA_CUENTA.RESULTADO_DECISION, resultado)
                .set(EXPEDIENTE_MEDIDA_CUENTA.FUNDAMENTO_DECISION, fundamento)
                .set(EXPEDIENTE_MEDIDA_CUENTA.FECHA_LIMITE_APELACION, limiteApelacion)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .and(EXPEDIENTE_MEDIDA_CUENTA.DECIDIDA_EN.isNull())
                .execute();
    }

    public void firme(DSLContext dsl, UUID id, OffsetDateTime ahora) {
        dsl.update(EXPEDIENTE_MEDIDA_CUENTA)
                .set(EXPEDIENTE_MEDIDA_CUENTA.FIRME_EN, ahora)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .execute();
    }

    public void cerrar(DSLContext dsl, UUID id, OffsetDateTime ahora) {
        dsl.update(EXPEDIENTE_MEDIDA_CUENTA)
                .set(EXPEDIENTE_MEDIDA_CUENTA.CERRADA_EN, ahora)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .and(EXPEDIENTE_MEDIDA_CUENTA.CERRADA_EN.isNull())
                .execute();
    }

    public Optional<Expediente> consultar(DSLContext dsl, UUID id) {
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .fetchOptional()
                .map(MedidasDeCuentaRepositorio::expediente);
    }

    /** Bloquea la fila para la transicion: dos decisiones simultaneas no pasan las dos. */
    public Optional<Expediente> paraActualizar(DSLContext dsl, UUID id) {
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.ID.eq(id))
                .forUpdate()
                .fetchOptional()
                .map(MedidasDeCuentaRepositorio::expediente);
    }

    public List<Expediente> deUsuario(DSLContext dsl, UUID usuarioId) {
        return dsl.selectFrom(EXPEDIENTE_MEDIDA_CUENTA)
                .where(EXPEDIENTE_MEDIDA_CUENTA.USUARIO_ID.eq(usuarioId))
                .orderBy(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_EN.desc())
                .limit(100)
                .fetch(MedidasDeCuentaRepositorio::expediente);
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
                .fetch(MedidasDeCuentaRepositorio::expediente);
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
                .and(EXPEDIENTE_MEDIDA_CUENTA.CERRADA_EN.isNull())
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
                                        "EN_APELACION"))))
                .orderBy(EXPEDIENTE_MEDIDA_CUENTA.PROPUESTA_EN)
                .limit(1)
                .fetchOptional(EXPEDIENTE_MEDIDA_CUENTA.ID);
    }

    public Optional<Apelacion> apelacion(DSLContext dsl, UUID expedienteId) {
        return dsl.selectFrom(APELACION_MEDIDA_CUENTA)
                .where(APELACION_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID.eq(expedienteId))
                .fetchOptional()
                .map(r -> new Apelacion(r.getId(), r.getEstado(), r.getFechaLimiteResolucion()));
    }

    public void apelar(
            DSLContext dsl,
            UUID expedienteId,
            UUID apelante,
            String argumento,
            OffsetDateTime ahora,
            OffsetDateTime limiteResolucion) {
        dsl.insertInto(APELACION_MEDIDA_CUENTA)
                .set(APELACION_MEDIDA_CUENTA.EXPEDIENTE_MEDIDA_CUENTA_ID, expedienteId)
                .set(APELACION_MEDIDA_CUENTA.APELANTE_ID, apelante)
                .set(APELACION_MEDIDA_CUENTA.ARGUMENTO, argumento)
                .set(APELACION_MEDIDA_CUENTA.ESTADO, "PRESENTADA")
                .set(APELACION_MEDIDA_CUENTA.PRESENTADA_EN, ahora)
                .set(APELACION_MEDIDA_CUENTA.FECHA_LIMITE_RESOLUCION, limiteResolucion)
                .execute();
    }

    public void resolverApelacion(
            DSLContext dsl, UUID apelacionId, String estado, UUID por, String resolucion, OffsetDateTime ahora) {
        dsl.update(APELACION_MEDIDA_CUENTA)
                .set(APELACION_MEDIDA_CUENTA.ESTADO, estado)
                .set(APELACION_MEDIDA_CUENTA.RESUELTA_POR, por)
                .set(APELACION_MEDIDA_CUENTA.RESOLUCION, resolucion)
                .set(APELACION_MEDIDA_CUENTA.RESUELTA_EN, ahora)
                .where(APELACION_MEDIDA_CUENTA.ID.eq(apelacionId))
                .and(APELACION_MEDIDA_CUENTA.RESUELTA_EN.isNull())
                .execute();
    }

    public Optional<String> estadoDeCuenta(DSLContext dsl, UUID usuarioId) {
        return dsl.select(USUARIO.ESTADO)
                .from(USUARIO)
                .where(USUARIO.ID.eq(usuarioId))
                .fetchOptional(USUARIO.ESTADO);
    }

    public void estadoDeCuenta(DSLContext dsl, UUID usuarioId, String estado) {
        dsl.update(USUARIO)
                .set(USUARIO.ESTADO, estado)
                .set(USUARIO.VERSION, USUARIO.VERSION.plus(1))
                .where(USUARIO.ID.eq(usuarioId))
                .execute();
    }

    public void bloquear(DSLContext dsl, UUID usuarioId, String motivo, OffsetDateTime ahora) {
        dsl.insertInto(BLOQUEO_CUENTA)
                .set(BLOQUEO_CUENTA.USUARIO_ID, usuarioId)
                .set(BLOQUEO_CUENTA.MOTIVO, motivo)
                .set(BLOQUEO_CUENTA.BLOQUEADA_EN, ahora)
                .execute();
    }

    /** Libera solo los bloqueos que pudo poner una medida, no los de intentos fallidos ni los pedidos. */
    public void liberarBloqueosDeMedida(DSLContext dsl, UUID usuarioId, UUID por, OffsetDateTime ahora) {
        dsl.update(BLOQUEO_CUENTA)
                .set(BLOQUEO_CUENTA.LIBERADA_EN, ahora)
                .set(BLOQUEO_CUENTA.LIBERADA_POR, por)
                .where(BLOQUEO_CUENTA.USUARIO_ID.eq(usuarioId))
                .and(BLOQUEO_CUENTA.MOTIVO.in("FRAUDE", "CUENTA_COMPROMETIDA"))
                .and(BLOQUEO_CUENTA.LIBERADA_EN.isNull())
                .execute();
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

    private static String recortar(String s, int largo) {
        return s.length() <= largo ? s : s.substring(0, largo);
    }
}
