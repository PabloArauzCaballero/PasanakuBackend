package bo.aportaya.plataforma.web.seguridad;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Como cada servicio valida la firma del token, sin preguntarle a nadie.
 *
 * <p>ADR-024: {@code identidad} es el unico emisor y los otros trece son servidores de
 * recurso que validan localmente contra el JWKS publicado. La clave publica se descarga
 * una vez y se cachea; a partir de ahi autorizar no cuesta un salto de red, que es lo
 * que permite que trece servicios decidan sin depender de la disponibilidad de uno.
 *
 * <p>El bean vive aca y no en cada servicio por la razon de siempre: catorce copias
 * divergen. La direccion del JWKS es lo unico que cambia, y viene por configuracion
 * ({@code aportaya.jwt.jwks-uri}) — si falta, el proceso no levanta, que es lo correcto:
 * un servicio que arranca sin saber contra que validar es un servicio que acepta
 * cualquier token.
 *
 * <p><b>Firma, vencimiento y sesion viva</b> (GU-15, ADR-030): a los validadores de siempre
 * ({@code exp}/{@code nbf} con su tolerancia) se suma {@link ValidadorDeRevocacion}, que
 * rechaza el token de una sesion que identidad ya cerro aunque no haya vencido.
 */
@Configuration
public class ConfiguracionDelDecodificador {

    @Bean
    public JwtDecoder decodificador(
            @Value("${aportaya.jwt.jwks-uri}") String jwksUri, ValidadorDeRevocacion revocacion) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithValidators(List.of(revocacion)));
        return decodificador;
    }
}
