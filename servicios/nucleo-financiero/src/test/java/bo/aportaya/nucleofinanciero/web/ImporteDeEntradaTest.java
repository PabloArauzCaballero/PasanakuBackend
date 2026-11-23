package bo.aportaya.nucleofinanciero.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * F1.L1.M01 · ningun cuerpo de entrada de nucleo-financiero lleva un importe con signo: los importes que entran son
 * {@code Importe} (patron sin signo), y {@code Dinero} queda para las salidas. Asi el negativo se rechaza en el
 * borde con el campo (400 AP-VAL-01) en TODAS las operaciones, no solo en las que tienen prueba web.
 */
class ImporteDeEntradaTest {

    private static final Path CONTRATO = Path.of("src/main/resources/openapi/nucleo-financiero.yaml");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @Test
    @DisplayName("ningun cuerpo de entrada referencia Dinero; todo importe que entra es Importe")
    @SuppressWarnings("unchecked")
    void entradasSinSigno() throws IOException {
        Map<String, Object> contrato = new Yaml().load(Files.readString(CONTRATO));
        Map<String, Object> esquemas = mapa(mapa(contrato.get("components")).get("schemas"));
        List<String> conSigno = new ArrayList<>();
        List<String> importes = new ArrayList<>();
        mapa(contrato.get("paths"))
                .forEach((ruta, operaciones) -> mapa(operaciones).forEach((metodo, op) -> {
                    Object esquema = mapa(mapa(mapa(mapa(op).get("requestBody")).get("content"))
                                    .get("application/json"))
                            .get("schema");
                    Object ref = mapa(esquema).get("$ref");
                    if (ref == null) {
                        return;
                    }
                    String nombre = ref.toString().substring(ref.toString().lastIndexOf('/') + 1);
                    mapa(mapa(esquemas.get(nombre)).get("properties")).forEach((campo, def) -> {
                        String texto = String.valueOf(def);
                        if (texto.contains("schemas/Dinero")) {
                            conSigno.add(mapa(op).get("operationId") + "." + campo);
                        }
                        if (texto.contains("schemas/Importe")) {
                            importes.add(mapa(op).get("operationId") + "." + campo);
                        }
                    });
                }));

        assertThat(conSigno).as("entradas con un importe que admite signo").isEmpty();
        // Las once operaciones de dinero que el contrato expone hoy: si una desaparece, que se vea.
        assertThat(importes)
                .contains(
                        "solicitarRecarga.monto",
                        "solicitarRetiro.monto",
                        "transferirSaldo.monto",
                        "retenerSaldo.monto",
                        "bloquearPorAutoridad.montoBloqueado",
                        "emitirQr.monto",
                        "pagarQr.monto",
                        "cotizarOperacion.monto",
                        "debitarInversion.monto",
                        "acreditarRescate.monto",
                        "conciliarCustodia.saldoCustodia");
        assertThat((Map<String, Object>) esquemas.get("Importe")).isNotNull();
        assertThat(String.valueOf(esquemas.get("Importe"))).doesNotContain("-?");
    }
}
