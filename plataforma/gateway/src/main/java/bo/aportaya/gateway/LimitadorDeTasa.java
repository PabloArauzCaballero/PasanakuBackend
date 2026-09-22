package bo.aportaya.gateway;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * H3.S1 (AMB-6): la clave del limitador para las rutas sensibles ({@code
 * RUTAS_SENSIBLES} en {@code scripts/modelo.py}, que {@code generar_gateway.py}
 * traduce a estas rutas con el filtro {@code RequestRateLimiter}).
 *
 * <p>El gateway no decodifica JWT — no le toca (ver el comentario en {@code
 * build.gradle.kts} sobre por que no trae Spring Security). "IP+usuario" se
 * resuelve sin decodificar nada: la IP del cliente siempre existe (incluso
 * antes de tener sesion, que es el caso de login y registro), y el valor
 * crudo de {@code Authorization} —cuando existe— separa a dos usuarios detras
 * del mismo NAT sin que el gateway sepa ni le importe quienes son.
 */
@Configuration
public class LimitadorDeTasa {

    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.KeyResolver ipYUsuarioKeyResolver() {
        return exchange -> Mono.just(clave(exchange));
    }

    /**
     * {@code RedisRateLimiter} devuelve 429 con las cabeceras {@code X-RateLimit-*},
     * pero no {@code Retry-After} (verificado corriendo el filtro de verdad contra
     * Redis, no en la documentacion: la cabecera no aparece). El kill-test del
     * encargo pide las dos. Con {@code replenishRate} tokens por segundo, un token
     * vuelve en {@code 1/replenishRate} segundos — se redondea para arriba porque
     * "esperá menos de lo que hace falta" es peor que "esperá de mas".
     *
     * <p>Dos intentos previos no funcionaban: (1) {@code chain.filter(exchange)
     * .then(...)} corre despues de comitear la respuesta, tarde para agregar una
     * cabecera; (2) {@code beforeCommit} sin precedencia alta nunca se registraba,
     * porque {@code RequestRateLimiter} — mas adelante en la cadena por defecto —
     * completaba la respuesta y cortaba la cadena antes de que este filtro
     * corriera. {@code Ordered.HIGHEST_PRECEDENCE} pone este filtro PRIMERO: se
     * registra el gancho {@code beforeCommit} antes de que nada mas se ejecute, y
     * ese gancho SI corre sin importar que otro filtro, mas adelante en la
     * cadena, sea el que efectivamente comitee la respuesta.
     */
    @Bean
    public GlobalFilter retryAfterEnRateLimit() {
        return new FiltroDeReintento();
    }

    /**
     * ADR-050 dice "Redis caído → deniega (fail closed)", pero el default de
     * {@code RedisRateLimiter} es lo CONTRARIO: su codigo fuente
     * (RedisRateLimiter.java, catch final de {@code isAllowed}) deja un
     * comentario explicito — "We don't want a hard dependency on Redis to
     * allow traffic" — y devuelve {@code allowed=true} con
     * {@code tokensRemaining=-1} ante cualquier error. Verificado apagando
     * Redis de verdad: la peticion pasaba igual (fail open), justo al reves
     * de lo que exige el encargo para estas ocho rutas.
     *
     * <p>Este filtro corre DESPUES de {@code RequestRateLimiter} (que ya puso
     * la cabecera {@code X-RateLimit-Remaining}) y ANTES de
     * {@code NettyRoutingFilter} (orden {@code LOWEST_PRECEDENCE}, el que
     * reenvia al backend): si ve el centinela {@code -1}, corta la cadena ACA
     * — el backend nunca llega a ver la peticion — y devuelve 503, no un 200
     * disfrazado de "no habia limite".
     */
    @Bean
    public GlobalFilter denegarSiRedisNoResponde() {
        return new FiltroDeCierreFalloDeRedis();
    }

    /**
     * Clase con nombre y no lambda/anonima: una {@code new GlobalFilter() {}}
     * anonima solo puede implementar UNA interfaz, y este filtro necesita dos
     * (GlobalFilter + Ordered) en la MISMA instancia — un bean {@code Ordered}
     * aparte no ordena nada, Spring Cloud Gateway mira si el propio GlobalFilter
     * implementa Ordered para decidir su lugar en la cadena.
     */
    private static final class FiltroDeReintento implements GlobalFilter, Ordered {
        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public Mono<Void> filter(
                ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain cadena) {
            exchange.getResponse().beforeCommit(() -> {
                if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    String tasa = exchange.getResponse().getHeaders().getFirst("X-RateLimit-Replenish-Rate");
                    int segundos = tasa == null ? 1 : Math.max(1, (int) Math.ceil(1.0 / Integer.parseInt(tasa)));
                    exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(segundos));
                }
                return Mono.empty();
            });
            return cadena.filter(exchange);
        }
    }

    /** Corre justo antes de enrutar: ve las cabeceras que puso RequestRateLimiter. */
    private static final class FiltroDeCierreFalloDeRedis implements GlobalFilter, Ordered {
        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE - 1;
        }

        @Override
        public Mono<Void> filter(
                ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain cadena) {
            String restantes = exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining");
            if ("-1".equals(restantes)) {
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                exchange.getResponse().getHeaders().set("Retry-After", "5");
                return exchange.getResponse().setComplete();
            }
            return cadena.filter(exchange);
        }
    }

    private String clave(ServerWebExchange exchange) {
        String ip = ip(exchange);
        String autorizacion = exchange.getRequest().getHeaders().getFirst("Authorization");
        return (autorizacion == null || autorizacion.isBlank()) ? ip : ip + ":" + autorizacion.hashCode();
    }

    private String ip(ServerWebExchange exchange) {
        InetSocketAddress remoto = exchange.getRequest().getRemoteAddress();
        return remoto == null || remoto.getAddress() == null
                ? "desconocido"
                : remoto.getAddress().getHostAddress();
    }
}
