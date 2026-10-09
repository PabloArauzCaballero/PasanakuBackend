package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.dominio.TipoProducto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * {@code producto_inversion}, {@code version_condiciones} y {@code valor_cuota}.
 *
 * <p>Las versiones y los valores son append-only: una cotizacion nueva es una fila
 * nueva, y un valor ya publicado no se reescribe.
 */
@Component
public class CatalogoRepositorio {

    public record Producto(
            UUID id,
            String codigo,
            TipoProducto tipo,
            String nombre,
            String emisor,
            String moneda,
            String nivelRiesgo,
            String estado) {}

    public record ValorGuardado(
            LocalDate fecha, BigDecimal valor, OffsetDateTime publicadoEn, OffsetDateTime recibidoEn, String origen) {}

    private static final String VERSION_CON_TIPO =
            "select v.*, p.tipo as tipo_producto from inversiones.version_condiciones v"
                    + " join inversiones.producto_inversion p on p.id = v.producto_inversion_id";

    public Optional<Producto> porCodigo(DSLContext dsl, String codigo) {
        return dsl.selectFrom(t("producto_inversion"))
                .where(f("codigo", String.class).eq(codigo))
                .fetchOptional(this::producto);
    }

    public Optional<Producto> porId(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("producto_inversion"))
                .where(f("id", UUID.class).eq(id))
                .fetchOptional(this::producto);
    }

    /** Idempotente por codigo: el producto es una identidad, sus condiciones son las versiones. */
    public Producto asegurar(
            DSLContext dsl,
            String codigo,
            TipoProducto tipo,
            String nombre,
            String emisor,
            String nivelRiesgo,
            OffsetDateTime ahora) {
        dsl.insertInto(t("producto_inversion"))
                .set(f("id", UUID.class), UUID.randomUUID())
                .set(f("codigo", String.class), codigo)
                .set(f("tipo", String.class), tipo.name())
                .set(f("nombre", String.class), nombre)
                .set(f("emisor", String.class), emisor)
                .set(f("moneda", String.class), "BOB")
                .set(f("nivel_riesgo", String.class), nivelRiesgo)
                .set(f("estado", String.class), "ACTIVO")
                .set(f("creado_en", OffsetDateTime.class), ahora)
                .onConflictDoNothing()
                .execute();
        return porCodigo(dsl, codigo).orElseThrow();
    }

    /** Bloquea el producto: dos sincronizaciones concurrentes no crean dos versiones iguales. */
    public void bloquear(DSLContext dsl, UUID productoId) {
        dsl.select(f("id", UUID.class))
                .from(t("producto_inversion"))
                .where(f("id", UUID.class).eq(productoId))
                .forUpdate()
                .fetch();
    }

    public List<Producto> activos(DSLContext dsl) {
        return dsl.selectFrom(t("producto_inversion"))
                .where(f("estado", String.class).eq("ACTIVO"))
                .orderBy(f("codigo", String.class))
                .fetch(this::producto);
    }

    public Optional<Condiciones> vigente(DSLContext dsl, UUID productoId) {
        return dsl.fetchOptional(
                        VERSION_CON_TIPO + " where v.producto_inversion_id = ? order by v.numero desc limit 1",
                        productoId)
                .map(this::condiciones);
    }

    public Optional<Condiciones> version(DSLContext dsl, UUID versionId) {
        return dsl.fetchOptional(VERSION_CON_TIPO + " where v.id = ?", versionId)
                .map(this::condiciones);
    }

    public int siguienteNumero(DSLContext dsl, UUID productoId) {
        Integer max = dsl.select(org.jooq.impl.DSL.max(f("numero", Integer.class)))
                .from(t("version_condiciones"))
                .where(f("producto_inversion_id", UUID.class).eq(productoId))
                .fetchOne(0, Integer.class);
        return max == null ? 1 : max + 1;
    }

    public UUID insertarVersion(DSLContext dsl, UUID productoId, int numero, Condiciones c, OffsetDateTime ahora) {
        UUID id = c.versionId();
        dsl.insertInto(t("version_condiciones"))
                .set(f("id", UUID.class), id)
                .set(f("producto_inversion_id", UUID.class), productoId)
                .set(f("numero", Short.class), (short) numero)
                .set(f("plazo_dias", Integer.class), c.plazoDias().orElse(null))
                .set(
                        f("base_dias", Short.class),
                        c.baseDias().map(Integer::shortValue).orElse(null))
                .set(
                        f("tasa_nominal_anual", BigDecimal.class),
                        c.tasaNominalAnual().orElse(null))
                .set(f("permite_rescate_anticipado", Boolean.class), c.permiteRescateAnticipado())
                .set(
                        f("penalizacion_anticipo", BigDecimal.class),
                        c.penalizacionAnticipo().orElse(null))
                .set(
                        f("dias_rescate", Short.class),
                        c.diasRescate().map(Integer::shortValue).orElse(null))
                .set(
                        f("hora_corte", String.class),
                        c.horaCorte().map(LocalTime::toString).orElse(null))
                .set(f("monto_minimo", BigDecimal.class), c.montoMinimo())
                .set(f("tasa_retencion", BigDecimal.class), c.tasaRetencion().orElse(null))
                .set(
                        f("tasa_comision_exito", BigDecimal.class),
                        c.tasaComisionExito().orElse(null))
                .set(f("costos_texto", String.class), c.costos())
                .set(f("texto_condiciones", String.class), c.texto())
                .set(f("texto_hash", String.class), c.textoHash())
                .set(f("fuente", String.class), c.fuente())
                .set(
                        f("fecha_cotizacion", OffsetDateTime.class),
                        c.fechaCotizacion().atOffset(java.time.ZoneOffset.UTC))
                .set(f("origen_datos", String.class), c.origen().name())
                .set(f("apto_produccion", Boolean.class), c.aptoProduccion())
                .set(f("vigente_desde", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    // ------------------------------------------------------------------ valor de cuota
    public boolean insertarValor(
            DSLContext dsl,
            UUID productoId,
            LocalDate fecha,
            BigDecimal valor,
            String fuente,
            OrigenDatos origen,
            OffsetDateTime publicadoEn,
            OffsetDateTime ahora) {
        return dsl.insertInto(t("valor_cuota"))
                        .set(f("id", UUID.class), UUID.randomUUID())
                        .set(f("producto_inversion_id", UUID.class), productoId)
                        .set(f("fecha", LocalDate.class), fecha)
                        .set(f("valor", BigDecimal.class), valor)
                        .set(f("moneda", String.class), "BOB")
                        .set(f("fuente", String.class), fuente)
                        .set(f("origen_datos", String.class), origen.name())
                        .set(f("publicado_en", OffsetDateTime.class), publicadoEn)
                        .set(f("recibido_en", OffsetDateTime.class), ahora)
                        .onConflictDoNothing()
                        .execute()
                == 1;
    }

    public Optional<ValorGuardado> valorDelDia(DSLContext dsl, UUID productoId, LocalDate fecha) {
        return dsl.selectFrom(t("valor_cuota"))
                .where(f("producto_inversion_id", UUID.class)
                        .eq(productoId)
                        .and(f("fecha", LocalDate.class).eq(fecha)))
                .fetchOptional(this::valor);
    }

    public Optional<ValorGuardado> ultimoValor(DSLContext dsl, UUID productoId) {
        return dsl.selectFrom(t("valor_cuota"))
                .where(f("producto_inversion_id", UUID.class).eq(productoId))
                .orderBy(f("fecha", LocalDate.class).desc())
                .limit(1)
                .fetchOptional(this::valor);
    }

    private ValorGuardado valor(Record r) {
        return new ValorGuardado(
                r.get("fecha", LocalDate.class),
                r.get("valor", BigDecimal.class),
                r.get("publicado_en", OffsetDateTime.class),
                r.get("recibido_en", OffsetDateTime.class),
                r.get("origen_datos", String.class));
    }

    private Producto producto(Record r) {
        return new Producto(
                r.get("id", UUID.class),
                r.get("codigo", String.class),
                TipoProducto.valueOf(r.get("tipo", String.class)),
                r.get("nombre", String.class),
                r.get("emisor", String.class),
                r.get("moneda", String.class),
                r.get("nivel_riesgo", String.class),
                r.get("estado", String.class));
    }

    private Condiciones condiciones(Record r) {
        return new Condiciones(
                r.get("id", UUID.class),
                r.get("numero", Integer.class),
                TipoProducto.valueOf(r.get("tipo_producto", String.class)),
                Optional.ofNullable(r.get("plazo_dias", Integer.class)),
                Optional.ofNullable(r.get("base_dias", Integer.class)),
                Optional.ofNullable(r.get("tasa_nominal_anual", BigDecimal.class)),
                r.get("permite_rescate_anticipado", Boolean.class),
                Optional.ofNullable(r.get("penalizacion_anticipo", BigDecimal.class)),
                Optional.ofNullable(r.get("dias_rescate", Integer.class)),
                Optional.ofNullable(r.get("hora_corte", String.class)).map(LocalTime::parse),
                r.get("monto_minimo", BigDecimal.class),
                Optional.ofNullable(r.get("tasa_retencion", BigDecimal.class)),
                Optional.ofNullable(r.get("tasa_comision_exito", BigDecimal.class)),
                r.get("costos_texto", String.class),
                r.get("texto_condiciones", String.class),
                r.get("texto_hash", String.class),
                r.get("fuente", String.class),
                r.get("fecha_cotizacion", OffsetDateTime.class).toInstant(),
                OrigenDatos.valueOf(r.get("origen_datos", String.class)),
                r.get("apto_produccion", Boolean.class));
    }
}
