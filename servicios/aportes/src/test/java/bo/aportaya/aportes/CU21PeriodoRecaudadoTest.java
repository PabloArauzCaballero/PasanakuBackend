package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L1.P02a · productor de {@code aportes.periodo_recaudado} (motivo COMPLETO) contra PostgreSQL real, y su pacto con
 * {@code entregas} ({@code contratos/pactos/aportes-entregas-periodo-recaudado.json}, propuesta de L2 en el PR #490).
 */
class CU21PeriodoRecaudadoTest extends BaseDeAportes {

    private static final String TIPO = "aportes.periodo_recaudado";

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
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

    private UUID periodoDe(UUID obligacionId) {
        return dsl.fetchOne("SELECT periodo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacionId)
                .get(0, UUID.class);
    }

    /** La cuota del cupo 2 en el MISMO periodo (mismo participante: alcanza para el conteo). */
    private UUID otraCuota(UUID obligacionId, String monto) {
        UUID cupo = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                SELECT ?, grupo_id, 2, participante_id, 'OCUPADO', 1.0, now()
                  FROM aportes.obligacion_aporte WHERE id = ?
                """,
                cupo,
                obligacionId);
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO aportes.obligacion_aporte
                    (id, grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda,
                     monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia, estado,
                     fecha_vencimiento, fecha_fin_gracia, dias_mora, version)
                SELECT ?, grupo_id, periodo_id, ?, participante_id, 'APORTE_PERIODICO', ?, moneda,
                       0, 0, 0, 0, 'PENDIENTE', fecha_vencimiento, fecha_fin_gracia, 0, 0
                  FROM aportes.obligacion_aporte WHERE id = ?
                """,
                id,
                cupo,
                new BigDecimal(monto),
                obligacionId);
        return id;
    }

    private int avisos(UUID periodoId) {
        return contar(
                "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?", TIPO, periodoId);
    }

    @Test
    @DisplayName(
            "cuadre: el pago que completa la unica cuota del periodo emite periodo_recaudado COMPLETO, sin importes")
    void cuadreCuotaUnica() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        UUID periodo = periodoDe(obligacion.id());

        pagar(obligacion.id(), "500.00", "rec-1", usuario);

        assertThat(avisos(periodo)).isEqualTo(1);
        var carga = dsl.fetchOne(
                """
                SELECT payload->>'periodoId', payload->>'grupoId', payload->>'motivo', payload->>'ocurridoEn',
                       jsonb_exists(payload, 'monto'), agregado
                  FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?
                """,
                TIPO,
                periodo);
        assertThat(carga.get(0, String.class)).isEqualTo(periodo.toString());
        assertThat(carga.get(1, String.class)).isEqualTo(obligacion.grupoId().toString());
        assertThat(carga.get(2, String.class)).isEqualTo("COMPLETO");
        assertThat(carga.get(3, String.class)).isNotBlank();
        assertThat(carga.get(4, Boolean.class)).as("el aviso no lleva importes").isFalse();
        assertThat(carga.get(5, String.class)).isEqualTo("periodo");
    }

    @Test
    @DisplayName("cuadre: con dos cuotas, pagar la primera no avisa y pagar la segunda avisa una sola vez")
    void cuadreDosCuotas() {
        UUID usuario = fixtura.usuario();
        var primera = fixtura.obligacion(usuario, "500.00", 10);
        UUID segunda = otraCuota(primera.id(), "300.00");
        UUID periodo = periodoDe(primera.id());

        pagar(primera.id(), "500.00", "rec-a", usuario);
        assertThat(avisos(periodo)).as("falta una cuota").isZero();

        pagar(segunda, "300.00", "rec-b", usuario);
        assertThat(avisos(periodo)).isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: un pago parcial de la ultima cuota no avisa; el que la salda, si")
    void cuadreParcial() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        UUID periodo = periodoDe(obligacion.id());

        pagar(obligacion.id(), "200.00", "rec-p1", usuario);
        assertThat(avisos(periodo)).isZero();

        pagar(obligacion.id(), "300.00", "rec-p2", usuario);
        assertThat(avisos(periodo)).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el mismo ultimo pago con la misma clave no vuelve a avisar")
    void reintentoNoDuplicaElAviso() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        UUID periodo = periodoDe(obligacion.id());

        pagar(obligacion.id(), "500.00", "rec-rep", usuario);
        pagar(obligacion.id(), "500.00", "rec-rep", usuario);

        assertThat(avisos(periodo)).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: los dos ultimos pagos del periodo llegan juntos y el aviso sale una vez, nunca cero")
    void concurrenciaDeUltimosPagos() throws Exception {
        // La carrera es probabilistica: se repite para que la falta del cerrojo se note en cada corrida.
        for (int ronda = 0; ronda < 20; ronda++) {
            UUID usuario = fixtura.usuario();
            var primera = fixtura.obligacion(usuario, "500.00", 10);
            UUID segunda = otraCuota(primera.id(), "500.00");
            UUID periodo = periodoDe(primera.id());
            var largada = new CountDownLatch(1);
            String r = Integer.toString(ronda);
            Callable<Boolean> pagoA = () -> {
                largada.await();
                pagar(primera.id(), "500.00", "rec-c1-" + r, usuario);
                return true;
            };
            Callable<Boolean> pagoB = () -> {
                largada.await();
                pagar(segunda, "500.00", "rec-c2-" + r, usuario);
                return true;
            };

            var resultados = new ArrayList<Boolean>();
            try (var hilos = Executors.newFixedThreadPool(2)) {
                var a = hilos.submit(pagoA);
                var b = hilos.submit(pagoB);
                largada.countDown();
                resultados.add(a.get());
                resultados.add(b.get());
            }

            assertThat(resultados).containsExactly(true, true);
            assertThat(avisos(periodo)).as("ronda %s", r).isEqualTo(1);
        }
    }

    @Test
    @DisplayName(
            "cuadre: el aviso cumple el pacto con entregas: los campos del pacto, los que exige el consumidor y un motivo valido")
    void cumpleElPactoConEntregas() throws IOException {
        JsonNode pacto = new ObjectMapper()
                .readTree(Files.readString(raiz().resolve("contratos/pactos/aportes-entregas-periodo-recaudado.json")));
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        UUID periodo = periodoDe(obligacion.id());
        pagar(obligacion.id(), "500.00", "rec-pacto", usuario);

        var fila = dsl.fetchOne(
                "SELECT tipo, payload::text, metadatos->>'tema' FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                TIPO,
                periodo);
        JsonNode carga = new ObjectMapper().readTree(fila.get(1, String.class));

        assertThat(fila.get(0, String.class)).isEqualTo(pacto.path("tipo").asText());
        assertThat(fila.get(2, String.class)).isEqualTo(pacto.path("topico").asText());
        var campos = new ArrayList<String>();
        for (Iterator<String> it = pacto.path("campos").fieldNames(); it.hasNext(); ) {
            campos.add(it.next());
        }
        var enCarga = new ArrayList<String>();
        carga.fieldNames().forEachRemaining(enCarga::add);
        assertThat(enCarga)
                .as("la carga trae exactamente los campos del pacto")
                .containsExactlyInAnyOrderElementsOf(campos);
        pacto.path("consumidorExige")
                .forEach(c -> assertThat(carga.path(c.asText()).asText()).isNotBlank());
        assertThat(pacto.path("campos").path("motivo").asText())
                .contains(carga.path("motivo").asText());
        // El ejemplo del pacto tiene la misma forma que lo que se emite.
        var enEjemplo = new ArrayList<String>();
        pacto.path("ejemplo").fieldNames().forEachRemaining(enEjemplo::add);
        assertThat(enEjemplo).containsExactlyInAnyOrderElementsOf(enCarga);
    }

    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("contratos/pactos"))) {
            p = p.getParent();
        }
        return p;
    }
}
