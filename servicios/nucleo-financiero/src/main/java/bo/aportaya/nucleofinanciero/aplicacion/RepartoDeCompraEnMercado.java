package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;

/**
 * El reparto de una compra en el mercado (decision delegada por el dueno, 2026-10-11; ADR-052): el comprador paga el
 * PRECIO, que es exactamente lo que retuvo; el vendedor cobra {@code precio - cargos} y la plataforma ingresa los
 * {@code cargos}, cada uno con su linea (regla 91.3.4). Sin comision del organizador (RN-18).
 */
final class RepartoDeCompraEnMercado {

    private RepartoDeCompraEnMercado() {}

    /** Verifica que precio y cargos cuadren con lo retenido y devuelve lo que cobra el vendedor. */
    static Dinero paraElVendedor(Dinero precio, Dinero cargos, Dinero retenido) {
        if (precio.moneda() != retenido.moneda()
                || cargos.moneda() != precio.moneda()
                || precio.esNegativo()
                || precio.esCero()
                || !precio.equals(retenido)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(13, 6),
                    "El precio de la oferta (" + precio + ") no es lo retenido (" + retenido + ").");
        }
        if (cargos.esNegativo() || cargos.esMayorQue(precio)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(13, 6),
                    "Los cargos de la oferta (" + cargos + ") no pueden ser negativos ni superar el precio (" + precio
                            + ").");
        }
        return precio.menos(cargos);
    }
}
