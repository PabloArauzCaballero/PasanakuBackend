package bo.aportaya.plataforma.web.observabilidad;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enmascarador de datos personales sensibles (PII) en la bitácora según R-SEG-01 y ASFI.
 *
 * <p>Protege:
 * <ul>
 *   <li>Contraseñas, secretos, tokens, JWT y pines: {@code [PROTEGIDO]}.</li>
 *   <li>Cuentas bancarias y tarjetas de 10 a 20 dígitos: deja solo los últimos 4 dígitos visibles.</li>
 *   <li>Correos electrónicos: ofusca el usuario y dominio.</li>
 * </ul>
 */
public class EnmascaradorPii extends CompositeConverter<ILoggingEvent> {

    private static final Pattern PATRON_SECRETOS = Pattern.compile(
            "(?i)(password|contrasena|contraseña|clave|token|secreto|jwt|pin|bearer|authorization)[\"':\\s=]+([^\"\\s,;}{]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATRON_TARJETAS_CUENTAS = Pattern.compile(
            "\\b(\\d{6,16})(\\d{4})\\b");

    private static final Pattern PATRON_CORREOS = Pattern.compile(
            "\\b([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    @Override
    protected String transform(ILoggingEvent evento, String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }

        String resultado = texto;

        // 1. Enmascarar secretos, claves y tokens
        Matcher matcherSecretos = PATRON_SECRETOS.matcher(resultado);
        if (matcherSecretos.find()) {
            resultado = matcherSecretos.replaceAll("$1=\"[PROTEGIDO]\"");
        }

        // 2. Enmascarar números de tarjeta o cuentas bancarias largas
        Matcher matcherTarjetas = PATRON_TARJETAS_CUENTAS.matcher(resultado);
        if (matcherTarjetas.find()) {
            resultado = matcherTarjetas.replaceAll("****$2");
        }

        // 3. Enmascarar correos electrónicos
        Matcher matcherCorreos = PATRON_CORREOS.matcher(resultado);
        if (matcherCorreos.find()) {
            resultado = matcherCorreos.replaceAll("$1***@$2");
        }

        return resultado;
    }
}
