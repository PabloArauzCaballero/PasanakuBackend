package bo.aportaya.plataforma.pruebas.barrido;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Lo que cada modulo hereda: las reglas propias aplicadas sobre SUS fuentes.
 *
 * <p>Viven aca y no en cada servicio a proposito. Ningun servicio puede
 * desactivarlas, y el que agrega la regla numero trece la agrega una vez.
 */
public final class Barrido {

    private final Path raiz;

    private Barrido(Path raiz) {
        this.raiz = raiz;
    }

    /** El barrido sobre el modulo desde el que corre la prueba. */
    public static Barrido delModulo() {
        return new Barrido(Path.of("src").toAbsolutePath().normalize());
    }

    public static Barrido de(Path raiz) {
        return new Barrido(raiz);
    }

    /**
     * Codigo de produccion solamente. La regla sin-umbral-literal dice, literal,
     * «fuera de seeders/ y PRUEBAS»: una prueba de dinero necesita escribir importes,
     * y prohibirselos la obligaria a construirlos por concatenacion para nada.
     */
    private Path soloProduccion() {
        Path principal = raiz.resolve("main/java");
        return Files.isDirectory(principal) ? principal : raiz;
    }

    /** tamano-archivo: 300 lineas o mas bloquean. */
    public void ningunArchivoBloquea() {
        assertThat(describir(TamanoDeArchivo.bloqueantes(raiz)))
                .as("tamano-archivo: 300 lineas o mas bloquean — son varias piezas que nadie separo")
                .isEmpty();
    }

    /** sin-umbral-literal: la cifra va a seeders/ y se lee del catalogo. */
    public void ningunUmbralEnElCodigo() {
        assertThat(describir(SinUmbralLiteral.revisar(soloProduccion())))
                .as("sin-umbral-literal (invariante 10): umbrales y tarifas son catalogo, no constantes")
                .isEmpty();
    }

    /**
     * clave-idempotencia-suelta (ADR-046): {@code .where(DSL.field("clave_idempotencia")}
     * sin {@code .and(...)} en la misma sentencia. Opt-in, no forma parte de la plantilla
     * de {@code nuevo_servicio.py}: cada servicio tiene su propio diseño de
     * {@code clave_idempotencia} (columnas, tabla, unicidad), y esta regla es especifica
     * del helper {@code Idempotencia} de {@code comun-web} — aplicarla a ciegas en todo el
     * monorepo gritaria sobre columnas de otros servicios que no tienen nada que ver.
     */
    public void ningunaClaveIdempotenciaSuelta() {
        assertThat(describir(ClaveIdempotenciaSuelta.revisar(soloProduccion())))
                .as("clave-idempotencia-suelta (ADR-046): la identidad es (usuario_id, operacion,"
                        + " clave_idempotencia), nunca la clave sola")
                .isEmpty();
    }

    private List<String> describir(List<Hallazgo> hallazgos) {
        return hallazgos.stream().map(h -> h.describir(raiz)).toList();
    }
}
