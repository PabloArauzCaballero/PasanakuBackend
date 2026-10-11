package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * H13.S1.M5 · Las reglas de alerta son verificables: cada una apunta a un detector que SOLO
 * LEE tablas que existen, a un responsable que existe en el catalogo de roles, a un
 * procedimiento que existe y esta completo, y a un aviso con la forma que
 * {@code notificaciones} acepta.
 *
 * <p>El doble de notificaciones (regla 65): este contrato no depende de que el evento de aviso
 * este sembrado ni de que el servicio corra; fija la FORMA del cuerpo
 * ({@code EntradaNotificacion}). Que cada alarma SUENE se prueba con incidentes inyectados en
 * {@code despliegue/ensayos/alarmas/ensayo_alarmas.sh}.
 */
class AlarmasContratoTest {

    private static final List<String> SECCIONES =
            List.of("sintomas", "diagnostico", "acciones seguras", "recuperacion", "validacion", "escalamiento");
    private static final Set<String> SEVERIDADES = Set.of("CRITICA", "ALTA", "NORMAL");
    private static final Set<String> TIPOS_ESPERADOS =
            Set.of("brecha_de_caja", "atraso_de_aportes", "cola_detenida", "conciliacion_vencida");
    private static final Pattern ESCRITURA =
            Pattern.compile("(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|copy)\\b");
    private static final Pattern PARAMETRO = Pattern.compile("(?<![:\\w]):([a-z_]+)\\b");
    private static final Pattern TABLA = Pattern.compile("\\b([a-z_]+)\\.([a-z_]+)\\b");

