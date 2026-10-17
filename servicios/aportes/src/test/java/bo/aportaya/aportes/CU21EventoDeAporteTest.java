package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    private String tipoEnElEvento(UUID obligacionId) {
        return dsl.fetchOne(
                        "SELECT payload->>'tipo' FROM aportes.evento_dominio"
                                + " WHERE tipo = 'aportes.aporte_cobrado' AND agregado_id = ?",
                        obligacionId)
                .get(0, String.class);
    }

    private void pagar(UUID obligacionId, String monto, String clave, UUID usuario) {
        transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        clave,
                        obligacionId,
                        Dinero.de(monto, Moneda.BOB),
                        Dinero.de("0.00", Moneda.BOB),
                        "BILLETERA_MOVIL",
                        "ref-" + clave,
                        Optional.empty(),
                        false,
                        true),
                contextoDe(usuario)));
    }

    @Test
    @DisplayName("tableros: el evento lleva el tipo de la obligación — un aporte periódico viaja como APORTE_PERIODICO")
    void elEventoTrae_elTipo_deUnAporte() {
        var usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);

        pagar(obligacion.id(), "500.00", "cob-tipo-aporte", usuario);

        assertThat(tipoEnElEvento(obligacion.id())).isEqualTo("APORTE_PERIODICO");
    }

    @Test
    @DisplayName("tableros: pagar un recargo por mora publica tipo RECARGO_MORA — es lo que lo separa del pozo")
    void elEventoTrae_elTipo_deUnRecargo() {
        var usuario = fixtura.usuario();
        var vencida = fixtura.obligacion(usuario, "500.00", -10);
        fixtura.politicaDeMora(vencida.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");
        var recargos = transaccion.execute(t -> cobroCU.generarRecargos(contextoDe(usuario)));
        UUID recargoId = recargos.obligacionesDeRecargo().get(0);

        pagar(recargoId, "25.00", "cob-tipo-recargo", usuario);

        assertThat(tipoEnElEvento(recargoId)).isEqualTo("RECARGO_MORA");
    }
}
