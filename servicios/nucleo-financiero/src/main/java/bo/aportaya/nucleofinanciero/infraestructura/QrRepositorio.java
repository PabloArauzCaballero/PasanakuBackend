package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code qr_transferencia}: el cobro que una billetera publica con un QR interno.
 *
 * <p>La fila es la verdad: el contenido del QR solo lleva el identificador y una firma. Importe,
 * vencimiento y estado viven aca, de modo que alterar el QR no puede cambiar a quien se paga ni
 * cuanto. El estado cambia con un UPDATE condicionado a que siga vigente: la carrera la decide la
 * base, no un {@code if}.
 */
@Component
public class QrRepositorio {

    public UUID crear(
            DSLContext dsl,
            UUID cuentaId,
            String modalidad,
            Optional<Dinero> monto,
            Moneda moneda,
            Optional<String> concepto,
            Optional<OffsetDateTime> expiraEn,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "qr_transferencia")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("cuenta_billetera_id", UUID.class), cuentaId)
                .set(DSL.field("modalidad", String.class), modalidad)
                .set(
                        DSL.field("monto", BigDecimal.class),
                        monto.map(Dinero::monto).orElse(null))
                .set(DSL.field("moneda", String.class), moneda.name())
                .set(DSL.field("concepto", String.class), concepto.orElse(null))
                .set(DSL.field("estado", String.class), "VIGENTE")
                .set(DSL.field("expira_en", OffsetDateTime.class), expiraEn.orElse(null))
                .set(DSL.field("creado_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    /** El QR bloqueado: dos pagos del mismo QR dinamico se turnan y el segundo lo encuentra usado. */
    public Optional<Qr> bloquear(DSLContext dsl, UUID qrId) {
        return consultar(dsl, qrId, true);
    }

    /** Lectura sin bloqueo, para mostrarle a quien va a pagar a quien y cuanto. */
    public Optional<Qr> ver(DSLContext dsl, UUID qrId) {
        return consultar(dsl, qrId, false);
    }

    private Optional<Qr> consultar(DSLContext dsl, UUID qrId, boolean bloquear) {
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("cuenta_billetera_id", UUID.class),
                        DSL.field("modalidad", String.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        DSL.field("concepto", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("expira_en", OffsetDateTime.class),
                        DSL.field("transaccion_id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "qr_transferencia")))
                .where(DSL.field("id", UUID.class).eq(qrId));
        Record fila = bloquear ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila).map(QrRepositorio::mapear);
    }

    private static Qr mapear(Record f) {
        Moneda moneda = Moneda.valueOf(f.get("moneda", String.class));
        BigDecimal monto = f.get("monto", BigDecimal.class);
        return new Qr(
                f.get("id", UUID.class),
                f.get("cuenta_billetera_id", UUID.class),
                f.get("modalidad", String.class),
                Optional.ofNullable(monto).map(m -> Dinero.de(m, moneda)),
                moneda,
                Optional.ofNullable(f.get("concepto", String.class)),
                f.get("estado", String.class),
                Optional.ofNullable(f.get("expira_en", OffsetDateTime.class))
                        .map(t -> t.withOffsetSameInstant(java.time.ZoneOffset.UTC)),
                Optional.ofNullable(f.get("transaccion_id", UUID.class)));
    }

    /** Consume el QR dinamico, solo si sigue vigente. */
    public boolean usar(DSLContext dsl, UUID qrId, UUID transaccionId, OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "qr_transferencia")))
                        .set(DSL.field("estado", String.class), "USADO")
                        .set(DSL.field("transaccion_id", UUID.class), transaccionId)
                        .set(DSL.field("usado_en", OffsetDateTime.class), ahora)
                        .where(DSL.field("id", UUID.class).eq(qrId))
                        .and(DSL.field("estado").eq("VIGENTE"))
                        .and(DSL.field("modalidad").eq("DINAMICO"))
                        .execute()
                > 0;
    }

    /** El numero de cuenta enmascarado: lo unico de la cuenta destino que ve quien paga. */
    public Optional<String> numeroDeCuenta(DSLContext dsl, UUID cuentaId) {
        return Optional.ofNullable(dsl.select(DSL.field("numero_cuenta", String.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field("id", UUID.class).eq(cuentaId))
                .fetchOne(DSL.field("numero_cuenta", String.class)));
    }

    public record Qr(
            UUID id,
            UUID cuentaId,
            String modalidad,
            Optional<Dinero> monto,
            Moneda moneda,
            Optional<String> concepto,
            String estado,
            Optional<OffsetDateTime> expiraEn,
            Optional<UUID> transaccionId) {

        public boolean esDinamico() {
            return "DINAMICO".equals(modalidad);
        }
    }
}
