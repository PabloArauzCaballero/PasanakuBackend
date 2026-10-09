package bo.aportaya.inversiones.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;

/**
 * El libro de la billetera, visto desde este servicio: SOLO por contrato con
 * {@code nucleo-financiero}. Este servicio no escribe asientos ni lee su esquema.
 *
 * <p>Toda operacion lleva clave de idempotencia: repetirla devuelve la referencia
 * original sin mover nada. El contrato exacto que pide este puerto esta en
 * {@code evidencia/carril-D/contrato-requerido-nucleo.md}; lo que hoy NO existe en el
 * productor (motivo de retencion de inversion, debito y credito de inversion, saldo con
 * lo comprometido en pozos) es una decision del carril del nucleo.
 *
 * <p>Falla cerrada: ante duda lanza {@link LibroNoDisponible} y quien llama NO asume que
 * la operacion ocurrio ni que no ocurrio.
 */
public interface LibroDelTitular {

    SaldoDelTitular saldo(UUID cuentaId);

    /** Aparta el importe sin sacarlo de la cuenta. Devuelve el id de la retencion. */
    UUID retener(UUID cuentaId, Dinero monto, String claveIdempotencia, UUID referenciaOrden);

    /** Devuelve lo retenido al disponible. Idempotente. */
    void liberar(UUID retencionId, String claveIdempotencia);

    /**
     * Convierte la retencion en el debito definitivo del importe (sale del disponible Y del
     * retenido en una sola operacion del libro). Devuelve el id de la transaccion.
     */
    UUID debitarInversion(UUID retencionId, Dinero monto, String claveIdempotencia);

    /** Acredita al titular lo que el aliado pago. Devuelve el id de la transaccion. */
    UUID acreditarRescate(UUID cuentaId, Dinero monto, String claveIdempotencia, UUID referenciaRescate);

    /**
     * @param titularId de quien es la cuenta: se compara con la sesion, no se confia en el id que mando el cliente
     * @param disponible lo que se puede usar ahora
     * @param retenido ya apartado por otro motivo
     * @param comprometidoEnPozos parte del disponible afectada a aportes y pozos de grupos
     */
    record SaldoDelTitular(UUID titularId, Dinero disponible, Dinero retenido, Dinero comprometidoEnPozos) {

        /** Lo invertible: el disponible que NO esta afectado a un pozo. */
        public Dinero invertible() {
            Dinero libre = disponible.menos(comprometidoEnPozos);
            return libre.esNegativo() ? Dinero.cero(disponible.moneda()) : libre;
        }
    }

    /** El libro rechazo, definitivamente, por falta de saldo. */
    class SaldoInsuficiente extends RuntimeException {
        public SaldoInsuficiente() {
            super("saldo insuficiente");
        }
    }

    /** No se sabe que paso: timeout, corte o respuesta ilegible. */
    class LibroNoDisponible extends RuntimeException {
        public LibroNoDisponible(String mensaje) {
            super(mensaje);
        }

        public LibroNoDisponible(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
