package bo.aportaya.plataforma.web;

import bo.aportaya.plataforma.pruebas.barrido.Barrido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las reglas propias de planes/00 §6, mas la especifica de este modulo (ADR-046),
 * aplicadas a las fuentes de {@code comun-web}.
 *
 * <p>{@code comun-web} no traia su propio {@code BarridoTest} todavia (la plantilla de
 * {@code nuevo_servicio.py} genera uno por servicio, no por modulo de plataforma). Es
 * este archivo el que hace que {@code ClaveIdempotenciaSuelta} corra contra el unico
 * codigo que le importa: {@code Idempotencia.java} y lo que dependa de el en este
 * modulo.
 */
class BarridoTest {

    private final Barrido barrido = Barrido.delModulo();

    @Test
    @DisplayName("tamano-archivo: ningun archivo llega a 300 lineas")
    void ningunArchivoBloquea() {
        barrido.ningunArchivoBloquea();
    }

    @Test
    @DisplayName("sin-umbral-literal: ninguna cifra regulatoria dentro del codigo")
    void ningunUmbralEnElCodigo() {
        barrido.ningunUmbralEnElCodigo();
    }

    @Test
    @DisplayName("clave-idempotencia-suelta (ADR-046, H1.S3.M2): la identidad es (usuario_id, operacion, clave)")
    void ningunaClaveIdempotenciaSuelta() {
        barrido.ningunaClaveIdempotenciaSuelta();
    }
}
