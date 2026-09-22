package bo.aportaya.gateway;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * H3.S2 (regla 90/95): {@code aportaya.cors.origenes} por perfil
 * (application-{local,test,staging,production}.yml), métodos y cabeceras
 * mínimos, sin {@code allowCredentials} (los servicios validan el JWT en el
 * header {@code Authorization}, no en una cookie — no hay credenciales que
 * cruzar el origen).
 *
 * <p>La guarda de producción es de este carril únicamente: rechaza que
 * {@code aportaya.cors.origenes} llegue vacío o con {@code *} cuando el
 * perfil activo no es {@code local} ni {@code test} (AMB-12). La guarda
 * GENERAL de arranque para "todo perfil no local/test es productivo, fail
 * closed" es de {@code GuardiaDeProduccion} (Leo, carril PR3,
 * plataforma-infra) — no existe todavía (carriles/PR3 vacío al momento de
 * escribir esto). Esta clase no la reemplaza: cubre solo el caso de CORS,
 * que es lo que me toca a mí.
 */
@Configuration
public class ConfiguracionCors {

    private static final List<String> METODOS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final List<String> CABECERAS = List.of("Authorization", "Content-Type", "X-Request-Id");

    @Value("${aportaya.cors.origenes:}")
    private String origenesCrudos;

    @Bean
    public CorsWebFilter corsWebFilter(Environment entorno) {
        List<String> origenes = origenes();
        // Lista de PERMITIDOS (staging/production), no de excluidos: negar
        // "todo lo que no sea local/test" tambien atrapaba el caso de NINGUN
        // perfil activo (el default de Spring cuando nadie fija
        // spring.profiles.active), que es exactamente lo que le pasaba a
        // ArranqueRateLimitGatewayTest — un test que no le importa CORS,
        // pero que arranca el MISMO Aplicacion.class y por lo tanto el
        // MISMO bean. Regresion real, encontrada corriendo la suite entera
        // de integrationTest junta despues de agregar este archivo (H3.S2),
        // no solo la prueba nueva en aislamiento.
        boolean produccion = entorno.acceptsProfiles(org.springframework.core.env.Profiles.of("staging", "production"));

        if (produccion && (origenes.isEmpty() || origenes.contains("*"))) {
            // Fail closed (regla 89/98): un gateway que arranca en un perfil
            // productivo sin origenes reales, o con "*", es un CORS abierto a
            // cualquiera. No se levanta con eso — se corta el arranque.
            throw new IllegalStateException(
                    "aportaya.cors.origenes vacío o con '*' en un perfil no local/test ("
                            + Arrays.toString(entorno.getActiveProfiles())
                            + "). AMB-12/DECISION_REQUIRED: falta la variable APORTAYA_CORS_ORIGENES"
                            + " con el dominio real. No se arranca con CORS abierto.");
        }

        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(METODOS);
        configuracion.setAllowedHeaders(CABECERAS);
        // Sin allowCredentials: la sesión viaja en el header Authorization, no
        // en una cookie — no hay credencial de navegador que cruzar el origen,
        // y declararlo sin usarlo es la superficie que el gate de seguridad
        // pide no abrir de más (regla 90.1.8, respuesta mínima).

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return new CorsWebFilter(fuente);
    }

    private List<String> origenes() {
        if (origenesCrudos == null || origenesCrudos.isBlank()) {
            return List.of();
        }
        return Arrays.stream(origenesCrudos.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    /** Deja constancia en el log de arranque de qué orígenes quedaron activos — nada más. */
    @EventListener(ApplicationReadyEvent.class)
    public void registrarOrigenesActivos() {
        org.slf4j.LoggerFactory.getLogger(ConfiguracionCors.class).info("CORS — orígenes activos: {}", origenes());
    }
}
