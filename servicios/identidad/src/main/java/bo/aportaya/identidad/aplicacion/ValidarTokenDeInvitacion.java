package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifica el secreto y la titularidad del teléfono sin revelar datos del usuario. */
@Service
public class ValidarTokenDeInvitacion {
    private final Datos datos;

    public ValidarTokenDeInvitacion(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public boolean ejecutar(UUID tokenId, String token, String telefono, String kycMinimo, ContextoSesion sesion) {
        if (token == null
                || !token.matches("[0-9a-f]{64}")
                || telefono == null
                || !telefono.matches("\\+591\\d{8}")
                || kycMinimo == null
                || !kycMinimo.matches("NINGUNO|BASICO|INTERMEDIO|COMPLETO")) {
            return false;
        }
        // Los tokens emitidos para invitaciones no tienen usuario_id: la política
        // de fila no deja leerlos como participante. El backend ya autenticó la
        // sesión, y este contexto de sistema queda limitado a esta comprobación.
        ContextoSesion interno = ContextoSesion.deSistema(
                sesion.usuarioId(), new Traza(sesion.traza().id()));
        return datos.conContexto(
                interno,
                dsl -> Boolean.TRUE.equals(dsl.fetchOne(
                                """
                        SELECT EXISTS (
                          SELECT 1 FROM identidad.token_verificacion t
                          JOIN identidad.usuario u ON u.id = ?
                          WHERE t.id = ? AND t.proposito = 'INVITACION_GRUPO'
                            AND t.estado = 'EMITIDO' AND t.expira_en > now()
                            AND t.hash_token = encode(digest(?, 'sha256'), 'hex')
                            AND u.telefono_e164 = ? AND u.estado = 'ACTIVO'
                            AND array_position(ARRAY['NINGUNO','BASICO','INTERMEDIO','COMPLETO'], u.nivel_kyc)
                              >= array_position(ARRAY['NINGUNO','BASICO','INTERMEDIO','COMPLETO'], ?)
                        ) AS valido
                        """,
                                sesion.usuarioId(),
                                tokenId,
                                token,
                                telefono,
                                kycMinimo)
                        .get("valido", Boolean.class)));
    }
}
