package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto.Entrada;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M2/M4/M6 · El pozo completo: el limite sin reserva, los dependidos caidos, el periodo
 * ajeno, el respaldo inconsistente y lo que la base exige por su cuenta.
 *
 * <p>Complementa a {@link CU133PozoCompletoTest}; aportes y garantia son los mismos dobles de tres
 * niveles (regla 65).
 */
class CU133PozoCompletoRechazosTest extends BaseDePozoCompleto {

    @Test
    @DisplayName(
            "LIMITE · Dado un grupo sin reserva vigente · Cuando se fondea · Entonces CON_PENDIENTE por el faltante completo")
    void sinReserva() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));
        garantiaDoble.sinReserva(true);

        var salida = fondear(c, "fondeo-7-" + c.e().turnoId());

        assertThat(salida.estadoDelFondeo()).isEqualTo("CON_PENDIENTE");
        assertThat(salida.cifras().pendiente()).isEqualTo(bob("1000.00"));
        assertThat(salida.cifras().cubiertoEmpresa().esCero()).isTrue();
    }

    @Test
    @DisplayName(
            "INVALIDO · APORTES CAIDO · Dado que aportes no responde · Cuando se fondea · Entonces AP-CU22-06 y no se escribe NADA; al volver aportes el pedido sale")
    void aportesCaido() {
        var c = caso();
        aportesDoble.seCae();
        garantiaDoble.conDisponible(bob("8000.00"));

        assertThatThrownBy(() -> fondear(c, "fondeo-8-" + c.e().turnoId()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-06"));
        assertThat(entregasDelTurno(c)).isZero();
        assertThat(garantiaDoble.llamadas.get()).isZero();

        aportesConfirma(c, "6000.00");
        assertThat(fondear(c, "fondeo-8-" + c.e().turnoId()).estadoDelFondeo()).isEqualTo("FONDEADO");
    }

    @Test
    @DisplayName(
            "INVALIDO · GARANTIA CAIDA · Dado que garantia no responde y hay faltante · Cuando se fondea · Entonces AP-CU22-08 sin escribir nada; al volver, el reintento cubre una sola vez")
    void garantiaCaida() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));
        garantiaDoble.seCae(true);

        assertThatThrownBy(() -> fondear(c, "fondeo-9-" + c.e().turnoId()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-08"));
        assertThat(entregasDelTurno(c)).isZero();

        garantiaDoble.seCae(false);
        var salida = fondear(c, "fondeo-9-" + c.e().turnoId());
        assertThat(salida.estadoDelFondeo()).isEqualTo("FONDEADO");
        assertThat(garantiaDoble.coberturasNuevas.get()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "INVALIDO · CAIDA DEL CONSUMIDOR TRAS LA COBERTURA · Dado que garantia cubrio pero la escritura local falla · Cuando se reintenta · Entonces la cobertura no se repite y la entrega nace una sola vez")
    void caidaTrasCubrir() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));
        // El beneficiario no existe: la escritura local revienta DESPUES de que garantia cubrio.
        var roto = new Entrada(
                c.e().grupoId(),
                c.e().periodoId(),
                c.e().turnoId(),
                c.e().cupoId(),
                UUID.randomUUID(),
                "BILLETERA_MOVIL",
                LocalDate.now(),
                "fondeo-10-" + c.e().turnoId());

        assertThatThrownBy(() -> pozoCU.fondear(roto, c.ctx())).isInstanceOf(RuntimeException.class);
        assertThat(entregasDelTurno(c)).isZero();
        assertThat(garantiaDoble.coberturasNuevas.get()).isEqualTo(1);

        var salida = fondear(c, "fondeo-10-" + c.e().turnoId());
        assertThat(salida.estadoDelFondeo()).isEqualTo("FONDEADO");
        assertThat(garantiaDoble.coberturasNuevas.get()).isEqualTo(1);
        assertThat(entregasDelTurno(c)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "INVALIDO · PERIODO DE OTRO GRUPO · Dado un periodo que aportes dice ser de otro grupo · Cuando se fondea · Entonces AP-CU22-07 y no se escribe nada")
    void periodoAjeno() {
        var c = caso();
        aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                c.e().periodoId(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                bob("6000.00"),
                bob("6000.00"),
                bob("0.00")));

        assertThatThrownBy(() -> fondear(c, "fondeo-11-" + c.e().turnoId()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-07"));
        assertThat(entregasDelTurno(c)).isZero();
    }

    @Test
    @DisplayName(
            "INVALIDO · RESPALDO INCONSISTENTE · Dado que garantia dice haber cubierto un importe distinto al faltante · Cuando se fondea · Entonces AP-CU22-07 y no se escribe nada")
    void respaldoCubreOtroImporte() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));
        garantiaDoble.cubreUnImporteDistinto(bob("999.99"));

        assertThatThrownBy(() -> fondear(c, "fondeo-12-" + c.e().turnoId()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-07"));
        assertThat(entregasDelTurno(c)).isZero();
    }

    @Test
    @DisplayName(
            "Dado un turno ya liquidado por el camino anterior · Cuando se intenta fondear · Entonces AP-CU22-09: no hay dos entregas del mismo turno")
    void turnoYaLiquidadoPorElCaminoAnterior() {
        var c = caso();
        transaccion.execute(t -> entregaCU.liquidar(
                new bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega.EntradaLiquidacion(
                        c.e().grupoId(),
                        c.e().periodoId(),
                        c.e().turnoId(),
                        c.e().cupoId(),
                        c.e().participanteId(),
                        bob("6000.00"),
                        bob("6000.00"),
                        java.util.List.of(),
                        "BILLETERA_MOVIL",
                        LocalDate.now()),
                c.ctx()));
        aportesConfirma(c, "6000.00");

        assertThatThrownBy(() -> fondear(c, "fondeo-13-" + c.e().turnoId()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-09"));
        assertThat(entregasDelTurno(c)).isEqualTo(1);
    }

    @Test
    @DisplayName("La base rechaza un fondeo que no cuadra o un pendiente sin deuda (R-DES-03)")
    void laBaseExigeElCuadre() {
        var c = caso();
        aportesConfirma(c, "6000.00");
        var salida = fondear(c, "fondeo-14-" + c.e().turnoId());

        assertThat(rechazaLaBase(
                        "UPDATE entregas.fondeo_entrega SET monto_confirmado = monto_confirmado - 1 WHERE id = ?",
                        salida.fondeoId()))
                .contains("ck_fondeo_entrega_cuadra");
        assertThat(rechazaLaBase(
                        "UPDATE entregas.fondeo_entrega SET estado = 'CON_PENDIENTE' WHERE id = ?", salida.fondeoId()))
                .contains("ck_fondeo_entrega_estado");
    }
}
