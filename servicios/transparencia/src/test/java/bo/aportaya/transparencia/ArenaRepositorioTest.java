package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import bo.aportaya.transparencia.aplicacion.ListarArenas;
import bo.aportaya.transparencia.dominio.ArenasDeReputacion;
import bo.aportaya.transparencia.infraestructura.ArenaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import bo.aportaya.transparencia.infraestructura.ReputacionRepositorio;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Arenas de reputacion (2026-10-30) contra PostgreSQL real: el catalogo sale de la SEMILLA
 * ({@code sql/60_semillas/16-reputacion-y-scoring.sql}, la misma que va a produccion) y el
 * puntaje se escribe como lo deja CU-71, con sus componentes cuadrando (R-REP-03).
 *
 * <p>Los tres niveles del contrato: correcto (un puntaje cualquiera), limite (549,99 · 550 ·
 * 1000 · 0 y sin historial) e invalido (la base rechaza solapes, tramos al reves, cupos y
 * monedas fuera del contrato; un modelo sin arenas deja la arena ausente). Datos sinteticos.
 */
class ArenaRepositorioTest extends BaseDeTransparencia {

    private ConsultarPuntaje puntajes;
    private ListarArenas arenas;

    @BeforeEach
    void preparar() {
        fixtura.catalogoDeReputacion();
        var datos = new Datos(dsl);
        var repositorio = new ArenaRepositorio();
        puntajes = new ConsultarPuntaje(
                datos, new ReputacionRepositorio(), new ModeloRepositorio(), repositorio, Reloj.delSistema());
        arenas = new ListarArenas(datos, new ModeloRepositorio(), repositorio, Reloj.delSistema());
    }

