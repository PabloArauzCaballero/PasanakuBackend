package bo.aportaya.inversiones.dominio;

/**
 * De donde sale un dato comercial o fiscal.
 *
 * <p>{@code SINTETICO} es un valor de prueba sin fuente aprobada: nunca es apto para
 * produccion ({@code ck_version_condiciones_sintetico} lo impide en la base) y la API
 * lo dice en cada respuesta. {@code VERIFICADO} exige fuente y fecha, y hoy no existe
 * ninguno: la decision de que aliado, que tasas y que impuestos es de negocio.
 */
public enum OrigenDatos {
    SINTETICO,
    VERIFICADO
}
