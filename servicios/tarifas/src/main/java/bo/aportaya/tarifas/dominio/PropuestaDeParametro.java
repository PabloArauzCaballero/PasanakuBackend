package bo.aportaya.tarifas.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * CU-132 · Si un valor propuesto es coherente con su parametro del catalogo. Puro.
 *
 * <p>La base lo vuelve a exigir (R-TAR-15); aca se contesta antes, con un motivo que la persona entiende, en vez de
 * un error de la base. Ninguna regla inventa un valor: solo dice si el que se propone encaja.
 */
public final class PropuestaDeParametro {

    private PropuestaDeParametro() {}

    /** El parametro del catalogo: unidad vacia mientras no este decidida (DECISION_REQUIRED). */
    public record Catalogo(String codigo, Optional<String> unidad, boolean porNivel) {}

    /** El motivo por el que la propuesta no encaja, o vacio si encaja. */
    public static Optional<String> incoherencia(
            Catalogo parametro, Optional<String> nivel, BigDecimal valor, Optional<String> moneda) {
        if (valor == null || valor.signum() < 0) {
            return Optional.of("El valor de un parametro financiero no puede ser negativo.");
        }
        if (parametro.porNivel() != nivel.isPresent()) {
            return Optional.of(
                    parametro.porNivel()
                            ? "Este parametro va por nivel de puntualidad: indica el nivel."
                            : "Este parametro no va por nivel: no indiques un nivel.");
        }
        boolean esMonto = parametro.unidad().map("MONTO"::equals).orElse(false);
        if (esMonto != moneda.isPresent()) {
            return Optional.of(
                    esMonto ? "Este parametro es un monto: indica la moneda." : "Solo un monto lleva moneda.");
        }
        return Optional.empty();
    }

    /** Sin unidad decidida no se aprueba un valor (R-TAR-15). */
    public static boolean aprobable(Catalogo parametro) {
        return parametro.unidad().isPresent();
    }
}
