package bo.aportaya.grupos.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Los estados del participante que admite el modelo ({@code ck_participante_estado} en {@code participante.sql}).
 *
 * <p>{@link #VINCULADOS} son los que siguen dentro del grupo con sus obligaciones: el que está en mora también.
 * Antes se contaba solo {@code ACTIVO}, y una persona {@code EN_MORA} salía con cero grupos activos y podía cerrar
 * su billetera debiéndole al grupo (F1.L3.M06). Los que salieron ({@code RETIRADO}, {@code REEMPLAZADO},
 * {@code EXPULSADO}) y los que nunca entraron ({@code INVITADO}, {@code POSTULANTE}) no cuentan.
 */
public enum EstadoDeParticipante {
    ACEPTADO_PENDIENTE_FIRMA,
    ACTIVO,
    EN_MORA,
    EXPULSADO,
    INVITADO,
    POSTULANTE,
    REEMPLAZADO,
    RETIRADO,
    SUSPENDIDO;

    public static final Set<EstadoDeParticipante> VINCULADOS =
            EnumSet.of(ACEPTADO_PENDIENTE_FIRMA, ACTIVO, EN_MORA, SUSPENDIDO);
}
