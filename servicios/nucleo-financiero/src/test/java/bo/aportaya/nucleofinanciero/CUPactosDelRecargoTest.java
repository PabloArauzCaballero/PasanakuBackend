package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H8.S3.M1 — lo que nucleo-financiero publica para garantia cumple sus pactos: {@code aporte_aplicado}
 * (con {@code destino}) y {@code transaccion_reversada}. El consumidor verifica el mismo archivo en garantia.
 */
class CUPactosDelRecargoTest extends BaseDeRecargo {

    @Test
    @DisplayName(
            "pacto con garantia · productor: aplicar un recargo publica aporte_aplicado con TODOS los campos del pacto"
                    + " y destino FONDO_GARANTIA")
    void cumpleElPactoConGarantia() throws IOException {
        JsonNode pacto = pacto("contratos/pactos/nucleo-financiero-garantia-aporte-aplicado.json");
        fondo();
        var aplicado = procesar(recargo(aporte("100.00"), "25.00"));

        JsonNode emitido = emitido(
                pacto,
                """
                SELECT payload::text FROM nucleo_financiero.evento_dominio
                 WHERE tipo = ? AND payload->>'transaccionId' = ?
                """,
                aplicado.transaccionId().orElseThrow());

        traeTodosLosCampos(pacto, emitido);
        assertThat(emitido.path("monto").asText()).matches("^[0-9]+[.][0-9]{2}$");
        assertThat(emitido.path("destino").asText())
                .isEqualTo(pacto.path("ejemplo").path("destino").asText());
    }

    @Test
    @DisplayName(
            "pacto con garantia · productor: reversar un recargo publica transaccion_reversada con TODOS los campos del"
                    + " pacto, la original y el reverso")
    void cumpleElPactoDeReverso() throws IOException {
        JsonNode pacto = pacto("contratos/pactos/nucleo-financiero-garantia-transaccion-reversada.json");
        var a = aporte("100.00");
        fondo();
        UUID original = procesar(recargo(a, "25.00")).transaccionId().orElseThrow();
        var salida = transaccion.execute(x -> reversoCU.ejecutar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso(
                        "rev-pacto-" + original,
                        original,
                        pacto.path("ejemplo").path("tipo").asText(),
                        "Prueba del pacto con garantia",
                        fixtura.usuario()),
                contextoDe(a.usuario())));

        JsonNode emitido = emitido(
                pacto,
                """
                SELECT payload::text FROM nucleo_financiero.evento_dominio
                 WHERE tipo = ? AND payload->>'transaccionOriginalId' = ?
                """,
                original);

        traeTodosLosCampos(pacto, emitido);
        assertThat(emitido.path("transaccionReversoId").asText())
                .isEqualTo(salida.transaccionReversoId().toString());
        assertThat(pacto.path("agregado").asText()).isEqualTo("reverso_transaccion");
    }

    private JsonNode emitido(JsonNode pacto, String consulta, UUID valor) throws IOException {
        String payload = dsl.fetchOne(consulta, pacto.path("tipo").asText(), valor.toString())
                .get(0, String.class);
        return new ObjectMapper().readTree(payload);
    }

    private static void traeTodosLosCampos(JsonNode pacto, JsonNode emitido) {
        var campos = new ArrayList<String>();
        pacto.path("campos").fieldNames().forEachRemaining(campos::add);
        for (String campo : campos) {
            assertThat(emitido.has(campo)).as("el evento trae " + campo).isTrue();
        }
    }

    private static JsonNode pacto(String archivo) throws IOException {
        Path raiz = Path.of("").toAbsolutePath();
        while (raiz != null && !Files.isRegularFile(raiz.resolve(archivo))) {
            raiz = raiz.getParent();
        }
        return new ObjectMapper().readTree(Files.readString(raiz.resolve(archivo), StandardCharsets.UTF_8));
    }
}
