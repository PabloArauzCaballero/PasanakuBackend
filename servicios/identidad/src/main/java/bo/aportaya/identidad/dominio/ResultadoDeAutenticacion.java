package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Como termino el intento. **No es una excepcion, y eso es deliberado.**
 *
 * <p>El intento fallido tiene que quedar registrado: si el caso de uso lanzara para
 * rechazar, la transaccion revertiria y con ella el {@code intento_autenticacion} que
 * acaba de escribir. Los fallidos se perderian, que es exactamente lo contrario de lo
 * que pide el caso de uso — «asi los fallidos tambien quedan».
 *
 * <p>La traduccion a {@code 422} la hace la pagina, despues del {@code COMMIT}.
 */
public record ResultadoDeAutenticacion(
        boolean exitoso,
        Optional<UUID> usuarioId,
        Optional<UUID> sesionId,
        Optional<OffsetDateTime> expiraEn,
        boolean requiereFactorAdicional,
        boolean dispositivoConfiable,
        /**
         * Si quien entra tiene algun rol de ambito GLOBAL.
         *
         * <p>Lo necesita quien emite el token para ponerle el rol que corresponde. Sin
         * esto la sesion de un operador salia con rol de participante, y las politicas
         * de fila —que miran `app.rol`— no le dejaban ver nada que no fuera suyo: el
         * backoffice entero devolvia listas vacias sin un solo error.
         */
        boolean esOperador,
        Optional<CodigoError> codigo,
        String mensaje) {

    /**
     * De quien es la sesion, ademas de cual.
     *
     * <p>El titular viaja en el resultado porque el token de acceso se emite a su nombre
     * (ADR-024) y quien lo emite no puede volver a preguntarle a la base quien acaba de
     * entrar: preguntar dos veces abre la ventana para que las dos respuestas difieran.
     */
    public static ResultadoDeAutenticacion sesionAbierta(
            UUID usuarioId, UUID sesionId, OffsetDateTime expiraEn, boolean dispositivoConfiable, boolean esOperador) {
        return new ResultadoDeAutenticacion(
                true,
                Optional.of(usuarioId),
                Optional.of(sesionId),
                Optional.of(expiraEn),
                false,
                dispositivoConfiable,
                esOperador,
                Optional.empty(),
                "");
    }

    public static ResultadoDeAutenticacion faltaSegundoFactor(boolean dispositivoConfiable) {
        return new ResultadoDeAutenticacion(
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                dispositivoConfiable,
                false,
                Optional.of(CodigoError.de(4, 3)),
                "Confirma el codigo que te enviamos para terminar de entrar.");
    }

    public static ResultadoDeAutenticacion rechazado(CodigoError codigo, String mensaje) {
        return new ResultadoDeAutenticacion(
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                false,
                Optional.of(codigo),
                mensaje);
    }
}
