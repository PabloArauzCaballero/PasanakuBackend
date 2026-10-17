package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicadorDeIndicadoresTest {

    private static final UUID CORRELACION = UUID.randomUUID();

    private static Indicador cargado(String monto, long casos) {
        return Indicador.publicar(
                new Definicion("SALDO_CARGADO", Unidad.DINERO, 0, false),
                Medicion.de(Dinero.de(monto, Moneda.BOB), casos),
                null);
    }

    @Test
    @DisplayName(
            "correcto: un indicador con valor se emite como <modulo>.indicador_calculado, con la moneda como unidad")
    void emite() {
        var e = PublicadorDeIndicadores.evento(
                        "nucleo_financiero", cargado("1500.00", 3), "Saldo cargado", "2026-10", true, CORRELACION)
                .orElseThrow();

        assertThat(e.tipo()).isEqualTo("nucleo_financiero.indicador_calculado");
        assertThat(e.carga())
                .containsEntry("codigo", "SALDO_CARGADO")
                .containsEntry("valor", "1500.00")
                .containsEntry("unidad", "BOB")
                .containsEntry("periodo", "2026-10")
                .containsEntry("casos", 3L)
                .containsEntry("provisorio", true)
                .containsEntry("dimension", "GLOBAL");
    }

    @Test
    @DisplayName("límite: el mismo código y período dan el mismo agregado (un recálculo pisa, no duplica)")
    void agregadoDeterminista() {
        var a = PublicadorDeIndicadores.evento("x_y", cargado("1.00", 1), "N", "2026", false, CORRELACION)
                .orElseThrow();
        var b = PublicadorDeIndicadores.evento("x_y", cargado("9.00", 4), "N", "2026", false, CORRELACION)
                .orElseThrow();
        var otroPeriodo = PublicadorDeIndicadores.evento("x_y", cargado("9.00", 4), "N", "2025", false, CORRELACION)
                .orElseThrow();

        assertThat(a.agregadoId()).isEqualTo(b.agregadoId()).isNotEqualTo(otroPeriodo.agregadoId());
    }

    @Test
    @DisplayName("límite: sin datos no se emite nada — nunca un cero")
    void sinDatosNoEmite() {
        assertThat(PublicadorDeIndicadores.evento("x_y", cargado("0.00", 0), "N", "2026-10-15", true, CORRELACION))
                .isEmpty();
    }

    @Test
    @DisplayName("límite: cada unidad tiene su nombre de 15 caracteres o menos")
    void unidades() {
        for (Unidad u : Unidad.values()) {
            var d = new Definicion("X", u, 0, true);
            var m = u == Unidad.DINERO
                    ? Medicion.de(Dinero.de("1.00", Moneda.USD), 1)
                    : u == Unidad.PORCENTAJE || u == Unidad.RAZON ? Medicion.tasa(1, 2) : Medicion.conteo(1);
            assertThat(PublicadorDeIndicadores.unidad(Indicador.publicar(d, m, null)))
                    .hasSizeLessThanOrEqualTo(15);
        }
    }

    @Test
    @DisplayName("inválido: período fuera de AAAA[-MM[-DD]] o nombre vacío/largo se rechazan")
    void rechazos() {
        assertThatThrownBy(() ->
                        PublicadorDeIndicadores.evento("x_y", cargado("1.00", 1), "N", "2026/10", true, CORRELACION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> PublicadorDeIndicadores.evento("x_y", cargado("1.00", 1), "", "2026", true, CORRELACION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PublicadorDeIndicadores.evento(
                        "x_y", cargado("1.00", 1), "n".repeat(81), "2026", true, CORRELACION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        PublicadorDeIndicadores.evento("x_y", cargado("1.00", 1), null, "2026", true, CORRELACION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> PublicadorDeIndicadores.evento("x_y", cargado("1.00", 1), "N", null, true, CORRELACION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
