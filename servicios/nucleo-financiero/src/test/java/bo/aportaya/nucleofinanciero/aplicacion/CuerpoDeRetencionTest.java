package bo.aportaya.nucleofinanciero.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lo que se guarda para repetir la respuesta de una retencion: id y vigencia, sin releer la tabla de retenciones. */
class CuerpoDeRetencionTest {

    private static final UUID RETENCION = UUID.fromString("11111111-2222-4333-8444-555555555555");

    @Test
    @DisplayName("ida y vuelta: el id y la vigencia que se guardan son los que se leen")
    void idaYVuelta() {
        var expira = OffsetDateTime.of(2026, 12, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        var leido = CuerpoDeRetencion.leer(CuerpoDeRetencion.escribir(RETENCION, expira));

        assertThat(leido.retencionId()).isEqualTo(RETENCION);
        assertThat(leido.expiraEn()).isEqualTo(expira);
    }

    @Test
    @DisplayName("limite: una retencion sin vencimiento se guarda y se lee sin vencimiento")
    void sinVencimiento() {
        var leido = CuerpoDeRetencion.leer(CuerpoDeRetencion.escribir(RETENCION, null));

        assertThat(leido.retencionId()).isEqualTo(RETENCION);
        assertThat(leido.expiraEn()).isNull();
    }

    @Test
    @DisplayName("compatibilidad: una respuesta guardada antes de este cambio (solo el id) se sigue leyendo")
    void respuestaAntigua() {
        var leido = CuerpoDeRetencion.leer("{\"retencionId\":\"" + RETENCION + "\"}");

        assertThat(leido.retencionId()).isEqualTo(RETENCION);
        assertThat(leido.expiraEn()).isNull();
    }

    @Test
    @DisplayName("rechaza una respuesta guardada ilegible: no se devuelve una retencion inventada")
    void rechazaLoIlegible() {
        assertThatThrownBy(() -> CuerpoDeRetencion.leer("{\"retencionId\":\"no-es-un-uuid\"}"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CuerpoDeRetencion.leer("esto no es json")).isInstanceOf(IllegalStateException.class);
    }
}
