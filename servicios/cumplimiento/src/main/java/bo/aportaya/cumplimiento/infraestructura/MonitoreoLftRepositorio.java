package bo.aportaya.cumplimiento.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code regla_monitoreo_lft} y {@code alerta_monitoreo_lft}.
 *
 * <p>**Una alerta no se cierra sin conclusion** (R-UIF-07, y lo verifica
 * {@code ck_alerta_conclusion} con un minimo de veinte caracteres). No es formalismo:
 * una alerta cerrada con «revisado» no le sirve a quien audite ni a quien calibre la
 * regla, y deja al analista sin poder explicar dos años despues por que decidio que no
 * era nada.
 */
@Component
public class MonitoreoLftRepositorio {

    public Optional<UUID> reglaPorCodigo(DSLContext dsl, String codigo) {
        return dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("cumplimiento", "regla_monitoreo_lft")))
                .where(DSL.field("codigo", String.class).eq(codigo))
                .fetchOptional(f -> f.get("id", UUID.class));
    }

    public UUID crearRegla(
            DSLContext dsl,
            String codigo,
            String tipologia,
            String descripcion,
            String expresionJson,
            String ventana,
            BigDecimal umbralMonto,
            Integer umbralCantidad,
            String severidad,
            String accion,
            String fuenteNormativa,
            OffsetDateTime vigenteDesde) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("cumplimiento", "regla_monitoreo_lft")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("codigo", String.class), codigo)
                .set(DSL.field("tipologia", String.class), tipologia)
                .set(DSL.field("descripcion", String.class), descripcion)
                .set(DSL.field("expresion", JSONB.class), JSONB.valueOf(expresionJson))
                .set(DSL.field("ventana_evaluacion", String.class), ventana)
                .set(DSL.field("umbral_monto", BigDecimal.class), umbralMonto)
                .set(DSL.field("umbral_cantidad", Integer.class), umbralCantidad)
                .set(DSL.field("severidad", String.class), severidad)
                .set(DSL.field("accion_automatica", String.class), accion)
                .set(DSL.field("fuente_normativa", String.class), fuenteNormativa)
                // Nace INACTIVA: activar exige simulacion previa. Una regla que entra
                // encendida puede marcar el 40% del trafico y nadie lo sabe hasta que
                // la bandeja explota.
                .set(DSL.field("activa", Boolean.class), false)
                .set(DSL.field("vigente_desde", OffsetDateTime.class), vigenteDesde)
                .execute();
        return id;
    }

    /** Activar exige quien aprueba: una regla vigente sin firma no la puso nadie. */
    public boolean activar(DSLContext dsl, UUID reglaId, UUID aprobadaPor, OffsetDateTime vigenteDesde) {
        return dsl.update(DSL.table(DSL.name("cumplimiento", "regla_monitoreo_lft")))
                        .set(DSL.field("activa", Boolean.class), true)
                        .set(DSL.field("aprobada_por", UUID.class), aprobadaPor)
                        .set(DSL.field("vigente_desde", OffsetDateTime.class), vigenteDesde)
                        .where(DSL.field("id", UUID.class)
                                .eq(reglaId)
                                .and(DSL.field("activa", Boolean.class).isFalse()))
                        .execute()
                == 1;
    }

    public UUID levantarAlerta(
            DSLContext dsl,
            UUID reglaId,
            UUID usuarioId,
            UUID transaccionId,
            BigDecimal monto,
            String detalleJson,
            String severidad,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("regla_monitoreo_id", UUID.class), reglaId)
                .set(DSL.field("usuario_id", UUID.class), usuarioId)
                .set(DSL.field("transaccion_id", UUID.class), transaccionId)
                .set(DSL.field("monto_involucrado", BigDecimal.class), monto)
                .set(DSL.field("detalle", JSONB.class), JSONB.valueOf(detalleJson))
                .set(DSL.field("severidad", String.class), severidad)
                .set(DSL.field("estado", String.class), "ABIERTA")
                .set(DSL.field("detectada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    public List<Alerta> alertas(DSLContext dsl, List<UUID> ids) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("usuario_id", UUID.class),
                        DSL.field("severidad", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("monto_involucrado", BigDecimal.class),
                        DSL.field("detectada_en", OffsetDateTime.class))
                .from(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                .where(DSL.field("id", UUID.class).in(ids))
                .fetch(f -> new Alerta(
                        f.get("id", UUID.class),
                        f.get("usuario_id", UUID.class),
                        f.get("severidad", String.class),
                        f.get("estado", String.class),
                        f.get("monto_involucrado", BigDecimal.class),
                        f.get("detectada_en", OffsetDateTime.class)));
    }

    /** A que caso ya estan enlazadas estas alertas (las que no estan en ninguno no aparecen). */
    public java.util.Map<UUID, UUID> casosDe(DSLContext dsl, List<UUID> ids) {
        return dsl.select(DSL.field("id", UUID.class), DSL.field("caso_id", UUID.class))
                .from(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                .where(DSL.field("id", UUID.class)
                        .in(ids)
                        .and(DSL.field("caso_id").isNotNull()))
                .fetchMap(DSL.field("id", UUID.class), DSL.field("caso_id", UUID.class));
    }

    /** Cerrar una alerta: siempre con conclusion, y siempre atada a su caso si escalo. */
    public boolean cerrar(
            DSLContext dsl, UUID alertaId, String estado, String conclusion, UUID casoId, OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                        .set(DSL.field("estado", String.class), estado)
                        .set(DSL.field("conclusion", String.class), conclusion)
                        .set(DSL.field("caso_id", UUID.class), casoId)
                        .set(DSL.field("cerrada_en", OffsetDateTime.class), ahora)
                        .where(DSL.field("id", UUID.class)
                                .eq(alertaId)
                                .and(DSL.field("estado", String.class).in("ABIERTA", "EN_ANALISIS")))
                        .execute()
                == 1;
    }

    /**
     * La alerta entra al caso: queda asignada al analista Y enlazada al caso desde que se abre
     * (backoffice carril A · H5). Antes el enlace solo se escribia al cerrar, y la decision tenia que
     * recibir del cliente que alertas cerrar.
     */
    public boolean asignar(DSLContext dsl, UUID alertaId, UUID analistaId, UUID casoId) {
        return dsl.update(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                        .set(DSL.field("asignada_a", UUID.class), analistaId)
                        .set(DSL.field("caso_id", UUID.class), casoId)
                        .set(DSL.field("estado", String.class), "EN_ANALISIS")
                        .where(DSL.field("id", UUID.class)
                                .eq(alertaId)
                                .and(DSL.field("estado", String.class).eq("ABIERTA")))
                        .execute()
                == 1;
    }

    /** Las alertas todavia abiertas de un caso: las que su decision tiene que cerrar. */
    public List<UUID> abiertasDelCaso(DSLContext dsl, UUID casoId) {
        return dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                .where(DSL.field("caso_id", UUID.class)
                        .eq(casoId)
                        .and(DSL.field("estado", String.class).in("ABIERTA", "EN_ANALISIS")))
                .orderBy(DSL.field("detectada_en"), DSL.field("id"))
                .fetch(DSL.field("id", UUID.class));
    }

    /** Cuantas alertas del caso ya cerro su decision. */
    public int cerradasDelCaso(DSLContext dsl, UUID casoId) {
        return dsl.fetchCount(
                DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")),
                DSL.field("caso_id", UUID.class)
                        .eq(casoId)
                        .and(DSL.field("estado", String.class).in("DESCARTADA", "ESCALADA")));
    }

    /** Alertas abiertas sin analista pasado su plazo: lo que el trabajo diario escala. */
    public List<Alerta> abiertasSinAnalista(DSLContext dsl, OffsetDateTime limite) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("usuario_id", UUID.class),
                        DSL.field("severidad", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("monto_involucrado", BigDecimal.class),
                        DSL.field("detectada_en", OffsetDateTime.class))
                .from(DSL.table(DSL.name("cumplimiento", "alerta_monitoreo_lft")))
                .where(DSL.field("estado", String.class)
                        .eq("ABIERTA")
                        .and(DSL.field("asignada_a", UUID.class).isNull())
                        .and(DSL.field("detectada_en", OffsetDateTime.class).lt(limite)))
                .fetch(f -> new Alerta(
                        f.get("id", UUID.class),
                        f.get("usuario_id", UUID.class),
                        f.get("severidad", String.class),
                        f.get("estado", String.class),
                        f.get("monto_involucrado", BigDecimal.class),
                        f.get("detectada_en", OffsetDateTime.class)));
    }

    public record Alerta(
            UUID id, UUID usuarioId, String severidad, String estado, BigDecimal monto, OffsetDateTime detectadaEn) {}
}
