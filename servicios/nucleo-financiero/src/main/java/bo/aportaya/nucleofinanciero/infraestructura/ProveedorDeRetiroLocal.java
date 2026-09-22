package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El doble de {@code local}/{@code test} de {@link ProveedorDeRetiro} (H3.S1.M3,
 * completado a los tres niveles en H4.S2.M2).
 *
 * <p>Tres montos marca, sin flags nuevas ni configuracion — cada uno ejercita una rama
 * real del puerto:
 *
 * <ul>
 *   <li>Cualquier otro monto: {@code ACEPTADO} de una — el camino feliz.
 *   <li>{@code 666.66}: {@code RECHAZADO} de una — {@code instruirPago} lo traduce a un
 *       rechazo automatico (ver {@code CU11RechazosTest#elProveedorRechazaTrasAutorizar}).
 *   <li>{@code 111.11}: {@code TIMEOUT} — la orden queda {@code EN_PROCESO} sin saber
 *       que paso de verdad, que es exactamente el caso que {@code ReconciliacionDeRetiros}
 *       existe para resolver. {@link #resolverComoSiElProveedorHubieraContestado} es la
 *       puerta de prueba para simular que, más tarde, el proveedor SI contesta.
 * </ul>
 *
 * <p>{@code @Profile({"local","test"})}, mismo patron que {@code SegundoFactorLocal}:
 * en {@code production} este bean no existe. Hasta que exista un adaptador HTTP real
 * contra el proveedor verdadero (fuera del alcance de este carril: no hay contrato
 * publicado con el que integrar sin inventarlo), un servicio en {@code production} que
 * necesite instruir un pago NO arranca — es el mismo "fail closed" que Q-04 fija para
 * el proveedor de retiros. Esto es un rojo INTENCIONAL en {@code ArranqueProduccionTest},
 * declarado en el daily, no escondido.
 */
@Component
@Profile({"local", "test"})
public class ProveedorDeRetiroLocal implements ProveedorDeRetiro {

    /** Un monto exacto reservado para forzar el rechazo en una prueba, sin flags nuevas. */
    private static final BigDecimal MONTO_QUE_RECHAZA = new BigDecimal("666.66");

    /** Un monto exacto reservado para forzar un TIMEOUT — H4.S2.M2, tercer nivel. */
    private static final BigDecimal MONTO_QUE_DEMORA = new BigDecimal("111.11");

    private final ConcurrentHashMap<String, Estado> referencias = new ConcurrentHashMap<>();

    @Override
    public Resultado instruir(UUID ordenRetiroId, Dinero monto) {
        String referencia = "PROV-LOCAL-" + ordenRetiroId;
        Estado estado;
        if (monto.monto().compareTo(MONTO_QUE_RECHAZA) == 0) {
            estado = Estado.RECHAZADO;
        } else if (monto.monto().compareTo(MONTO_QUE_DEMORA) == 0) {
            estado = Estado.TIMEOUT;
        } else {
            estado = Estado.ACEPTADO;
        }
        referencias.put(referencia, estado);
        return new Resultado(estado, referencia);
    }

    /** Lo que {@code ReconciliacionDeRetiros} (H4.S2.M3) consulta. {@code TIMEOUT} si no se conoce. */
    @Override
    public Estado consultar(String referencia) {
        return referencias.getOrDefault(referencia, Estado.TIMEOUT);
    }

    /**
     * Puerta de prueba: simula que el proveedor, mas tarde, SI contesto una referencia
     * que antes quedo en {@code TIMEOUT}. Sin esto no hay forma de probar
     * {@code ReconciliacionDeRetiros} sin esperar un timeout real — que es exactamente
     * lo que un doble tiene que evitar.
     */
    public void resolverComoSiElProveedorHubieraContestado(String referencia, Estado estadoFinal) {
        referencias.put(referencia, estadoFinal);
    }
}
