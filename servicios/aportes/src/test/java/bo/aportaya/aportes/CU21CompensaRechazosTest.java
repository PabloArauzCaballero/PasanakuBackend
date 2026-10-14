package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.EntradaReembolso;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.AporteNoAplicado;
import bo.aportaya.aportes.aplicacion.CU21CompensarAporte.Resultado;
import bo.aportaya.aportes.infraestructura.AuditoriaRepositorio;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · CU-21 · los rechazos y los bordes de la compensacion: sobre incoherente, lo reembolsado o en disputa, pago
 * inexistente, concurrencia y caida a mitad. El camino correcto esta en {@link CU21CompensaTest}.
 */
class CU21CompensaRechazosTest extends BaseDeCompensacion {

    @Test
    @DisplayName(
            "rechaza un aviso cuyo pago es de OTRA obligacion: no se compensa a ciegas, no queda marca y nada cambia")
    void rechazaElSobreIncoherente() {
        Caso c = pagado("500.00", 10);
        Caso otro = pagado("300.00", 10);
        var a = new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), c.pago(), otro.obligacion(), FALLA);

        assertThatThrownBy(() -> compensar(a)).isInstanceOf(IllegalArgumentException.class);

        assertThat(estadoDelPago(c.pago())).isEqualTo("ACREDITADO");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");
        assertThat(estadoDe(otro.obligacion())).isEqualTo("PAGADO");
        assertThat(contar("SELECT count(*)::int FROM aportes.evento_consumido WHERE id_evento = ?", a.eventoId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "rechaza compensar lo que ya se toco por otro camino: un pago con reembolso solicitado o en disputa no se"
                    + " reversa solo (queda para una persona) y no se pierde el evento")
    void noCompensaLoReembolsadoNiEnDisputa() {
        Caso conReembolso = pagado("500.00", 10);
        transaccion.execute(t -> reembolsoCU.solicitar(
                new EntradaReembolso(conReembolso.pago(), bob("100.00"), "DUPLICADO"), contextoDe(fixtura.usuario())));
        var a1 = aviso(conReembolso);
        assertThat(compensar(a1).resultado()).isEqualTo(Resultado.NO_COMPENSABLE);
        assertThat(estadoDelPago(conReembolso.pago())).isEqualTo("ACREDITADO");
        assertThat(estadoDe(conReembolso.obligacion())).isEqualTo("PAGADO");

        Caso enDisputa = pagado("400.00", 10);
        dslFixtura.execute("UPDATE aportes.pago SET estado = 'EN_DISPUTA' WHERE id = ?", enDisputa.pago());
        assertThat(compensar(aviso(enDisputa)).resultado()).isEqualTo(Resultado.NO_COMPENSABLE);
        assertThat(estadoDe(enDisputa.obligacion())).isEqualTo("PAGADO");
        assertThat(eventos("aportes.aporte_compensado", enDisputa.obligacion())).isZero();
    }

    @Test
    @DisplayName(
            "rechaza compensar un pago que no existe en aportes: se marca consumido sin tocar nada (no hay a quien compensar)")
    void pagoInexistente() {
        Caso c = pagado("500.00", 10);
        var a = new AporteNoAplicado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), c.obligacion(), FALLA);

        assertThat(compensar(a).resultado()).isEqualTo(Resultado.PAGO_INEXISTENTE);

        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");
    }

    @Test
    @DisplayName(
            "concurrencia: dos eventos DISTINTOS del mismo pago al mismo tiempo compensan UNA sola vez: ni cero negativo ni"
                    + " dos avisos")
    void concurrencia() throws Exception {
        Caso c = pagado("500.00", 10);

        var resultados = enParalelo(
                () -> compensar(aviso(c)).resultado(), () -> compensar(aviso(c)).resultado());

        assertThat(resultados).containsExactlyInAnyOrder(Resultado.COMPENSADO, Resultado.YA_COMPENSADO);
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PENDIENTE");
        assertThat(eventos("aportes.aporte_compensado", c.obligacion())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza una caida a mitad: si se cae la auditoria DESPUES de reversar el pago, todo se revierte (pago y"
                    + " obligacion intactos, sin marca) y el reproceso compensa una sola vez")
    void caidaAMitad() {
        Caso c = pagado("500.00", 10);
        var a = aviso(c);
        var auditoriaCaida = new AuditoriaRepositorio() {
            @Override
            public UUID registrar(
                    DSLContext d,
                    String entidad,
                    UUID entidadId,
                    String accion,
                    UUID actorUsuarioId,
                    String actorRol,
                    UUID correlationId,
                    String valorAnteriorJson,
                    String valorNuevoJson,
                    String motivo,
                    java.time.OffsetDateTime ahora) {
                throw new IllegalStateException("se cayo la bitacora a mitad de la transaccion");
            }
        };

        assertThatThrownBy(() ->
                        transaccion.execute(t -> compensarCon(auditoriaCaida).procesar(a)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(estadoDelPago(c.pago())).isEqualTo("ACREDITADO");
        assertThat(estadoDe(c.obligacion())).isEqualTo("PAGADO");
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("500.00");
        assertThat(contar("SELECT count(*)::int FROM aportes.evento_consumido WHERE id_evento = ?", a.eventoId()))
                .isZero();

        assertThat(compensar(a).resultado()).isEqualTo(Resultado.COMPENSADO);
        assertThat(compensar(a).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(pagadoDe(c.obligacion())).isEqualByComparingTo("0.00");
    }
}
