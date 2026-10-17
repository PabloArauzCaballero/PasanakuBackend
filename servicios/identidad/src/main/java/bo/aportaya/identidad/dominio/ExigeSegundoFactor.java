package bo.aportaya.identidad.dominio;

/**
 * La unica respuesta a «¿le pido el segundo factor?».
 *
 * <p>Para el operador devuelve **siempre** verdadero: ni el dispositivo de confianza
 * ni nada mas lo exime ({@code R-SEG-10}). Que no exista un camino que lo omita es
 * justamente el criterio de aceptacion, y por eso la decision vive en un solo lugar y
 * no repartida en tres {@code if}.
 *
 * <p>Al participante no se le pide para entrar, ni desde un telefono nuevo (CU-04 paso 3,
 * decision de Pablo del 2026-10-17): solo cuando la operacion lo exige, que es donde se
 * mueve plata (retiro, cambio de cuenta).
 */
public final class ExigeSegundoFactor {

    private ExigeSegundoFactor() {}

    public static boolean decidir(PerfilDeAcceso perfil, boolean permisoExigeMfa) {
        if (perfil.esOperador()) {
            return true;
        }
        return permisoExigeMfa;
    }
}
