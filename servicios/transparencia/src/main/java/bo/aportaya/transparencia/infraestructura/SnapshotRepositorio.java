package bo.aportaya.transparencia.infraestructura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code snapshot_reputacion}: la foto congelada de un puntaje.
 *
 * <p>Existe porque el puntaje vigente se reemplaza en cada recalculo y un certificado
 * emitido hace tres meses tiene que seguir diciendo lo que decia. La foto guarda los
 * factores, no solo el numero: un certificado que dice «85» sin decir de que esta hecho
 * no se puede verificar, solo creer.
 */
@Component
public class SnapshotRepositorio {

    /** Una foto del puntaje: es lo que un certificado congela y despues se verifica. */
    public UUID tomarSnapshot(
            DSLContext dsl,
            UUID usuarioId,
            BigDecimal puntaje,
            String nivel,
            String factoresJson,
            String motivo,
            OffsetDateTime ahora) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("transparencia", "snapshot_reputacion")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("usuario_id", UUID.class), usuarioId)
                .set(DSL.field("puntaje", BigDecimal.class), puntaje)
                .set(DSL.field("nivel_confianza", String.class), nivel)
                .set(DSL.field("fotografia_factores", JSONB.class), JSONB.valueOf(factoresJson))
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("tomado_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Lo que un certificado PUEDE mostrar, leido de la foto del propio titular (F1.L5.M21): puntaje, nivel, fecha de la
     * foto y cada factor escalar congelado. Vacio si la foto no existe o no es de {@code usuarioId}: una foto ajena no
     * se distingue de una que no existe.
     */
    public Optional<Map<String, String>> disponiblesDelTitular(DSLContext dsl, UUID snapshotId, UUID usuarioId) {
        var fila = dsl.fetchOne(
                """
                SELECT puntaje::text AS puntaje, nivel_confianza, tomado_en, fotografia_factores::text AS factores
                  FROM transparencia.snapshot_reputacion
                 WHERE id = ? AND usuario_id = ?
                """,
                snapshotId,
                usuarioId);
        if (fila == null) {
            return Optional.empty();
        }
        var disponibles = new LinkedHashMap<String, String>();
        try {
            JsonNode factores = JSON.readTree(fila.get("factores", String.class));
            if (factores != null && factores.isObject()) {
                for (var f : factores.properties()) {
                    if (f.getValue().isValueNode() && !f.getValue().isNull()) {
                        disponibles.put(f.getKey(), f.getValue().asText());
                    }
                }
            }
        } catch (java.io.IOException ilegible) {
            // Factores ilegibles: el certificado se arma solo con las columnas, nunca con lo que mande el cliente.
        }
        // Las columnas mandan sobre un factor con el mismo nombre.
        disponibles.put("puntaje", fila.get("puntaje", String.class));
        disponibles.put("nivel", fila.get("nivel_confianza", String.class));
        disponibles.put("tomadoEn", String.valueOf(fila.get("tomado_en", OffsetDateTime.class)));
        return Optional.of(disponibles);
    }
}
