package bo.aportaya.notificaciones.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CargaFirmadaTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant EMITIDO = Instant.parse("2026-11-21T12:00:00Z");
    private static final UUID NOTIFICACION = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void conservaLosCamposDeLaCargaIncluidaLaNotificacionRelacionada() {
        var carga = cargaValida().put("notificacionRelacionadaId", NOTIFICACION.toString());

        assertThat(CargaFirmada.leer(carga.toString()))
                .contains(new CargaFirmada(
                        "mensaje-1", "SMS", "+59170000001", "Consulta", EMITIDO, Optional.of(NOTIFICACION)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"[]", "{"})
    void rechazaUnaCargaQueNoEsUnObjetoJson(String carga) {
        assertThat(CargaFirmada.leer(carga)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"idMensaje", "canal", "remitente", "contenido", "emitidoEn"})
    void rechazaUnaCargaCuandoFaltaUnCampoObligatorio(String campo) {
        var carga = cargaValida();
        carga.remove(campo);

        assertThat(CargaFirmada.leer(carga.toString())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "7", "\" \""})
    void rechazaUnCampoObligatorioQueNoContieneTexto(String valorJson) throws JsonProcessingException {
        var carga = cargaValida();
        carga.set("contenido", JSON.readTree(valorJson));

        assertThat(CargaFirmada.leer(carga.toString())).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"emitidoEn,ayer", "notificacionRelacionadaId,no-es-un-uuid"})
    void rechazaValoresConFormatoInvalido(String campo, String valor) {
        var carga = cargaValida().put(campo, valor);

        assertThat(CargaFirmada.leer(carga.toString())).isEmpty();
    }

    @Test
    void aceptaLosExtremosDeLaVentanaYRechazaLoQueQuedaFuera() {
        var carga = CargaFirmada.leer(cargaValida().toString()).orElseThrow();
        var ventana = Duration.ofMinutes(5);

        assertThat(carga.dentroDeVentana(EMITIDO.plus(ventana), ventana)).isTrue();
        assertThat(carga.dentroDeVentana(EMITIDO.minus(ventana), ventana)).isTrue();
        assertThat(carga.dentroDeVentana(EMITIDO.plus(ventana).plusSeconds(1), ventana))
                .isFalse();
        assertThat(carga.dentroDeVentana(EMITIDO.minus(ventana).minusSeconds(1), ventana))
                .isFalse();
    }

    private static ObjectNode cargaValida() {
        return JSON.createObjectNode()
                .put("idMensaje", "mensaje-1")
                .put("canal", "SMS")
                .put("remitente", "+59170000001")
                .put("contenido", "Consulta")
                .put("emitidoEn", EMITIDO.toString());
    }
}
