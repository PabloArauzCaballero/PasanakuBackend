package bo.aportaya.grupos.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que un grupo le pide a quien entra y lo que hace falta para saber si la etapa de esa persona lo desbloquea
 * (2026-10-30). Se lee ANTES de preguntarle a {@code transparencia} y de abrir la transaccion del ingreso
 * (invariante 6): ninguno de estos datos cambia mientras el grupo recibe gente, salvo {@code estado}, que el caso de
 * uso vuelve a mirar con la fila del grupo tomada.
 */
public record CondicionesDeIngreso(
        UUID grupoId,
        String estado,
        BigDecimal reputacionMinima,
        int cuposTotales,
        BigDecimal montoAporte,
        String moneda) {

    public boolean recibeIntegrantes() {
        return InicioDelGrupo.ABIERTO.equals(estado);
    }
}
