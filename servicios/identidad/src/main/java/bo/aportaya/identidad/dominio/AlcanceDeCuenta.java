package bo.aportaya.identidad.dominio;

import java.util.Optional;

/**
 * Para que sirve una invitacion de operador (CU-09 §10-§11).
 *
 * <p>El alcance es del <b>proposito del token</b>, no de lo que el cliente diga: quien abre el
 * enlace no elige que se le pide, y un token de una invitacion no sirve para restablecer una
 * clave ni al reves.
 */
public enum AlcanceDeCuenta {
    /** Primer acceso: clave elegida por la persona y TOTP propio. */
    ALTA("INVITACION_OPERADOR", true, false, true),
    /** Recuperacion aprobada de la clave: solo cambia la clave. */
    CLAVE("RECUPERACION_OPERADOR_CLAVE", true, false, false),
    /** Reinscripcion aprobada del factor: pide la clave actual y reemplaza solo el TOTP. */
    FACTOR("REINSCRIPCION_FACTOR", false, true, true);

    private final String proposito;
    private final boolean pideClaveNueva;
    private final boolean pideClaveActual;
    private final boolean pideFactor;

    AlcanceDeCuenta(String proposito, boolean pideClaveNueva, boolean pideClaveActual, boolean pideFactor) {
        this.proposito = proposito;
        this.pideClaveNueva = pideClaveNueva;
        this.pideClaveActual = pideClaveActual;
        this.pideFactor = pideFactor;
    }

    /** El valor de {@code token_verificacion.proposito}. */
    public String proposito() {
        return proposito;
    }

    public boolean pideClaveNueva() {
        return pideClaveNueva;
    }

    public boolean pideClaveActual() {
        return pideClaveActual;
    }

    public boolean pideFactor() {
        return pideFactor;
    }

    public static Optional<AlcanceDeCuenta> dePropositoDeToken(String proposito) {
        for (AlcanceDeCuenta alcance : values()) {
            if (alcance.proposito.equals(proposito)) return Optional.of(alcance);
        }
        return Optional.empty();
    }

    /** El alcance de una solicitud de recuperacion: {@code CLAVE} o {@code FACTOR}; {@code ALTA} no se pide. */
    public static Optional<AlcanceDeCuenta> dePedido(String alcance) {
        if ("CLAVE".equals(alcance)) return Optional.of(CLAVE);
        if ("FACTOR".equals(alcance)) return Optional.of(FACTOR);
        return Optional.empty();
    }
}
