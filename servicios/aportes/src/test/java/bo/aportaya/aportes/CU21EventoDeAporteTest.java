package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
                        """
                        SELECT payload->>'tipo' FROM aportes.evento_dominio
                         WHERE tipo = 'aportes.aporte_cobrado' AND agregado_id = ?
                        """,
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
    @DisplayName("cuadre: el evento lleva el tipo de la obligación — un aporte periódico viaja como APORTE_PERIODICO")
    void elEventoTrae_elTipo_deUnAporte() {
        var usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);

        pagar(obligacion.id(), "500.00", "cob-tipo-aporte", usuario);

        assertThat(tipoEnElEvento(obligacion.id())).isEqualTo("APORTE_PERIODICO");
    }

    @Test
    @DisplayName("cuadre: pagar un recargo por mora publica tipo RECARGO_MORA — es lo que lo separa del pozo")
    void elEventoTrae_elTipo_deUnRecargo() {
        var usuario = fixtura.usuario();
        var vencida = fixtura.obligacion(usuario, "500.00", -10);
        fixtura.politicaDeMora(vencida.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");
        var recargos = transaccion.execute(t -> cobroCU.generarRecargos(contextoDe(usuario)));
        UUID recargoId = recargos.obligacionesDeRecargo().get(0);

        pagar(recargoId, "25.00", "cob-tipo-recargo", usuario);

        assertThat(tipoEnElEvento(recargoId)).isEqualTo("RECARGO_MORA");
    }

    @Test
    @DisplayName(
            "cuadre: pacto con nucleo-financiero · productor: el cobro de un recargo publica TODOS los campos del pacto"
                    + " aporte_cobrado, con el importe en dos decimales y el tipo RECARGO_MORA del ejemplo")
    void cumpleElPactoConNucleo() throws IOException {
        Path raiz = Path.of("").toAbsolutePath();
        String archivo = "contratos/pactos/aportes-nucleo-financiero-aporte-cobrado.json";
        while (raiz != null && !Files.isRegularFile(raiz.resolve(archivo))) {
            raiz = raiz.getParent();
        }
        JsonNode pacto = new ObjectMapper().readTree(Files.readString(raiz.resolve(archivo), StandardCharsets.UTF_8));
        var usuario = fixtura.usuario();
        var vencida = fixtura.obligacion(usuario, "500.00", -10);
        fixtura.politicaDeMora(vencida.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");
        UUID recargoId = transaccion
                .execute(t -> cobroCU.generarRecargos(contextoDe(usuario)))
                .obligacionesDeRecargo()
                .get(0);
        pagar(recargoId, "25.00", "cob-pacto-nucleo", usuario);

        String payload = dsl.fetchOne(
                        "SELECT payload::text FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                        pacto.path("tipo").asText(),
                        recargoId)
                .get(0, String.class);
        JsonNode emitido = new ObjectMapper().readTree(payload);
        var campos = new ArrayList<String>();
        pacto.path("campos").fieldNames().forEachRemaining(campos::add);
        for (String campo : campos) {
            assertThat(emitido.has(campo)).as("el evento trae " + campo).isTrue();
        }
        assertThat(emitido.path("monto").asText()).matches("^[0-9]+[.][0-9]{2}$");
        assertThat(emitido.path("tipo").asText())
                .isEqualTo(pacto.path("ejemplo").path("tipo").asText());
        assertThat(pacto.path("agregado").asText()).isEqualTo("obligacion_aporte");
    }
}
