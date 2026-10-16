package bo.aportaya.garantia;

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
 * DOBLE de nucleo-financiero (regla 65): lo que haria su consumidor con CU-24.
 *
 * <p>Hoy ningun consumidor del mayor escucha estos eventos, asi que lo que se prueba es el
 * CONTRATO de la carga: que las partidas viajen, que cada asiento cuadre, que un evento
 * repetido no se registre dos veces y que el contra-asiento deje la cuenta como estaba.
 * Esto NO es el mayor real: cuando el consumidor exista, esta prueba se repite contra el.
 */
final class MayorDeDoble {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, BigDecimal> saldoPorCuenta = new HashMap<>();
    private final Set<UUID> eventosRegistrados = new HashSet<>();
    private int asientos;

    /** Lee el outbox del esquema y registra los asientos de los agregados indicados. */
    void consumirOutbox(DSLContext dsl, String esquema, UUID... agregados) {
        var consulta =
                switch (esquema) {
                    case "garantia" ->
                        """
                    SELECT id, payload::text FROM garantia.evento_dominio
                    WHERE agregado_id = ANY(?) ORDER BY ocurrido_en, id""";
                    default -> throw new IllegalArgumentException("Esquema de outbox no previsto: " + esquema);
                };
        var filas = dsl.fetch(consulta, (Object) agregados);
        for (var f : filas) {
            registrar(f.get(0, UUID.class), leer(f.get(1, String.class)));
        }
    }

    /** Un evento entregado dos veces (at-least-once) se registra una sola vez. */
    void registrar(UUID eventoId, JsonNode payload) {
        if (!eventosRegistrados.add(eventoId)) {
            return;
        }
        JsonNode asiento = payload.get("asiento");
        if (asiento == null) {
            return;
        }
        BigDecimal debe = BigDecimal.ZERO;
        BigDecimal haber = BigDecimal.ZERO;
        for (JsonNode p : asiento.get("partidas")) {
            BigDecimal d = new BigDecimal(p.get("debe").asText());
            BigDecimal h = new BigDecimal(p.get("haber").asText());
            debe = debe.add(d);
            haber = haber.add(h);
            saldoPorCuenta.merge(p.get("cuenta").asText(), d.subtract(h), BigDecimal::add);
        }
        if (debe.compareTo(haber) != 0) {
            throw new IllegalStateException("asiento descuadrado: debe " + debe + " haber " + haber);
        }
        asientos++;
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
