package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.aplicacion.ListarEtapas;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El catalogo de etapas (2026-10-30) contra PostgreSQL real: sale de la @@INICIAL@@
 * ({@code sql/60_semillas/16-reputacion-y-scoring.sql}, la misma que va a produccion), la semilla
 * es idempotente y corrige una base ya sembrada, y la base rechaza solapes (R-REP-07), tramos al
 * reves, cupos, monedas y estados fuera del contrato. Datos sinteticos.
 */
class EtapaCatalogoTest extends BaseDeTransparencia {

    private ListarEtapas etapas;

    @BeforeEach
    void preparar() {
        fixtura.catalogoDeReputacion();
        etapas = new ListarEtapas(new Datos(dsl), new ModeloRepositorio(), new EtapaRepositorio(), Reloj.delSistema());
    }

    @Test
    @DisplayName("etapas · el catálogo de la semilla: 11 etapas en orden, contiguas de 0 a 1000")
    void catalogo() {
        var lista = transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_transparencia");
            return etapas.ejecutar(contextoDe(UUID.randomUUID()));
        });

        assertThat(lista)
                .extracting(EtapasDeReputacion.Etapa::codigo)
                .containsExactly(
                        "RECUPERACION",
                        "INICIAL",
                        "EN_MARCHA",
                        "CONSTANTE",
                        "PUNTUAL",
                        "COMPROMETIDO",
                        "SOLIDO",
                        "DESTACADO",
                        "EXPERTO",
                        "EJEMPLAR",
                        "PILAR");
        assertThat(lista.get(0).desde()).isEqualByComparingTo("0");
        assertThat(lista.get(10).hasta()).isEqualByComparingTo("1000");
        for (int i = 1; i < lista.size(); i++) {
            assertThat(lista.get(i).desde())
                    .as(
                            "sin hueco entre %s y %s",
                            lista.get(i - 1).codigo(), lista.get(i).codigo())
                    .isEqualByComparingTo(lista.get(i - 1).hasta());
        }
        assertThat(lista.get(9).nombre()).isEqualTo("Etapa 9 · Ejemplar");
    }

    @Test
    @DisplayName("etapas · los desbloqueos se siembran PROPUESTO en BOB: se muestran y no limitan")
    void desbloqueosPropuestos() {
        var lista = transaccion.execute(s -> etapas.ejecutar(contextoDe(UUID.randomUUID())));

        assertThat(lista).allSatisfy(a -> {
            assertThat(a.desbloqueo()).isNotNull();
            assertThat(a.desbloqueo().estado()).isEqualTo("PROPUESTO");
            assertThat(a.desbloqueo().vigente()).isFalse();
            assertThat(a.desbloqueo().moneda()).isEqualTo("BOB");
        });
        var inicial = lista.get(1).desbloqueo();
        assertThat(inicial.cuposMax()).isEqualTo(6);
        assertThat(inicial.aporteMax().toPlainString()).isEqualTo("200.00");
    }

    // --- la semilla corrige el catalogo ---------------------------------------------------

    /**
     * Las dos sentencias de etapas de la semilla de produccion, tal como las genero
     * {@code scripts/generar_semillas.py} a partir de {@code seeders/minimos/16-reputacion-y-scoring.json}.
     */
    private static List<String> sentenciasDeEtapas() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("sql"))) {
            actual = actual.getParent();
        }
        try {
            String archivo = Files.readString(actual.resolve("sql/60_semillas/16-reputacion-y-scoring.sql"));
            return Arrays.stream(archivo.split(";\\n"))
                    .map(String::strip)
                    .map(s -> s.replaceAll("(?m)^--.*\\n", "").strip())
                    .filter(s -> s.startsWith("INSERT INTO etapa ") || s.startsWith("INSERT INTO desbloqueo_etapa "))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("semilla · volver a sembrar una base ya sembrada no escribe ni una fila")
    void semillaIdempotente() {
        var sentencias = sentenciasDeEtapas();
        assertThat(sentencias).hasSize(2);

        int escritas = transaccion.execute(s -> {
            int total = sentencias.stream().mapToInt(dsl::execute).sum();
            s.setRollbackOnly();
            return total;
        });

        assertThat(escritas).isZero();
    }

    @Test
    @DisplayName("semilla · un desbloqueo que pasa a VIGENTE y una frontera que sube llegan a una base ya sembrada")
    void semillaCorrigeElCatalogo() {
        // Lo que emite el generador cuando en el JSON Inicial pasa a VIGENTE y la frontera
        // Inicial/En marcha sube de 550 a 560: solo cambian literales de fila. Con DO NOTHING la base
        // ya sembrada se quedaba con PROPUESTO y 550, y la corrida igual salia en verde.
        var corregidas = sentenciasDeEtapas().stream()
                .map(s -> s.replace(
                                "'INICIAL', 'Etapa 1 · Inicial', 1, '500.00', '550.00'",
                                "'INICIAL', 'Etapa 1 · Inicial', 1, '500.00', '560.00'")
                        .replace(
                                "'EN_MARCHA', 'Etapa 2 · En marcha', 2, '550.00', '600.00'",
                                "'EN_MARCHA', 'Etapa 2 · En marcha', 2, '560.00', '600.00'")
                        .replace(
                                "codigo = 'INICIAL'), 6, '200.00', 'BOB', 'PROPUESTO')",
                                "codigo = 'INICIAL'), 6, '200.00', 'BOB', 'VIGENTE')"))
                .toList();
        assertThat(String.join("\n", corregidas)).contains("'560.00'").contains("'VIGENTE'");

        var despues = transaccion.execute(s -> {
            int escritas = corregidas.stream().mapToInt(dsl::execute).sum();
            // R-REP-07 se verifica al confirmar: se adelanta para que hable dentro de la prueba.
            dsl.execute("SET CONSTRAINTS ALL IMMEDIATE");
            var filas = dsl.fetch(
                    """
                    SELECT a.codigo, a.desde::text AS desde, a.hasta::text AS hasta, d.estado
                      FROM transparencia.etapa a JOIN transparencia.desbloqueo_etapa d ON d.etapa_id = a.id
                     WHERE a.codigo IN ('INICIAL', 'EN_MARCHA') AND a.modelo_id = ?
                     ORDER BY a.orden
                    """,
                    fixtura.modeloVigente());
            s.setRollbackOnly();
            return List.of(
                    String.valueOf(escritas),
                    filas.get(0).get("hasta", String.class),
                    filas.get(0).get("estado", String.class),
                    filas.get(1).get("desde", String.class),
                    filas.get(1).get("estado", String.class));
        });

        assertThat(despues)
                .as("escritas, Inicial.hasta, Inicial.estado, EnMarcha.desde, EnMarcha.estado")
                .containsExactly("3", "560.00", "VIGENTE", "560.00", "PROPUESTO");
    }

    // --- invalido -------------------------------------------------------------------------

    @Test
    @DisplayName("R-REP-07 · la base rechaza una etapa que se pisa con otra del mismo modelo")
    void solape() {
        assertThat(rechazaAlConfirmar(
                        """
                        INSERT INTO transparencia.etapa (modelo_id, codigo, nombre, orden, desde, hasta)
                        VALUES (?, 'INTRUSA', 'Intrusa', 99, 540.00, 560.00)
                        """,
                        fixtura.modeloVigente()))
                .contains("ex_etapa_sin_solape");
    }

    @Test
    @DisplayName("R-REP-07 · la base rechaza un tramo al revés o vacío")
    void tramoAlReves() {
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO transparencia.etapa (modelo_id, codigo, nombre, orden, desde, hasta)
                        VALUES (?, 'AL_REVES', 'Al reves', 98, 1200.00, 1100.00)
                        """,
                        fixtura.modeloVigente()))
                .contains("ck_etapa_tramo");
    }

    @Test
    @DisplayName("desbloqueo · la base rechaza cupos fuera de 3..40 y una moneda fuera del contrato")
    void desbloqueoFueraDelContrato() {
        UUID modelo = fixtura.modeloVigente();
        String cupos =
                """
                UPDATE transparencia.desbloqueo_etapa SET cupos_max = ?
                 WHERE etapa_id = (SELECT id FROM transparencia.etapa WHERE codigo = 'PILAR' AND modelo_id = ?)
                """;
        String moneda =
                """
                UPDATE transparencia.desbloqueo_etapa SET moneda = ?
                 WHERE etapa_id = (SELECT id FROM transparencia.etapa WHERE codigo = 'PILAR' AND modelo_id = ?)
                """;

        assertThat(rechazaLaBase(cupos, 41, modelo)).contains("ck_desbloqueo_etapa_cupos_max");
        assertThat(rechazaLaBase(cupos, 2, modelo)).contains("ck_desbloqueo_etapa_cupos_max");
        assertThat(rechazaLaBase(moneda, "EUR", modelo)).contains("ck_desbloqueo_etapa_moneda");
    }

    @Test
    @DisplayName("desbloqueo · la base rechaza un estado que no es PROPUESTO ni VIGENTE")
    void estadoFueraDelContrato() {
        assertThat(rechazaLaBase(
                        """
                        UPDATE transparencia.desbloqueo_etapa SET estado = 'APLICADO'
                         WHERE etapa_id = (SELECT id FROM transparencia.etapa WHERE codigo = 'INICIAL' AND modelo_id = ?)
                        """,
                        fixtura.modeloVigente()))
                .contains("ck_desbloqueo_etapa_estado");
    }
}
