package bo.aportaya.transparencia.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

/**
 * La forma canonica de un contenido: **orden fijo de campos, importes como cadena,
 * fechas en UTC**.
 *
 * <p>Un hash solo sirve si dos implementaciones producen el mismo. Si el orden de las
 * claves dependiera del {@code HashMap} de turno, dos verificadores honestos
 * obtendrian hashes distintos del mismo hecho y la cadena diria que el grupo fue
 * alterado cuando no lo fue. Por eso: {@link TreeMap}, nunca {@code Map.of}.
 *
 * <p>Y por eso los importes van como cadena. {@code 100.0} y {@code 100.00} son el
 * mismo dinero y hashes distintos; {@code 0.1 + 0.2} en coma flotante no es
 * {@code 0.3} en ningun lenguaje. La escala se fija, se serializa el texto, y el
 * verificador de veinte lineas en otro lenguaje llega al mismo resultado.
 */
public final class ContenidoCanonico {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ISO_INSTANT;

    private ContenidoCanonico() {}

    /**
     * Serializa un mapa a JSON canonico: claves ordenadas, sin espacios, escape minimo.
     * El resultado es la entrada del hash, y esta publicado para que cualquiera lo
     * reproduzca.
     */
    public static String serializar(Map<String, String> campos) {
        var ordenados = new TreeMap<>(campos);
        var sb = new StringBuilder("{");
        boolean primero = true;
        for (var e : ordenados.entrySet()) {
            if (!primero) {
                sb.append(',');
            }
            primero = false;
            sb.append('"').append(escapar(e.getKey())).append("\":");
            if (e.getValue() == null) {
                sb.append("null");
            } else {
                sb.append('"').append(escapar(e.getValue())).append('"');
            }
        }
        return sb.append('}').toString();
    }

    /** Un importe, siempre con dos decimales y punto. Nunca un {@code double}. */
    public static String importe(BigDecimal monto) {
        return monto.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    /**
     * Una fecha, siempre en UTC, en ISO-8601 y con precision de microsegundos.
     *
     * <p>El huso del servidor no entra al hash, y la precision del reloj de quien llamo,
     * tampoco. Lo segundo no es simetria: es que un bloque se sella con el instante que le
     * pasan y se guarda en un {@code TIMESTAMPTZ}, que redondea a microsegundos. Si el
     * hash se calculara sobre los nanosegundos originales, el bloque quedaria firmado con
     * un valor que la base ya no tiene, y recomputar su hash desde lo guardado —que es
     * exactamente lo que promete CU-73— daria distinto. La cadena se declararia alterada
     * sin que nadie la tocara.
     *
     * <p>Y pasaba solo en algunas maquinas: {@code OffsetDateTime.now()} da nanosegundos
     * en Linux y microsegundos en macOS, asi que las pruebas de CU-72 y CU-73 pasaban en
     * la laptop y fallaban en el CI. Es el mismo filo que ya obligo a truncar
     * {@code Reloj.delSistema()}; aca ademas cambia lo que queda firmado.
     */
    public static String instante(OffsetDateTime cuando) {
        return UTC.format(
                cuando.withOffsetSameInstant(ZoneOffset.UTC).toInstant().truncatedTo(ChronoUnit.MICROS));
    }

    private static String escapar(String texto) {
        var sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
