package bo.aportaya.grupos.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Los estados del grupo que admite el modelo ({@code ck_grupo_estado} en {@code grupo.sql}), y cuáles cierran su
 * ciclo de vida.
 *
 * <p>F1.L3.M06: «grupos activos» se contaba con una lista escrita a mano ({@code CERRADO}, {@code DISUELTO}) que
 * no existe en la base, así que un grupo {@code FINALIZADO} seguía vivo y uno en curso podía no estarlo. La lista
 * vive acá, una sola vez, y {@code EstadosContraElModeloTest} la compara con el {@code CHECK}.
 */
public enum EstadoDeGrupo {
    ABIERTO_A_INSCRIPCION,
    ACTIVO,
    BORRADOR,
    CANCELADO,
    CONFORMADO,
    DISUELTO_ANTICIPADAMENTE,
    EN_CURSO,
    FINALIZADO,
    SUSPENDIDO;

    /** Un grupo en estos estados ya no genera obligaciones ni las espera. */
    public static final Set<EstadoDeGrupo> TERMINALES = EnumSet.of(CANCELADO, DISUELTO_ANTICIPADAMENTE, FINALIZADO);
}
