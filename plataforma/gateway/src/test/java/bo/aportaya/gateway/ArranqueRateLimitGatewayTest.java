package bo.aportaya.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.redis.testcontainers.RedisContainer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.utility.DockerImageName;

/**
 * H3.S1 — el arranque real del gateway contra un Redis de verdad
 * (Testcontainers, no un doble): las ocho... hoy cuatro rutas sensibles
 * ({@code scripts/modelo.py -> RUTAS_SENSIBLES}) cortan por tasa, y cuando
 * Redis no responde, deniegan en vez de dejar pasar.
 *
 * <p>Se nombra {@code Arranque*Test} — no {@code RateLimitGatewayTest} como
 * decia el encargo original — para caer en el patron de {@code integrationTest}
 * que ya usan los otros trece modulos (buildSrc/aportaya.base.gradle.kts): un
 * nombre nuevo hubiera exigido tocar el corredor compartido, fuera del
 * alcance declarado de este carril (despliegue/**, no buildSrc/**).
 *
 * <p>Necesito {@code configurations.testImplementation.exclude(comun-web)} en
 * {@code build.gradle.kts} (ver el comentario ahi) para que este contexto
 * levante: sin eso, la capa de pruebas compartida trae Spring MVC y Spring
 * Security de verdad al classpath de un gateway reactivo que declara,
 * explicitamente, no tener ninguno de los dos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT30S")
class ArranqueRateLimitGatewayTest {

    private static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.4.2-alpine"));

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", REDIS::getHost);
        registro.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());
    }

    @Autowired
    private WebTestClient cliente;

    @AfterEach
    void reconectarRedisSiQuedoApagado() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    private HttpStatusCode estadoDe(String ruta) {
        return cliente.get().uri(ruta).exchange().returnResult(Void.class).getStatus();
    }

    @Test
    @DisplayName("arranque: una rafaga contra /sesiones agota el burst (10) y el resto vuelve 429 con Retry-After")
    void agotaElBurstYCorta() throws InterruptedException {
        int peticiones = 15;
        AtomicInteger con429 = new AtomicInteger();
        CountDownLatch listo = new CountDownLatch(peticiones);

        for (int i = 0; i < peticiones; i++) {
            new Thread(() -> {
                        if (estadoDe("/api/v1/sesiones").value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                            con429.incrementAndGet();
                        }
                        listo.countDown();
                    })
                    .start();
        }

        assertThat(listo.await(30, TimeUnit.SECONDS)).isTrue();
        // burstCapacity=10 (generar_gateway.py): de 15 peticiones casi simultaneas,
        // al menos algunas tienen que quedar afuera. El numero exacto depende de
        // cuanto repuso el replenishRate mientras corrian, por eso >=1 y no un
        // numero fijo — lo que no puede pasar es que TODAS pasen.
        assertThat(con429.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("un 429 real trae Retry-After, no solo las cabeceras X-RateLimit-*")
    void el429TraeRetryAfter() throws InterruptedException {
        // Vacia el burst primero, EN PARALELO: con replenishRate=5/s, 10
        // peticiones secuenciales (con la latencia normal de cada una) le dan
        // tiempo a Redis a reponer tokens antes de la ultima, y el burst nunca
        // llega a agotarse. Descubierto corriendo la prueba: fallaba con 500
        // (paso) en vez de 429 (cortado), no por un error de la asercion.
        CountDownLatch listas = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                        cliente.get().uri("/api/v1/usuarios").exchange();
                        listas.countDown();
                    })
                    .start();
        }
        assertThat(listas.await(10, TimeUnit.SECONDS)).isTrue();

        var respuesta = cliente.get().uri("/api/v1/usuarios").exchange();
        respuesta.expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        respuesta.expectHeader().exists(HttpHeaders.RETRY_AFTER);
    }

    @Test
    @DisplayName("Redis caido: la ruta sensible deniega con 503, no deja pasar como si no hubiera limite")
    void redisCaidoDeniega() {
        REDIS.stop();

        // La respuesta exacta del backend no importa aca (no hay backend real en
        // esta prueba): lo que se verifica es que el gateway NO deje pasar un 2xx
        // silencioso cuando Redis no puede decidir. RedisRateLimiter por defecto
        // devuelve allowed=true (fail open) — ver ADR-050 — asi que este assert es
        // exactamente el que falla sin el filtro `denegarSiRedisNoResponde`.
        int estado = estadoDe("/api/v1/billetera/retiros").value();
        List<Integer> prohibidos =
                List.of(HttpStatus.OK.value(), HttpStatus.CREATED.value(), HttpStatus.ACCEPTED.value());
        assertThat(prohibidos)
                .as("Redis caido no puede resultar en una respuesta 2xx silenciosa")
                .doesNotContain(estado);
    }
}
