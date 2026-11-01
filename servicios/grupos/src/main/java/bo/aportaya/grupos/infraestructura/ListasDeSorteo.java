package bo.aportaya.grupos.infraestructura;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Las listas del sorteo (entropias y resultado) como JSON de la base. Salio de {@link SorteoRepositorio} (limite de 300
 * lineas) sin cambiar lo que hace; la usa tambien {@link ConsultasRepositorio}.
 */
final class ListasDeSorteo {

    private ListasDeSorteo() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    static String comoJson(List<String> valores) {
        try {
            return JSON.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<String> leerLista(org.jooq.JSONB json) {
        if (json == null) return List.of();
        try {
            return JSON.readValue(json.data(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON de sorteo ilegible", e);
        }
    }
}