    /**
     * Un puntaje vigente de UN componente que lo explica entero, en una transaccion: el
     * disparador diferido de R-REP-03 exige que el total cuadre al confirmar.
     */
    private void puntaje(UUID usuario, String valor, UUID modeloId, String version) {
        transaccion.execute(s -> {
            UUID id = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO transparencia.puntaje_reputacion
                        (id, usuario_id, modelo_id, puntaje, nivel_confianza, indice_puntualidad,
                         tasa_incumplimiento, monto_total_aportado, grupos_completados, grupos_abandonados,
                         incumplimientos_abiertos, antiguedad_meses, eventos_considerados,
                         modelo_version, vigente_desde, calculado_en, proximo_recalculo_en)
                    VALUES (?, ?, ?, ?::numeric, 'BASICO', 90.00, 0.00, 1000.00, 1, 0, 0, 6, 10,
                            ?, now(), now(), now() + interval '30 days')
                    """,
                    id,
                    usuario,
                    modeloId,
                    valor,
                    version);
            dsl.execute(
                    """
                    INSERT INTO transparencia.componente_score
                        (puntaje_id, codigo_factor, valor_crudo, valor_normalizado, contribucion, tendencia)
                    VALUES (?, 'PUNTUALIDAD_DE_APORTE', 1, 0.5, ?::numeric, 'ESTABLE')
                    """,
                    id,
                    valor);
            return null;
        });
    }

    /**
     * Como corre en produccion: con {@code svc_transparencia} (miembro de {@code rol_aplicacion},
     * sujeto a la politica de fila) y una sesion PARTICIPANTE. El usuario dueño del contenedor se
     * saltea la RLS, y con el las pruebas pasaban mientras el servicio real no veia ningun puntaje.
     */
    private ConsultarPuntaje.Puntaje consultar(UUID usuario) {
        return consultarComo(usuario, usuario);
    }

    /** {@code quien} pregunta por el puntaje de {@code usuario}, con el rol real del servicio. */
    private ConsultarPuntaje.Puntaje consultarComo(UUID quien, UUID usuario) {
        return transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_transparencia");
            return puntajes.ejecutar(usuario, contextoDe(quien));
        });
    }

    // --- correcto -------------------------------------------------------------------------

    @Test
    @DisplayName("arenas · el catálogo de la semilla: 11 arenas en orden, contiguas de 0 a 1000")
    void catalogo() {
        var lista = transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_transparencia");
            return arenas.ejecutar(contextoDe(UUID.randomUUID()));
        });

        assertThat(lista)
                .extracting(ArenasDeReputacion.Arena::codigo)
                .containsExactly(
                        "CAMPAMENTO",
                        "SEMILLA",
                        "BROTE",
                        "AGUAYO",
                        "FERIA",
                        "MERCADO",
                        "ALTIPLANO",
                        "SALAR",
                        "ILLIMANI",
                        "CONDOR",
                        "INTI");
        assertThat(lista.get(0).desde()).isEqualByComparingTo("0");
        assertThat(lista.get(10).hasta()).isEqualByComparingTo("1000");
        for (int i = 1; i < lista.size(); i++) {
            assertThat(lista.get(i).desde())
                    .as(
                            "sin hueco entre %s y %s",
                            lista.get(i - 1).codigo(), lista.get(i).codigo())
                    .isEqualByComparingTo(lista.get(i - 1).hasta());
        }
        assertThat(lista.get(9).nombre()).isEqualTo("Cóndor");
    }

    @Test
    @DisplayName("arenas · los desbloqueos se siembran PROPUESTO en BOB: se muestran y no limitan")
    void desbloqueosPropuestos() {
        var lista = transaccion.execute(s -> arenas.ejecutar(contextoDe(UUID.randomUUID())));

        assertThat(lista).allSatisfy(a -> {
            assertThat(a.desbloqueo()).isNotNull();
            assertThat(a.desbloqueo().estado()).isEqualTo("PROPUESTO");
            assertThat(a.desbloqueo().vigente()).isFalse();
            assertThat(a.desbloqueo().moneda()).isEqualTo("BOB");
        });
        var semilla = lista.get(1).desbloqueo();
        assertThat(semilla.cuposMax()).isEqualTo(6);
        assertThat(semilla.aporteMax().toPlainString()).isEqualTo("200.00");
    }

    @Test
    @DisplayName("puntaje · 575 está en Brote, a mitad de camino hacia Aguayo")
    void correcto() {
        UUID ana = fixtura.usuario();
        puntaje(ana, "575.00", fixtura.modeloVigente(), "v1");

        var p = consultar(ana);

        assertThat(p.tieneHistorial()).isTrue();
        assertThat(p.arena().arena().codigo()).isEqualTo("BROTE");
        assertThat(p.arena().siguiente().codigo()).isEqualTo("AGUAYO");
        assertThat(p.arena().progreso().toPlainString()).isEqualTo("0.5000");
    }

    @Test
    @DisplayName("puntaje · con el rol real, otra persona ve la arena de quien quiere entrar (420 → Campamento)")
    void otraPersonaConElRolReal() {
        // Es el uso de grupos: el organizador decide con el puntaje de OTRA persona y su token
        // viaja en la llamada. Bajo la politica de fila (denegar por omision) esa fila no se ve,
        // y sin esta prueba alguien de Campamento pasaba por Semilla, el puntaje base.
        UUID postulante = fixtura.usuario();
        UUID organizador = fixtura.usuario();
        puntaje(postulante, "420.00", fixtura.modeloVigente(), "v1");

        var p = consultarComo(organizador, postulante);

        assertThat(p.tieneHistorial()).isTrue();
        assertThat(p.puntaje()).isEqualByComparingTo("420.00");
        assertThat(p.nivelDeConfianza()).isEqualTo("BASICO");
        assertThat(p.arena().arena().codigo()).isEqualTo("CAMPAMENTO");
        assertThat(p.arena().progreso().toPlainString()).isEqualTo("0.8400");
    }

    @Test
    @DisplayName("puntaje · el rol de sistema dura solo la lectura: la sesión sigue siendo PARTICIPANTE")
    void elRolDeSistemaNoSeFiltra() {
        UUID ana = fixtura.usuario();
        puntaje(ana, "575.00", fixtura.modeloVigente(), "v1");

        String rolDespues = transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_transparencia");
            puntajes.ejecutar(ana, contextoDe(ana));
            return dsl.fetchOne("select current_setting('app.rol', true)").get(0, String.class);
        });

        assertThat(rolDespues).isEqualTo("PARTICIPANTE");
    }

    // --- limite ---------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1} ({2}), siguiente {3}")
    @CsvSource({
        "549.99, SEMILLA, 0.9998, BROTE",
        "550.00, BROTE, 0.0000, AGUAYO",
        "1000.00, INTI, 1.0000, ''",
        "0.00, CAMPAMENTO, 0.0000, SEMILLA",
    })
    @DisplayName("puntaje · bordes: desde incluido, hasta excluido, y la última incluye el máximo")
    void bordes(String valor, String codigo, String progreso, String siguiente) {
        UUID usuario = fixtura.usuario();
        puntaje(usuario, valor, fixtura.modeloVigente(), "v1");

        var p = consultar(usuario);

        assertThat(p.puntaje()).isEqualByComparingTo(valor);
        assertThat(p.arena().arena().codigo()).isEqualTo(codigo);
        assertThat(p.arena().progreso().toPlainString()).isEqualTo(progreso);
        if (siguiente.isEmpty()) {
            assertThat(p.arena().siguiente()).isNull();
        } else {
            assertThat(p.arena().siguiente().codigo()).isEqualTo(siguiente);
        }
    }

    @Test
    @DisplayName("puntaje · sin historial: la arena del puntaje BASE (Semilla), nunca la del cero")
    void sinHistorial() {
        UUID nueva = fixtura.usuario();

        var p = consultar(nueva);

        assertThat(p.tieneHistorial()).isFalse();
        assertThat(p.nivelDeConfianza()).isEqualTo("SIN_HISTORIAL");
        assertThat(p.puntaje()).as("el contrato del puntaje no cambia").isEqualByComparingTo("0");
        assertThat(p.arena().arena().codigo()).isEqualTo("SEMILLA");
        assertThat(p.arena().progreso().toPlainString()).isEqualTo("0.0000");
        assertThat(p.arena().siguiente().codigo()).isEqualTo("BROTE");
    }

    // --- la semilla corrige el catalogo ---------------------------------------------------

    /**
     * Las dos sentencias de arenas de la semilla de produccion, tal como las genero
     * {@code scripts/generar_semillas.py} a partir de {@code seeders/minimos/16-reputacion-y-scoring.json}.
     */
    private static List<String> sentenciasDeArenas() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("sql"))) {
            actual = actual.getParent();
        }
        try {
            String archivo = Files.readString(actual.resolve("sql/60_semillas/16-reputacion-y-scoring.sql"));
            return Arrays.stream(archivo.split(";\\n"))
                    .map(String::strip)
                    .map(s -> s.replaceAll("(?m)^--.*\\n", "").strip())
                    .filter(s -> s.startsWith("INSERT INTO arena ") || s.startsWith("INSERT INTO desbloqueo_arena "))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("semilla · volver a sembrar una base ya sembrada no escribe ni una fila")
    void semillaIdempotente() {
        var sentencias = sentenciasDeArenas();
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
        // Lo que emite el generador cuando en el JSON Semilla pasa a VIGENTE y la frontera
        // Semilla/Brote sube de 550 a 560: solo cambian literales de fila. Con DO NOTHING la base
        // ya sembrada se quedaba con PROPUESTO y 550, y la corrida igual salia en verde.
        var corregidas = sentenciasDeArenas().stream()
                .map(s -> s.replace(
                                "'SEMILLA', 'Semilla', 1, '500.00', '550.00'",
                                "'SEMILLA', 'Semilla', 1, '500.00', '560.00'")
                        .replace("'BROTE', 'Brote', 2, '550.00', '600.00'", "'BROTE', 'Brote', 2, '560.00', '600.00'")
                        .replace(
                                "codigo = 'SEMILLA'), 6, '200.00', 'BOB', 'PROPUESTO')",
                                "codigo = 'SEMILLA'), 6, '200.00', 'BOB', 'VIGENTE')"))
                .toList();
        assertThat(String.join("\n", corregidas)).contains("'560.00'").contains("'VIGENTE'");

        var despues = transaccion.execute(s -> {
            int escritas = corregidas.stream().mapToInt(dsl::execute).sum();
            // R-REP-07 se verifica al confirmar: se adelanta para que hable dentro de la prueba.
            dsl.execute("SET CONSTRAINTS ALL IMMEDIATE");
            var filas = dsl.fetch(
                    """
                    SELECT a.codigo, a.desde::text AS desde, a.hasta::text AS hasta, d.estado
                      FROM transparencia.arena a JOIN transparencia.desbloqueo_arena d ON d.arena_id = a.id
                     WHERE a.codigo IN ('SEMILLA', 'BROTE') AND a.modelo_id = ?
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
                .as("escritas, Semilla.hasta, Semilla.estado, Brote.desde, Brote.estado")
                .containsExactly("3", "560.00", "VIGENTE", "560.00", "PROPUESTO");
    }

    // --- invalido -------------------------------------------------------------------------

    @Test
    @DisplayName("puntaje · un modelo sin arenas deja la arena ausente, no una inventada")
    void modeloSinArenas() {
        UUID modelo = transaccion.execute(s -> dsl.fetchOne(
                        """
                        INSERT INTO transparencia.modelo_scoring
                            (version, descripcion, puntaje_base, puntaje_minimo, puntaje_maximo,
                             factor_decaimiento_mensual, ventana_historica_meses, min_eventos_para_score,
                             vigente_desde, es_produccion)
                        VALUES ('prueba-sin-arenas', 'Modelo de prueba sin catalogo de arenas', 500, 0, 1000,
                                0.98, 36, 3, now(), FALSE)
                        ON CONFLICT (version) DO UPDATE SET descripcion = EXCLUDED.descripcion
                        RETURNING id
                        """)
                .get(0, UUID.class));
        UUID usuario = fixtura.usuario();
        puntaje(usuario, "700.00", modelo, "prueba-sin-arenas");

        var p = consultar(usuario);

        assertThat(p.tieneHistorial()).isTrue();
        assertThat(p.arena()).isNull();
    }

    @Test
    @DisplayName("R-REP-07 · la base rechaza una arena que se pisa con otra del mismo modelo")
    void solape() {
        assertThat(rechazaAlConfirmar(
                        """
                        INSERT INTO transparencia.arena (modelo_id, codigo, nombre, orden, desde, hasta)
                        VALUES (?, 'INTRUSA', 'Intrusa', 99, 540.00, 560.00)
                        """,
                        fixtura.modeloVigente()))
                .contains("ex_arena_sin_solape");
    }

    @Test
    @DisplayName("R-REP-07 · la base rechaza un tramo al revés o vacío")
    void tramoAlReves() {
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO transparencia.arena (modelo_id, codigo, nombre, orden, desde, hasta)
                        VALUES (?, 'AL_REVES', 'Al reves', 98, 1200.00, 1100.00)
                        """,
                        fixtura.modeloVigente()))
                .contains("ck_arena_tramo");
    }

    @Test
    @DisplayName("desbloqueo · la base rechaza cupos fuera de 3..40 y una moneda fuera del contrato")
    void desbloqueoFueraDelContrato() {
        UUID modelo = fixtura.modeloVigente();
        String cupos =
                """
                UPDATE transparencia.desbloqueo_arena SET cupos_max = ?
                 WHERE arena_id = (SELECT id FROM transparencia.arena WHERE codigo = 'INTI' AND modelo_id = ?)
                """;
        String moneda =
                """
                UPDATE transparencia.desbloqueo_arena SET moneda = ?
                 WHERE arena_id = (SELECT id FROM transparencia.arena WHERE codigo = 'INTI' AND modelo_id = ?)
                """;

        assertThat(rechazaLaBase(cupos, 41, modelo)).contains("ck_desbloqueo_arena_cupos_max");
        assertThat(rechazaLaBase(cupos, 2, modelo)).contains("ck_desbloqueo_arena_cupos_max");
        assertThat(rechazaLaBase(moneda, "EUR", modelo)).contains("ck_desbloqueo_arena_moneda");
    }

    @Test
    @DisplayName("desbloqueo · la base rechaza un estado que no es PROPUESTO ni VIGENTE")
    void estadoFueraDelContrato() {
        assertThat(rechazaLaBase(
                        """
                        UPDATE transparencia.desbloqueo_arena SET estado = 'APLICADO'
                         WHERE arena_id = (SELECT id FROM transparencia.arena WHERE codigo = 'SEMILLA' AND modelo_id = ?)
                        """,
                        fixtura.modeloVigente()))
                .contains("ck_desbloqueo_arena_estado");
    }
}
