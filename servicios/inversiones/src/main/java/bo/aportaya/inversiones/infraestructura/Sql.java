package bo.aportaya.inversiones.infraestructura;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Atajos para nombrar tablas y columnas del esquema propio, igual que el resto de los servicios. */
final class Sql {

    static final String ESQUEMA = "inversiones";

    private Sql() {}

    static Table<Record> t(String tabla) {
        return DSL.table(DSL.name(ESQUEMA, tabla));
    }

    static <T> Field<T> f(String columna, Class<T> tipo) {
        return DSL.field(DSL.name(columna), tipo);
    }
}
