package bo.aportaya.plataforma.web.traza;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * El identificador de la peticion en curso, accesible desde cualquier capa sin
 * pasarlo por parametro.
 *
 * <p>Vive en el MDC de SLF4J: toda linea de registro lo lleva, y lo mismo el cuerpo
 * de un error {@code 500} —donde es lo UNICO que sale—. Con hilos virtuales el MDC
 * viaja con el hilo, que es exactamente lo que hace falta.
 */
public final class Traza {

    public static final String CABECERA = "X-Request-Id";
    static final String CLAVE = "trazaId";
    static final String CLAVE_CU = "cu";
    static final String CLAVE_USUARIO = "usuarioId";

    private Traza() {}

    public static String actual() {
        String valor = MDC.get(CLAVE);
        return valor != null ? valor : "sin-traza";
    }

    static void fijar(String valor) {
        MDC.put(CLAVE, normalizar(valor));
    }

    /**
     * La traza siempre queda como UUID canonico.
     *
     * <p>NGINX propaga {@code X-Request-Id} con su {@code $request_id}, que son 32
     * digitos hexadecimales SIN guiones. Media docena de casos de uso hacen
     * {@code UUID.fromString(ctx.traza().id())} para guardar la traza en el outbox, y
     * con ese formato reventaban con {@code Invalid UUID string} — todo lo que
     * entraba por la entrada publica terminaba en 500, incluido el ingreso. Se
     * normaliza aca, en el borde, y no en cada caso de uso: un formato invalido que
     * se arregla en seis lugares se olvida en el septimo.
     */
    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String limpio = valor.trim();
        if (limpio.length() == 32 && limpio.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return new StringBuilder(limpio)
                    .insert(20, '-')
                    .insert(16, '-')
                    .insert(12, '-')
                    .insert(8, '-')
                    .toString();
        }
        return limpio;
    }

    /** Lo que hace que una traza de produccion lleve al caso de uso sin herramientas. */
    public static void marcarCasoDeUso(String cu, String usuarioId) {
        MDC.put(CLAVE_CU, cu);
        MDC.put(CLAVE_USUARIO, usuarioId);
    }

    static void limpiar() {
        MDC.remove(CLAVE);
        MDC.remove(CLAVE_CU);
        MDC.remove(CLAVE_USUARIO);
    }
}
