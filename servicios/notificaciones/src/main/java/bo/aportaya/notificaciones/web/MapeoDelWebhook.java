package bo.aportaya.notificaciones.web;

import bo.aportaya.notificaciones.web.generado.modelo.EntradaRespuesta;
import java.util.Optional;
import java.util.UUID;

/**
 * Los campos de afuera de la carga firmada del webhook (CU-82), deprecados desde F1.L5.M23: se leen solo para que
 * el caso de uso compruebe que coinciden con lo firmado. Se borra con los campos.
 */
@SuppressWarnings("deprecation")
final class MapeoDelWebhook {

    private MapeoDelWebhook() {}

    static String canal(EntradaRespuesta c) {
        return c.getCanal() == null ? null : c.getCanal().getValue();
    }

    static String remitente(EntradaRespuesta c) {
        return c.getRemitente();
    }

    static String contenido(EntradaRespuesta c) {
        return c.getContenido();
    }

    static String clave(EntradaRespuesta c) {
        return c.getClaveIdempotencia();
    }

    static Optional<UUID> relacionada(EntradaRespuesta c) {
        return Optional.ofNullable(c.getNotificacionRelacionadaId());
    }
}
