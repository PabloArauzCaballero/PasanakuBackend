package bo.aportaya.identidad.dominio.puertos;

/**
 * Verificar una credencial toca algo de afuera: la **pimienta**, que es un secreto de
 * configuracion y no un dato de la fila.
 *
 * <p>Por eso es un puerto y no una funcion suelta. El adaptador por omision es
 * Argon2id local; ningun nombre de proveedor sale de el.
 */
public interface HasheadorDeCredencial {

    String hashear(char[] credencial);

    /**
     * Comparacion en tiempo constante. Nunca {@code equals} sobre el hash: la
     * diferencia de milisegundos entre un fallo temprano y uno tardio alcanza para
     * adivinar de a un caracter.
     */
    boolean coincide(char[] credencial, String hashGuardado);

    /**
     * Con que se calculo el hash. Se guarda junto a el porque endurecer el KDF maniana
     * —mas memoria, mas iteraciones— no puede invalidar los hashes de ayer: hay que
     * poder saber con que parametros se calculo cada uno para migrarlos al verificar.
     */
    Parametros parametros();

    /** {@code comoJson} entra tal cual en {@code credencial_acceso.parametros_kdf}. */
    record Parametros(String algoritmo, String comoJson) {}
}
