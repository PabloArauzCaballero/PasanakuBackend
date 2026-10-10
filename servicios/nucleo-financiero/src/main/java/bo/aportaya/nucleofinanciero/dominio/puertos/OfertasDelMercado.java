package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.UUID;

/**
 * Los hechos de una oferta del mercado, que son de {@code entregas} y este servicio no lee.
 *
 * <p>CONTRATO PROPUESTO: {@code GET /entregas/mercado/ofertas/{ofertaId}} con vendedor, precio y estado.
 * {@code entregas} todavia no lo publica; hasta entonces la respuesta es vacio y el pago al vendedor
 * se rechaza (falla cerrada). Quien llama NO dicta el beneficiario ni el reparto: se derivan de aqui.
 */
public interface OfertasDelMercado {

    /** {@code estado} tal como lo informa entregas (RESERVADA, LIQUIDANDO, ...). */
    record Oferta(UUID ofertaId, UUID vendedorUsuarioId, Dinero precio, String estado) {}

    /** Vacio si no existe, no responde o la respuesta no se puede leer: quien pregunta no asume nada. */
    Optional<Oferta> oferta(UUID ofertaId);
}
