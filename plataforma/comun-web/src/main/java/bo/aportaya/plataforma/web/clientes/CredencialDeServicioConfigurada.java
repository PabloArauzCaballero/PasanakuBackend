package bo.aportaya.plataforma.web.clientes;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

/**
 * Elige de donde sale la credencial de servicio de un servicio que le habla a otro sin una persona detras. Por orden:
 *
 * <ol>
 *   <li>cuenta de servicio en identidad (telefono, clave y la semilla TOTP de su segundo factor): se renueva sola
 *       ({@link CredencialPorLogin}). Los roles {@code SERVICIO_*} son de ambito GLOBAL: identidad trata la cuenta como
 *       operador y solo le admite TOTP; el codigo fijo de TEST dejo de existir con #412;
 *   <li>un token ya emitido: vive minutos, sirve para una prueba manual;
 *   <li>nada: las operaciones que mueven plata fallan cerradas.
 * </ol>
 *
 * Una configuracion A MEDIAS (telefono sin clave, o sin semilla) es un error de arranque: no se degrada en silencio a
 * "sin credencial" (regla 90.3.4). Cada servicio le pasa los nombres de SUS variables para que el mensaje diga cual falta.
 */
public final class CredencialDeServicioConfigurada {

    private CredencialDeServicioConfigurada() {}

    /**
     * @param servicio quien la pide («inversiones», «entregas»)
     * @param prefijoDeVariables el prefijo de sus variables de entorno, en mayusculas («INVERSIONES», «ENTREGAS»)
     */
    public static CredencialDeServicio de(
            String servicio,
            String prefijoDeVariables,
            URI identidad,
            Duration timeout,
            String tokenEmitido,
            String telefono,
            String clave,
            String semilla) {
        boolean hayCuenta = !telefono.isBlank() || !clave.isBlank() || !semilla.isBlank();
        if (!hayCuenta) {
            return CredencialDeServicio.fija(tokenEmitido);
        }
        if (telefono.isBlank() || clave.isBlank() || semilla.isBlank()) {
            throw new IllegalStateException("La cuenta de servicio de " + servicio + " esta a medias: hacen falta "
                    + prefijoDeVariables + "_SERVICIO_TELEFONO, " + prefijoDeVariables + "_SERVICIO_CLAVE y "
                    + prefijoDeVariables + "_SERVICIO_TOTP_SEMILLA.");
        }
        return new CredencialPorLogin(
                identidad,
                timeout,
                servicio,
                telefono.strip(),
                clave,
                CredencialPorLogin.totp(semilla.strip()),
                Clock.systemUTC());
    }
}
