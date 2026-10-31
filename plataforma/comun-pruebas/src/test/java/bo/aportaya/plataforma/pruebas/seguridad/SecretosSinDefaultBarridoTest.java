package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M3 · Ningun secreto arranca con un valor por omision (regla 90.3.4: «el servicio falla si
 * falta o es invalido; prohibido arrancar con un default silencioso para un secreto»).
 *
 * <p>Barre los {@code application*.yml} de los quince servicios y las librerias. Un
 * {@code ${SECRETO:}} o {@code ${SECRETO:algo}} deja arrancar el proceso sin el secreto y falla
 * recien al usarlo, cuando ya hay trafico. Los que existen hoy estan declarados abajo con su
 * razon y su duenio: la lista no crece sola, y una linea que ya no existe tambien rompe la
 * prueba, asi que bajar la deuda es borrar entradas.
 */
class SecretosSinDefaultBarridoTest {

    private static final Pattern SECRETO_CON_DEFAULT = Pattern.compile(
            "\\$\\{([A-Z0-9_]*(?:CLAVE|SECRET|PIMIENTA|TOKEN|PASSWORD|API_KEY|FIRMA|CREDENCIAL)[A-Z0-9_]*):[^}]*\\}");

    /** Variable -> por que tiene default hoy y quien lo arregla. Cada una es deuda, no aprobacion. */
    private static final Map<String, String> DEUDA_CONOCIDA = Map.ofEntries(
            Map.entry(
                    "JWT_CLAVE_FIRMA",
                    "identidad: sin ella genera una clave en memoria (valido con UNA replica; con dos, cada una firma distinto, ADR-037)"),
            Map.entry(
                    "JWT_CLAVE_FIRMA_ANTERIOR",
                    "identidad: OPCIONAL por diseno, no es un secreto que falte: vacia fuera de una rotacion; con ella cargada y sin la vigente el servicio NO arranca"),
            Map.entry(
                    "INVITACIONES_CLAVE",
                    "identidad: default vacio; la longitud minima (32) se valida recien al primer uso (SecretoDeInvitacion), no al arrancar"),
            Map.entry("GMAIL_CLIENT_SECRET", "identidad: el correo de verificacion queda deshabilitado sin ella"),
            Map.entry("GMAIL_REFRESH_TOKEN", "identidad: idem"),
            Map.entry(
                    "INVERSIONES_ALIADO_API_KEY",
                    "inversiones: el aliado esta deshabilitado (DR-INV-01); vacio = sin aliado"),
            Map.entry("INVERSIONES_ALIADO_FIRMA_SECRETO", "inversiones: idem"),
            Map.entry(
                    "INVERSIONES_LIBRO_CREDENCIAL_SERVICIO",
                    "inversiones: vacia = sin credencial de servicio hacia el libro (DR-INV-01)"),
            Map.entry(
                    "INVERSIONES_SERVICIO_CLAVE",
                    "inversiones: OPCIONAL por diseno: vacia = sin cuenta de servicio (el libro no se toca, falla cerrado); con telefono o factor y sin ella el servicio NO arranca (ConfiguracionDeCredencialDeServicio)"),
            Map.entry(
                    "ENTREGAS_LIBRO_CREDENCIAL_SERVICIO",
                    "entregas: vacia = sin credencial de servicio hacia el libro: pagarAlVendedor falla cerrado y la compra queda detenida y reanudable"),
            Map.entry(
                    "ENTREGAS_SERVICIO_CLAVE",
                    "entregas: OPCIONAL por diseno: vacia = sin cuenta de servicio (no se le paga al vendedor, falla cerrado); con telefono o factor y sin ella el servicio NO arranca (CredencialDeServicioConfigurada)"),
            Map.entry(
                    "GRUPOS_TRANSPARENCIA_CREDENCIAL_SERVICIO",
                    "grupos: vacia = sin token emitido hacia transparencia (solo prueba manual); sin cuenta de servicio los ingresos fallan cerrados (EtapasPorHttp)"),
            Map.entry(
                    "GRUPOS_SERVICIO_CLAVE",
                    "grupos: OPCIONAL por diseno: vacia = sin cuenta de servicio SERVICIO_GRUPOS (no se lee la etapa ajena, altas e ingresos fallan cerrados); con telefono o factor y sin ella el servicio NO arranca (CredencialDeServicioConfigurada)"));

    @Test
    @DisplayName(
            "barrido: ningun secreto nuevo arranca con valor por omision; la deuda conocida esta declarada y vigente")
    void ningunSecretoConDefault() {
        Path raiz = raiz();
        TreeSet<String> encontrados = new TreeSet<>();
        List<String> donde = new ArrayList<>();
        for (Path yml : configuraciones(raiz)) {
            try {
                String texto = Files.readString(yml);
                Matcher m = SECRETO_CON_DEFAULT.matcher(texto);
                while (m.find()) {
                    encontrados.add(m.group(1));
                    donde.add(m.group(1) + " en "
                            + raiz.relativize(yml).toString().replace('\\', '/'));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        assertThat(encontrados.stream().filter(v -> !DEUDA_CONOCIDA.containsKey(v)))
                .as("secreto con valor por omision que no esta declarado (arrancaria sin el): %s", donde)
                .isEmpty();
        assertThat(DEUDA_CONOCIDA.keySet().stream().filter(v -> !encontrados.contains(v)))
                .as("entradas de DEUDA_CONOCIDA que ya no existen en los application.yml: borrarlas")
                .isEmpty();
    }

    private static List<Path> configuraciones(Path raiz) {
        List<Path> archivos = new ArrayList<>();
        for (String carpeta : List.of("servicios", "plataforma")) {
            try (Stream<Path> arbol = Files.walk(raiz.resolve(carpeta))) {
                arbol.filter(p -> p.getFileName().toString().matches("application.*\\.yml"))
                        .filter(p -> p.toString().replace('\\', '/').contains("/src/main/resources/"))
                        .sorted()
                        .forEach(archivos::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return archivos;
    }

    private static Path raiz() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("servicios"))) {
            actual = actual.getParent();
        }
        return actual;
    }
}
