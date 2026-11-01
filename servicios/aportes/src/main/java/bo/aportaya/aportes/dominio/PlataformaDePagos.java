package bo.aportaya.aportes.dominio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que aportes muestra a Sistemas de sus integraciones: sus proveedores de pago y los webhooks que recibio. Sin
 * credenciales, sin payload y sin firma: la vista no los necesita y son lo primero que se filtra (regla 90.2).
 */
public final class PlataformaDePagos {

    private PlataformaDePagos() {}

    public record Proveedor(
            UUID id,
            String codigo,
            String nombre,
            String tipo,
            boolean activo,
            boolean soportaWebhook,
            int prioridad) {}

    public record Webhook(
            UUID id,
            String proveedorCodigo,
            String evento,
            String estado,
            boolean firmaValida,
            OffsetDateTime recibidoEn,
            Optional<OffsetDateTime> procesadoEn,
            int intentos) {}
}
