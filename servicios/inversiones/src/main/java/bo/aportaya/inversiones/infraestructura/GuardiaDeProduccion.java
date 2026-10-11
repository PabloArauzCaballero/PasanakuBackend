package bo.aportaya.inversiones.infraestructura;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El proceso NO arranca en un entorno productivo con el aliado simulado.
 *
 * <p>Los productos, tasas, retenciones y comisiones de este servicio son SINTETICOS
 * mientras no exista una decision de negocio con fuente ({@code DR-INV-01..04}). Dejar que
 * un proceso productivo hable con el simulador seria presentar plata de mentira como real,
 * asi que la guarda corta el arranque, antes de abrir un solo puerto.
 *
 * <p>{@code aportaya.entorno.productivo} la fijan los perfiles ({@code staging} y
 * {@code production} la dejan en {@code true}); si falta, se asume productivo: fallar
 * cerrado es la unica omision aceptable.
 */
@Component
public class GuardiaDeProduccion {

    private final boolean productivo;

    public GuardiaDeProduccion(
            @Value("${aportaya.entorno.productivo:true}") boolean productivo,
            @Value("${aportaya.inversiones.aliado.modo:deshabilitado}") String modoDelAliado) {
        if (productivo && !"deshabilitado".equals(modoDelAliado)) {
            throw new IllegalStateException("inversiones: el aliado '" + modoDelAliado
                    + "' (datos SINTETICOS) no puede correr en un entorno productivo."
                    + " Use aportaya.inversiones.aliado.modo=deshabilitado hasta que exista un aliado contratado.");
        }
        this.productivo = productivo;
    }

    public boolean productivo() {
        return productivo;
    }
}
