package bo.aportaya.grupos.dominio;

/**
 * Cuando un grupo se puede iniciar (CU-69 · inicio al 75 %, decision del dueño 2026-10-30).
 *
 * <p>El organizador lo inicia con al menos {@code ceil(porcentaje × cupos totales / 100)} cupos OCUPADO (reglamento
 * firmado). El porcentaje NO esta en el codigo: llega de configuracion ({@code aportaya.grupos.inicio.porcentaje}),
 * porque es una decision de producto (invariante 10). Se redondea hacia arriba: con 10 cupos y 75 %, 7,5 pide 8.
 */
public final class InicioDelGrupo {

    /** El unico estado desde el que se inicia y en el que entra gente. */
    public static final String ABIERTO = "ABIERTO_A_INSCRIPCION";

    private InicioDelGrupo() {}

    /** Cupos OCUPADO que hacen falta para iniciar un grupo de {@code cuposTotales} cupos. */
    public static int minimoParaIniciar(int cuposTotales, int porcentaje) {
        return Math.ceilDiv(cuposTotales * porcentaje, 100);
    }

    /**
     * Si el grupo se puede iniciar hoy: esta abierto, alcanza el minimo y no hay nadie aceptado sin firmar. Con un cupo
     * RESERVADO no se inicia: achicar el grupo le quitaria el lugar a quien ya fue aceptado (AP-CU69-19).
     */
    public static boolean sePuedeIniciar(
            String estado, int ocupados, int reservados, int cuposTotales, int porcentaje) {
        return ABIERTO.equals(estado) && reservados == 0 && ocupados >= minimoParaIniciar(cuposTotales, porcentaje);
    }
}
