package bo.aportaya.inversiones.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.infraestructura.clientes.AliadoSimuladoHttp;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lo que impide que datos sinteticos lleguen a un entorno productivo: la guardia que corta
 * el arranque, la validacion del adaptador y el modo deshabilitado por omision.
 */
class ConfiguracionDelAliadoTest {

    private static final String CLAVE_LARGA = "k".repeat(40);

    @Test
    @DisplayName("PRODUCCION con el aliado simulado: el proceso NO arranca")
    void produccionConSimuladoFalla() {
        assertThatThrownBy(() -> new GuardiaDeProduccion(true, "simulado"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SINTETICOS")
                .hasMessageContaining("productivo");
    }

    @Test
    @DisplayName("produccion con un modo desconocido (por ejemplo «real») tambien falla")
    void produccionConOtroModoFalla() {
        assertThatThrownBy(() -> new GuardiaDeProduccion(true, "real")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("produccion con el aliado deshabilitado arranca, y local con el simulado tambien")
    void lasCombinacionesPermitidas() {
        assertThat(new GuardiaDeProduccion(true, "deshabilitado").productivo()).isTrue();
        assertThat(new GuardiaDeProduccion(false, "simulado").productivo()).isFalse();
        assertThat(new GuardiaDeProduccion(false, "deshabilitado").productivo()).isFalse();
    }

    @Test
    @DisplayName("el adaptador deshabilitado no confirma nada: toda llamada es «no disponible»")
    void deshabilitadoNoConfirma() {
        var a = new AliadoSimuladoHttp(
                "deshabilitado", URI.create("http://127.0.0.1:4030"), "", "", Duration.ofSeconds(1));
        assertThatThrownBy(a::catalogo).isInstanceOf(AliadoNoDisponible.class);
        assertThatThrownBy(() -> a.consultar(java.util.UUID.randomUUID())).isInstanceOf(AliadoNoDisponible.class);
    }

    @Test
    @DisplayName("el adaptador real no existe: un modo distinto de deshabilitado/simulado se rechaza")
    void modoDesconocido() {
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "real", URI.create("http://127.0.0.1:4030"), CLAVE_LARGA, CLAVE_LARGA, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el simulador solo habla con loopback, por http, sin credenciales en la URL y con claves de 32 bytes")
    void validacionDelSimulado() {
        Duration t = Duration.ofSeconds(1);
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("http://aliado.ejemplo.test"), CLAVE_LARGA, CLAVE_LARGA, t))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("https://127.0.0.1:4030"), CLAVE_LARGA, CLAVE_LARGA, t))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("http://u:p@127.0.0.1:4030"), CLAVE_LARGA, CLAVE_LARGA, t))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("http://127.0.0.1:4030"), "corta", CLAVE_LARGA, t))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("http://127.0.0.1:4030"), CLAVE_LARGA, "corta", t))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("http://127.0.0.1:4030"), CLAVE_LARGA, CLAVE_LARGA, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new AliadoSimuladoHttp(
                        "simulado", URI.create("http://127.0.0.1:4030"), CLAVE_LARGA, CLAVE_LARGA, t))
                .doesNotThrowAnyException();
    }
}
