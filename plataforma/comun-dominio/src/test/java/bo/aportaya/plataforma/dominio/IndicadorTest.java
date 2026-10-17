package bo.aportaya.plataforma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Motivo;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndicadorTest {

    private static final Definicion CARGADO = new Definicion("SALDO_CARGADO", Unidad.DINERO, 0, false);
    private static final Definicion TASA_MORA = new Definicion("TASA_MORA", Unidad.PORCENTAJE, 20, false);
    private static final Definicion REVERSOS = new Definicion("REVERSOS", Unidad.CONTEO, 0, true);
    private static final Definicion COBERTURA = new Definicion("COBERTURA_FONDO", Unidad.RAZON, 0, false);
    private static final Definicion DIAS = new Definicion("DIAS_PAGO", Unidad.DIAS, 0, false);

    @Test
    @DisplayName("correcto: un importe se publica con su moneda, su muestra y la variación contra el período anterior")
    void dinero() {
        var i = Indicador.publicar(
                CARGADO,
                Medicion.de(Dinero.de("1500.00", Moneda.BOB), 3),
                Medicion.de(Dinero.de("900.00", Moneda.BOB), 2));

        assertThat(i.valor()).isEqualByComparingTo("1500.00");
        assertThat(i.motivo()).isNull();
        assertThat(i.moneda()).isEqualTo(Moneda.BOB);
        assertThat(i.muestra()).isEqualTo(3);
        assertThat(i.valorAnterior()).isEqualByComparingTo("900.00");
        assertThat(i.motivoAnterior()).isNull();
    }

    @Test
    @DisplayName("correcto: una tasa viaja con numerador y denominador; porcentaje a 2 decimales, razón a 4")
    void tasas() {
        var mora = Indicador.publicar(TASA_MORA, Medicion.tasa(7, 30), null);
        assertThat(mora.valor()).isEqualByComparingTo("23.33");
        assertThat(mora.numerador()).isEqualByComparingTo("7");
        assertThat(mora.denominador()).isEqualByComparingTo("30");

        var razon =
                Indicador.publicar(COBERTURA, new Medicion(new BigDecimal("1000"), new BigDecimal("3"), null, 3), null);
        assertThat(razon.valor()).isEqualByComparingTo("333.3333");
    }

    @Test
    @DisplayName("límite: sin casos el valor es null con SIN_DATOS, nunca 0; sin período anterior, SIN_HISTORIA")
    void nullNoEsCero() {
        var i = Indicador.publicar(DIAS, Medicion.conteo(0), null);

        assertThat(i.valor()).isNull();
        assertThat(i.motivo()).isEqualTo(Motivo.SIN_DATOS);
        assertThat(i.valorAnterior()).isNull();
        assertThat(i.motivoAnterior()).isEqualTo(Motivo.SIN_HISTORIA);
    }

    @Test
    @DisplayName("límite: un indicador que cuenta lo que no debería pasar publica su cero (ceroEsReal)")
    void ceroReal() {
        assertThat(Indicador.publicar(REVERSOS, Medicion.conteo(0), null).valor())
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("límite: debajo de la muestra mínima se retiene el valor con los conteos intactos")
    void muestraMinima() {
        var i = Indicador.publicar(TASA_MORA, Medicion.tasa(3, 10), Medicion.tasa(5, 25));

        assertThat(i.valor()).isNull();
        assertThat(i.motivo()).isEqualTo(Motivo.MUESTRA_INSUFICIENTE);
        assertThat(i.numerador()).isEqualByComparingTo("3");
        assertThat(i.denominador()).isEqualByComparingTo("10");
        assertThat(i.valorAnterior()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("límite: una tasa con denominador cero no divide: SIN_DATOS")
    void denominadorCero() {
        var sinMinimo = new Definicion("T", Unidad.PORCENTAJE, 0, true);
        var i = Indicador.publicar(sinMinimo, new Medicion(BigDecimal.ONE, BigDecimal.ZERO, null, 1), null);

        assertThat(i.valor()).isNull();
        assertThat(i.motivo()).isEqualTo(Motivo.SIN_DATOS);
    }

    @Test
    @DisplayName("límite: un indicador todavía no calculable se publica con su motivo")
    void sinValor() {
        var i = Indicador.sinValor(CARGADO, Motivo.NO_IMPLEMENTADO);

        assertThat(i.valor()).isNull();
        assertThat(i.motivo()).isEqualTo(Motivo.NO_IMPLEMENTADO);
        assertThat(i.motivoAnterior()).isEqualTo(Motivo.NO_IMPLEMENTADO);
        assertThat(i.codigo()).isEqualTo("SALDO_CARGADO");
    }

    @Test
    @DisplayName("inválido: un importe sin moneda o una tasa sin denominador no se publican")
    void rechazos() {
        assertThatThrownBy(() -> Indicador.publicar(CARGADO, Medicion.conteo(5), null))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("moneda");
        assertThatThrownBy(() -> Indicador.publicar(TASA_MORA, Medicion.conteo(5), null))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("denominador");
        assertThatThrownBy(() -> new Definicion("X", Unidad.CONTEO, -1, false)).isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> new Medicion(BigDecimal.ONE, null, null, -1)).isInstanceOf(ErrorDeDominio.class);
    }
}
