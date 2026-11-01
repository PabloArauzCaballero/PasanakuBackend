package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.PagoDeRecargaDePrueba;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.RecargasProveedorLocal;
import bo.aportaya.nucleofinanciero.infraestructura.RecargasProveedorSimulado;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Que proveedor de recargas queda vivo en cada perfil, con el contexto de Spring entero.
 *
 * <p>TEST corre con perfil {@code local} y {@code aportaya.recarga.qr.modo=prueba}: ahi el QR es de prueba y
 * existe «Simular pago». En {@code production} el doble no puede existir ni pidiendolo por propiedad, y el
 * pago de prueba no esta disponible.
 */
class ArranqueRecargaQrTest {

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        var contenedor = BaseDePrueba.contenedor();
        registro.add("spring.datasource.url", contenedor::getJdbcUrl);
        registro.add("spring.datasource.username", contenedor::getUsername);
        registro.add("spring.datasource.password", contenedor::getPassword);
        registro.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registro.add("aportaya.jwt.jwks-uri", () -> "http://identidad:8080/.well-known/jwks.json");
        registro.add("SEGURIDAD_PIMIENTA", () -> "pimienta-de-prueba");
        registro.add("CUENTA_PUENTE_CUSTODIA", () -> "00000000-0000-0000-0000-0000000000c0");
        registro.add("QR_INTERNO_CLAVE", () -> "clave-de-prueba-de-los-qr-internos-0001");
        registro.add("APORTAYA_CORS_ORIGENES", () -> "https://app.aportaya.bo");
    }

    /** Lo que corre en TEST: perfil local, que trae {@code modo: prueba} en application-local.yml. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("local")
    class EnTest {

        @Autowired
        private ProveedorDeRecargas proveedor;

        @Autowired
        private PagoDeRecargaDePrueba pago;

        @Test
        void elQrEsDePruebaYSePuedeSimularElPago() {
            assertThat(proveedor).isInstanceOf(RecargasProveedorLocal.class);
            assertThat(pago.disponible()).isTrue();
        }
    }

    /**
     * Production con el modo pedido a mano: el doble esta atado al perfil y el adaptador de siempre se
     * retira por el modo, asi que no queda proveedor de recargas y el proceso NO arranca. Falla cerrado: en
     * production nunca hay QR de prueba ni pago simulado.
     */
    @Test
    void enProduccionElModoPruebaNoArranca() {
        var contenedor = BaseDePrueba.contenedor();
        var arranque = new SpringApplicationBuilder(Aplicacion.class)
                .profiles("production")
                .initializers(contexto -> contexto.getBeanFactory()
                        .registerSingleton("proveedorDeRetiro", Mockito.mock(ProveedorDeRetiro.class)));

        // Como argumentos y no con .properties(): esas son propiedades por defecto y el perfil las pisa.
        String[] argumentos = {
            "--server.port=0",
            "--aportaya.recarga.qr.modo=prueba",
            "--spring.datasource.url=" + contenedor.getJdbcUrl(),
            "--spring.datasource.username=" + contenedor.getUsername(),
            "--spring.datasource.password=" + contenedor.getPassword(),
            "--spring.kafka.bootstrap-servers=localhost:9092",
            "--aportaya.jwt.jwks-uri=http://identidad:8080/.well-known/jwks.json",
            "--SEGURIDAD_PIMIENTA=pimienta-de-prueba",
            "--CUENTA_PUENTE_CUSTODIA=00000000-0000-0000-0000-0000000000c0",
            "--QR_INTERNO_CLAVE=clave-de-prueba-de-los-qr-internos-0001",
            "--APORTAYA_CORS_ORIGENES=https://app.aportaya.bo"
        };

        assertThatThrownBy(() -> arranque.run(argumentos))
                .rootCause()
                .hasMessageContaining(ProveedorDeRecargas.class.getName());
    }

    /** Production sin nada: el proveedor sigue siendo el de siempre. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("production")
    class EnProduccionPorOmision {

        @MockitoBean
        private ProveedorDeRetiro proveedorDeRetiro;

        @Autowired
        private ProveedorDeRecargas proveedor;

        @Test
        void elProveedorEsElAdaptadorDeSiempre() {
            assertThat(proveedor).isInstanceOf(RecargasProveedorSimulado.class);
        }
    }
}
