package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code cuenta_billetera}: el saldo, su estado y su nivel.
 *
 * <p>El saldo **no se guarda como verdad**: se deriva del libro append-only y las
 * columnas son un cache que la base recalcula. Por eso aca no hay ningun metodo que
 * sume o reste saldo — quien mueve plata escribe movimientos, y el saldo sale de
 * ellos. Un {@code UPDATE saldo = saldo + x} seria una cifra que nadie puede
 * reconstruir despues.
 *
 * <p><b>H2.S1.M5 · la cache no autoriza nada.</b> {@code disponible} y {@code retenido} que
 * devuelve este repositorio se calculan desde el LIBRO ({@code movimiento_billetera} y las
 * retenciones vigentes), no se leen de las columnas {@code saldo_*}. Si la cache quedo
 * atrasada o alguien la toco, una decision de dinero (retener, transferir, retirar) se
 * toma igual contra lo que dice el libro. La columna se sigue leyendo solo para
 * detectar la discrepancia, no para decidir.
 */
@Component
public class CuentaBilleteraRepositorio {

    /**
     * La cuenta, **bloqueada** para la operacion.
     *
     * <p>{@code FOR UPDATE} y no una lectura suelta: dos operaciones sobre la misma
     * billetera tienen que serializarse, o las dos leen el mismo saldo y las dos
     * pasan.
     */
    public Optional<Cuenta> bloquear(DSLContext dsl, UUID cuentaId) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("usuario_id", UUID.class),
                        DSL.field("moneda", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("nivel_debida_diligencia", String.class),
                        DSL.field("permite_saldo_negativo", Boolean.class),
                        DSL.field("version", Integer.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field("id", UUID.class).eq(cuentaId))
                .forUpdate()
                .fetchOne();
        return Optional.ofNullable(fila).map(f -> mapear(dsl, f));
    }

    /**
     * La cuenta {@code USUARIO} activa de una persona en esa moneda, vista con la sesion de quien pregunta.
     * Vacio si no hay ninguna o hay mas de una: no se elige por la persona.
     */
    public Optional<UUID> operativaDe(DSLContext dsl, UUID usuarioId, Moneda moneda) {
        var ids = dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field("tipo", String.class).eq("USUARIO"))
                .and(DSL.field("usuario_id", UUID.class).eq(usuarioId))
                .and(DSL.field("moneda", String.class).eq(moneda.name()))
                .and(DSL.field("estado", String.class).eq("ACTIVA"))
                .fetch(DSL.field("id", UUID.class));
        return ids.size() == 1 ? Optional.of(ids.get(0)) : Optional.empty();
    }

    /** Lectura sin bloqueo, para consultar. Nunca para decidir sobre plata. */
    public Optional<Cuenta> ver(DSLContext dsl, UUID cuentaId) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("usuario_id", UUID.class),
                        DSL.field("moneda", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("nivel_debida_diligencia", String.class),
                        DSL.field("permite_saldo_negativo", Boolean.class),
                        DSL.field("version", Integer.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field("id", UUID.class).eq(cuentaId))
                .fetchOne();
        return Optional.ofNullable(fila).map(f -> mapear(dsl, f));
    }

    /**
     * Disponible y retenido desde el libro: {@code SUM(movimientos) - SUM(retenciones vigentes)}.
     * Es la misma formula que {@code fn_bil_recalcular_saldos}, evaluada ahora y no cacheada.
     * Se lee con el rol de sistema porque el libro es del sistema; el filtro por cuenta lo pone
     * esta consulta, y a ella solo se llega despues de que la fila de la cuenta fue visible
     * para quien pregunta.
     */
    public static SaldosDelLibro delLibro(DSLContext dsl, UUID cuentaId, Moneda moneda) {
        return Datos.comoSistema(dsl, () -> {
            var signado = DSL.when(
                            DSL.field("sentido", String.class).eq("CREDITO"), DSL.field("monto", BigDecimal.class))
                    .otherwise(DSL.field("monto", BigDecimal.class).neg());
            BigDecimal movimientos = dsl.select(DSL.coalesce(DSL.sum(signado), BigDecimal.ZERO))
                    .from(DSL.table(DSL.name("nucleo_financiero", "movimiento_billetera")))
                    .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                    .fetchOne(0, BigDecimal.class);
            BigDecimal retenido = dsl.select(
                            DSL.coalesce(DSL.sum(DSL.field("monto", BigDecimal.class)), BigDecimal.ZERO))
                    .from(DSL.table(DSL.name("nucleo_financiero", "retencion_saldo")))
                    .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                    .and(DSL.field("estado", String.class).eq("VIGENTE"))
                    .fetchOne(0, BigDecimal.class);
            return new SaldosDelLibro(Dinero.de(movimientos.subtract(retenido), moneda), Dinero.de(retenido, moneda));
        });
    }

    /** Lo que dice el libro ahora mismo: es lo unico que autoriza un debito. */
    public record SaldosDelLibro(Dinero disponible, Dinero retenido) {}

    private static Cuenta mapear(DSLContext dsl, Record fila) {
        Moneda moneda = Moneda.valueOf(fila.get("moneda", String.class));
        UUID id = fila.get("id", UUID.class);
        SaldosDelLibro libro = delLibro(dsl, id, moneda);
        return new Cuenta(
                id,
                fila.get("usuario_id", UUID.class),
                moneda,
                fila.get("estado", String.class),
                fila.get("nivel_debida_diligencia", String.class),
                libro.disponible(),
                libro.retenido(),
                fila.get("permite_saldo_negativo", Boolean.class),
                fila.get("version", Integer.class));
    }

    public record Cuenta(
            UUID id,
            UUID usuarioId,
            Moneda moneda,
            String estado,
            String nivelDiligencia,
            Dinero disponible,
            Dinero retenido,
            boolean permiteNegativo,
            int version) {

        /** Solo una cuenta activa opera. El resto —cerrada, limitada— no. */
        public boolean operativa() {
            return "ACTIVA".equals(estado);
        }
    }
}
