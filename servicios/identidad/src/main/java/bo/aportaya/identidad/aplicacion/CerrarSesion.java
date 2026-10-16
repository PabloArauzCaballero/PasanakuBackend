package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Revoca idempotentemente la sesión autenticada y el refresh vivo de su familia. */
@Service
public class CerrarSesion {
    private final Datos datos;
    private final Reloj reloj;

    public CerrarSesion(Datos datos, Reloj reloj) {
        this.datos = datos;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(UUID usuarioId, UUID sesionId, String trazaId) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        datos.conContexto(ContextoSesion.deSistema(EntradaAutenticacion.PROCESO_INGRESO, new Traza(trazaId)), dsl -> {
            var sesion = dsl.fetchOne(
                    """
                            UPDATE identidad.sesion
                               SET revocada_en = COALESCE(revocada_en, ?::timestamptz),
                                   motivo_revocacion = COALESCE(motivo_revocacion, 'cierre solicitado')
                             WHERE id = ? AND usuario_id = ?
                            RETURNING refresco_familia_id
                            """,
                    ahora,
                    sesionId,
                    usuarioId);
            if (sesion != null) {
                UUID familia = sesion.get("refresco_familia_id", UUID.class);
                if (familia != null) {
                    dsl.execute(
                            """
                                    UPDATE identidad.token_verificacion
                                       SET estado = 'INVALIDADO', invalidado_en = ?::timestamptz,
                                           motivo_invalidacion = 'cierre de sesión'
                                     WHERE familia_id = ? AND tipo_token = 'REFRESCO' AND estado = 'EMITIDO'
                                    """,
                            ahora,
                            familia);
                }
            }
            return null;
        });
    }
}
