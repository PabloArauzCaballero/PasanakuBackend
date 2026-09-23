package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.plataforma.dominio.CodigoError;

/** Traduce los veredictos de retiro a los codigos publicos de CU-11. */
final class CodigosDeRetiro {
    private CodigosDeRetiro() {}

    static CodigoError de(String codigo) {
        return switch (codigo) {
            case "SALDO_INSUFICIENTE" -> CodigoError.de(11, 1);
            case "MFA_REQUERIDO" -> CodigoError.de(11, 2);
            case "INSTRUMENTO_EN_ENFRIAMIENTO" -> CodigoError.de(11, 3);
            case "TITULAR_NO_COINCIDE" -> CodigoError.de(11, 4);
            case "BLOQUEO_DE_AUTORIDAD" -> CodigoError.de(11, 5);
            case "ENCAJE_INCUMPLIDO" -> CodigoError.de(11, 6);
            default -> CodigoError.de(11, 7);
        };
    }
}
