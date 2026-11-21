package bo.aportaya.nucleofinanciero;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/**
 * El escenario de la liberacion de CU-13 (mercado de turnos): comprador con Bs 1.000, vendedor y una retencion de compra
 * de Bs 500. El pago de la compra se retiro con la cesion (D-L2-1); la liberacion queda para devolver lo retenido.
 */
abstract class BaseDeMercado extends BaseDeInversiones {

    protected UUID comprador;
    protected UUID cuentaComprador;
    protected UUID vendedor;
    protected UUID cuentaVendedor;
    protected UUID oferta;
    protected UUID retencion;
    protected ContextoSesion servicio;

    @BeforeEach
    void preparar() {
        comprador = fixtura.usuario();
        cuentaComprador = fixtura.billetera(comprador, "ESTANDAR", new BigDecimal("1000.00"));
        vendedor = fixtura.usuario();
        cuentaVendedor = fixtura.billetera(vendedor, "ESTANDAR", BigDecimal.ZERO);
        oferta = UUID.randomUUID();
        retencion = retenerCompra(comprador, cuentaComprador, "500.00", oferta);
        servicio = comoServicio("SERVICIO_ENTREGAS");
    }

    protected static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }
}
