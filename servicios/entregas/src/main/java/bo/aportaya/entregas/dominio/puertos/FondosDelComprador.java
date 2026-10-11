package bo.aportaya.entregas.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * El saldo del comprador en {@code nucleo-financiero}: retener, pagar al vendedor, liberar.
 *
 * <p>Implementado por {@code FondosPorHttp} contra {@code retenerSaldo}, {@code cerrarRetencion(LIBERADA)} y
 * {@code pagarAlTercero} (carril G). La retencion nombra la OFERTA ({@code COMPRA_EN_MERCADO},
 * {@code OFERTA_MERCADO}): es lo unico que nucleo usa para saber a quien pagar, porque quien llama NO dicta ni el
 * beneficiario ni el reparto. Este servicio no mueve saldo (invariante 12): solo lo pide.
 *
 * <p>Las tres operaciones son idempotentes por {@code clave}: repetirlas tras una caida devuelve
 * la misma referencia y no retiene ni paga dos veces. Retener va ANTES de dar el titulo (es
 * compensable); pagar al vendedor va al final (es lo irreversible).
 */
public interface FondosDelComprador {

    enum Resultado {
        OK,
        SALDO_INSUFICIENTE,
        NO_DISPONIBLE
    }

    record Referencia(Resultado resultado, UUID referencia) {}

    Referencia retener(UUID compradorUsuarioId, UUID ofertaId, Dinero monto, String clave);

    /**
     * Ejecuta la retencion a favor del vendedor: `paraElVendedor` a el y `cargos` a la plataforma. Los importes
     * son lo que ESTE servicio espera; el libro los deriva de la oferta y rechaza si no cuadran con lo retenido.
     */
    Referencia pagarAlVendedor(
            UUID retencionRef, UUID vendedorUsuarioId, Dinero paraElVendedor, Dinero cargos, String clave);

    Resultado liberar(UUID retencionRef, String clave);
}
