package bo.aportaya.entregas.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * Lo que `entregas` afirma de una oferta para que `nucleo-financiero` pague al vendedor: quien vende, a que precio y
 * en que estado. Nada mas (respuesta minima, regla 90.1.8): ni el grupo, ni el turno, ni los cargos, ni el comprador.
 */
public record OfertaParaElPago(UUID ofertaId, UUID vendedorUsuarioId, Dinero precio, String estado) {}
