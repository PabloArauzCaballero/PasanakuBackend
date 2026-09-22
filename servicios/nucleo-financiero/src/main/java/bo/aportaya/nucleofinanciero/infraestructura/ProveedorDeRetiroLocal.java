package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El doble de {@code local}/{@code test} de {@link ProveedorDeRetiro} (H3.S1.M3, base
 * para H4.S2).
 *
 * <p><b>Nivel minimo de este momento:</b> acepta siempre, salvo un monto marca — es lo
 * que H3 necesita para que {@code instruirPago} tenga a quien llamar. Los otros dos
 * niveles del contrato completo (responde tarde antes/despues del timeout, rechaza)
 * son H4.S2.M2 y se agregan ACA MISMO, sin tocar {@code CU11}: el punto entero de un
 * puerto es que el caso de uso no se entera del cambio.
 *
 * <p>{@code @Profile({"local","test"})}, mismo patron que {@code SegundoFactorLocal}:
 * en {@code production} este bean no existe. Hasta que H4.S2 escriba el adaptador HTTP
 * real, un servicio en {@code production} que necesite instruir un pago NO arranca —
 * es el mismo "fail closed" que Q-04 fija para el proveedor de retiros. Esto es un
 * rojo INTENCIONAL en {@code ArranqueProduccionTest} hasta H4.S2, declarado en el
 * daily, no escondido.
 */
@Component
@Profile({"local", "test"})
public class ProveedorDeRetiroLocal implements ProveedorDeRetiro {

    /** Un monto exacto reservado para forzar el rechazo en una prueba, sin flags nuevas. */
    private static final BigDecimal MONTO_QUE_RECHAZA = new BigDecimal("666.66");

    private final ConcurrentHashMap<String, Estado> referencias = new ConcurrentHashMap<>();

    @Override
    public Resultado instruir(UUID ordenRetiroId, Dinero monto) {
        String referencia = "PROV-LOCAL-" + ordenRetiroId;
        if (monto.monto().compareTo(MONTO_QUE_RECHAZA) == 0) {
            referencias.put(referencia, Estado.RECHAZADO);
            return new Resultado(Estado.RECHAZADO, referencia);
        }
        referencias.put(referencia, Estado.ACEPTADO);
        return new Resultado(Estado.ACEPTADO, referencia);
    }

    /** Lo que el job de reconciliacion (H4.S2.M3) consultaria. Vacio salvo pruebas. */
    Estado consultar(String referencia) {
        return referencias.getOrDefault(referencia, Estado.TIMEOUT);
    }
}
