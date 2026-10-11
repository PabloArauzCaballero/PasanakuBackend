package bo.aportaya.aportes.infraestructura;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Lee {@code catalogo.dia_no_habil}, de SOLO lectura para todo servicio (ADR-029).
 *
 * <p>No escribe ni siembra nada: los feriados son un dato con procedencia que entra por semilla. Aca
 * se leen los de alcance NACIONAL y PLATAFORMA, y los de alcance GRUPO solo del grupo pedido. Los
 * DEPARTAMENTALES no se leen: la tabla no dice a que departamento aplican, y aplicar uno a quien
 * no corresponde seria mover un vencimiento por una razon falsa (decision abierta, ficha O2).
 */
@Component
public class FeriadosRepositorio {

    public Map<LocalDate, String> noHabilesEntre(DSLContext dsl, LocalDate desde, LocalDate hasta, UUID grupoId) {
        var fecha = DSL.field("fecha", LocalDate.class);
        var alcance = DSL.field("alcance", String.class);
        var grupo = DSL.field("grupo_id", UUID.class);
        Map<LocalDate, String> encontrados = new LinkedHashMap<>();
        for (Record fila : dsl.select(fecha, DSL.field("descripcion", String.class))
                .from(DSL.table(DSL.name("catalogo", "dia_no_habil")))
                .where(fecha.between(desde, hasta))
                .and(alcance.in("NACIONAL", "PLATAFORMA")
                        .and(grupo.isNull())
                        .or(alcance.eq("GRUPO").and(grupo.eq(grupoId))))
                .orderBy(fecha)
                .fetch()) {
            encontrados.putIfAbsent(fila.get(fecha), fila.get(DSL.field("descripcion", String.class)));
        }
        return encontrados;
    }

    /** Un anio sin un solo dia cargado no es un anio sin feriados: es un calendario que falta. */
    public boolean hayCalendarioPara(DSLContext dsl, int anio) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("catalogo", "dia_no_habil")),
                        DSL.field("fecha", LocalDate.class)
                                .between(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31)))
                > 0;
    }
}
