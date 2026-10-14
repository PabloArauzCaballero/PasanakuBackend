package bo.aportaya.erp.aplicacion;

import bo.aportaya.erp.infraestructura.ComprasRepositorio;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import org.jooq.DSLContext;

/**
 * El reintento de un pago a proveedor ya hecho (R-CTB-11): nada se escribe y se responde lo
 * que respondio el original.
 *
 * <p>Va aparte de {@link CU103FacturaDeProveedor} para que el caso de uso siga leyendose de
 * arriba abajo: esto es la rama «ya lo hice», y no tiene mas efecto que decidir si el
 * reintento es el mismo pago o es otro que reusa la clave.
 */
final class ReintentoDePago {

    private ReintentoDePago() {}

    /**
     * La misma clave con el mismo pago (monto, moneda, forma y autorizante) devuelve la
     * factura tal como quedo al registrarse ese pago, aunque despues hayan entrado otros.
     * Con otro pago se rechaza: responder con el viejo le haria creer al cliente que el
     * nuevo se hizo.
     */
    static CU103FacturaDeProveedor.SalidaFactura responder(
            ComprasRepositorio compras,
            DSLContext dsl,
            ComprasRepositorio.Pago previo,
            CU103FacturaDeProveedor.EntradaPago entrada,
            ComprasRepositorio.Factura factura,
            ContextoSesion ctx) {
        boolean mismoPago = previo.monto().compareTo(entrada.monto()) == 0
                && previo.moneda().equals(entrada.moneda())
                && previo.formaPago().equals(entrada.formaPago())
                && previo.autorizadoPor().equals(ctx.usuarioId());
        if (!mismoPago) {
            throw new ErrorDeNegocio(
                    CodigoError.de(103, 5),
                    "Esa clave de idempotencia ya se uso para otro pago de esta factura (R-CTB-11).");
        }
        BigDecimal pagadoEntonces = compras.pagadoHasta(dsl, entrada.facturaId(), previo.fechaPago());
        return new CU103FacturaDeProveedor.SalidaFactura(
                entrada.facturaId(),
                pagadoEntonces.compareTo(factura.monto()) >= 0 ? "PAGADA" : "PAGADA_PARCIAL",
                factura.monto(),
                pagadoEntonces,
                factura.monto().subtract(pagadoEntonces));
    }
}