    private static Path raiz;
    private static Map<String, Object> config;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void leer() throws IOException {
        raiz = Path.of("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("servicios"))) {
            raiz = raiz.getParent();
        }
        config = new Yaml().load(Files.readString(raiz.resolve("despliegue/alertas/reglas.yml")));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> reglas() {
        return (List<Map<String, Object>>) config.get("reglas");
    }

    @Test
    @DisplayName(
            "estan las cuatro alarmas que pide el plan: brecha de caja, atraso, cola detenida, conciliacion vencida")
    void estanLasCuatro() {
        assertThat(reglas().stream().map(r -> (String) r.get("nombre")).collect(Collectors.toSet()))
                .isEqualTo(TIPOS_ESPERADOS);
        assertThat(reglas().stream().map(r -> (String) r.get("id")).distinct().count())
                .as("ids unicos")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("rechaza: umbrales sin fuente no se declaran aptos para produccion (regla 97.5.4)")
    void losUmbralesSinteticosNoSonDeProduccion() {
        assertThat(config.get("apto_produccion"))
                .as("mientras los umbrales sean sinteticos, apto_produccion debe ser false")
                .isEqualTo(false);
    }

    @Test
    @DisplayName("cada regla tiene severidad valida y responsable y escalamiento distintos, del catalogo de roles")
    void responsablesDelCatalogo() throws IOException {
        Set<String> roles = rolesSembrados();
        List<String> faltas = new ArrayList<>();
        for (Map<String, Object> r : reglas()) {
            String id = (String) r.get("id");
            if (!SEVERIDADES.contains(r.get("severidad"))) {
                faltas.add(id + ": severidad invalida");
            }
            for (String clave : List.of("responsable_rol", "escalamiento_rol")) {
                if (!roles.contains((String) r.get(clave))) {
                    faltas.add(id + ": " + clave + " «" + r.get(clave) + "» no es un rol sembrado");
                }
            }
            if (r.get("responsable_rol").equals(r.get("escalamiento_rol"))) {
                faltas.add(id + ": escalar al mismo rol no escala");
            }
            if (!String.valueOf(r.get("evento_notificable")).matches("[A-Z][A-Z_]{4,39}")) {
                faltas.add(id + ": evento_notificable con forma invalida (tipo de evento_notificable: 40 maximo)");
            }
        }
        assertThat(faltas).isEmpty();
    }

    @Test
    @DisplayName("rechaza: todo procedimiento existe y tiene las seis secciones del runbook")
    void procedimientoCompleto() throws IOException {
        List<String> faltas = new ArrayList<>();
        for (Map<String, Object> r : reglas()) {
            Path runbook = raiz.resolve((String) r.get("runbook"));
            if (!Files.isRegularFile(runbook)) {
                faltas.add(r.get("id") + ": no existe " + runbook);
                continue;
            }
            String texto = sinAcentos(Files.readString(runbook)).toLowerCase(java.util.Locale.ROOT);
            for (String seccion : SECCIONES) {
                if (!texto.contains("## " + seccion)) {
                    faltas.add(r.get("id") + ": al runbook le falta «## " + seccion + "»");
                }
            }
            if (!texto.contains(String.valueOf(r.get("id")).toLowerCase(java.util.Locale.ROOT))) {
                faltas.add(r.get("id") + ": el runbook no nombra a su alarma");
            }
        }
        assertThat(faltas).isEmpty();
    }

    @Test
    @DisplayName("rechaza: los detectores solo leen, usan parametros declarados y tablas que existen en el esquema")
    void detectoresDeSoloLectura() throws IOException {
        Set<String> declarados = ((Map<?, ?>) config.get("parametros"))
                .keySet().stream().map(String::valueOf).collect(Collectors.toSet());
        Set<String> tablas = tablasDelEsquema();
        List<String> faltas = new ArrayList<>();
        for (Map<String, Object> r : reglas()) {
            for (String clave : List.of("detector", "evidencia")) {
                String sql = String.valueOf(r.get(clave));
                if (sql.startsWith("@")) {
                    continue;
                }
                if (!sql.strip().toLowerCase(java.util.Locale.ROOT).startsWith("select")) {
                    faltas.add(r.get("id") + "." + clave + ": no empieza con SELECT");
                }
                if (ESCRITURA.matcher(sql).find()) {
                    faltas.add(r.get("id") + "." + clave + ": contiene una palabra de escritura");
                }
                Matcher p = PARAMETRO.matcher(sql);
                while (p.find()) {
                    if (!declarados.contains(p.group(1))) {
                        faltas.add(r.get("id") + "." + clave + ": parametro :" + p.group(1) + " sin declarar");
                    }
                }
                Matcher t = TABLA.matcher(sql);
                while (t.find()) {
                    String referencia = t.group(1) + "." + t.group(2);
                    if (esEsquemaDeServicio(t.group(1)) && !tablas.contains(referencia)) {
                        faltas.add(r.get("id") + "." + clave + ": la tabla " + referencia + " no existe en sql/");
                    }
                }
            }
        }
        assertThat(faltas).isEmpty();
    }

    @Test
    @DisplayName("la alarma de cola detenida cubre el outbox de los quince servicios, ni uno menos")
    @SuppressWarnings("unchecked")
    void colaDeLosQuinceServicios() throws IOException {
        Set<String> enLaRegla =
                new TreeSet<>((List<String>) ((Map<String, Object>) config.get("outbox")).get("esquemas"));
        Set<String> servicios;
        try (Stream<Path> hijos = Files.list(raiz.resolve("servicios"))) {
            servicios = hijos.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString().replace('-', '_'))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        assertThat(enLaRegla).isEqualTo(servicios);
        Set<String> tablas = tablasDelEsquema();
        assertThat(enLaRegla.stream().filter(e -> !tablas.contains(e + ".evento_dominio")))
                .as("esquemas sin outbox")
                .isEmpty();
    }

    @Test
    @DisplayName("el aviso tiene la forma que notificaciones acepta: destinatarioId, evento y datos, y nada mas")
    @SuppressWarnings("unchecked")
    void formaDelAviso() throws IOException {
        Map<String, Object> contrato = new Yaml()
                .load(Files.readString(
                        raiz.resolve("servicios/notificaciones/src/main/resources/openapi/notificaciones.yaml")));
        Map<String, Object> esquemas =
                (Map<String, Object>) ((Map<String, Object>) contrato.get("components")).get("schemas");
        Map<String, Object> entrada = (Map<String, Object>) esquemas.get("EntradaNotificacion");

        assertThat((List<String>) entrada.get("required"))
                .as("el evaluador de alarmas arma exactamente esto (despliegue/alertas/evaluar_alarmas.py)")
                .containsExactlyInAnyOrder("destinatarioId", "evento", "datos");
        assertThat(entrada.get("additionalProperties")).isEqualTo(false);
    }

    private static boolean esEsquemaDeServicio(String candidato) {
        return Files.isDirectory(raiz.resolve("servicios").resolve(candidato.replace('_', '-')));
    }

    private Set<String> rolesSembrados() throws IOException {
        Map<String, Object> semilla =
                new Yaml().load(Files.readString(raiz.resolve("seeders/minimos/10-roles-y-permisos.json")));
        Set<String> roles = new HashSet<>();
        for (Object bloque : (List<?>) semilla.get("bloques")) {
            Map<?, ?> b = (Map<?, ?>) bloque;
            if ("rol".equals(b.get("tabla"))) {
                for (Object fila : (List<?>) b.get("filas")) {
                    roles.add(String.valueOf(((Map<?, ?>) fila).get("codigo")));
                }
            }
        }
        return roles;
    }

    /** schema.tabla de cada CREATE TABLE de sql/10_tablas y de la infraestructura de mensajeria. */
    private static Set<String> tablasDelEsquema() throws IOException {
        Pattern crear = Pattern.compile("CREATE TABLE IF NOT EXISTS ([a-z_]+\\.[a-z_]+)");
        Set<String> tablas = new HashSet<>();
        try (Stream<Path> sql = Files.walk(raiz.resolve("sql"))) {
            for (Path archivo : sql.filter(p -> p.toString().endsWith(".sql"))
                    .filter(p -> {
                        String ruta = p.toString().replace('\\', '/');
                        return ruta.contains("/10_tablas/") || ruta.contains("/15_infra/");
                    })
                    .toList()) {
                Matcher m = crear.matcher(Files.readString(archivo));
                while (m.find()) {
                    tablas.add(m.group(1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return tablas;
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
