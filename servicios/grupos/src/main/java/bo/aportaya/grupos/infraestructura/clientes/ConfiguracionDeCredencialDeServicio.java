package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicioConfigurada;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La credencial de servicio de grupos hacia transparencia (2026-10-30): cuenta de servicio en identidad con el rol
 * {@code SERVICIO_GRUPOS} (se renueva sola), un token ya emitido, o nada (falla cerrado). La logica de eleccion es de la
 * plataforma ({@link CredencialDeServicioConfigurada}), la misma de entregas e inversiones; aca solo viven los nombres de
 * las propiedades de ESTE servicio.
 */
@Configuration
class ConfiguracionDeCredencialDeServicio {

    @Bean
    CredencialDeServicio credencialDeServicio(
            @Value("${aportaya.servicios.identidad}") URI identidad,
            @Value("${aportaya.grupos.transparencia.timeout:PT3S}") Duration timeout,
            @Value("${aportaya.grupos.transparencia.credencial-servicio:}") String tokenEmitido,
            @Value("${aportaya.grupos.transparencia.cuenta-servicio.telefono:}") String telefono,
            @Value("${aportaya.grupos.transparencia.cuenta-servicio.clave:}") String clave,
            @Value("${aportaya.grupos.transparencia.cuenta-servicio.factor-totp-semilla:}") String semilla,
            @Value("${aportaya.grupos.transparencia.cuenta-servicio.factor-fijo:}") String factorFijo) {
        return CredencialDeServicioConfigurada.de(
                "grupos", "GRUPOS", identidad, timeout, tokenEmitido, telefono, clave, semilla, factorFijo);
    }
}
