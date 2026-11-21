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
 * H8.S1.M2/M4/M6 · El pozo completo: el limite de un centavo, aportes caido, la escritura local que
 * falla, el periodo ajeno y lo que la base exige por su cuenta.
 *
 * <p>Complementa a {@link CU133PozoCompletoTest}; aportes es el mismo doble de tres niveles (regla 65).
 * El respaldo empresarial se retiro (F2.L2.M01): no hay garantia caida ni respaldo inconsistente que probar.
 */
class CU133PozoCompletoRechazosTest extends BaseDePozoCompleto {

    @Test
    @DisplayName(
            "LIMITE · Dado un faltante de un centavo · Cuando se fondea · Entonces CON_PENDIENTE por ese centavo y la empresa no cubre nada")
    void faltaUnCentavo() {
        var c = caso();
        aportesConfirma(c, "5999.99");

        var salida = fondear(c, "fondeo-7-" + c.e().turnoId());

        // Nadie redondea a favor de la entrega: un centavo que falta es un centavo de deuda conservada.
        assertThat(salida.estadoDelFondeo()).isEqualTo("CON_PENDIENTE");
        assertThat(salida.cifras().pendiente()).isEqualTo(bob("0.01"));
        assertThat(salida.cifras().cubiertoEmpresa().esCero()).isTrue();
    }

    @Test
    @DisplayName(
            "INVALIDO · APORTES CAIDO · Dado que aportes no responde · Cuando se fondea · Entonces AP-CU22-06 y no se escribe NADA; al volver aportes el pedido sale")
    void aportesCaido() {
        var c = caso();
        aportesDoble.seCae();

        assertThatThrownBy(() -> fondear(c, "fondeo-8-" + c.e().turnoId()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-06"));
        assertThat(entregasDelTurno(c)).isZero();

        aportesConfirma(c, "6000.00");
        assertThat(fondear(c, "fondeo-8-" + c.e().turnoId()).estadoDelFondeo()).isEqualTo("FONDEADO");
    }

    @Test
    @DisplayName(
            "INVALIDO · LA ESCRITURA LOCAL FALLA · Dado que la escritura del fondeo revienta · Cuando se reintenta con el pedido correcto · Entonces no quedo nada a medias y la entrega nace una sola vez")
    void escrituraLocalFalla() {
        var c = caso();
        aportesConfirma(c, "6000.00");
        // El beneficiario no existe: la escritura local revienta dentro de la transaccion.
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
        assertThat(contar("SELECT count(*)::int FROM entregas.fondeo_entrega WHERE turno_id = ?", c.e().turnoId()))
                .isZero();

        var salida = fondear(c, "fondeo-10-" + c.e().turnoId());
        assertThat(salida.estadoDelFondeo()).isEqualTo("FONDEADO");
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
            "Dado un turno ya liquidado por el camino anterior · Cuando se intenta fondear · Entonces AP-CU22-09: no hay dos entregas del mismo turno")
    void turnoYaLiquidadoPorElCaminoAnterior() {
        var c = caso();
        // La liquidacion vieja se retiro (F1.L2.M09): su entrega llega como dato de antes, escrito directo en la base.
        dslFixtura.execute(
                """
                INSERT INTO entregas.entrega_fondo
                    (id, grupo_id, periodo_id, turno_id, cupo_id, beneficiario_participante_id,
                     monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar,
                     monto_efectivamente_entregado, moneda, estado, metodo_desembolso,
                     fecha_programada, version)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 6000.00, 0, 6000.00, 0,
                        'BOB', 'PROGRAMADA', 'BILLETERA_MOVIL', current_date, 0)
                """,
                c.e().grupoId(),
                c.e().periodoId(),
                c.e().turnoId(),
                c.e().cupoId(),
                c.e().participanteId());
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
