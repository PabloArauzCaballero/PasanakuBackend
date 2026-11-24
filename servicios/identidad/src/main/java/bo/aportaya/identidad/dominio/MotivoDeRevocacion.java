package bo.aportaya.identidad.dominio;

import java.util.Objects;

/**
 * El motivo que se escribe en la fila revocada, recortado a lo que la columna admite.
 *
 * <p>{@code asignacion_rol.motivo_revocacion} es {@code VARCHAR(120)} y {@code sesion.motivo_revocacion}
 * {@code VARCHAR(80)}; el motivo de un acto de gobierno puede ser mas largo (la suspension admite 200). Un
 * motivo largo hacia fallar la revocacion entera con {@code AP-BD-03}. El texto completo no se pierde: viaja
 * en el evento de bitacora del acto; la fila guarda el prefijo que dice que acto la cerro.
 */
public final class MotivoDeRevocacion {

    /** Largo de {@code asignacion_rol.motivo_revocacion}. */
    public static final int LARGO_EN_ASIGNACION = 120;

    /** Largo de {@code sesion.motivo_revocacion}. */
    public static final int LARGO_EN_SESION = 80;

    private MotivoDeRevocacion() {}

    public static String de(String prefijo, String motivo, int largo) {
        Objects.requireNonNull(prefijo, "prefijo");
        String texto = prefijo.isEmpty() ? motivo.strip() : prefijo + ": " + motivo.strip();
        return texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
