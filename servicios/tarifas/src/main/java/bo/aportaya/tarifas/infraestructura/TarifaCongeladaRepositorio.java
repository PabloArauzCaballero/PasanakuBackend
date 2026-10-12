package bo.aportaya.tarifas.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * El precio pactado de un grupo, congelado (R-TAR-07, CU-20).
 *
 * <p>El snapshot es el tarifario con TODOS sus conceptos activos y las reglas (tramos) de cada uno, armado por la
 * propia base: asi el texto es el mismo que se guarda y el hash se puede verificar con una consulta
 * ({@code encode(digest(snapshot_conceptos::text, 'sha256'), 'hex') = hash_snapshot}). {@code jsonb} tiene una
 * representacion textual canonica, que es lo que hace estable el hash.
 */
@Component
public class TarifaCongeladaRepositorio {

    /** Lo que quedo congelado: el tarifario y el hash de su snapshot. */
    public record Congelada(UUID tarifarioId, String hashSnapshot) {}

    /** El snapshot del tarifario como texto jsonb canonico, o vacio si el tarifario no existe. */
    public Optional<String> snapshotDe(DSLContext dsl, UUID tarifarioId) {
        return Optional.ofNullable(dsl.fetchOne(
                        """
                        SELECT jsonb_build_object(
                                 'tarifarioId', t.id,
                                 'codigo', t.codigo,
                                 'version', t.version,
                                 'conceptos', COALESCE((
                                     SELECT jsonb_agg(
                                              to_jsonb(c) || jsonb_build_object('reglas', COALESCE((
                                                  SELECT jsonb_agg(to_jsonb(r) ORDER BY r.orden)
                                                    FROM tarifas.regla_tarifa r
                                                   WHERE r.concepto_tarifa_id = c.id), '[]'::jsonb))
                                              ORDER BY c.orden_aplicacion, c.codigo)
                                       FROM tarifas.concepto_tarifa c
                                      WHERE c.tarifario_id = t.id AND c.activo), '[]'::jsonb))::text AS snapshot
                          FROM catalogo.tarifario t
                         WHERE t.id = ?
                        """,
                        tarifarioId))
                .map(f -> f.get("snapshot", String.class));
    }

    /** El hash que se guarda con el snapshot: SHA-256 hexadecimal de su texto. */
    public static String hashDe(String snapshot) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(snapshot.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tiene que existir en cualquier JVM", e);
        }
    }

    /** Si el grupo ya tiene su precio congelado. */
    public boolean yaCongelado(DSLContext dsl, UUID grupoId) {
        return dsl.fetchOne("SELECT 1 FROM tarifas.tarifa_congelada_grupo WHERE grupo_id = ?", grupoId) != null;
    }

    /**
     * Congela el precio del grupo. {@code uq_tarifa_congelada_grupo} decide: si ya estaba congelado no se escribe nada
     * y se devuelve lo que quedo (una sola tarifa por grupo, aunque lleguen dos eventos o dos replicas a la vez).
     */
    public Congelada congelar(
            DSLContext dsl, UUID grupoId, UUID tarifarioId, String snapshot, String hash, OffsetDateTime ahora) {
        dsl.execute(
                """
                INSERT INTO tarifas.tarifa_congelada_grupo
                    (grupo_id, tarifario_id, snapshot_conceptos, hash_snapshot, congelada_en)
                VALUES (?, ?, ?::jsonb, ?, ?)
                ON CONFLICT (grupo_id) DO NOTHING
                """,
                grupoId,
                tarifarioId,
                snapshot,
                hash,
                ahora);
        var fila = dsl.fetchOne(
                "SELECT tarifario_id, hash_snapshot FROM tarifas.tarifa_congelada_grupo WHERE grupo_id = ?", grupoId);
        return new Congelada(fila.get("tarifario_id", UUID.class), fila.get("hash_snapshot", String.class));
    }
}
