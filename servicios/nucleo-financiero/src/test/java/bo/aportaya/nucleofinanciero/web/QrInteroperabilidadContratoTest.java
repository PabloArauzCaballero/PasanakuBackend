package bo.aportaya.nucleofinanciero.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S1.M6 · LADO API: el contrato publicado de {@code nucleo-financiero} NUNCA afirma que un QR sea interoperable. Un QR
 * de Pasanaku es interno (se paga entre billeteras de Pasanaku); uno de otro sistema de pagos se rechaza como no aceptado
 * (AP-CU12-10) y no se procesa como interno. Lo que ve el cliente en esta capa son las formas del contrato: si algun dia
 * alguien agrega una clase, un campo o un texto que prometa interoperabilidad sin proveedor habilitado, esta prueba se
 * rompe. La prueba del lado del cliente web/movil (tipos, rutas y mensajes) es del frontend.
 */
class QrInteroperabilidadContratoTest {

    private static final Pattern NEGACION =
            Pattern.compile("(?i)\\bno\\b|NO_HABILITADO|sin un proveedor|se rechaza|no se afirma|no se procesa");

    private static String contrato() throws IOException {
        try (InputStream in =
                QrInteroperabilidadContratoTest.class.getResourceAsStream("/openapi/nucleo-financiero.yaml")) {
            assertThat(in).as("el contrato esta en el classpath del servicio").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r", "");
        }
    }

    private static String esquema(String yaml, String nombre) {
        int desde = yaml.indexOf("    " + nombre + ":\n");
        assertThat(desde).as("esquema " + nombre).isGreaterThanOrEqualTo(0);
        int hasta = yaml.indexOf("\n    ", desde + nombre.length() + 6);
        while (hasta > 0 && yaml.startsWith("\n      ", hasta)) {
            hasta = yaml.indexOf("\n    ", hasta + 1);
        }
        return yaml.substring(desde, hasta < 0 ? yaml.length() : hasta);
    }

    @Test
    @DisplayName(
            "contrato · una lectura de QR solo puede responder la clase INTERNO: no existe ninguna clase interoperable")
    void laLecturaSoloConoceLaClaseInterna() throws IOException {
        String leido = esquema(contrato(), "SalidaQrLeido");
        assertThat(leido).contains("clase: { type: string, enum: [INTERNO]");
        assertThat(leido).doesNotContainIgnoringCase("interoperable");
        assertThat(leido).doesNotContainIgnoringCase("bancario");
    }

    @Test
    @DisplayName(
            "contrato · ningun esquema del servicio tiene un campo ni un valor 'interoperable' (ni EMV, ni QR Simple)")
    void ningunEsquemaPrometeInteroperabilidad() throws IOException {
        String yaml = contrato();
        String esquemas = yaml.substring(yaml.indexOf("\ncomponents:"));
        List<String> infractores = new ArrayList<>();
        for (String linea : esquemas.split("\n")) {
            String l = linea.strip();
            boolean propiedadOValor = l.matches("(?i)^[\"']?[A-Za-z_]*interoperab[A-Za-z_]*[\"']?\\s*:.*")
                    || l.matches("(?i).*\\benum:\\s*\\[.*(interoperab|emv|qr_simple).*")
                    || l.matches("(?i)^-\\s*[\"']?[A-Z_]*(INTEROPERAB|EMV|QR_SIMPLE)[A-Z_]*[\"']?\\s*$");
            if (propiedadOValor) {
                infractores.add(l);
            }
        }
        assertThat(infractores)
                .as("campos o valores que prometen interoperabilidad")
                .isEmpty();
    }

    @Test
    @DisplayName(
            "contrato · toda mencion de 'interoperable' en las operaciones de QR es una NEGACION (no es, no se procesa, no habilitado)")
    void cadaMencionEsUnaNegacion() throws IOException {
        String yaml = contrato();
        int desde = yaml.indexOf("  /billetera/qr");
        int hasta = yaml.indexOf("\ncomponents:");
        assertThat(desde).isGreaterThan(0);
        String operaciones = yaml.substring(desde, hasta).replace("\r", "");
        List<String> afirmativas = new ArrayList<>();
        // Una "oracion" del contrato: de un punto (o inicio de bloque) al siguiente punto.
        for (String oracion : operaciones.split("(?<=\\.)\\s+|\\n\\s*\\n")) {
            if (oracion.toLowerCase().contains("interoperab")
                    && !NEGACION.matcher(oracion).find()) {
                afirmativas.add(oracion.strip());
            }
        }
        assertThat(afirmativas)
                .as("menciones de interoperabilidad que no son una negacion")
                .isEmpty();
        assertThat(operaciones).as("el rechazo documentado").contains("AP-CU12-10 QR_INTEROPERABLE_NO_HABILITADO");
    }

    @Test
    @DisplayName(
            "contrato · emitir un QR solo ofrece las modalidades internas (DINAMICO, ESTATICO), sin elegir el rail")
    void emitirNoElegiUnRail() throws IOException {
        String entrada = esquema(contrato(), "EntradaQr");
        assertThat(entrada).contains("modalidad: { type: string, enum: [DINAMICO, ESTATICO] }");
        assertThat(entrada).doesNotContainIgnoringCase("rail").doesNotContainIgnoringCase("proveedor");
    }
}
