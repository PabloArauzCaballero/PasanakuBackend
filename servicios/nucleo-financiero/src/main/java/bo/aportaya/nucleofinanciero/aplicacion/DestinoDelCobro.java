package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DestinoRepositorio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Adónde va un aporte cobrado. El recargo por mora no es del grupo: va al fondo de garantía (tableros, D1). Repartirlo
 * en el pozo sería premiar a todos con la mora de uno. Un evento sin {@code tipo} (anterior al campo) va al grupo,
 * como siempre: así conviven el productor viejo y el nuevo durante el despliegue.
 */
enum DestinoDelCobro {
    GRUPO("SALDO_GRUPOS", "El grupo", "La billetera del grupo", "APORTE_A_GRUPO", "Aporte cobrado, pago "),
    FONDO_GARANTIA(
            // La misma cuenta lógica que usa garantia para sus coberturas.
            "RESERVA_RESPALDO",
            "El fondo de garantia",
            "La billetera del fondo de garantia",
            "RECARGO_MORA",
            "Recargo por mora cobrado, pago ");

    /** El tipo de obligación que NO es del grupo. */
    static final String RECARGO_MORA = "RECARGO_MORA";

    final String cuentaLogica;
    final String quien;
    final String billetera;
    final String tipoTransaccion;
    final String glosa;

    DestinoDelCobro(String cuentaLogica, String quien, String billetera, String tipoTransaccion, String glosa) {
        this.cuentaLogica = cuentaLogica;
        this.quien = quien;
        this.billetera = billetera;
        this.tipoTransaccion = tipoTransaccion;
        this.glosa = glosa;
    }

    static DestinoDelCobro de(Optional<String> tipoDeObligacion) {
        return tipoDeObligacion.filter(RECARGO_MORA::equals).isPresent() ? FONDO_GARANTIA : GRUPO;
    }

    /** La billetera que recibe, en esa moneda: la del grupo, o la de sistema del fondo (una por moneda). */
    Optional<UUID> cuenta(
            DSLContext dsl, DestinoRepositorio destinos, CuentasDeSistemaRepositorio sistema, UUID grupoId, Moneda m) {
        return this == FONDO_GARANTIA
                ? sistema.fondoDeGarantia(dsl, m)
                : destinos.cuentaDeGrupoEn(dsl, grupoId, m.name());
    }
}
