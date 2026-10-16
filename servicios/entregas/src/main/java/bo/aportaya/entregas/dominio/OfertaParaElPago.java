package bo.aportaya.entregas.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * Lo que `entregas` afirma de una oferta para que `nucleo-financiero` pague al vendedor: quien vende, a que precio, con
 * que cargos y en que estado. Nada mas (respuesta minima, regla 90.1.8): ni el grupo, ni el turno, ni el comprador.
 *
 * <p>Los cargos viajan porque el reparto lo decide la oferta (decision adoptada por delegacion del dueno, 2026-10-11): el
 * comprador paga el PRECIO, el vendedor cobra `precio - cargos` y la plataforma ingresa los cargos; `nucleo-financiero`
 * no puede derivarlos de lo retenido (que es el precio) y no los inventa.
 */
public record OfertaParaElPago(UUID ofertaId, UUID vendedorUsuarioId, Dinero precio, Dinero cargos, String estado) {}
