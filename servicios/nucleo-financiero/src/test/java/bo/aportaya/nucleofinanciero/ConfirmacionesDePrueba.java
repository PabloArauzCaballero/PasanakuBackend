package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas.Confirmacion;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jooq.DSLContext;

/** Doble exclusivo de pruebas de dominio; las firmas se prueban en el adaptador HTTP. */
final class ConfirmacionesDePrueba {
    private ConfirmacionesDePrueba() {}

    static Confirmacion para(DSLContext dsl, UUID ordenId) {
        var orden = new OrdenRecargaRepositorio().ver(dsl, ordenId).orElseThrow();
        return new Confirmacion(
                ordenId,
                UUID.nameUUIDFromBytes(ordenId.toString().getBytes(StandardCharsets.UTF_8)),
                orden.bruto(),
                "CONFIRMADO",
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}
