package bo.aportaya.plataforma.pruebas;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El doble de {@code GET /reclamos/usuarios/{usuarioId}/efecto} de {@code cumplimiento}, para que
 * {@code garantia} (cobranza, restricciones) y {@code organizador} (sanciones) terminen su parte
 * sin esperar a que el otro servicio este desplegado (regla 65).
 *
 * <p>Contrato: {@code evidencia/carril-A3/contrato-efecto-reclamo.md} y el OpenAPI de
 * {@code cumplimiento} ({@code consultarEfectoDeReclamos}). Este doble habla EXACTAMENTE ese JSON:
 * la prueba de contrato del lado productor ({@code ReclamosControllerWebTest}) alimenta a
 * {@link #leer} con la respuesta real del controlador, de modo que si el productor cambia la forma,
 * cae la prueba de aca.
 *
 * <p><b>Es un doble declarado, no el servicio real</b>: lo verificado contra el queda declarado
 * como «contra el doble». La integracion contra {@code cumplimiento} de verdad sigue pendiente de
 * quien implemente el consumidor.
 *
 * <p><b>La regla del consumidor</b> ({@link #leer}) es la que el contrato exige y la que se copia
 * al adaptador real: <b>cualquier cosa que no sea una respuesta 200 entendible FRENA</b> —el
 * servicio caido, un 5xx, un 403, un JSON roto, una respuesta sin los campos, una demora de mas—.
 * Que "no se pudo saber si hay un reclamo" no se lea nunca como "no hay reclamo": ejecutar una
 * deuda o dejar firme una sancion con un reclamo en curso es el dano que esta consulta evita
 * (regla 91.4).
 */
public final class EfectoDeReclamosDeEnsayo implements AutoCloseable {

    /** Que se quiere ejecutar. Cada uno mira su indicador. */
    public enum Que {
        DEUDA_EJECUTABLE("bloqueaDeudaEjecutable"),
        SANCION_FIRME("bloqueaSancionFirme");

        private final String campo;

        Que(String campo) {
            this.campo = campo;
        }
    }

    public enum Decision {
        PROCEDE,
        FRENA
    }

    /** Un caso del rastro, como lo devuelve el contrato. */
    public record Caso(String motivo, boolean bloqueaSancionFirme, boolean bloqueaDeudaEjecutable) {

        public static Caso queFrenaTodo(String motivo) {
            return new Caso(motivo, true, true);
        }

        public static Caso libre() {
            return new Caso("TERMINADO_SIN_BLOQUEO", false, false);
        }

        public static Caso soloFrenaLaSancion(String motivo) {
            return new Caso(motivo, true, false);
        }
    }

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(1);

    private final DependidoDeEnsayo dependido = new DependidoDeEnsayo();
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(TIEMPO_MAXIMO).build();

    public String urlBase() {
        return dependido.urlBase();
    }

    public int peticionesRecibidas() {
        return dependido.peticionesRecibidas();
    }

    /** Nivel correcto: la persona no tiene reclamos. */
    public EfectoDeReclamosDeEnsayo sinReclamos(UUID usuario) {
        return conCasos(usuario);
    }

    /** Los indicadores agregados son el OR de los casos, como los calcula el servicio real. */
    public EfectoDeReclamosDeEnsayo conCasos(UUID usuario, Caso... casos) {
        dependido.responder(200, cuerpo(usuario, List.of(casos)));
        return this;
    }

    /** Nivel invalido: el servicio responde un error (5xx, 403...). */
    public EfectoDeReclamosDeEnsayo respondeEstado(int estadoHttp) {
        dependido.responder(estadoHttp, "{\"codigo\":\"AP-INT-01\"}");
        return this;
    }

    /** Nivel invalido: 200 pero con un cuerpo que no es el contrato. */
    public EfectoDeReclamosDeEnsayo respondeCuerpo(String cuerpoCrudo) {
        dependido.responder(200, cuerpoCrudo);
        return this;
    }

    public EfectoDeReclamosDeEnsayo tardar(long milisegundos) {
        dependido.tardar(milisegundos);
        return this;
    }

    public void caer() {
        dependido.caer();
    }

    /** Pregunta al doble como lo haria el adaptador real, con la regla de {@link #leer}. */
    public Decision consultar(UUID usuario, Que que) {
        try {
            HttpResponse<String> respuesta = http.send(
                    HttpRequest.newBuilder(URI.create(urlBase() + "/reclamos/usuarios/" + usuario + "/efecto"))
                            .timeout(TIEMPO_MAXIMO)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            return leer(respuesta.statusCode(), respuesta.body(), que);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Decision.FRENA;
        } catch (Exception cualquierFalla) {
            // Caido, sin respuesta a tiempo, conexion rechazada: no se sabe, y no saber FRENA.
            return Decision.FRENA;
        }
    }

    /**
     * La lectura de referencia del consumidor. PROCEDE solo si el servicio dijo 200 y el indicador
     * pedido es {@code false} y coherente con los casos; todo lo demas FRENA.
     */
    public static Decision leer(int estadoHttp, String cuerpo, Que que) {
        if (estadoHttp != 200 || cuerpo == null) {
            return Decision.FRENA;
        }
        try {
            Object agregado = JsonPath.read(cuerpo, "$." + que.campo);
            Object casos = JsonPath.read(cuerpo, "$.casos");
            if (!(agregado instanceof Boolean frena) || !(casos instanceof List<?> lista)) {
                return Decision.FRENA;
            }
            if (frena) {
                return Decision.FRENA;
            }
            // Defensa en profundidad: aunque el agregado diga que no, un solo caso que frena manda.
            for (int i = 0; i < lista.size(); i++) {
                Object porCaso = JsonPath.read(cuerpo, "$.casos[" + i + "]." + que.campo);
                if (!(porCaso instanceof Boolean b) || b) {
                    return Decision.FRENA;
                }
            }
            return Decision.PROCEDE;
        } catch (RuntimeException ilegible) {
            return Decision.FRENA;
        }
    }

    private static String cuerpo(UUID usuario, List<Caso> casos) {
        boolean sancion = casos.stream().anyMatch(Caso::bloqueaSancionFirme);
        boolean deuda = casos.stream().anyMatch(Caso::bloqueaDeudaEjecutable);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso c = casos.get(i);
            items.add("{\"reclamoId\":\"" + new UUID(0L, i + 1L) + "\",\"codigo\":\"RC-" + (i + 1)
                    + "\",\"categoria\":\"COBRO\",\"producto\":\"APORTE\",\"estado\":\"INGRESADO\""
                    + ",\"resultado\":null,\"fechaIngreso\":\"2026-03-01T10:00:00Z\",\"fechaRespuesta\":null"
                    + ",\"bloqueaSancionFirme\":" + c.bloqueaSancionFirme()
                    + ",\"bloqueaDeudaEjecutable\":" + c.bloqueaDeudaEjecutable()
                    + ",\"motivo\":\"" + c.motivo() + "\"}");
        }
        return "{\"usuarioId\":\"" + usuario + "\",\"bloqueaSancionFirme\":" + sancion
                + ",\"bloqueaDeudaEjecutable\":" + deuda
                + (casos.isEmpty() ? ",\"motivoGlobal\":\"SIN_RECLAMOS\"" : "")
                + ",\"casos\":[" + String.join(",", items) + "]}";
    }

    @Override
    public void close() {
        dependido.close();
    }
}
