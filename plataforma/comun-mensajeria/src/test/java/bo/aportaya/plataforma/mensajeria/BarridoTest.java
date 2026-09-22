package bo.aportaya.plataforma.mensajeria;

import bo.aportaya.plataforma.pruebas.barrido.Barrido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las reglas propias de planes/00 §6 aplicadas a las fuentes de {@code comun-mensajeria}.
 *
 * <p>H2.S3.M4: {@code aportaya.outbox.*} ({@code intentos-maximos}, {@code backoff-tope}...)
 * viaja por {@code @Value} con defaults (H2.S1.M2/H2.S3.M3), nunca como constante {@code
 * INTENTOS_MAXIMOS = 10} en {@link Relevo} — que {@code sin-umbral-literal} atraparía por el
 * {@code MAXIMO}/{@code TOPE} en el nombre (mismo hallazgo que {@code UMBRAL_TRANSITORIO} en
 * `comun-web`, H1.S3.M2). Este modulo no tenia {@code BarridoTest} todavia.
 */
class BarridoTest {

    private final Barrido barrido = Barrido.delModulo();

    @Test
    @DisplayName("tamano-archivo: ningun archivo llega a 300 lineas")
    void ningunArchivoBloquea() {
        barrido.ningunArchivoBloquea();
    }

    @Test
    @DisplayName("sin-umbral-literal (H2.S3.M4): aportaya.outbox.* no vive como constante en el codigo")
    void ningunUmbralEnElCodigo() {
        barrido.ningunUmbralEnElCodigo();
    }
}
