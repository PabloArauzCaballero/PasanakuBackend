package bo.aportaya.grupos.infraestructura;

import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Quien ocupa un cupo y todavia no acepto la version VIGENTE del reglamento del grupo.
 *
 * <p>Existe porque al iniciar el grupo se publica una version nueva del reglamento (con el plantel y el fondo reales,
 * decision del dueño 2026-10-30) y el sorteo exige que todos hayan firmado ESA version (AP-CU60-05), no cualquiera: una
 * aceptacion de la version anterior es la aceptacion de otro contrato.
 */
@Component
public class AceptacionVigenteRepositorio {

    /** Los numeros de los cupos OCUPADO cuyo titular no firmo el reglamento vigente, en orden. */
    public List<Integer> cuposSinAceptacionVigente(DSLContext dsl, UUID grupoId) {
        return dsl.fetch(
                        """
                        SELECT c.numero
                          FROM grupos.cupo c
                         WHERE c.grupo_id = ? AND c.estado = 'OCUPADO' AND c.participante_id IS NOT NULL
                           AND NOT EXISTS (
                                 SELECT 1
                                   FROM grupos.aceptacion_reglamento a
                                   JOIN grupos.reglamento_grupo r ON r.id = a.reglamento_id
                                  WHERE a.participante_id = c.participante_id
                                    AND r.grupo_id = c.grupo_id
                                    AND r.vigente_hasta IS NULL)
                         ORDER BY c.numero
                        """,
                        grupoId)
                .map(f -> f.get("numero", Integer.class));
    }
}
