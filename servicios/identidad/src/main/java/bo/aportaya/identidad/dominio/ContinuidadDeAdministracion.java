package bo.aportaya.identidad.dominio;

/**
 * La plataforma nunca queda sin quien administre accesos (CU-08 5a, decision P-7).
 *
 * <p>Se compara antes y despues del acto, contando PERSONAS ACTIVAS con una asignacion vigente del permiso.
 * Un acto que no baja el numero no se rechaza aunque el numero ya sea cero: no empeora nada, y rechazarlo
 * dejaria trabada la recuperacion de una plataforma que ya quedo sin administrador.
 */
public final class ContinuidadDeAdministracion {

    /** El permiso que nunca puede quedar sin titular. */
    public static final String PERMISO = "ACCESOS_ADMINISTRAR";

    private ContinuidadDeAdministracion() {}

    public static boolean laDejaSinAdministrador(int titularesAntes, int titularesDespues) {
        return titularesDespues == 0 && titularesAntes > titularesDespues;
    }
}
