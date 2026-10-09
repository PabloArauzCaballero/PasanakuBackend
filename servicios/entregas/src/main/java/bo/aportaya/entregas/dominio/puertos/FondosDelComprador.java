package bo.aportaya.entregas.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * El saldo del comprador en {@code nucleo-financiero}: retener, pagar al vendedor, liberar.
 *
 * <p>CONTRATO PROPUESTO. Existen {@code retenerSaldo} y {@code cerrarRetencion(LIBERADA|EJECUTADA)},
 * pero NO existe «ejecutar una retencion A FAVOR de un tercero» (hoy ejecutar es un debito sin
 * destinatario). Esa operacion la define nucleo-financiero; mientras no exista, la implementacion
 * por omision deniega ({@code NO_DISPONIBLE}) y las pruebas corren contra un doble de tres niveles.
 * Este servicio no mueve saldo (invariante 12): solo lo pide.
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

    Referencia retener(UUID compradorUsuarioId, Dinero monto, String clave);

    /** Ejecuta la retencion a favor del vendedor: `paraElVendedor` a el y `cargos` a la plataforma. */
    Referencia pagarAlVendedor(
            UUID retencionRef, UUID vendedorUsuarioId, Dinero paraElVendedor, Dinero cargos, String clave);

    Resultado liberar(UUID retencionRef, String clave);
}
