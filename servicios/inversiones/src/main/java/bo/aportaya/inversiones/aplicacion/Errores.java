package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Map;

/** Los {@code AP-CU<NN>-<nn>} de este servicio, en un solo lugar para que el contrato y el codigo no diverjan. */
final class Errores {

    private Errores() {}

    static ErrorDeNegocio de(int caso, int numero, String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(caso, numero), mensaje);
    }

    static ErrorDeNegocio de(int caso, int numero, String mensaje, Map<String, Object> detalle) {
        return new ErrorDeNegocio(CodigoError.de(caso, numero), mensaje, detalle);
    }
}
