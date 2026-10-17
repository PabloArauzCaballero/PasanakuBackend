package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Las cuentas que no son de nadie en particular: la custodia donde se reune lo invertido y
 * los ingresos de la plataforma, y la cuenta operativa de una persona en una moneda.
 *
 * <p>Se resuelven por <b>tipo y moneda</b>, no por un identificador de entorno: la base
 * garantiza una custodia de inversion activa por moneda (R-BIL-25), asi que la pregunta
 * tiene una sola respuesta o ninguna. Ninguna significa «no configurada» y quien pregunta
 * rechaza: mandar plata a una cuenta adivinada no tiene vuelta atras.
 *
 * <p>Son cuentas ajenas al titular que opera: se leen con el rol de sistema y la consulta
 * devuelve solo el identificador, nunca la fila.
 */
@Repository
public class CuentasDeSistemaRepositorio {

    /** La custodia de inversion de esa moneda, si hay una activa. */
    public Optional<UUID> custodiaDeInversion(DSLContext dsl, Moneda moneda) {
        return unicaActiva(dsl, "CUSTODIA_INVERSION", moneda);
    }

    /**
     * Donde van los cargos de la plataforma. Si hubiera mas de una activa no se elige una:
     * repartir ingresos entre dos cuentas «por las dudas» es el error que esta consulta evita.
     */
    public Optional<UUID> ingresosDePlataforma(DSLContext dsl, Moneda moneda) {
        return unicaActiva(dsl, "PLATAFORMA_INGRESOS", moneda);
    }

    /**
     * El fondo de garantia: UNA billetera de sistema por moneda (R-BIL-05, sin grupo). Recibe lo que se cobra para el
     * fondo —hoy, el recargo por mora—; a que grupo corresponde lo lleva {@code garantia} en {@code movimiento_fondo}.
     * Con dos activas no se elige, igual que con los ingresos.
     */
    public Optional<UUID> fondoDeGarantia(DSLContext dsl, Moneda moneda) {
        return unicaActiva(dsl, "FONDO_GARANTIA", moneda);
    }

    /** La cuenta {@code USUARIO} activa de una persona en esa moneda. */
    public Optional<UUID> cuentaOperativaDe(DSLContext dsl, UUID usuarioId, Moneda moneda) {
        List<UUID> cuentas = Datos.comoSistema(dsl, () -> dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field("tipo", String.class).eq("USUARIO"))
                .and(DSL.field("usuario_id", UUID.class).eq(usuarioId))
                .and(DSL.field("moneda", String.class).eq(moneda.name()))
                .and(DSL.field("estado", String.class).eq("ACTIVA"))
                .fetch(DSL.field("id", UUID.class)));
        return cuentas.size() == 1 ? Optional.of(cuentas.get(0)) : Optional.empty();
    }

    private Optional<UUID> unicaActiva(DSLContext dsl, String tipo, Moneda moneda) {
        List<UUID> cuentas = Datos.comoSistema(dsl, () -> dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field("tipo", String.class).eq(tipo))
                .and(DSL.field("moneda", String.class).eq(moneda.name()))
                .and(DSL.field("estado", String.class).eq("ACTIVA"))
                .fetch(DSL.field("id", UUID.class)));
        return cuentas.size() == 1 ? Optional.of(cuentas.get(0)) : Optional.empty();
    }
}
