package bo.aportaya.inversiones.dominio.puertos;

import bo.aportaya.inversiones.dominio.TipoProducto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * El banco o la SAFI con la que se contrata. Lo unico que el servicio sabe de el.
 *
 * <p>Tres cosas que el contrato deja explicitas porque son las que rompen los flujos de
 * dinero:
 *
 * <ul>
 *   <li>{@link AliadoNoDisponible}: no se sabe que paso (timeout, corte, 5xx). Jamas se
 *       interpreta como rechazo; se consulta por la misma referencia.
 *   <li>{@link AliadoRechazo}: el aliado dijo que no, de forma definitiva y SIN haber
 *       registrado nada. Es el unico caso en que se puede liberar lo retenido.
 *   <li>La consulta que devuelve vacio significa «el aliado no conoce esa referencia»:
 *       nunca la recibio, y reenviarla con la misma clave es seguro.
 * </ul>
 *
 * <p>La referencia de toda operacion es el id de nuestra orden o rescate, estable en los
 * reintentos. Hoy existe un solo adaptador y es simulado ({@code herramientas/aliado_simulado}).
 */
public interface AliadoDeInversion {

    List<ProductoDelAliado> catalogo();

    Optional<ValorDeCuotaDelAliado> valorDeCuota(String codigoProducto);

    OperacionDelAliado suscribir(UUID referencia, String codigoProducto, BigDecimal monto, UUID titularRef);

    OperacionDelAliado rescatar(UUID referencia, String posicionExterna, Optional<BigDecimal> cuotas);

    Optional<OperacionDelAliado> consultar(UUID referencia);

    record ProductoDelAliado(
            String codigo,
            TipoProducto tipo,
            String nombre,
            Optional<Integer> plazoDias,
            Optional<Integer> baseDias,
            Optional<BigDecimal> tasaNominalAnual,
            boolean permiteRescateAnticipado,
            Optional<BigDecimal> penalizacionAnticipo,
            Optional<Integer> diasRescate,
            Optional<String> horaCorte,
            BigDecimal montoMinimo,
            BigDecimal tasaRetencion,
            Optional<BigDecimal> tasaComisionExito,
            String fuente,
            Instant fechaCotizacion,
            boolean sintetico) {}

    record ValorDeCuotaDelAliado(String producto, LocalDate fecha, BigDecimal valor, Instant publicadoEn) {}

    enum Estado {
        PENDIENTE,
        CONFIRMADO,
        RECHAZADO
    }

    /** Lo que el aliado dice de una operacion, ya verificada su firma. */
    record OperacionDelAliado(
            UUID referencia,
            String tipo,
            Estado estado,
            Optional<BigDecimal> monto,
            Optional<BigDecimal> cuotas,
            Optional<String> posicionExterna,
            String transaccion,
            Optional<LocalDate> fechaValor,
            Optional<Instant> liquidaEn,
            Map<String, String> detalle) {}

    class AliadoNoDisponible extends RuntimeException {
        public AliadoNoDisponible(String mensaje) {
            super(mensaje);
        }
    }

    class AliadoRechazo extends RuntimeException {
        private final String codigo;

        public AliadoRechazo(String codigo) {
            super(codigo);
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }
}
