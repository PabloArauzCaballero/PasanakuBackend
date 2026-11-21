package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** {@code bloqueo_saldo}: la plata inmovilizada por orden de una autoridad. */
@Component
public class BloqueoRepositorio {

    public UUID registrar(
            DSLContext dsl,
            UUID cuentaId,
            UUID retencionId,
            String autoridad,
            String tipoOrden,
            String numeroOficio,
            Optional<Dinero> montoBloqueado,
            String alcance,
            String documentoUrl,
            String hashDocumento,
            UUID registradoPor,
            OffsetDateTime recibidoEn) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "bloqueo_saldo")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("cuenta_billetera_id", UUID.class), cuentaId)
                .set(DSL.field("retencion_id", UUID.class), retencionId)
                .set(DSL.field("autoridad", String.class), autoridad)
                .set(DSL.field("tipo_orden", String.class), tipoOrden)
                .set(DSL.field("numero_oficio", String.class), numeroOficio)
                .set(
                        DSL.field("monto_bloqueado", BigDecimal.class),
                        montoBloqueado.map(Dinero::monto).orElse(null))
                .set(DSL.field("alcance", String.class), alcance)
                .set(DSL.field("documento_url", String.class), documentoUrl)
                .set(DSL.field("hash_documento", String.class), hashDocumento)
                .set(DSL.field("estado", String.class), "VIGENTE")
                // F1.L1.M10 · R-BIL-32: quien lo registro, desde su token; no puede ser quien lo levante.
                .set(DSL.field("registrado_por", UUID.class), registradoPor)
                .set(DSL.field("recibido_en", OffsetDateTime.class), recibidoEn)
                .execute();
        return id;
    }

    public boolean existeOficio(DSLContext dsl, String numeroOficio) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("nucleo_financiero", "bloqueo_saldo")),
                        DSL.field("numero_oficio").eq(numeroOficio))
                > 0;
    }

    public Optional<Bloqueo> vigenteDe(DSLContext dsl, UUID cuentaId) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("numero_oficio", String.class),
                        DSL.field("autoridad", String.class),
                        DSL.field("retencion_id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "bloqueo_saldo")))
                .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                .and(DSL.field("estado").eq("VIGENTE"))
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Bloqueo(
                        f.get("id", UUID.class),
                        f.get("numero_oficio", String.class),
                        f.get("autoridad", String.class),
                        Optional.ofNullable(f.get("retencion_id", UUID.class))));
    }

    /** El bloqueo vigente por su id, con quien lo registro y su retencion (F1.L1.M10). */
    public Optional<Vigente> vigente(DSLContext dsl, UUID bloqueoId) {
        return dsl.select(DSL.field("registrado_por", UUID.class), DSL.field("retencion_id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "bloqueo_saldo")))
                .where(DSL.field("id", UUID.class).eq(bloqueoId))
                .and(DSL.field("estado").eq("VIGENTE"))
                .fetchOptional(f -> new Vigente(
                        Optional.ofNullable(f.get("registrado_por", UUID.class)),
                        Optional.ofNullable(f.get("retencion_id", UUID.class))));
    }

    public record Vigente(Optional<UUID> registradoPor, Optional<UUID> retencionId) {}

    /** Levantar el bloqueo es decision de la misma autoridad que lo puso, nunca nuestra. */
    public boolean levantar(DSLContext dsl, UUID bloqueoId, UUID levantadaPor, OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "bloqueo_saldo")))
                        .set(DSL.field("estado", String.class), "LEVANTADO")
                        .set(DSL.field("levantada_por", UUID.class), levantadaPor)
                        .set(DSL.field("levantado_en", OffsetDateTime.class), ahora)
                        .where(DSL.field("id", UUID.class).eq(bloqueoId))
                        .and(DSL.field("estado").eq("VIGENTE"))
                        .execute()
                > 0;
    }

    /**
     * El listado de la bandeja de Cumplimiento: cursor (recibido_en, id) descendente, filtro opcional por estado. La
     * moneda del importe es la de la cuenta (bloqueo_saldo no la modela; regla 91.1.2: ningun importe sin moneda).
     */
    public java.util.List<FilaDeBloqueo> listar(
            DSLContext dsl,
            Optional<String> estado,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {
        var parametros = new java.util.ArrayList<Object>();
        var condiciones = new java.util.ArrayList<String>();
        estado.ifPresent(e -> {
            condiciones.add("b.estado = ?");
            parametros.add(e);
        });
        if (antesDe.isPresent() && antesDeId.isPresent()) {
            condiciones.add("(b.recibido_en, b.id) < (?::timestamptz, ?::uuid)");
            parametros.add(antesDe.get());
            parametros.add(antesDeId.get());
        }
        parametros.add(limite);
        String donde = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return dsl.fetch(
                        """
                        SELECT b.id, b.cuenta_billetera_id, b.autoridad, b.tipo_orden, b.alcance, b.estado,
                               b.monto_bloqueado, c.moneda, b.registrado_por, b.levantada_por, b.recibido_en,
                               b.vence_en, b.levantado_en
                          FROM nucleo_financiero.bloqueo_saldo b
                          JOIN nucleo_financiero.cuenta_billetera c ON c.id = b.cuenta_billetera_id"""
                                + donde
                                + " ORDER BY b.recibido_en DESC, b.id DESC LIMIT ?",
                        parametros.toArray())
                .map(f -> new FilaDeBloqueo(
                        f.get("id", UUID.class),
                        f.get("cuenta_billetera_id", UUID.class),
                        f.get("autoridad", String.class),
                        f.get("tipo_orden", String.class),
                        f.get("alcance", String.class),
                        f.get("estado", String.class),
                        Optional.ofNullable(f.get("monto_bloqueado", BigDecimal.class))
                                .map(m -> Dinero.de(
                                        m,
                                        bo.aportaya.plataforma.dominio.Moneda.valueOf(f.get("moneda", String.class)))),
                        Optional.ofNullable(f.get("registrado_por", UUID.class)),
                        Optional.ofNullable(f.get("levantada_por", UUID.class)),
                        f.get("recibido_en", OffsetDateTime.class),
                        Optional.ofNullable(f.get("vence_en", OffsetDateTime.class)),
                        Optional.ofNullable(f.get("levantado_en", OffsetDateTime.class))));
    }

    public record FilaDeBloqueo(
            UUID id,
            UUID cuentaBilleteraId,
            String autoridad,
            String tipoOrden,
            String alcance,
            String estado,
            Optional<Dinero> montoBloqueado,
            Optional<UUID> registradoPor,
            Optional<UUID> levantadoPor,
            OffsetDateTime recibidoEn,
            Optional<OffsetDateTime> venceEn,
            Optional<OffsetDateTime> levantadoEn) {}

    public record Bloqueo(UUID id, String numeroOficio, String autoridad, Optional<UUID> retencionId) {}
}
