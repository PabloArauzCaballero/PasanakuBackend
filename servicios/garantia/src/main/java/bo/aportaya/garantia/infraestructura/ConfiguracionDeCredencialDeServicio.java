package bo.aportaya.garantia.infraestructura;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicioConfigurada;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La credencial de servicio de garantia hacia aportes ({@code SERVICIO_GARANTIA}, F1.L1.M07, regla 98.6): cuenta de
 * servicio en identidad (se renueva sola) o nada (el recaudo no se pregunta: falla cerrado). La eleccion y el ingreso
 * por {@code POST /sesiones} son de la plataforma ({@link CredencialDeServicioConfigurada}), la misma de entregas,
 * grupos e inversiones; aca solo viven los nombres de las propiedades de ESTE servicio.
 */
@Configuration
class ConfiguracionDeCredencialDeServicio {

    @Bean
    CredencialDeServicio credencialDeServicio(
            @Value("${aportaya.servicios.identidad}") URI identidad,
            @Value("${aportaya.garantia.aportes.timeout:PT3S}") Duration timeout,
            @Value("${aportaya.garantia.aportes.cuenta-servicio.telefono:}") String telefono,
            @Value("${aportaya.garantia.aportes.cuenta-servicio.clave}") String clave,
            @Value("${aportaya.garantia.aportes.cuenta-servicio.factor-totp-semilla:}") String semilla) {
        // Sin token ya emitido ni factor fijo: la unica forma de garantia es la cuenta de servicio con TOTP (el factor
        // fijo no sirve desde #412, y un token suelto vive minutos).
        return CredencialDeServicioConfigurada.de(
                "garantia", "GARANTIA", identidad, timeout, "", telefono, clave, semilla);
    }
}
