package bo.aportaya.inversiones;

import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate.EntradaRescate;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Lo comun de las pruebas de rescate: una posicion de fondo lista y el armado de pedidos. */
abstract class BaseDeRescates extends BaseDeInversiones {

    protected String codigoDe(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    protected UUID fondo1000() {
        conSaldo("20000.00");
        return posicionConfirmada(productoFondo, "1000.00"); // 10 cuotas a 100,000000
    }

    protected EntradaRescate pedir(UUID posicion, String cuotas) {
        return new EntradaRescate(
                posicion,
                Optional.ofNullable(cuotas).map(BigDecimal::new),
                UUID.randomUUID().toString());
    }

    protected BigDecimal cuotasDe(UUID posicion) {
        return dslFixtura
                .fetchOne("select cuotas from inversiones.posicion_inversion where id = ?", posicion)
                .get(0, BigDecimal.class);
    }
}
