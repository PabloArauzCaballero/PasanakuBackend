package bo.aportaya.plataforma.web.errores;

import java.time.Duration;

/**
 * Quien pregunta superó el tope de consultas que se le permite: {@code 429} con
 * {@code Retry-After}.
 *
 * <p>Es para las rutas que contestan un si/no sobre una persona («este contacto pidió no recibir»,
 * «este teléfono tiene cuenta»): sin tope sirven para recorrer una agenda entera (regla 90.1.14).
 * No lleva el contenido de la consulta: el mensaje es fijo.
 */
public class LimiteDeUsoExcedido extends RuntimeException {

    private final Duration reintentarEn;

    public LimiteDeUsoExcedido(Duration reintentarEn) {
        super("limite de uso excedido");
        this.reintentarEn = reintentarEn;
    }

    public Duration reintentarEn() {
        return reintentarEn;
    }
}
