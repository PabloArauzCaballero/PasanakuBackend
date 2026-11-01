package bo.aportaya.notificaciones.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que notificaciones muestra a Sistemas de sus integraciones: sus proveedores y los avisos de entrega que le
 * mandaron. Sin credenciales, sin payload y sin nada que identifique al destinatario (regla 90.2).
 */
public final class PlataformaDeMensajeria {

    private PlataformaDeMensajeria() {}

    public record Proveedor(
            UUID id, String codigo, String nombre, String canales, boolean activo, int prioridad, BigDecimal salud) {}

    public record EventoDeEntrega(
            UUID id,
            String proveedorCodigo,
            String canal,
            String tipoEvento,
            OffsetDateTime recibidoEn,
            OffsetDateTime fechaHoraProveedor,
            Optional<String> codigoError) {}
}
