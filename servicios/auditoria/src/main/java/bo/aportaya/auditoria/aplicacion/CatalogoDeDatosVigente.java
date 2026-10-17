package bo.aportaya.auditoria.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * Tableros · H7 — el catalogo de datos que genera {@code scripts/catalogo_de_datos.py}, tal como quedo en el
 * recurso del servicio. No se calcula en la peticion: es estructura y narrativa, cambia con el modelo y no con los
 * datos. Se lee una vez al arrancar; si falta, el servicio no arranca (un catalogo vacio diria que no hay tablas).
 *
 * <p>Vive aca y no en {@code apps/backoffice/public/} porque describe el esquema completo de la base: servido como
 * archivo estatico quedaria al alcance de cualquiera, sin sesion (D14).
 */
@Service
public class CatalogoDeDatosVigente {

    static final String RECURSO = "/catalogo/catalogo-datos.json";

    private final JsonNode catalogo;

    public CatalogoDeDatosVigente(ObjectMapper mapper) {
        try (InputStream entrada = CatalogoDeDatosVigente.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException(
                        "Falta " + RECURSO + ": se genera con scripts/catalogo_de_datos.py --escribir");
            }
            this.catalogo = mapper.readTree(entrada);
        } catch (IOException e) {
            throw new IllegalStateException("El catalogo de datos " + RECURSO + " no es JSON valido", e);
        }
    }

    public JsonNode catalogo() {
        return catalogo;
    }
}
