package bo.aportaya.nucleofinanciero.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Objects;

/**
 * Una respuesta del proveedor que no se puede creer: ni se acredita ni se descarta en
 * silencio, se registra como discrepancia para que alguien la mire.
 *
 * <p>No es lo mismo que un proveedor que no contesta. Un timeout o un 503 dejan el
 * resultado <b>desconocido</b> y se resuelven consultando de nuevo; una firma que no
 * coincide o un estado que contradice al libro son <b>evidencia</b> y se conservan.
 */
public final class DiscrepanciaDelProveedor extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    /** Los motivos que la base admite en {@code discrepancia_proveedor.tipo}. */
    public enum Tipo {
        FIRMA_INVALIDA,
        ESTADO_CONTRADICTORIO,
        MONTO_DISTINTO,
        REFERENCIA_DISTINTA,
        RESPUESTA_INVALIDA
    }

    private final Tipo tipo;
    private final String huella;

    public DiscrepanciaDelProveedor(Tipo tipo, String huella, String detalle) {
        super(CodigoError.de(10, 5), detalle);
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.huella = Objects.requireNonNull(huella, "huella");
    }

    public Tipo tipo() {
        return tipo;
    }

    /** SHA-256 hexadecimal de lo recibido: identifica la evidencia sin guardarla entera. */
    public String huella() {
        return huella;
    }
}
