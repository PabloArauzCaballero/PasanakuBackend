package bo.aportaya.inversiones.infraestructura.clientes;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige de donde sale la credencial de servicio del libro. Por orden:
 *
 * <ol>
 *   <li>cuenta de servicio en identidad (telefono, clave y un segundo factor: semilla TOTP, o el codigo fijo SOLO
 *       de TEST): se renueva sola;
 *   <li>un token ya emitido ({@code INVERSIONES_LIBRO_CREDENCIAL_SERVICIO}): vive minutos, sirve para una prueba manual;
 *   <li>nada: las operaciones que mueven plata fallan cerradas.
 * </ol>
 *
 * Una configuracion A MEDIAS (telefono sin clave, o sin factor) es un error de arranque: no se degrada en silencio a
 * "sin credencial" (regla 90.3.4).
 */
@Configuration
class ConfiguracionDeCredencialDeServicio {

    @Bean
    CredencialDeServicio credencialDeServicio(
            @Value("${aportaya.servicios.identidad:http://identidad:8080}") URI identidad,
            @Value("${aportaya.inversiones.libro.timeout:PT3S}") Duration timeout,
            @Value("${aportaya.inversiones.libro.credencial-servicio:}") String tokenEmitido,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.telefono:}") String telefono,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.clave:}") String clave,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.factor-totp-semilla:}") String semilla,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.factor-fijo:}") String factorFijo) {
        boolean hayCuenta = !telefono.isBlank() || !clave.isBlank() || !semilla.isBlank() || !factorFijo.isBlank();
        if (!hayCuenta) {
            return CredencialDeServicio.fija(tokenEmitido);
        }
        if (telefono.isBlank() || clave.isBlank() || (semilla.isBlank() && factorFijo.isBlank())) {
            throw new IllegalStateException("La cuenta de servicio de inversiones esta a medias: hacen falta "
                    + "INVERSIONES_SERVICIO_TELEFONO, INVERSIONES_SERVICIO_CLAVE y un segundo factor "
                    + "(INVERSIONES_SERVICIO_TOTP_SEMILLA, o INVERSIONES_SERVICIO_FACTOR_FIJO solo en TEST).");
        }
        var factor = semilla.isBlank()
                ? CredencialPorLogin.fijo(factorFijo.strip())
                : CredencialPorLogin.totp(semilla.strip());
        return new CredencialPorLogin(identidad, timeout, telefono.strip(), clave, factor, Clock.systemUTC());
    }
}
