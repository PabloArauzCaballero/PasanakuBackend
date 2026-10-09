package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular;
import bo.aportaya.inversiones.infraestructura.GuardiaDeProduccion;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * El proceso levanta, y NO levanta en produccion con el aliado simulado.
 *
 * <p>Esta prueba ejercita de verdad la guardia por omision ({@code TodoEndpointDecideSuAcceso}
 * tumba el arranque si un endpoint no declara permiso), el decodificador de token, el cableado
 * de los doce endpoints del contrato y —lo especifico de este servicio— que el modo por
 * omision del aliado es {@code deshabilitado}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArranqueTest {

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        propiedades().forEach((k, v) -> registro.add(k, () -> v));
    }

    static Map<String, String> propiedades() {
        var contenedor = BaseDePrueba.contenedor();
        return Map.of(
                "spring.datasource.url",
                contenedor.getJdbcUrl(),
                "spring.datasource.username",
                contenedor.getUsername(),
                "spring.datasource.password",
                contenedor.getPassword(),
                "spring.kafka.bootstrap-servers",
                "localhost:9092",
                "aportaya.jwt.jwks-uri",
                "http://identidad:8080/.well-known/jwks.json",
                "SEGURIDAD_PIMIENTA",
                "pimienta-de-prueba");
    }

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rutas;

    @Autowired
    private AliadoDeInversion aliado;

    @Autowired
    private LibroDelTitular libro;

    @Autowired
    private GuardiaDeProduccion guardia;

    @Test
    @DisplayName(
            "arranque: el contexto levanta, las doce rutas del contrato quedan mapeadas y el aliado esta deshabilitado por omision")
    void elContextoLevanta() {
        long delContrato = rutas.getHandlerMethods().keySet().stream()
                .filter(info -> info.getPathPatternsCondition() != null)
                .filter(info -> info.getPathPatternsCondition().getPatternValues().stream()
                        .anyMatch(p -> p.startsWith("/inversiones")))
                .count();

        assertThat(delContrato).isEqualTo(12);
        assertThat(libro).isNotNull();
        // Sin configuracion el aliado no confirma nada: pedirle el catalogo es «no disponible».
        assertThatThrownBy(aliado::catalogo).isInstanceOf(AliadoDeInversion.AliadoNoDisponible.class);
        // Sin perfil, el entorno se asume productivo: fallar cerrado es la omision aceptable.
        assertThat(guardia.productivo()).isTrue();
    }

    @Test
    @DisplayName("produccion con el aliado SIMULADO: el proceso NO arranca y dice por que")
    void produccionConSimuladoNoArranca() {
        var args = new java.util.ArrayList<String>();
        propiedades().forEach((k, v) -> args.add("--" + k + "=" + v));
        args.add("--server.port=0");
        args.add("--aportaya.entorno.productivo=true");
        args.add("--aportaya.inversiones.aliado.modo=simulado");
        args.add("--aportaya.inversiones.aliado.api-key=" + "k".repeat(40));
        args.add("--aportaya.inversiones.aliado.firma-secreto=" + "s".repeat(40));

        assertThatThrownBy(() -> {
                    try (ConfigurableApplicationContext c =
                            new SpringApplicationBuilder(Aplicacion.class).run(args.toArray(String[]::new))) {
                        c.getId();
                    }
                })
                .hasStackTraceContaining("no puede correr en un entorno productivo")
                .hasStackTraceContaining("SINTETICOS");
    }

    @Test
    @DisplayName("el mismo modo simulado SI arranca fuera de produccion (control negativo de la prueba anterior)")
    void localConSimuladoArranca() {
        var args = new java.util.ArrayList<String>();
        propiedades().forEach((k, v) -> args.add("--" + k + "=" + v));
        args.add("--server.port=0");
        args.add("--aportaya.entorno.productivo=false");
        args.add("--aportaya.inversiones.aliado.modo=simulado");
        args.add("--aportaya.inversiones.aliado.api-key=" + "k".repeat(40));
        args.add("--aportaya.inversiones.aliado.firma-secreto=" + "s".repeat(40));

        try (ConfigurableApplicationContext c =
                new SpringApplicationBuilder(Aplicacion.class).run(args.toArray(String[]::new))) {
            assertThat(c.getBean(GuardiaDeProduccion.class).productivo()).isFalse();
        }
    }
}
