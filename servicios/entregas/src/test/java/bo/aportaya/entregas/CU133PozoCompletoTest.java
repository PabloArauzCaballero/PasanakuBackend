package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M2/M4/M6 · El pozo completo, una sola vez, contra PostgreSQL real.
 *
 * <p>aportes y garantia van como DOBLES de tres niveles (regla 65): lo que queda verificado es
 * la orquestacion de este servicio y su persistencia, no los servicios vecinos. Los limites y los
 * invalidos (dependidos caidos, periodo ajeno, respaldo inconsistente, restricciones de la base)
 * estan en {@link CU133PozoCompletoRechazosTest}, separada por la regla de tamano de archivo.
 */
class CU133PozoCompletoTest extends BaseDePozoCompleto {

    @Test
    @DisplayName(
            "CORRECTO · Dado un pozo con todos los aportes confirmados · Cuando se fondea · Entonces nace la entrega PROGRAMADA con bruto = neto = Bs 6.000 y no se le pide nada a garantia")
    void pozoCompletoSinFaltante() {
        var c = caso();
        aportesConfirma(c, "6000.00");
        garantiaDoble.conDisponible(bob("8000.00"));

        var salida = fondear(c, "fondeo-1-" + c.e().turnoId());

        assertThat(salida.estadoDelFondeo()).isEqualTo("FONDEADO");
        assertThat(salida.cifras().pendiente()).isEqualTo(bob("0.00"));
        assertThat(garantiaDoble.llamadas.get()).isZero();
        var fila = dsl.fetchOne(
                "SELECT estado, monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar FROM entregas.entrega_fondo WHERE id = ?",
                salida.entregaId());
        assertThat(fila.get(0, String.class)).isEqualTo("PROGRAMADA");
        assertThat(fila.get(1, BigDecimal.class)).isEqualByComparingTo("6000.00");
        assertThat(fila.get(2, BigDecimal.class)).isEqualByComparingTo("0.00");
        assertThat(fila.get(3, BigDecimal.class)).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName(
            "CORRECTO · Dado pozo Bs 6.000 y Bs 5.000 confirmados · Cuando se fondea · Entonces pide Bs 1.000 a la empresa, la entrega queda con el principal COMPLETO y el fondeo cuadra en la base")
    void ejemploDelPlan() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));

        var salida = fondear(c, "fondeo-2-" + c.e().turnoId());

        assertThat(salida.estadoDelFondeo()).isEqualTo("FONDEADO");
        assertThat(salida.cifras().cubiertoEmpresa()).isEqualTo(bob("1000.00"));
        assertThat(garantiaDoble.coberturasAplicadas()).isEqualTo(1);
        // Cuadre desde la fuente: pozo = confirmado + mutual + faltante; faltante = empresa + pendiente.
        var f = dsl.fetchOne(
                """
                SELECT monto_pozo, monto_confirmado, monto_cubierto_mutual, monto_faltante,
                       monto_cubierto_empresa, monto_pendiente, estado, cobertura_respaldo_id
                  FROM entregas.fondeo_entrega WHERE turno_id = ?
                """,
                c.e().turnoId());
        assertThat(f.get(0, BigDecimal.class))
                .isEqualByComparingTo(f.get(1, BigDecimal.class)
                        .add(f.get(2, BigDecimal.class))
                        .add(f.get(3, BigDecimal.class)));
        assertThat(f.get(3, BigDecimal.class)).isEqualByComparingTo("1000.00");
        assertThat(f.get(4, BigDecimal.class)).isEqualByComparingTo("1000.00");
        assertThat(f.get(5, BigDecimal.class)).isEqualByComparingTo("0.00");
        assertThat(f.get(6, String.class)).isEqualTo("FONDEADO");
        assertThat(f.get(7, UUID.class)).isNotNull();
        assertThat(numero("SELECT monto_bolsa_bruto FROM entregas.entrega_fondo WHERE id = ?", salida.entregaId()))
                .isEqualByComparingTo("6000.00");
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = 'entregas.pozo_fondeado' AND agregado_id = ?",
                        salida.entregaId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un pozo fondeado · Cuando se autoriza, se ejecuta y se intenta ejecutar otra vez · Entonces el beneficiario recibe Bs 6.000 UNA sola vez")
    void seEntregaUnaSolaVez() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));
        var salida = fondear(c, "fondeo-3-" + c.e().turnoId());
        ContextoSesion supervisor = contextoDe(fixtura.usuario());

        transaccion.execute(t -> entregaCU.autorizar(salida.entregaId(), c.ctx()));
        var ejecutada = transaccion.execute(t -> entregaCU.ejecutar(salida.entregaId(), bob("6000.00"), supervisor));

        assertThat(ejecutada.montoEntregado()).isEqualTo(bob("6000.00"));
        assertThatThrownBy(() ->
                        transaccion.execute(t -> entregaCU.ejecutar(salida.entregaId(), bob("6000.00"), supervisor)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-03"));
        // Y volver a fondear el turno ya entregado no abre otra entrega.
        var otraVez = fondear(c, "fondeo-3-" + c.e().turnoId());
        assertThat(otraVez.entregaId()).isEqualTo(salida.entregaId());
        assertThat(otraVez.esNuevo()).isFalse();
        assertThat(entregasDelTurno(c)).isEqualTo(1);
        assertThat(numero(
                        "SELECT monto_efectivamente_entregado FROM entregas.entrega_fondo WHERE id = ?",
                        salida.entregaId()))
                .isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName(
            "Dada la misma clave dos veces · Cuando se fondea · Entonces hay una entrega, un fondeo y una sola cobertura aunque garantia se haya consultado dos veces (idempotencia)")
    void idempotente() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));

        var primera = fondear(c, "fondeo-4-" + c.e().turnoId());
        var segunda = fondear(c, "fondeo-4-" + c.e().turnoId());

        assertThat(segunda.entregaId()).isEqualTo(primera.entregaId());
        assertThat(segunda.fondeoId()).isEqualTo(primera.fondeoId());
        assertThat(segunda.esNuevo()).isFalse();
        assertThat(entregasDelTurno(c)).isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM entregas.fondeo_entrega WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(1);
        assertThat(garantiaDoble.coberturasNuevas.get()).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = 'entregas.pozo_fondeado' AND agregado_id = ?",
                        primera.entregaId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una reserva de Bs 800 para un faltante de Bs 1.000 · Cuando se fondea · Entonces la entrega queda BLOQUEADA con el pozo completo, la deuda de Bs 1.000 conservada y una incidencia CRITICA abierta; autorizarla se rechaza")
    void faltaExtraordinariaDeCaja() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("800.00"));

        var salida = fondear(c, "fondeo-5-" + c.e().turnoId());
        // reintento sin cambios: no duplica incidencia ni entrega
        fondear(c, "fondeo-5-" + c.e().turnoId());

        assertThat(salida.estadoDelFondeo()).isEqualTo("CON_PENDIENTE");
        assertThat(salida.cifras().pendiente()).isEqualTo(bob("1000.00"));
        assertThat(dsl.fetchOne("SELECT estado FROM entregas.entrega_fondo WHERE id = ?", salida.entregaId())
                        .get(0, String.class))
                .isEqualTo("BLOQUEADA_POR_FONDO_INCOMPLETO");
        assertThat(numero("SELECT monto_bolsa_bruto FROM entregas.entrega_fondo WHERE id = ?", salida.entregaId()))
                .isEqualByComparingTo("6000.00");
        var inc = dsl.fetchOne(
                "SELECT count(*)::int, min(severidad), min(estado) FROM entregas.incidencia_entrega WHERE entrega_id = ? AND tipo = 'FONDO_INCOMPLETO'",
                salida.entregaId());
        assertThat(inc.get(0, Integer.class)).isEqualTo(1);
        assertThat(inc.get(1, String.class)).isEqualTo("CRITICA");
        assertThat(inc.get(2, String.class)).isEqualTo("ABIERTA");
        assertThatThrownBy(() -> transaccion.execute(t -> entregaCU.autorizar(salida.entregaId(), c.ctx())))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU22-03"));
    }

    @Test
    @DisplayName(
            "Dada la contingencia (la reserva se amplia) · Cuando se reintenta el MISMO pedido · Entonces se completa el mismo fondeo, la entrega se destraba y la incidencia queda RESUELTA")
    void contingenciaYReintento() {
        var c = caso();
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("800.00"));
        var bloqueada = fondear(c, "fondeo-6-" + c.e().turnoId());

        garantiaDoble.conDisponible(bob("1000.00")); // el responsable amplio la reserva
        var completada = fondear(c, "fondeo-6-" + c.e().turnoId());

        assertThat(completada.entregaId()).isEqualTo(bloqueada.entregaId());
        assertThat(completada.fondeoId()).isEqualTo(bloqueada.fondeoId());
        assertThat(completada.estadoDelFondeo()).isEqualTo("FONDEADO");
        assertThat(dsl.fetchOne("SELECT estado FROM entregas.entrega_fondo WHERE id = ?", completada.entregaId())
                        .get(0, String.class))
                .isEqualTo("PROGRAMADA");
        assertThat(dsl.fetchOne(
                                "SELECT estado FROM entregas.incidencia_entrega WHERE entrega_id = ? AND tipo = 'FONDO_INCOMPLETO'",
                                completada.entregaId())
                        .get(0, String.class))
                .isEqualTo("RESUELTA");
        assertThat(entregasDelTurno(c)).isEqualTo(1);
        // y ahora si sale, una sola vez
        transaccion.execute(t -> entregaCU.autorizar(completada.entregaId(), c.ctx()));
        assertThat(contar("SELECT count(*)::int FROM entregas.fondeo_entrega WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(1);
    }
}
