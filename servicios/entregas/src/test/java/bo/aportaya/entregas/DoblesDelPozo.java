package bo.aportaya.entregas;

import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.entregas.dominio.puertos.RespaldoEmpresarial;
import bo.aportaya.plataforma.dominio.Dinero;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOBLES de {@code aportes} y {@code garantia} para las pruebas del fondeo (regla 65).
 *
 * <p>Cada uno cumple el contrato real en tres niveles —correcto, limite, invalido— y se declara
 * como lo que es: ninguna prueba que los use verifica a los servicios reales. El contrato real
 * se ejercita en {@code RecaudoPorHttpTest} y {@code RespaldoPorHttpTest} contra un servidor HTTP
 * local, y el lado productor en las pruebas de garantia y aportes.
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

    /**
     * garantia: IDEMPOTENTE por turno como la real. Aplica todo o nada contra un disponible que
     * se fija desde la prueba, y recuerda la primera cobertura de cada turno.
     */
    static final class Garantia implements RespaldoEmpresarial {

        private Dinero disponible;
        private boolean caida;
        private boolean sinReserva;
        private Dinero cubreOtroImporte;
        private final Map<UUID, Cobertura> aplicadas = new HashMap<>();
        final AtomicInteger llamadas = new AtomicInteger();
        final AtomicInteger coberturasNuevas = new AtomicInteger();

        void conDisponible(Dinero disponible) {
            this.disponible = disponible;
            this.caida = false;
            this.sinReserva = false;
            this.cubreOtroImporte = null;
            this.aplicadas.clear();
            this.llamadas.set(0);
            this.coberturasNuevas.set(0);
        }

        void seCae(boolean caida) {
            this.caida = caida;
        }

        void sinReserva(boolean sinReserva) {
            this.sinReserva = sinReserva;
        }

        void cubreUnImporteDistinto(Dinero importe) {
            this.cubreOtroImporte = importe;
        }

        int coberturasAplicadas() {
            return aplicadas.size();
        }

        @Override
        public synchronized Optional<Cobertura> cubrir(
                UUID grupoId, UUID periodoId, UUID turnoId, Dinero faltanteEsperado) {
            llamadas.incrementAndGet();
            if (caida) {
                return Optional.empty();
            }
            Dinero cero = Dinero.cero(faltanteEsperado.moneda());
            if (sinReserva) {
                return Optional.of(new Cobertura(Resultado.SIN_RESERVA, null, cero, faltanteEsperado));
            }
            var previa = aplicadas.get(turnoId);
            if (previa != null) {
                return Optional.of(previa);
            }
            if (faltanteEsperado.esMayorQue(disponible)) {
                return Optional.of(
                        new Cobertura(Resultado.INSUFICIENTE, null, cero, faltanteEsperado.menos(disponible)));
            }
            disponible = disponible.menos(faltanteEsperado);
            Dinero cubierto = cubreOtroImporte != null ? cubreOtroImporte : faltanteEsperado;
            var cobertura = new Cobertura(Resultado.APLICADA, UUID.randomUUID(), cubierto, cero);
            aplicadas.put(turnoId, cobertura);
            coberturasNuevas.incrementAndGet();
            return Optional.of(cobertura);
        }
    }
}
