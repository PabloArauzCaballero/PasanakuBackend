package bo.aportaya.identidad.infraestructura;

import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** A que correo se puede mandar el codigo de acceso de una cuenta. */
@Component
public class CorreoDeAccesoRepositorio {

    private static final String PROPOSITO = VerificacionCorreoRepositorio.PROPOSITO;

    private final ProteccionDeVerificacionCorreo proteccion;

    public CorreoDeAccesoRepositorio(ProteccionDeVerificacionCorreo proteccion) {
        this.proteccion = proteccion;
    }

    /**
     * El correo del usuario, solo si lo confirmo con codigo en el alta.
     *
     * <p>La prueba es el token {@code VERIFICACION_CORREO} consumido y vinculado a la
     * cuenta, con la huella del correo que la cuenta tiene HOY: si el correo cambio
     * despues, el token viejo no lo respalda y no se manda nada. Mandar un codigo de
     * acceso a un correo que nadie confirmo es entregarle la cuenta a quien lo escribio.
     *
     * <p>No se usa {@code usuario.correo_verificado_en}: el alta no la completa (hoy esta
     * vacia en todas las cuentas de TEST), y el token es lo que realmente se verifico.
     */
    public Optional<String> correoVerificadoDe(DSLContext dsl, UUID usuarioId) {
        String correo = dsl.fetchOptional("SELECT correo FROM identidad.usuario WHERE id = ?", usuarioId)
                .map(fila -> fila.get(0, String.class))
                .orElse(null);
        if (correo == null || correo.isBlank()) {
            return Optional.empty();
        }
        boolean confirmado = dsl.fetchExists(dsl.selectOne()
                .from("identidad.token_verificacion")
                .where(
                        "usuario_id = ? AND proposito = ? AND estado = 'CONSUMIDO' AND firma_hmac = ?",
                        usuarioId,
                        PROPOSITO,
                        proteccion.huellaDestino(correo)));
        return confirmado ? Optional.of(correo) : Optional.empty();
    }
}
