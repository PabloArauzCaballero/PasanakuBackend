package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.transparencia.dominio.ContenidoCanonico;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La forma canonica de un instante, que es lo que queda FIRMADO en cada bloque.
 *
 * <p>No necesita base ni contexto: son cuatro cadenas. Vive aparte porque lo que fija no
 * es un caso de uso sino la condicion que hace verificable a la cadena entera — que dos
 * implementaciones honestas, en dos maquinas distintas, obtengan el mismo hash del mismo
 * hecho.
 */
class ContenidoCanonicoTest {

    @Test
    @DisplayName("el instante canonico no lleva nanosegundos: es lo unico que la base puede devolver")
    void sinNanosegundos() {
        // Un bloque se sella con el instante que le pasan y se guarda en un TIMESTAMPTZ,
        // que redondea a microsegundos. Si el hash se calculara sobre los nanosegundos
        // originales, quedaria firmado un valor que la base ya no tiene y recomputar el
        // hash desde lo guardado —lo que promete CU-73— daria distinto: la cadena se
        // declararia alterada sin que nadie la tocara.
        OffsetDateTime conNanos = OffsetDateTime.parse("2026-03-06T12:00:00.123456789Z");
        OffsetDateTime enMicros = OffsetDateTime.parse("2026-03-06T12:00:00.123456Z");

        assertThat(ContenidoCanonico.instante(conNanos)).isEqualTo(ContenidoCanonico.instante(enMicros));
        assertThat(ContenidoCanonico.instante(conNanos)).isEqualTo("2026-03-06T12:00:00.123456Z");
    }

    @Test
    @DisplayName("el huso de quien firma no entra al hash: el mismo instante en dos husos da la misma cadena")
    void sinHuso() {
        OffsetDateTime enLaPaz = OffsetDateTime.parse("2026-03-06T08:00:00-04:00");
        OffsetDateTime enUtc = enLaPaz.withOffsetSameInstant(ZoneOffset.UTC);

        assertThat(ContenidoCanonico.instante(enLaPaz)).isEqualTo(ContenidoCanonico.instante(enUtc));
        assertThat(ContenidoCanonico.instante(enLaPaz)).isEqualTo("2026-03-06T12:00:00Z");
    }
}
