package bo.aportaya.nucleofinanciero.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lo que se guarda para repetir la respuesta de una retencion con la misma clave: el id y la vigencia.
 *
 * <p>Carril G (hallado con cuatro envios simultaneos de la misma clave): la repeticion releia la retencion de
 * {@code retencion_saldo}, y esa tabla solo la ve el rol privilegiado o el sistema (la politica de fila). Para el titular, que
 * es quien reintenta, la fila «no existia» y el reintento terminaba en 500 en vez de devolver la retencion que el original
 * ya habia creado. Con el id y la vigencia guardados, la repeticion no necesita releer la tabla: los saldos se leen de la
 * billetera, que si es del titular.
 */
final class CuerpoDeRetencion {

    private static final ObjectMapper JSON = new ObjectMapper();

    private CuerpoDeRetencion() {}

    record Guardado(UUID retencionId, OffsetDateTime expiraEn) {}

    static String escribir(UUID retencionId, OffsetDateTime expiraEn) {
        var cuerpo = JSON.createObjectNode().put("retencionId", retencionId.toString());
        if (expiraEn != null) {
            cuerpo.put("expiraEn", expiraEn.toString());
        }
        return cuerpo.toString();
    }

    static Guardado leer(String cuerpoGuardado) {
        try {
            JsonNode nodo = JSON.readTree(cuerpoGuardado);
            String expira = nodo.path("expiraEn").asText("");
            return new Guardado(
                    UUID.fromString(nodo.path("retencionId").asText()),
                    expira.isEmpty() ? null : OffsetDateTime.parse(expira));
        } catch (IOException | IllegalArgumentException ilegible) {
            throw new IllegalStateException("La respuesta guardada de la retencion no se puede leer", ilegible);
        }
    }
}
