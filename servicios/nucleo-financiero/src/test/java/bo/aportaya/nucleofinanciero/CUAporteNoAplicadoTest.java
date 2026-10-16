package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.Resultado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · el aporte PAGADO sin saldo se COMPENSA: nucleo no mueve nada, marca el evento consumido y publica
 * {@code nucleo_financiero.aporte_no_aplicado}; el efecto en {@code aportes} (revertir el pago) se prueba alla con el mismo
 * sobre ({@code CU21CompensaTest}). El contrato del evento lo fija este test: pagoId, obligacionId, grupoId, importe con su
 * moneda, motivo y codigo.
 */
class CUAporteNoAplicadoTest extends BaseDeAportes {

    private int avisos(UUID obligacion) {
        return contar(
                """
                SELECT count(*) FROM nucleo_financiero.evento_dominio
                 WHERE tipo = 'nucleo_financiero.aporte_no_aplicado' AND agregado_id = ?
                """,
                obligacion);
    }

    @Test
    @DisplayName("compensa: con Bs 300 y un aporte de Bs 500 no se mueve el libro ni el mayor, y se publica"
            + " aporte_no_aplicado con el sobre completo (pago, obligacion, grupo, importe+moneda, motivo, codigo)")
    void publicaElFalloConSuSobre() {
        var a = aporte("300.00");
        var e = cobrado(a, "500.00");

        var salida = procesar(e);

        assertThat(salida.resultado()).isEqualTo(Resultado.RECHAZADO_POR_SALDO);
        assertThat(salida.transaccionId()).isEmpty();
        assertThat(salida.asientoId()).isEmpty();
        // Cuadre desde el libro y el mayor: nada se movio.
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("300.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
        assertThat(transacciones(a.obligacion())).isZero();
        assertThat(asientosDeAportes(a.grupo())).isZero();
        // El aviso: uno solo, con los campos del contrato.
        assertThat(avisos(a.obligacion())).isEqualTo(1);
        var p = dsl.fetchOne(
                """
                SELECT payload->>'pagoId' AS pago, payload->>'obligacionId' AS obligacion, payload->>'grupoId' AS grupo,
                       payload->>'monto' AS monto, payload->>'moneda' AS moneda, payload->>'motivo' AS motivo,
                       payload->>'codigo' AS codigo, correlation_id
                  FROM nucleo_financiero.evento_dominio
                 WHERE tipo = 'nucleo_financiero.aporte_no_aplicado' AND agregado_id = ?
                """,
                a.obligacion());
        assertThat(p.get("pago")).isEqualTo(e.pagoId().toString());
        assertThat(p.get("obligacion")).isEqualTo(a.obligacion().toString());
        assertThat(p.get("grupo")).isEqualTo(a.grupo().toString());
        assertThat(new BigDecimal(p.get("monto").toString())).isEqualByComparingTo("500.00");
        assertThat(p.get("moneda")).isEqualTo("BOB");
        assertThat(p.get("motivo")).isEqualTo("SALDO_INSUFICIENTE");
        assertThat(p.get("codigo")).isEqualTo("AP-CU12-01");
        assertThat(p.get("correlation_id")).isEqualTo(e.correlationId());
        System.out.println("COMPENSA | aporte_no_aplicado pago=" + e.pagoId() + " | libro 300.00 -> "
                + saldoDelLibro(a.billetera()) + " | grupo 0.00 -> " + saldoDelLibro(a.billeteraDelGrupo()));
    }

    @Test
    @DisplayName(
            "evento duplicado: el mismo aporte_cobrado sin saldo dos veces publica UN solo aporte_no_aplicado, y recargar"
                    + " despues NO lo aplica solo (el pago ya se compensa: la persona vuelve a pagar)")
    void duplicadoNoRepiteElAviso() {
        var a = aporte("300.00");
        var e = cobrado(a, "500.00");

        assertThat(procesar(e).resultado()).isEqualTo(Resultado.RECHAZADO_POR_SALDO);
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(avisos(a.obligacion())).isEqualTo(1);

        fixtura.acreditar(a.billetera(), new BigDecimal("1000.00"));
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(transacciones(a.obligacion())).isZero();
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1300.00");
    }

    @Test
    @DisplayName(
            "reintento: el MISMO pago llega con otro id de evento y sigue sin saldo · Entonces el aviso viaja con el mismo"
                    + " pagoId (aportes compensa por pago, idempotente) y el libro sigue intacto")
    void mismoPagoOtroEvento() {
        var a = aporte("300.00");
        var e = cobrado(a, "500.00");

        assertThat(procesar(e).resultado()).isEqualTo(Resultado.RECHAZADO_POR_SALDO);
        assertThat(procesar(reenviado(e)).resultado()).isEqualTo(Resultado.RECHAZADO_POR_SALDO);

        assertThat(contar(
                        """
                        SELECT count(DISTINCT payload->>'pagoId') FROM nucleo_financiero.evento_dominio
                         WHERE tipo = 'nucleo_financiero.aporte_no_aplicado' AND agregado_id = ?
                        """,
                        a.obligacion()))
                .isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName(
            "Dado saldo justo (Bs 500 para un aporte de Bs 500) · Entonces SE aplica: el rechazo es solo por falta de saldo")
    void conSaldoJustoSeAplica() {
        var a = aporte("500.00");
        var e = cobrado(a, "500.00");

        assertThat(procesar(e).resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(avisos(a.obligacion())).isZero();
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("0.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "pacto con aportes · productor: el evento publicado trae TODOS los campos del pacto, con el formato del"
                    + " ejemplo (importe decimal de dos decimales, moneda ISO, motivo y codigo)")
    void cumpleElPactoConAportes() throws IOException {
        Path raiz = Path.of("").toAbsolutePath();
        while (raiz != null && !Files.isRegularFile(raiz.resolve("contratos/pactos/aportes-nucleo-financiero.json"))) {
            raiz = raiz.getParent();
        }
        JsonNode pacto = new ObjectMapper()
                .readTree(Files.readString(
                        raiz.resolve("contratos/pactos/aportes-nucleo-financiero.json"), StandardCharsets.UTF_8));
        var a = aporte("300.00");
        procesar(cobrado(a, "500.00"));

        String payload = (String) dsl.fetchOne(
                        "SELECT payload::text FROM nucleo_financiero.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                        pacto.path("tipo").asText(),
                        a.obligacion())
                .get(0);
        JsonNode emitido = new ObjectMapper().readTree(payload);
        var campos = new ArrayList<String>();
        pacto.path("campos").fieldNames().forEachRemaining(campos::add);
        for (String campo : campos) {
            assertThat(emitido.has(campo)).as("el evento trae " + campo).isTrue();
        }
        assertThat(emitido.path("monto").asText()).matches("^-?[0-9]+[.][0-9]{2}$");
        assertThat(emitido.path("motivo").asText())
                .isEqualTo(pacto.path("ejemplo").path("motivo").asText());
        assertThat(emitido.path("codigo").asText())
                .isEqualTo(pacto.path("ejemplo").path("codigo").asText());
        assertThat(pacto.path("agregado").asText()).isEqualTo("obligacion_aporte");
    }
}
