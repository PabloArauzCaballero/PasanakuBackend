package bo.aportaya.erp.infraestructura;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Lectura de un {@code estado_financiero_generado} ya sellado (append-only): solo lectura. */
@Component
public class EstadosFinancierosRepositorio {

    public Optional<Guardado> guardado(DSLContext dsl, UUID estadoFinancieroId) {
        return Optional.ofNullable(dsl.fetchOne(
                        """
                SELECT id, tipo, generado_en, cast(datos AS text) AS datos, hash_contenido
                  FROM erp.estado_financiero_generado WHERE id = ?
                """,
                        estadoFinancieroId))
                .map(f -> new Guardado(
                        f.get("id", UUID.class),
                        f.get("tipo", String.class),
                        f.get("generado_en", OffsetDateTime.class),
                        f.get("datos", String.class),
                        f.get("hash_contenido", String.class)));
    }

    /** {@code datos} como lo devuelve JSONB: normalizado, por eso NO es el texto que se hasheo. */
    public record Guardado(UUID id, String tipo, OffsetDateTime generadoEn, String datos, String hashContenido) {}
}
