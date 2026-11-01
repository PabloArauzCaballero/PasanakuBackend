package bo.aportaya.auditoria.infraestructura;

import bo.aportaya.auditoria.dominio.IncidenteOperativo;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Lee {@code auditoria.incidente_operativo} para Sistemas › Incidentes. Solo lectura y solo las columnas que la lista
 * necesita: la descripcion, la causa raiz y las acciones son texto largo de la ficha, y {@code impacto_monetario} no
 * sale porque la tabla no guarda su moneda (regla 91.1.2).
 *
 * <p>Orden determinista ({@code detectado_en desc, id}): sin el desempate por {@code id}, dos incidentes detectados en
 * el mismo instante pueden cambiar de pagina entre dos pedidos (regla 96.6.2).
 */
@Component
public class IncidentesOperativosRepositorio {

    /** Una pagina, con los filtros como parametros enlazados: nunca interpolados. */
    public List<IncidenteOperativo> pagina(
            DSLContext dsl, Optional<String> estado, Optional<String> severidad, int desde, int limite) {
        List<IncidenteOperativo> filas = new ArrayList<>();
        dsl.fetch(
                        """
                        select id, codigo, titulo, severidad, sistema_afectado, estado, detectado_en, resuelto_en,
                               impacto_usuarios
                          from auditoria.incidente_operativo
                         where (?::text is null or estado = ?::text)
                           and (?::text is null or severidad = ?::text)
                         order by detectado_en desc, id
                         limit ? offset ?
                        """,
                        estado.orElse(null),
                        estado.orElse(null),
                        severidad.orElse(null),
                        severidad.orElse(null),
                        limite,
                        desde)
                .forEach(r -> filas.add(new IncidenteOperativo(
                        r.get("id", UUID.class),
                        r.get("codigo", String.class),
                        r.get("titulo", String.class),
                        r.get("severidad", String.class),
                        r.get("sistema_afectado", String.class),
                        r.get("estado", String.class),
                        r.get("detectado_en", OffsetDateTime.class),
                        Optional.ofNullable(r.get("resuelto_en", OffsetDateTime.class)),
                        r.get("impacto_usuarios", Integer.class))));
        return filas;
    }

    /** Cuantos hay con los mismos filtros, para el total de la paginacion. */
    public int contar(DSLContext dsl, Optional<String> estado, Optional<String> severidad) {
        Integer total = dsl.fetchOne(
                        """
                        select count(*)::int as total
                          from auditoria.incidente_operativo
                         where (?::text is null or estado = ?::text)
                           and (?::text is null or severidad = ?::text)
                        """,
                        estado.orElse(null),
                        estado.orElse(null),
                        severidad.orElse(null),
                        severidad.orElse(null))
                .get("total", Integer.class);
        return total == null ? 0 : total;
    }
}
