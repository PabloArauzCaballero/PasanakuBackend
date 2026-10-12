package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.UUID;

/**
 * Los hechos de una oferta del mercado, que son de {@code entregas} y este servicio no lee.
 *
 * <p>CONTRATO: {@code GET /entregas/mercado/ofertas/{ofertaId}} ({@code consultarOfertaParaPago}) con vendedor, precio,
 * cargos y estado. Si {@code entregas} no responde, o no informa los cargos, la respuesta es vacio y el pago al vendedor se
 * rechaza (falla cerrada). Quien llama NO dicta el beneficiario ni el reparto: se derivan de aqui.
 *
 * <p><b>Reparto (decision delegada por el dueno, 2026-10-11):</b> el comprador paga el PRECIO; el vendedor cobra
 * {@code precio - cargos} y la plataforma ingresa los {@code cargos}, cada uno con su linea explicita (regla 91.3.4).
 */
public interface OfertasDelMercado {

    /** {@code estado} tal como lo informa entregas (RESERVADA, LIQUIDANDO, ...). */
    record Oferta(UUID ofertaId, UUID vendedorUsuarioId, Dinero precio, Dinero cargos, String estado) {}

    /** Vacio si no existe, no responde o la respuesta no se puede leer: quien pregunta no asume nada. */
    Optional<Oferta> oferta(UUID ofertaId);
}
