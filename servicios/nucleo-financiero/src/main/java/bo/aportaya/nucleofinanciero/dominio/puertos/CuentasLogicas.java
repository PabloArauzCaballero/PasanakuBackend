package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;

/**
 * De cuenta LOGICA a codigo del plan de cuentas.
 *
 * <p>Los servicios que proponen un asiento (garantia, entregas) hablan de bolsillos —CAJA_EMPRESA,
 * RESERVA_RESPALDO, POZO_GRUPO— y no conocen el plan de cuentas. Cual codigo real corresponde a cada
 * uno lo decide contabilidad (ambiguedad A5 del carril C): este servicio NO lo inventa. Sin mapeo la
 * respuesta es vacio y quien consume rechaza, sin registrar nada.
 */
public interface CuentasLogicas {

    /** Una cuenta contable es de una sola moneda (R-CTB-10): el mismo bolsillo logico tiene un codigo por moneda. */
    Optional<String> codigoDe(String cuentaLogica, Moneda moneda);
}
