package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.Solicitud;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio.FilaDeSolicitud;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** La propuesta de una solicitud a cuatro ojos como se guarda en {@code solicitud_cambio_rol.propuesta} (JSON). */
final class PropuestaDeRol {

    private static final ObjectMapper JSON = new ObjectMapper();

    private PropuestaDeRol() {}

    static String serializar(String codigo, String nombre, String descripcion, Set<String> permisos) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", codigo);
        m.put("nombre", nombre);
        m.put("descripcion", descripcion);
        m.put("permisos", List.copyOf(new TreeSet<>(permisos)));
        try {
            return JSON.writeValueAsString(m);
        } catch (JsonProcessingException imposible) {
            throw new IllegalStateException("La propuesta de rol no se puede serializar", imposible);
        }
    }

    @SuppressWarnings("unchecked")
    static Solicitud leer(FilaDeSolicitud fila) {
        try {
            Map<String, Object> m = JSON.readValue(fila.propuestaJson(), Map.class);
            Set<String> permisos = new TreeSet<>((List<String>) m.get("permisos"));
            return new Solicitud(
                    fila.id(),
                    fila.tipo(),
                    fila.rolId().orElse(null),
                    (String) m.get("codigo"),
                    (String) m.get("nombre"),
                    (String) m.get("descripcion"),
                    permisos,
                    fila.motivo(),
                    fila.propuestaPor(),
                    fila.propuestaEn());
        } catch (JsonProcessingException ilegible) {
            throw new IllegalStateException("La propuesta guardada de la solicitud no se puede leer", ilegible);
        }
    }
}
