package bo.aportaya.auditoria.dominio;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Que familias de indicadores puede ver quien pregunta (tableros por rol). Pura.
 *
 * <p>La interfaz oculta las pestañas; esta clase es la que PROTEGE: el servidor filtra las familias antes de
 * responder (regla 95.6.1). {@code AUDITORIA_LEER} —el permiso original de CU-98— sigue viendo todo.
 */
public final class FamiliasVisibles {

    public static final Set<String> TODAS = Set.of("CUMPLIMIENTO", "FINANZAS", "NEGOCIO", "OPERACION", "RIESGO");

    private static final Map<String, Set<String>> POR_PERMISO = Map.of(
            "AUDITORIA_LEER", TODAS,
            "TABLERO_FINANCIERO_VER", Set.of("FINANZAS", "RIESGO"),
            "TABLERO_COMERCIAL_VER", Set.of("NEGOCIO"),
            "TABLERO_SISTEMAS_VER", Set.of("OPERACION"));

    private FamiliasVisibles() {}

    /** La union de lo que habilitan el rol y los permisos. Vacia si no habilitan nada. */
    public static Set<String> para(String rol, Set<String> permisos) {
        var visibles = new TreeSet<String>();
        POR_PERMISO.forEach((permiso, familias) -> {
            if (permiso.equals(rol) || permisos.contains(permiso)) {
                visibles.addAll(familias);
            }
        });
        return Set.copyOf(visibles);
    }
}
