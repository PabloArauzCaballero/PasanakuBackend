package bo.aportaya.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Nadie autoriza y ejecuta el mismo tipo de operacion ({@code R-SEG-04}).
 *
 * <p>Los pares van escritos y no deducidos de una convencion de nombres: una regla de
 * control interno que depende de como alguien bautizo un permiso se rompe el dia que
 * alguien lo bautiza distinto, y nadie se entera hasta la auditoria. Por la misma razon
 * los codigos son los del catalogo real ({@code seeders/minimos/10-roles-y-permisos.json}):
 * una prueba falla si alguno deja de existir.
 *
 * <p>La lista es la cerrada de P-3 (decision de Pablo, 2026-11-24, corregida el mismo dia para el par contable):
 * quien autoriza una entrega no la ejecuta; quien aprueba una factura de proveedor no la paga; quien registra un
 * bloqueo por autoridad no lo levanta; y los tres permisos de publicidad (alta de anunciantes, aprobacion de campanas
 * y moderacion de piezas) son de tres personas distintas, asi que cada par entre ellos es incompatible.
 */
public final class SegregacionDeFunciones {

    /** Dos permisos que una misma persona no puede reunir. */
    public record Par(String uno, String otro) {

        boolean loReune(Set<String> permisos) {
            return permisos.contains(uno) && permisos.contains(otro);
        }

        @Override
        public String toString() {
            return uno + " con " + otro;
        }
    }

    private static final List<Par> PARES_INCOMPATIBLES = List.of(
            new Par("ENTREGA_AUTORIZAR", "ENTREGA_EJECUTAR"),
            new Par("CONTABILIDAD_ERP_CUENTAS_POR_PAGAR", "CONTABILIDAD_ERP_PAGAR"),
            new Par("BLOQUEO_AUTORIDAD_REGISTRAR", "BLOQUEO_AUTORIDAD_LEVANTAR"),
            new Par("PUBLICIDAD_ANUNCIANTES", "PUBLICIDAD_APROBAR_CAMPANA"),
            new Par("PUBLICIDAD_ANUNCIANTES", "PUBLICIDAD_MODERAR"),
            new Par("PUBLICIDAD_APROBAR_CAMPANA", "PUBLICIDAD_MODERAR"));

    private SegregacionDeFunciones() {}

    /** Los pares vigentes, en el orden en que se evaluan. */
    public static List<Par> pares() {
        return PARES_INCOMPATIBLES;
    }

    /** El par en conflicto, si la combinacion resultante rompe la segregacion. */
    public static Optional<String> conflicto(Set<String> permisosResultantes) {
        return PARES_INCOMPATIBLES.stream()
                .filter(par -> par.loReune(permisosResultantes))
                .findFirst()
                .map(Par::toString);
    }

    public static boolean viola(Set<String> permisosResultantes) {
        return conflicto(permisosResultantes).isPresent();
    }
}
