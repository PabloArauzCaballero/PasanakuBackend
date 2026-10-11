package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril I · contrato del productor de {@code aportes.aporte_cobrado}: lo que {@code nucleo-financiero} CONSUME para mover el dinero.
 * El importe nunca viaja sin su moneda (regla 91.1.2) y el evento nombra quien paga, la obligacion, el pago, el grupo y el canal.
 */
class CU21EventoDeAporteTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName(
            "cuadre: el evento aporte_cobrado lleva monto, moneda, canal, usuario, obligacion, pago, grupo y participante")
    void elEventoTrae_todoLoQueNucleoNecesita() {
        var usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        var ctx = contextoDe(usuario);
        var salida = transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        "cob-evento",
                        obligacion.id(),
                        Dinero.de("500.00", Moneda.BOB),
                        Dinero.de("0.00", Moneda.BOB),
                        "BILLETERA_MOVIL",
                        "ref-evento",
                        Optional.empty(),
                        false,
                        true),
                ctx));

        var carga = dsl.fetchOne(
                """
                SELECT payload->>'monto', payload->>'moneda', payload->>'canal', payload->>'usuarioId',
                       payload->>'obligacionId', payload->>'pagoId', payload->>'grupoId', payload->>'participanteId'
                  FROM aportes.evento_dominio WHERE tipo = 'aportes.aporte_cobrado' AND agregado_id = ?
                """,
                obligacion.id());

        assertThat(List.of(
                        carga.get(0),
                        carga.get(1),
                        carga.get(2),
                        carga.get(3),
                        carga.get(4),
                        carga.get(5),
                        carga.get(6)))
                .containsExactly(
                        "500.00",
                        "BOB",
                        "BILLETERA_MOVIL",
                        usuario.toString(),
                        obligacion.id().toString(),
                        salida.pagoId().toString(),
                        obligacion.grupoId().toString());
        assertThat(carga.get(7)).isNotNull();
    }
}
