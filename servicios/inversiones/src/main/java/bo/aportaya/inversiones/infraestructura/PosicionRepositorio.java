package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import bo.aportaya.inversiones.dominio.TipoProducto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code posicion_inversion} y el devengo de un DPF.
 *
 * <p>El costo base que le queda a una posicion NO se guarda: se deriva de los
 * comprobantes (append-only), igual que el saldo de una billetera se deriva del libro.
 * Ver {@link ComprobanteRepositorio#costoBaseVigente}.
 */
@Component
public class PosicionRepositorio {

    public record Posicion(
            UUID id,
            UUID usuarioId,
            UUID productoId,
            UUID versionId,
            UUID ordenId,
            TipoProducto tipo,
            BigDecimal principal,
            String moneda,
            BigDecimal cuotas,
            BigDecimal valorCuotaEntrada,
            BigDecimal marcaMaxima,
            LocalDate fechaConstitucion,
            LocalDate fechaVencimiento,
            String posicionExterna,
            String estado) {}

    public record Devengo(LocalDate fecha, int diasAcumulados, BigDecimal interesAcumulado) {}

    public UUID crear(
            DSLContext dsl,
            UUID usuarioId,
            UUID productoId,
            UUID versionId,
            UUID ordenId,
            TipoProducto tipo,
            BigDecimal principal,
            String moneda,
            BigDecimal cuotas,
            BigDecimal valorCuotaEntrada,
            LocalDate constitucion,
            LocalDate vencimiento,
            String posicionExterna,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(t("posicion_inversion"))
                .set(f("id", UUID.class), id)
                .set(f("usuario_id", UUID.class), usuarioId)
                .set(f("producto_inversion_id", UUID.class), productoId)
                .set(f("version_condiciones_id", UUID.class), versionId)
                .set(f("orden_inversion_id", UUID.class), ordenId)
                .set(f("tipo", String.class), tipo.name())
                .set(f("principal", BigDecimal.class), principal)
                .set(f("moneda", String.class), moneda)
                .set(f("cuotas", BigDecimal.class), cuotas)
                .set(f("valor_cuota_entrada", BigDecimal.class), valorCuotaEntrada)
                // La marca maxima de un lote arranca en su valor de entrada: no se cobra
                // exito por ganancias anteriores a que la persona entrara.
                .set(f("marca_maxima", BigDecimal.class), valorCuotaEntrada)
                .set(f("fecha_constitucion", LocalDate.class), constitucion)
                .set(f("fecha_vencimiento", LocalDate.class), vencimiento)
                .set(f("posicion_externa", String.class), posicionExterna)
                .set(f("estado", String.class), "ABIERTA")
                .set(f("version", Integer.class), 0)
                .set(f("creada_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    public Optional<Posicion> porId(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("posicion_inversion"))
                .where(f("id", UUID.class).eq(id))
                .fetchOptional(this::posicion);
    }

    public Optional<Posicion> porIdBloqueando(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("posicion_inversion"))
                .where(f("id", UUID.class).eq(id))
                .forUpdate()
                .fetchOptional(this::posicion);
    }

    public Optional<Posicion> porOrden(DSLContext dsl, UUID ordenId) {
        return dsl.selectFrom(t("posicion_inversion"))
                .where(f("orden_inversion_id", UUID.class).eq(ordenId))
                .fetchOptional(this::posicion);
    }

    public List<Posicion> delTitular(DSLContext dsl, UUID usuarioId) {
        return dsl.selectFrom(t("posicion_inversion"))
                .where(f("usuario_id", UUID.class).eq(usuarioId))
                .orderBy(f("creada_en", OffsetDateTime.class), f("id", UUID.class))
                .fetch(this::posicion);
    }

    public List<Posicion> dpfAbiertos(DSLContext dsl) {
        return dsl.selectFrom(t("posicion_inversion"))
                .where(f("tipo", String.class)
                        .eq("DPF")
                        .and(f("estado", String.class).eq("ABIERTA")))
                .orderBy(f("creada_en", OffsetDateTime.class), f("id", UUID.class))
                .fetch(this::posicion);
    }

    /** Descuenta las cuotas que el aliado ya liquido; la ultima cierra la posicion. */
    public void descontarCuotas(DSLContext dsl, UUID id, BigDecimal restantes, OffsetDateTime ahora) {
        boolean cierra = restantes.signum() == 0;
        dsl.update(t("posicion_inversion"))
                .set(f("cuotas", BigDecimal.class), restantes)
                .set(f("estado", String.class), cierra ? "CERRADA" : "ABIERTA")
                .set(f("cerrada_en", OffsetDateTime.class), cierra ? ahora : null)
                .set(f("version", Integer.class), f("version", Integer.class).plus(1))
                .where(f("id", UUID.class).eq(id))
                .execute();
    }

    public void cerrar(DSLContext dsl, UUID id, OffsetDateTime ahora) {
        dsl.update(t("posicion_inversion"))
                .set(f("estado", String.class), "CERRADA")
                .set(f("cerrada_en", OffsetDateTime.class), ahora)
                .set(f("version", Integer.class), f("version", Integer.class).plus(1))
                .where(f("id", UUID.class).eq(id))
                .execute();
    }

    // ------------------------------------------------------------------ devengo
    /** @return {@code true} si este actor lo inserto; {@code false} si ya estaba (idempotente por posicion y fecha). */
    public boolean insertarDevengo(
            DSLContext dsl,
            UUID posicionId,
            LocalDate fecha,
            int dias,
            BigDecimal acumulado,
            BigDecimal monto,
            String moneda,
            OffsetDateTime ahora) {
        return dsl.insertInto(t("devengo_dpf"))
                        .set(f("id", UUID.class), UUID.randomUUID())
                        .set(f("posicion_inversion_id", UUID.class), posicionId)
                        .set(f("fecha", LocalDate.class), fecha)
                        .set(f("dias_acumulados", Integer.class), dias)
                        .set(f("interes_acumulado", BigDecimal.class), acumulado)
                        .set(f("monto", BigDecimal.class), monto)
                        .set(f("moneda", String.class), moneda)
                        .set(f("creado_en", OffsetDateTime.class), ahora)
                        .onConflictDoNothing()
                        .execute()
                == 1;
    }

    /** El devengo mas reciente hasta {@code fecha} inclusive. */
    public Optional<Devengo> ultimoDevengo(DSLContext dsl, UUID posicionId, LocalDate fecha) {
        return dsl.selectFrom(t("devengo_dpf"))
                .where(f("posicion_inversion_id", UUID.class)
                        .eq(posicionId)
                        .and(f("fecha", LocalDate.class).le(fecha)))
                .orderBy(f("fecha", LocalDate.class).desc())
                .limit(1)
                .fetchOptional(r -> new Devengo(
                        r.get("fecha", LocalDate.class),
                        r.get("dias_acumulados", Integer.class),
                        r.get("interes_acumulado", BigDecimal.class)));
    }

    /** La suma de los devengos diarios: debe ser igual al ultimo acumulado (comprobacion de cuadre). */
    public BigDecimal sumaDeDevengos(DSLContext dsl, UUID posicionId) {
        BigDecimal suma = dsl.select(DSL.sum(f("monto", BigDecimal.class)))
                .from(t("devengo_dpf"))
                .where(f("posicion_inversion_id", UUID.class).eq(posicionId))
                .fetchOne(0, BigDecimal.class);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    private Posicion posicion(Record r) {
        return new Posicion(
                r.get("id", UUID.class),
                r.get("usuario_id", UUID.class),
                r.get("producto_inversion_id", UUID.class),
                r.get("version_condiciones_id", UUID.class),
                r.get("orden_inversion_id", UUID.class),
                TipoProducto.valueOf(r.get("tipo", String.class)),
                r.get("principal", BigDecimal.class),
                r.get("moneda", String.class),
                r.get("cuotas", BigDecimal.class),
                r.get("valor_cuota_entrada", BigDecimal.class),
                r.get("marca_maxima", BigDecimal.class),
                r.get("fecha_constitucion", LocalDate.class),
                r.get("fecha_vencimiento", LocalDate.class),
                r.get("posicion_externa", String.class),
                r.get("estado", String.class));
    }
}
