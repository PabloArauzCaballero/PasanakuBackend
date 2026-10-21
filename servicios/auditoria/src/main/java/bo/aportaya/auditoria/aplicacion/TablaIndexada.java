package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos.Clasificacion;
import bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos.Fila;
import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Una tabla del recurso con lo que el filtro y la fila de {@link ConsultarCatalogoDeDatos} necesitan, calculado una
 * sola vez al arrancar.
 */
record TablaIndexada(
        JsonNode nodo,
        String clave,
        String tabla,
        String esquema,
        String tipo,
        String modulo,
        String nombreModulo,
        Optional<String> servicio,
        Optional<String> queEs,
        boolean appendOnly,
        List<String> columnas,
        int salientes,
        int entrantes,
        Clasificacion clasificacion,
        String buscable) {

    static TablaIndexada de(JsonNode t) {
        String tabla = t.path("tabla").asText();
        String esquema = t.path("esquema").asText();
        List<String> columnas = new ArrayList<>();
        t.path("columnas").forEach(c -> columnas.add(c.path("nombre").asText()));
        Optional<String> queEs = ConsultarCatalogoDeDatos.texto(t.path("negocio"), "queEs");
        String buscable = ConsultarCatalogoDeDatos.normalizar(
                String.join(" ", esquema, tabla, String.join(" ", columnas), queEs.orElse("")));
        return new TablaIndexada(
                t,
                FichaDeGobierno.clave(esquema, tabla),
                tabla,
                esquema,
                t.path("tipo").asText("NEGOCIO"),
                t.path("modulo").asText(),
                t.path("nombreModulo").asText(),
                ConsultarCatalogoDeDatos.texto(t, "servicio"),
                queEs,
                t.path("appendOnly").asBoolean(),
                List.copyOf(columnas),
                t.path("relaciones").path("salientes").size(),
                t.path("relaciones").path("entrantes").size(),
                Clasificacion.de(t.path("clasificacion")),
                buscable);
    }

    Fila fila(FichaDeGobierno gobierno, int revisadas) {
        return new Fila(
                tabla,
                esquema,
                tipo,
                modulo,
                nombreModulo,
                servicio,
                queEs,
                appendOnly,
                columnas.size(),
                revisadas,
                salientes,
                entrantes,
                clasificacion,
                gobierno);
    }
}
