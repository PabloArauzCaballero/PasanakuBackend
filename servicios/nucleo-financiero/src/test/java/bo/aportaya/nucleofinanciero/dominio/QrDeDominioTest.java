package bo.aportaya.nucleofinanciero.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.dominio.ClasificadorDeQr.Clase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H4.S1.M5 / M6 · Qué es un texto de QR, y que solo uno firmado por nosotros valga como interno. */
class QrDeDominioTest {

    private static final String CLAVE = "clave-de-prueba-de-los-qr-internos-0001";
    private final ContenidoDeQr contenido = new ContenidoDeQr(CLAVE);

    @Test
    @DisplayName("clasifica: interno por su prefijo, bancario por el indicador de formato EMV, el resto desconocido")
    void clasifica() {
        assertThat(ClasificadorDeQr.de("PSNK1.abc")).isEqualTo(Clase.INTERNO);
        assertThat(ClasificadorDeQr.de("  PSNK1.abc  ")).isEqualTo(Clase.INTERNO);
        assertThat(ClasificadorDeQr.de("00020101021226280012ejemplo")).isEqualTo(Clase.INTEROPERABLE_BANCARIO);
        for (String otro :
                new String[] {null, "", "   ", "hola", "psnk1.abc", "https://example.com", "0002", "x".repeat(3000)}) {
            assertThat(ClasificadorDeQr.de(otro))
                    .as("«%s»", otro == null ? "null" : otro.substring(0, Math.min(10, otro.length())))
                    .isEqualTo(Clase.DESCONOCIDO);
        }
    }

    @Test
    @DisplayName("un QR firmado se verifica y devuelve su identificador; la firma depende de la clave")
    void firmaYVerifica() {
        UUID id = UUID.randomUUID();
        String texto = contenido.de(id);
        assertThat(texto).startsWith("PSNK1.").hasSize(6 + 22 + 1 + 22);
        assertThat(contenido.verificar(texto)).contains(id);
        assertThat(contenido.verificar("  " + texto + "\n")).contains(id);
        assertThat(new ContenidoDeQr("otra-clave-de-prueba-distinta-de-la-primera-0002").verificar(texto))
                .isEmpty();
    }

    @Test
    @DisplayName("lo alterado, truncado o mal codificado no verifica")
    void loAlteradoNoVerifica() {
        String texto = contenido.de(UUID.randomUUID());
        String[] partes = texto.split("\\.");
        for (String malo : new String[] {
            null,
            "",
            "PSNK1.",
            texto.substring(0, texto.length() - 1),
            texto + "A",
            "PSNK1." + partes[1] + ".",
            "PSNK1." + partes[1].substring(1) + "A." + partes[2],
            "PSNK1." + partes[1] + "." + "A".repeat(22),
            "PSNK1." + "!".repeat(22) + "." + partes[2],
            "PSNK1." + partes[1] + "." + partes[2] + ".extra",
        }) {
            assertThat(contenido.verificar(malo)).as("«%s»", malo).isEmpty();
        }
    }

    @Test
    @DisplayName("una codificacion no canonica del mismo identificador no valida como el mismo QR")
    void noCanonica() {
        UUID id = new UUID(0L, 0L);
        String canonico = contenido.de(id);
        String[] partes = canonico.split("\\.");
        // El ultimo caracter de 22 lleva solo 4 bits utiles: 'A' y 'B' decodifican igual el id.
        String variante = "PSNK1." + partes[1].substring(0, 21) + "B." + partes[2];
        assertThat(contenido.verificar(canonico)).contains(id);
        assertThat(contenido.verificar(variante)).isEmpty();
    }

    @Test
    @DisplayName("la clave de los QR no puede ser corta ni vacia: no hay valor por omision")
    void claveObligatoria() {
        for (String corta : new String[] {null, "", "corta", "x".repeat(31)}) {
            assertThatThrownBy(() -> new ContenidoDeQr(corta)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
