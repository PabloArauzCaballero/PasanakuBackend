package bo.aportaya.plataforma.pruebas.seguridad;

import java.util.List;

/**
 * Una operacion de un contrato OpenAPI: lo que el contrato dice, sin mirar el codigo.
 *
 * @param servicio nombre del servicio (el directorio bajo {@code servicios/})
 * @param metodo get, post, put, patch o delete
 * @param ruta la ruta tal cual esta en el contrato
 * @param id el {@code operationId}, que es el nombre del metodo del controlador
 * @param parametrosDeRuta nombres de los parametros de ruta
 * @param propiedadesDelCuerpo propiedades de primer nivel del cuerpo JSON, si lo hay
 */
public record Operacion(
        String servicio,
        String metodo,
        String ruta,
        String id,
        List<String> parametrosDeRuta,
        List<String> propiedadesDelCuerpo) {

    /** Identificadores que nombran a una persona: el cuerpo o la ruta apuntan a «alguien». */
    private static final List<String> IDENTIFICADORES_DE_PERSONA = List.of("usuarioId", "participanteId", "titularId");

    public boolean cambiaEstado() {
        return !"get".equals(metodo);
    }

    /** La operacion recibe, en la ruta o en el cuerpo, el identificador de una persona. */
    public boolean nombraAUnaPersona() {
        return IDENTIFICADORES_DE_PERSONA.stream()
                .anyMatch(n -> parametrosDeRuta.contains(n) || propiedadesDelCuerpo.contains(n));
    }

    @Override
    public String toString() {
        return "%s %s %s (%s)".formatted(servicio, metodo.toUpperCase(java.util.Locale.ROOT), ruta, id);
    }
}
