package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.APELACION_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.BLOQUEO_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EVIDENCIA_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.EXPEDIENTE_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.HISTORIAL_ESTADO_MEDIDA_CUENTA;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import bo.aportaya.identidad.dominio.PlazosDeMedida;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Expediente;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Persistencia del expediente de una medida sobre la cuenta (CU-134). Evidencia e historial son
 * append-only (R-AUD-01): aca solo se insertan. La segregacion la repite la base (R-SEG-13..15). Las
 * consultas viven en {@link LecturasDeMedidasRepositorio}; aca quedan las escrituras.
 */
@Component
public class MedidasDeCuentaRepositorio extends LecturasDeMedidasRepositorio {

    public record PoliticaVigente(
            UUID id, PlazosDeMedida.Politica plazos, Optional<Integer> resolucionApelacionDiasHabiles) {}

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

    private static String recortar(String s, int largo) {
        return s.length() <= largo ? s : s.substring(0, largo);
    }
}
