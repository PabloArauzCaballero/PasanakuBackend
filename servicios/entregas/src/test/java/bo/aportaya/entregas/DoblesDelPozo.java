package bo.aportaya.entregas;

import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOBLE de {@code aportes} para las pruebas del fondeo (regla 65).
 *
 * <p>Cumple el contrato real en tres niveles —correcto, limite, invalido— y se declara como lo que
 * es: ninguna prueba que lo use verifica al servicio real. El contrato real se ejercita en
 * {@code AdaptadoresDelPozoTest} contra un servidor HTTP local. El fondeo ya no le pregunta a
 * {@code garantia} (F2.L2.M01): no hay doble de respaldo porque no hay dependencia que simular.
 */
final class DoblesDelPozo {

    private DoblesDelPozo() {}

    /** aportes: devuelve lo que se le cargue; vacio = caido. */
    static final class Aportes implements RecaudoDelPozo {

        private Optional<Recaudo> respuesta = Optional.empty();
        final AtomicInteger consultas = new AtomicInteger();

        void responde(Recaudo recaudo) {
            this.respuesta = Optional.of(recaudo);
        }

        void seCae() {
            this.respuesta = Optional.empty();
        }

        @Override
        public Optional<Recaudo> consultar(UUID periodoId) {
            consultas.incrementAndGet();
            return respuesta;
        }
    }
}
