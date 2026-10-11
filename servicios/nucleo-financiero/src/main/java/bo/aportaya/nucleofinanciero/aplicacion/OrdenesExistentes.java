package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ¿Esta clave ya abrio una orden en esta billetera? Lo que decide si una peticion es un reintento.
 *
 * <p>Se pregunta antes de cotizar: el reintento de una orden ya confirmada no debe exigir una
 * cotizacion vigente, porque el precio se acepto la primera vez. Solo responde para el titular; para
 * cualquier otro es «no», y la operacion misma rechaza el acceso.
 */
@Service
public class OrdenesExistentes {

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final OrdenRecargaRepositorio recargas;
    private final OrdenRetiroRepositorio retiros;

    public OrdenesExistentes(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            OrdenRecargaRepositorio recargas,
            OrdenRetiroRepositorio retiros) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.recargas = recargas;
        this.retiros = retiros;
    }

    @Transactional(readOnly = true)
    public boolean recarga(UUID cuentaId, String clave, ContextoSesion ctx) {
        return datos.conContexto(
                ctx,
                dsl -> propia(dsl, cuentaId, ctx)
                        && recargas.porClaveIdempotencia(dsl, cuentaId, clave).isPresent());
    }

    @Transactional(readOnly = true)
    public boolean retiro(UUID cuentaId, String clave, ContextoSesion ctx) {
        return datos.conContexto(
                ctx,
                dsl -> propia(dsl, cuentaId, ctx)
                        && retiros.porClaveIdempotencia(dsl, cuentaId, clave).isPresent());
    }

    private boolean propia(org.jooq.DSLContext dsl, UUID cuentaId, ContextoSesion ctx) {
        return cuentas.ver(dsl, cuentaId)
                .filter(c -> ctx.usuarioId().equals(c.usuarioId()))
                .isPresent();
    }
}
