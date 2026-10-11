package bo.aportaya.cumplimiento.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.dominio.EfectoDelReclamo.Caso;
import bo.aportaya.cumplimiento.dominio.EfectoDelReclamo.Motivo;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M6 · El contrato de {@link EfectoDelReclamo}, en los tres niveles (regla 65): lo
 * correcto, los bordes del plazo y lo invalido. Es la politica pura: sin base, sin reloj del
 * sistema. Lo que hace con una base real lo prueba {@code CU52EfectoDelReclamoTest}.
 */
class EfectoDelReclamoTest {

    private static final int DIAS_PARA_ELEVAR = 30;
    private static final OffsetDateTime RESPUESTA = OffsetDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static Caso caso(
            String estado, String resultado, OffsetDateTime respuesta, boolean sinResolver, boolean resarcida) {
        return new Caso(estado, resultado, respuesta, sinResolver, resarcida);
    }

    private static EfectoDelReclamo efecto(Caso caso, OffsetDateTime ahora) {
        return EfectoDelReclamo.de(caso, ahora, DIAS_PARA_ELEVAR);
    }

    // ------------------------------------------------------------------ correcto

    @Test
    @DisplayName("correcto: un reclamo en revision NO es sancion firme ni deuda ejecutable: frena las dos")
    void enRevisionFrenaLasDos() {
        for (String estado : new String[] {"INGRESADO", "EN_ANALISIS", "RESPONDIDO"}) {
            var e = efecto(caso(estado, null, null, false, false), RESPUESTA);

            assertThat(e.bloqueaSancionFirme()).as(estado).isTrue();
            assertThat(e.bloqueaDeudaEjecutable()).as(estado).isTrue();
            assertThat(e.motivo()).isEqualTo(Motivo.EN_REVISION);
        }
    }

    @Test
    @DisplayName("correcto: elevado a otra instancia frena mientras la instancia no resuelva")
    void elevadoSinResolverFrena() {
        var e = efecto(caso("ELEVADO", "DESFAVORABLE", RESPUESTA, true, false), RESPUESTA.plusDays(400));

        assertThat(e.bloqueaSancionFirme()).isTrue();
        assertThat(e.bloqueaDeudaEjecutable()).isTrue();
        assertThat(e.motivo()).isEqualTo(Motivo.INSTANCIA_SIN_RESOLVER);
    }

    @Test
    @DisplayName("correcto: la instancia resolvio sin resarcimiento y ya no queda nada abierto: no frena")
    void elevadoYResueltoNoFrena() {
        var e = efecto(caso("ELEVADO", "DESFAVORABLE", RESPUESTA, false, false), RESPUESTA.plusDays(400));

        assertThat(e.bloqueaSancionFirme()).isFalse();
        assertThat(e.bloqueaDeudaEjecutable()).isFalse();
    }

    @Test
    @DisplayName("correcto: cerrado a favor del cliente frena para siempre: hay una reparacion de por medio")
    void favorableFrena() {
        for (String resultado : new String[] {"FAVORABLE", "PARCIAL"}) {
            var e = efecto(caso("CERRADO", resultado, RESPUESTA, false, false), RESPUESTA.plusDays(1000));

            assertThat(e.bloqueaDeudaEjecutable()).as(resultado).isTrue();
            assertThat(e.motivo()).isEqualTo(Motivo.FAVORABLE_AL_CLIENTE);
        }
    }

    @Test
    @DisplayName("correcto: si el cliente desistio, el reclamo no frena nada")
    void desistidoNoFrena() {
        var e = efecto(caso("CERRADO", "DESISTIDO", RESPUESTA, false, false), RESPUESTA);

        assertThat(e.bloqueaSancionFirme()).isFalse();
        assertThat(e.bloqueaDeudaEjecutable()).isFalse();
    }

    // -------------------------------------------------------------------- limite

    @Test
    @DisplayName("limite: cerrado en contra frena hasta el ULTIMO instante del plazo de elevacion y ni un segundo mas")
    void elBordeDelPlazoDeElevacion() {
        OffsetDateTime ultimo = RESPUESTA.plusDays(DIAS_PARA_ELEVAR);

        var dentro = efecto(caso("CERRADO", "DESFAVORABLE", RESPUESTA, false, false), ultimo);
        var fuera = efecto(caso("CERRADO", "DESFAVORABLE", RESPUESTA, false, false), ultimo.plusSeconds(1));

        assertThat(dentro.bloqueaDeudaEjecutable())
                .as("en el ultimo instante todavia puede elevar")
                .isTrue();
        assertThat(dentro.motivo()).isEqualTo(Motivo.PLAZO_DE_ELEVACION_ABIERTO);
        assertThat(fuera.bloqueaDeudaEjecutable())
                .as("un segundo despues, ya no")
                .isFalse();
        assertThat(fuera.bloqueaSancionFirme()).isFalse();
    }

    @Test
    @DisplayName(
            "limite: un resarcimiento dispuesto por la instancia frena aunque el reclamo figure como elevado y viejo")
    void resarcimientoFrenaSiempre() {
        var e = efecto(caso("ELEVADO", "DESFAVORABLE", RESPUESTA, false, true), RESPUESTA.plusDays(3650));

        assertThat(e.bloqueaDeudaEjecutable()).isTrue();
        assertThat(e.motivo()).isEqualTo(Motivo.RESARCIMIENTO_DISPUESTO);
    }

    @Test
    @DisplayName("limite: una persona sin reclamos no tiene nada que frene")
    void sinReclamos() {
        var e = EfectoDelReclamo.sinReclamos();

        assertThat(e.bloqueaSancionFirme()).isFalse();
        assertThat(e.bloqueaDeudaEjecutable()).isFalse();
        assertThat(e.motivo()).isEqualTo(Motivo.SIN_RECLAMOS);
    }

    // ------------------------------------------------------------------ invalido

    @Test
    @DisplayName("invalido: lo que no se puede interpretar FRENA (denegar por omision), nunca deja pasar")
    void loInvalidoFrena() {
        var casos = new Caso[] {
            caso(null, null, null, false, false),
            caso("INVENTADO", null, null, false, false),
            caso("CERRADO", null, RESPUESTA, false, false),
            caso("CERRADO", "RARO", RESPUESTA, false, false),
            caso("CERRADO", "DESFAVORABLE", null, false, false),
        };

        for (Caso c : casos) {
            var e = efecto(c, RESPUESTA.plusDays(5000));

            assertThat(e.bloqueaSancionFirme()).as(c.toString()).isTrue();
            assertThat(e.bloqueaDeudaEjecutable()).as(c.toString()).isTrue();
            assertThat(e.motivo()).as(c.toString()).isEqualTo(Motivo.ESTADO_NO_RECONOCIDO);
        }
    }
}
