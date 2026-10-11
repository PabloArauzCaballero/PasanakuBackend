package bo.aportaya.entregas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Hallazgo de H9.S1.M1 (PLAN H9: «la oferta expira antes del punto de cierre del beneficiario del turno») · la
 * vigencia de la oferta contra la fecha de cierre que informa `grupos`, en tres niveles. La regla es pura: no necesita base.
 */
class ReglasDeOfertaCierreTest {

    private static final Instant AHORA = Instant.parse("2026-10-09T12:00:00Z");
    private static final Duration MAXIMA = Duration.ofDays(7);
    private static final UUID VENDEDOR = UUID.fromString("b9000000-0000-4000-8000-000000000001");

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static HechosDeGrupos.Turno turnoConCierre(Instant cierre) {
        return new HechosDeGrupos.Turno(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                VENDEDOR,
                "PROGRAMADO",
                bob("6000.00"),
                cierre);
    }

    private static void publicarCon(Instant cierre, Instant vigenteHasta) {
        ReglasDeOferta.validarPublicacion(
                turnoConCierre(cierre), VENDEDOR, bob("5500.00"), bob("50.00"), AHORA, vigenteHasta, MAXIMA);
    }

    private static void rechazaCon(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo(codigo));
    }

    @Test
    @DisplayName("CORRECTO · una oferta que vence antes del cierre del turno se acepta")
    void vencerAntesDelCierreSeAcepta() {
        assertThatCode(() -> publicarCon(AHORA.plus(Duration.ofDays(5)), AHORA.plus(Duration.ofDays(3))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("LIMITE · un segundo antes del cierre vale; justo en el cierre ya no: «antes» es estrictamente antes")
    void elBordeDelCierre() {
        Instant cierre = AHORA.plus(Duration.ofDays(2));

        assertThatCode(() -> publicarCon(cierre, cierre.minusSeconds(1))).doesNotThrowAnyException();
        rechazaCon(() -> publicarCon(cierre, cierre), "AP-CU130-03");
        rechazaCon(() -> publicarCon(cierre, cierre.plusSeconds(1)), "AP-CU130-03");
    }

    @Test
    @DisplayName(
            "INVALIDO · rechaza una oferta que seguiria viva despues del cierre, aunque respete la vigencia maxima")
    void rechazaVigenciaPosteriorAlCierre() {
        // Vigencia de 5 dias (dentro de los 7 maximos) contra un cierre en 2 dias: el cierre manda.
        rechazaCon(() -> publicarCon(AHORA.plus(Duration.ofDays(2)), AHORA.plus(Duration.ofDays(5))), "AP-CU130-03");
    }

    @Test
    @DisplayName("INVALIDO · un turno cuyo cierre ya paso no se puede ofrecer")
    void rechazaTurnoYaCerrado() {
        rechazaCon(() -> publicarCon(AHORA.minus(Duration.ofHours(1)), AHORA.plus(Duration.ofDays(1))), "AP-CU130-03");
    }

    @Test
    @DisplayName("INVALIDO · si grupos no informa el cierre no se publica nada: no se supone un plazo (AP-CU130-08)")
    void rechazaSinCierreInformado() {
        rechazaCon(() -> publicarCon(null, AHORA.plus(Duration.ofDays(1))), "AP-CU130-08");
    }

    @Test
    @DisplayName("LIMITE · la vigencia maxima sigue rigiendo aunque el cierre este muy lejos")
    void laVigenciaMaximaSigueRigiendo() {
        rechazaCon(() -> publicarCon(AHORA.plus(Duration.ofDays(60)), AHORA.plus(Duration.ofDays(8))), "AP-CU130-03");
    }
}
