package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.infraestructura.OrdenRepositorio.Orden;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lo que la persona ve de su orden, con el estado intermedio dicho en voz clara. */
public record VistaOrden(
        UUID ordenId,
        String estado,
        String productoCodigo,
        BigDecimal monto,
        String moneda,
        UUID versionCondicionesId,
        UUID consentimientoId,
        UUID posicionId,
        LocalDate fechaValor,
        String motivoRechazo,
        String mensaje) {

    static VistaOrden de(Orden o, String productoCodigo, UUID posicionId) {
        return new VistaOrden(
                o.id(),
                o.estado(),
                productoCodigo,
                o.monto(),
                o.moneda(),
                o.versionId(),
                o.consentimientoId(),
                posicionId,
                o.fechaValor(),
                o.motivoRechazo(),
                mensaje(o.estado()));
    }

    static String mensaje(String estado) {
        return switch (estado) {
            case "CREADA" -> "Estamos reservando el importe de tu saldo disponible.";
            case "RETENIDA" -> "El importe esta reservado; falta enviar la orden al aliado.";
            case "ENVIADA" -> "La orden esta enviada; el aliado todavia no la confirmo.";
            case "INCIERTA" ->
                "No pudimos confirmar la orden con el aliado. Tu importe sigue reservado y lo vamos a consultar.";
            case "CONFIRMADA" -> "Inversion confirmada. El valor puede subir o bajar: no hay rendimiento garantizado.";
            case "RECHAZADA" -> "La orden no se concreto. Lo que estaba reservado se libera a tu saldo disponible.";
            default -> "La orden fue cancelada.";
        };
    }
}
