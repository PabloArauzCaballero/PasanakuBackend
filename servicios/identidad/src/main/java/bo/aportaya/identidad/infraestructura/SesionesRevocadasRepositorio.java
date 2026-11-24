package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.SESION;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Lo que lee {@link VigiaDeRevocaciones}: las sesiones revocadas despues de un punto {@code (revocada_en, id)}, en
 * orden. La paginacion es por clave y no por desplazamiento: una revocacion masiva (cientos de sesiones en el mismo
 * instante) se recorre entera en vez de devolver siempre las mismas primeras filas. Sirve {@code ix_sesion_revocada_en}
 * (GU-15): sin el indice, mirar cada cuarto de segundo seria recorrer la tabla entera.
 */
@Component
public class SesionesRevocadasRepositorio {

    public record SesionRevocada(UUID sesionId, UUID usuarioId, OffsetDateTime revocadaEn) {}

    public List<SesionRevocada> despuesDe(DSLContext dsl, OffsetDateTime desde, UUID despuesDeId, int porTanda) {
        return dsl.select(SESION.ID, SESION.USUARIO_ID, SESION.REVOCADA_EN)
                .from(SESION)
                .where(SESION.REVOCADA_EN.ge(desde))
                .and(DSL.row(SESION.REVOCADA_EN, SESION.ID).gt(desde, despuesDeId))
                .orderBy(SESION.REVOCADA_EN, SESION.ID)
                .limit(porTanda)
                .fetch(fila -> new SesionRevocada(
                        fila.get(SESION.ID), fila.get(SESION.USUARIO_ID), fila.get(SESION.REVOCADA_EN)));
    }
}
