package bo.aportaya.entregas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * DOBLE de nucleo-financiero (regla 65): lo que haria su consumidor del mayor (CU-24) con el evento de entregas.
 *
 * <p>Hoy ningun consumidor del mayor escucha {@code entregas.desembolso_acreditado}, asi que lo que se prueba
 * es el CONTRATO de la carga: que las partidas viajen, que el asiento cuadre, que el mismo evento entregado
 * dos veces (at-least-once) y el mismo origen {@code (ENTREGA, id)} bajo otro evento NO se registren dos veces.
 * Esto NO es el mayor real: cuando el consumidor exista, {@code ContratoDeAsientosTest} se repite contra el.
 * El contrato esta escrito en {@code evidencia/carril-C/contrato-nucleo-partidas-y-pago-vendedor.md}.
 */
final class MayorDeDoble {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, BigDecimal> saldoPorCuenta = new HashMap<>();
    private final Set<UUID> eventosRegistrados = new HashSet<>();
    private final Set<String> origenesRegistrados = new HashSet<>();
    private int asientos;

    /** Lee el outbox de entregas y registra el asiento de cada evento del agregado indicado. */
    void consumirOutbox(DSLContext dsl, UUID... agregados) {
        var filas = dsl.fetch(
                "SELECT id, payload::text FROM entregas.evento_dominio WHERE agregado_id = ANY(?) ORDER BY ocurrido_en, id",
                (Object) agregados);
        for (var f : filas) {
            registrar(f.get(0, UUID.class), leer(f.get(1, String.class)));
        }
    }

    /** @return true si el asiento se registro; false si era un duplicado (por evento o por origen) o no traia asiento */
    boolean registrar(UUID eventoId, JsonNode payload) {
        if (!eventosRegistrados.add(eventoId)) {
            return false;
        }
        JsonNode asiento = payload.get("asiento");
        if (asiento == null) {
            return false;
        }
        String origen = asiento.get("origenTipo").asText() + ":"
                + asiento.get("origenId").asText();
        if (!origenesRegistrados.add(origen)) {
            return false;
        }
        BigDecimal debe = BigDecimal.ZERO;
        BigDecimal haber = BigDecimal.ZERO;
        for (JsonNode p : asiento.get("partidas")) {
            BigDecimal d = new BigDecimal(p.get("debe").asText());
            BigDecimal h = new BigDecimal(p.get("haber").asText());
            if (d.signum() < 0 || h.signum() < 0) {
                throw new IllegalStateException("partida con importe negativo");
            }
            debe = debe.add(d);
            haber = haber.add(h);
            saldoPorCuenta.merge(p.get("cuenta").asText(), d.subtract(h), BigDecimal::add);
        }
        if (debe.compareTo(haber) != 0 || debe.signum() == 0) {
            throw new IllegalStateException("asiento descuadrado o vacio: debe " + debe + " haber " + haber);
        }
        asientos++;
        return true;
    }

    BigDecimal saldo(String cuenta) {
        return saldoPorCuenta.getOrDefault(cuenta, BigDecimal.ZERO);
    }

    BigDecimal sumaDeSaldos() {
        return saldoPorCuenta.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    int asientos() {
        return asientos;
    }

    static JsonNode leer(String json) {
        try {
            return JSON.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
