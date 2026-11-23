package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.HechosDeGrupos;
import bo.aportaya.aportes.aplicacion.PertenenciaDelParticipante;
import bo.aportaya.aportes.infraestructura.EstadoDelParticipanteRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L1.M07 (ANF-SEG-002, BOLA) contra la base: la pertenencia de un participante y de un pago se resuelve
 * con lo que guarda este esquema y lo que contesta {@code grupos}. {@code grupos} es un DOBLE declarado
 * (regla 65) en tres niveles: dice que es del usuario, dice que no, y no contesta.
 */
class AutorizacionNegativaTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static HechosDeGrupos grupos(Optional<Boolean> esDelUsuario) {
        return (participanteId, periodoId) -> esDelUsuario.map(e -> new HechosDeGrupos.Admisibilidad(e, true));
    }

    private PertenenciaDelParticipante pertenencia(Optional<Boolean> esDelUsuario) {
        return new PertenenciaDelParticipante(
                new Datos(dsl),
                transaccion.getTransactionManager(),
                new EstadoDelParticipanteRepositorio(),
                new ObligacionRepositorio(),
                new PagoRepositorio(),
                grupos(esDelUsuario));
    }

    private ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario,
                "PARTICIPANTE",
                new bo.aportaya.plataforma.dominio.Traza(UUID.randomUUID().toString()));
    }

    private UUID pagoDe(UUID usuario, FixturaDeAportes.Obligacion obligacion) {
        return transaccion
                .execute(t -> cobroCU.acreditar(
                        new EntradaCobro(
                                "cob-" + UUID.randomUUID(),
                                obligacion.id(),
                                Dinero.de("100.00", Moneda.BOB),
                                Dinero.de("0.00", Moneda.BOB),
                                "BILLETERA_MOVIL",
                                "ref-" + UUID.randomUUID(),
                                Optional.empty(),
                                false,
                                true),
                        ctx(usuario)))
                .pagoId();
    }

    @Test
    @DisplayName("correcto: grupos dice que el participante y el pago son de la sesion")
    void esDelUsuario() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        UUID pago = pagoDe(usuario, obligacion);
        var p = pertenencia(Optional.of(true));
        assertThat(p.participanteEsDelUsuario(obligacion.participanteId(), ctx(usuario)))
                .isTrue();
        assertThat(p.pagoEsDelUsuario(pago, ctx(usuario))).isTrue();
    }

    @Test
    @DisplayName("invalido: otro usuario u otro grupo, grupos dice que no y la pertenencia es falsa")
    void esDeOtro() {
        UUID duenio = fixtura.usuario();
        var obligacion = fixtura.obligacion(duenio, "100.00", 10);
        UUID pago = pagoDe(duenio, obligacion);
        var p = pertenencia(Optional.of(false));
        UUID ajeno = fixtura.usuario();
        assertThat(p.participanteEsDelUsuario(obligacion.participanteId(), ctx(ajeno)))
                .isFalse();
        assertThat(p.pagoEsDelUsuario(pago, ctx(ajeno))).isFalse();
    }

    @Test
    @DisplayName(
            "limite: un pago que no existe no es de nadie, y un participante sin obligaciones no tiene que mostrar")
    void limites() {
        var p = pertenencia(Optional.of(true));
        UUID usuario = fixtura.usuario();
        assertThat(p.pagoEsDelUsuario(UUID.randomUUID(), ctx(usuario))).isFalse();
        assertThat(p.participanteEsDelUsuario(UUID.randomUUID(), ctx(usuario))).isTrue();
    }

    @Test
    @DisplayName("invalido: si grupos no contesta no se afirma la pertenencia (falla cerrado, AP-CU21-07)")
    void gruposCaido() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        var p = pertenencia(Optional.empty());
        assertThatThrownBy(() -> p.participanteEsDelUsuario(obligacion.participanteId(), ctx(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("No pudimos verificar");
    }
}
