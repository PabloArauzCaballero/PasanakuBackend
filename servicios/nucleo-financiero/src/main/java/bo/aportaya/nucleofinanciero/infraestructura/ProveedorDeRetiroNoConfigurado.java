package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * F1.L1.M04 (ANF-HUECO-005) · el puerto de desembolso fuera de {@code local}/{@code test}, mientras no haya
 * proveedor contratado (P-15): falla cerrado y lo dice.
 *
 * <p>Antes no habia ningun bean del puerto fuera de esos perfiles, asi que el servicio no arrancaba con un
 * perfil productivo y TEST tenia que correr con {@code local}, que arrastra los dobles de desarrollo (el
 * origen de ANF-SEG-001). Con este adaptador arranca con cualquier perfil, y ningun retiro sale.
 *
 * <p>{@link #instruir} no devuelve {@code RECHAZADO}: eso diria que el proveedor rechazo en firme y CU-11
 * liberaria la orden como rechazada. Que no haya proveedor no es un rechazo de negocio, es una ausencia: se
 * lanza {@code AP-CU11-15 PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO} y la orden queda {@code AUTORIZADA}, con su
 * saldo retenido, hasta que exista el adaptador real. {@link #consultar} contesta {@code TIMEOUT}: no se sabe,
 * y la reconciliacion no resuelve nada por suposicion.
 */
@Component
@Profile("!local & !test")
public class ProveedorDeRetiroNoConfigurado implements ProveedorDeRetiro {

    public static final String CODIGO = "PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO";

    @Override
    public Resultado instruir(UUID ordenRetiroId, Dinero monto) {
        throw new ErrorDeNegocio(
                CodigoError.de(11, 15),
                CODIGO + ": todavia no hay un proveedor de desembolso contratado; el retiro queda autorizado y"
                        + " retenido, sin salir.");
    }

    @Override
    public Estado consultar(String referencia) {
        return Estado.TIMEOUT;
    }
}
