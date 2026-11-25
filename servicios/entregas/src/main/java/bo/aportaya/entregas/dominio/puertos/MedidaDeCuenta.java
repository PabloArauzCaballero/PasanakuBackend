package bo.aportaya.entregas.dominio.puertos;

import java.util.UUID;

/**
 * Si el beneficiario tiene una medida vigente sobre su cuenta que retiene la entrega (CU-134, D-11).
 *
 * <p>La medida vive en {@code identidad}; este servicio no la lee (regla 98.1) y pregunta FUERA de la
 * transaccion (invariante 6). {@code identidad} contesta solo un booleano, nunca la causal ni la evidencia.
 * Ante la duda la respuesta es {@code true}: retener demora un pago, desembolsar a una cuenta bajo medida
 * puede ser irreversible.
 */
public interface MedidaDeCuenta {
    boolean retieneEntrega(UUID usuarioId);
}
