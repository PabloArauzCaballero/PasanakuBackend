package bo.aportaya.plataforma.pruebas.web;

import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.Publico;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Una ruta del producto, tal como el servicio la expone de verdad.
 *
 * <p>Se lee del mapeo de MVC y no del YAML del contrato a proposito: el YAML dice lo
 * que <b>deberia</b> haber, el mapeo dice lo que hay. Para una sabana de seguridad
 * interesa lo segundo — un endpoint que el contrato no declara es exactamente el que
 * nadie va a acordarse de proteger.
 *
 * @param metodo verbo HTTP
 * @param patron el patron con sus variables, para poder nombrarlo en un fallo
 * @param ruta el patron con las variables ya rellenadas, listo para pedir
 * @param permiso el codigo que exige, o {@code null} si es publico
 * @param publico si declara {@link Publico}
 * @param exigeClave si declara {@code Idempotency-Key} como cabecera obligatoria
 */
public record Endpoint(
        String metodo,
        String patron,
        String ruta,
        String permiso,
        boolean publico,
        boolean exigeClave,
        /** Recibe un archivo: la sabana tiene que hablarle multipart o come un 415. */
        boolean recibeArchivo) {

    /** Un valor que sirve para {@code UUID}, para {@code String} y para el comparador de rutas. */
    private static final String RELLENO = "00000000-0000-4000-8000-000000000000";

    @Override
    public String toString() {
        return metodo + " " + patron;
    }

    /** Todas las rutas del producto. Sin las de actuator, el JWKS ni el reenvio de error. */
    public static List<Endpoint> de(RequestMappingHandlerMapping mapeo) {
        List<Endpoint> hallados = new ArrayList<>();
        mapeo.getHandlerMethods().forEach((info, manejador) -> {
            var condicion = info.getPathPatternsCondition();
            if (condicion == null) {
                return;
            }
            var metodos = info.getMethodsCondition().getMethods();
            for (String patron : condicion.getPatternValues()) {
                if (esDeInfraestructura(patron)) {
                    continue;
                }
                for (var verbo : metodos.isEmpty()
                        ? List.of(org.springframework.web.bind.annotation.RequestMethod.GET)
                        : metodos) {
                    hallados.add(new Endpoint(
                            verbo.name(),
                            patron,
                            rellenar(patron),
                            permisoDe(manejador),
                            esPublico(manejador),
                            exigeClaveDeIdempotencia(manejador),
                            info.getConsumesCondition().getConsumableMediaTypes().stream()
                                    .anyMatch(
                                            t -> t.includes(org.springframework.http.MediaType.MULTIPART_FORM_DATA))));
                }
            }
        });
        hallados.sort(java.util.Comparator.comparing(Endpoint::patron).thenComparing(Endpoint::metodo));
        return List.copyOf(hallados);
    }

    /** Los que exigen un permiso: los unicos donde el 403 tiene sentido. */
    public static List<Endpoint> conPermiso(RequestMappingHandlerMapping mapeo) {
        return de(mapeo).stream().filter(e -> e.permiso() != null).toList();
    }

    /** Los que exigen sesion: los unicos donde el 401 tiene sentido. */
    public static List<Endpoint> conSesion(RequestMappingHandlerMapping mapeo) {
        return de(mapeo).stream().filter(e -> !e.publico()).toList();
    }

    /** Los que exigen clave de idempotencia por cabecera. */
    public static List<Endpoint> conClave(RequestMappingHandlerMapping mapeo) {
        return de(mapeo).stream().filter(Endpoint::exigeClave).toList();
    }

    /**
     * Todos los codigos que este servicio exige, en alguno de sus endpoints.
     *
     * <p>Es la munición del barrido cruzado: para probar que el permiso de una
     * operacion no abre la de al lado hace falta saber cuales son las de al lado, y eso
     * se lee del mapeo — no de una lista que alguien tendria que mantener.
     */
    public static java.util.Set<String> permisosDelServicio(RequestMappingHandlerMapping mapeo) {
        return de(mapeo).stream()
                .map(Endpoint::permiso)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * {@code Idempotency-Key} declarada como cabecera obligatoria.
     *
     * <p>Se lee de los parametros del manejador y no del YAML: lo que decide si la
     * peticion se rechaza es el codigo generado, y una cabecera que el contrato declara
     * y el codigo no exige es exactamente el hueco que interesa encontrar.
     */
    private static boolean exigeClaveDeIdempotencia(HandlerMethod manejador) {
        for (var parametro : manejador.getMethodParameters()) {
            var cabecera =
                    parametro.getParameterAnnotation(org.springframework.web.bind.annotation.RequestHeader.class);
            if (cabecera != null && "Idempotency-Key".equalsIgnoreCase(cabecera.value())) {
                return cabecera.required();
            }
        }
        return false;
    }

    private static boolean esDeInfraestructura(String patron) {
        return patron.startsWith("/actuator") || patron.startsWith("/.well-known") || patron.equals("/error");
    }

    private static String rellenar(String patron) {
        StringBuilder ruta = new StringBuilder();
        for (String tramo : patron.split("/", -1)) {
            if (tramo.isEmpty()) {
                continue;
            }
            ruta.append('/').append(tramo.startsWith("{") ? RELLENO : tramo);
        }
        return ruta.isEmpty() ? "/" : ruta.toString();
    }

    /** El identificador con el que se rellena una variable de ruta. */
    public static UUID relleno() {
        return UUID.fromString(RELLENO);
    }

    private static String permisoDe(HandlerMethod manejador) {
        Permiso propio = manejador.getMethodAnnotation(Permiso.class);
        if (propio != null) {
            return propio.value();
        }
        if (manejador.getMethod().isAnnotationPresent(Publico.class)) {
            return null;
        }
        Permiso deClase = manejador.getBeanType().getAnnotation(Permiso.class);
        return deClase == null ? null : deClase.value();
    }

    private static boolean esPublico(HandlerMethod manejador) {
        if (manejador.getMethod().isAnnotationPresent(Publico.class)) {
            return true;
        }
        return manejador.getBeanType().isAnnotationPresent(Publico.class)
                && !manejador.getMethod().isAnnotationPresent(Permiso.class);
    }
}
