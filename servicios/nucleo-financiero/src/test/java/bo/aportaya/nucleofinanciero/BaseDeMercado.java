package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.SalidaEjecucion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/**
 * El escenario compartido de las pruebas de CU-13 (mercado de turnos): comprador con Bs 1.000, vendedor, la cuenta de
 * ingresos de la plataforma y una oferta de Bs 500 con cargos de Bs 20 ya retenida. {@code entregas} es un DOBLE declarado
 * ({@link BaseDeInversiones.Ofertas}).
 */
abstract class BaseDeMercado extends BaseDeInversiones {

    protected UUID comprador;
    protected UUID cuentaComprador;
    protected UUID vendedor;
    protected UUID cuentaVendedor;
    protected UUID ingresos;
    protected UUID oferta;
    protected UUID retencion;
    protected ContextoSesion servicio;

    @BeforeEach
    void preparar() {
        comprador = fixtura.usuario();
        cuentaComprador = fixtura.billetera(comprador, "ESTANDAR", new BigDecimal("1000.00"));
        vendedor = fixtura.usuario();
        cuentaVendedor = fixtura.billetera(vendedor, "ESTANDAR", BigDecimal.ZERO);
        ingresos = sistemaFx.ingresosDePlataforma("BOB");
        oferta = UUID.randomUUID();
        ofertas.seCae(false);
        // Carril Q (decision delegada por el dueno, 2026-10-11): el comprador paga el PRECIO (500), el vendedor cobra
        // precio - cargos (480) y la plataforma ingresa los cargos (20).
        ofertas.publica(oferta, vendedor, "500.00", "20.00", "RESERVADA");
        retencion = retenerCompra(comprador, cuentaComprador, "500.00", oferta);
        servicio = comoServicio("SERVICIO_ENTREGAS");
    }

    protected SalidaEjecucion pagar(String clave, ContextoSesion quien) {
        return transaccion.execute(t -> pagoCU.ejecutar(retencion, clave, quien));
    }

    protected static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }
}
