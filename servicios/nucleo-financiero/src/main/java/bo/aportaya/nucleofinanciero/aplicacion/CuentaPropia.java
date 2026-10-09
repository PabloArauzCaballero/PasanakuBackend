package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ¿Esta billetera es de quien pregunta? Para lo que se hace antes de abrir la transaccion de la
 * operacion (cotizar, por ejemplo) y no debe valer para billeteras ajenas.
 *
 * <p>Vive en su propia clase porque {@code @Transactional} solo tiene efecto cuando la llamada entra
 * por el proxy: llamado desde adentro del mismo objeto correria sin transaccion y sin
 * {@code SET LOCAL} (invariante 3).
 */
@Service
public class CuentaPropia {

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;

    public CuentaPropia(Datos datos, CuentaBilleteraRepositorio cuentas) {
        this.datos = datos;
        this.cuentas = cuentas;
    }

    /** @return la moneda de la billetera, para cotizar en ella. */
    @Transactional(readOnly = true)
    public bo.aportaya.plataforma.dominio.Moneda exigirTitular(UUID cuentaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var cuenta = cuentas.ver(dsl, cuentaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 1), "Esa billetera no existe."));
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(13, 1), "Esa billetera no es tuya.");
            }
            return cuenta.moneda();
        });
    }
}
