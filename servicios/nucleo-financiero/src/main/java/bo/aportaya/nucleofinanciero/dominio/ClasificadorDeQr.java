package bo.aportaya.nucleofinanciero.dominio;

/**
 * Que clase de codigo QR es lo que llego, antes de decidir que hacer con el.
 *
 * <p>Hay tres clases y la diferencia es de seguridad, no de forma: un QR <b>interno</b> lo
 * emitio una billetera de Pasanaku y se paga entre billeteras; uno <b>interoperable</b> es del
 * sistema bancario y solo se puede pagar a traves de un proveedor habilitado; y el resto no se
 * sabe que es. Procesar un QR bancario como si fuera interno, o afirmar que se acepta
 * interoperabilidad sin proveedor, le haria creer a la persona que pago lo que no pago.
 *
 * <p>El reconocimiento de lo interoperable es una <b>heuristica</b> sobre el indicador de formato
 * de carga util del estandar EMV de QR de comercio (los primeros caracteres, {@code 000201}).
 * No es un analisis del contenido ni una afirmacion sobre ningun esquema bancario del pais: solo
 * sirve para NO tratarlo como interno. Cuando exista un proveedor habilitado, el reconocimiento
 * correcto sera suyo.
 */
public final class ClasificadorDeQr {

    /** Prefijo de los QR que emite esta plataforma. */
    public static final String PREFIJO_INTERNO = "PSNK1.";

    private static final String INDICADOR_EMV = "000201";
    private static final int LARGO_MAXIMO = 2048;

    public enum Clase {
        INTERNO,
        INTEROPERABLE_BANCARIO,
        DESCONOCIDO
    }

    private ClasificadorDeQr() {}

    public static Clase de(String contenido) {
        if (contenido == null || contenido.isBlank() || contenido.length() > LARGO_MAXIMO) {
            return Clase.DESCONOCIDO;
        }
        String limpio = contenido.strip();
        if (limpio.startsWith(PREFIJO_INTERNO)) {
            return Clase.INTERNO;
        }
        if (limpio.startsWith(INDICADOR_EMV)) {
            return Clase.INTEROPERABLE_BANCARIO;
        }
        return Clase.DESCONOCIDO;
    }
}
