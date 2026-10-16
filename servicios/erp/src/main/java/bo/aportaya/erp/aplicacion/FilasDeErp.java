package bo.aportaya.erp.aplicacion;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Las filas que {@link ConsultarListadosDeErp} entrega a la web (carril A · A6). Viven en esta capa
 * porque la web no ve {@code infraestructura}; los importes ya son {@link Dinero} con su moneda.
 *
 * <p>Respuesta minima: ni el documento del tercero, ni su contacto, ni la descripcion libre de la orden.
 */
public final class FilasDeErp {

    private FilasDeErp() {}

    public record Partida(
            UUID partidaId,
            UUID presupuestoId,
            String presupuestoNombre,
            String estado,
            UUID centroCostoId,
            String centroCostoNombre,
            UUID cuentaContableId,
            UUID periodoContableId,
            int anio,
            int mes,
            LocalDate fechaInicioPeriodo,
            Dinero presupuestado,
            Dinero ejecutado) {}

    public record Orden(
            UUID ordenCompraId,
            String numero,
            UUID terceroId,
            String terceroRazonSocial,
            Optional<UUID> centroCostoId,
            Optional<String> centroCostoNombre,
            String estado,
            Dinero monto,
            OffsetDateTime creadaEn) {}

    public record Factura(
            UUID facturaProveedorId,
            String numeroFactura,
            UUID terceroId,
            String terceroRazonSocial,
            Optional<UUID> ordenCompraId,
            LocalDate fechaEmision,
            LocalDate fechaVencimiento,
            String estado,
            Dinero monto,
            Dinero pagado,
            Dinero saldoPendiente,
            boolean aprobadaPorLaSesion,
            Optional<UUID> asientoContableId) {}

    public record CuentaPorCobrar(
            UUID cuentaPorCobrarId,
            String origenTipo,
            UUID origenId,
            Optional<UUID> terceroId,
            Optional<String> terceroRazonSocial,
            LocalDate fechaVencimiento,
            String estado,
            Dinero monto,
            Dinero cobrado,
            Dinero saldoPendiente) {}

    public record Activo(
            UUID activoFijoId,
            String codigo,
            String descripcion,
            String categoriaNombre,
            int vidaUtilMeses,
            LocalDate fechaAdquisicion,
            String estado,
            Dinero costo,
            Dinero residual,
            Dinero acumulada,
            Dinero valorEnLibros,
            boolean depreciadoEnElPeriodoVigente) {}
}
