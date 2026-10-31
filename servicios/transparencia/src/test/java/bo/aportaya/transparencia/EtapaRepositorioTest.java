package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.transparencia.aplicacion.ConsultarEtapasDePersonas;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import bo.aportaya.transparencia.aplicacion.ListarEtapas;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
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
import org.springframework.security.access.AccessDeniedException;

/**
 * Etapas de reputacion (2026-10-30) contra PostgreSQL real: el catalogo sale de la @@INICIAL@@
 * ({@code sql/60_semillas/16-reputacion-y-scoring.sql}, la misma que va a produccion) y el
 * puntaje se escribe como lo deja CU-71, con sus componentes cuadrando (R-REP-03).
 *
 * <p>Los tres niveles del contrato: correcto (un puntaje cualquiera), limite (549,99 · 550 ·
 * 1000 · 0 y sin historial) e invalido (la base rechaza solapes, tramos al reves, cupos y
 * monedas fuera del contrato; un modelo sin etapas deja la etapa ausente). Datos sinteticos.
 */
class EtapaRepositorioTest extends BaseDeTransparencia {

    private ConsultarPuntaje puntajes;
    private ListarEtapas etapas;
    private ConsultarEtapasDePersonas etapasDePersonas;

    @BeforeEach
    void preparar() {
        fixtura.catalogoDeReputacion();
        var datos = new Datos(dsl);
        var repositorio = new EtapaRepositorio();
        puntajes = new ConsultarPuntaje(
                datos, new ReputacionRepositorio(), new ModeloRepositorio(), repositorio, Reloj.delSistema());
        etapas = new ListarEtapas(datos, new ModeloRepositorio(), repositorio, Reloj.delSistema());
        etapasDePersonas = new ConsultarEtapasDePersonas(
                datos, new ReputacionRepositorio(), new ModeloRepositorio(), repositorio, Reloj.delSistema());
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

    /**
     * Como la llama grupos en produccion: rol de base {@code svc_transparencia} y la sesion de la
     * identidad de servicio {@code SERVICIO_GRUPOS} (no una persona).
     */
    private List<ConsultarEtapasDePersonas.EtapaDePersona> etapasComoServicio(List<UUID> usuarios) {
        return transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_transparencia");
            return etapasDePersonas.ejecutar(
                    usuarios,
                    ContextoSesion.de(
                            UUID.randomUUID(),
                            "SERVICIO_GRUPOS",
                            new Traza(UUID.randomUUID().toString())));
        });
    }

    // --- correcto -------------------------------------------------------------------------

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

    @Test
    @DisplayName("puntaje · 575 está en Etapa 2 · En marcha, a mitad de camino hacia Etapa 3 · Constante")
    void correcto() {
        UUID ana = fixtura.usuario();
        puntaje(ana, "575.00", fixtura.modeloVigente(), "v1");

        var p = consultar(ana);

        assertThat(p.tieneHistorial()).isTrue();
        assertThat(p.etapa().etapa().codigo()).isEqualTo("EN_MARCHA");
        assertThat(p.etapa().siguiente().codigo()).isEqualTo("CONSTANTE");
        assertThat(p.etapa().progreso().toPlainString()).isEqualTo("0.5000");
    }

    @Test
    @DisplayName("puntaje · otro participante pide el puntaje ajeno: 403 (AccessDenied) y no se lee nada")
    void puntajeAjenoNegado() {
        // Privacidad del puntaje (decision del dueño 2026-10-30, regla 90.2.8): el puntaje exacto
        // es solo de su dueño, aunque quien pregunta comparta un grupo con esa persona. Antes de
        // este cambio, el organizador leia el 420.00 de quien queria entrar.
        UUID postulante = fixtura.usuario();
        UUID organizador = fixtura.usuario();
        puntaje(postulante, "420.00", fixtura.modeloVigente(), "v1");

        assertThatThrownBy(() -> consultarComo(organizador, postulante))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageNotContaining("420");
    }

    @Test
    @DisplayName(
            "etapa · el servicio de grupos lee la etapa de otra persona con el rol real (420 → RECUPERACION), sin puntaje")
    void servicioLeeLaEtapaDeOtro() {
        // Es el uso de grupos: decide un ingreso con la etapa de OTRA persona. Bajo la politica de
        // fila (denegar por omision) esa fila no se ve con el rol del servicio, y sin leerla como
        // sistema alguien en recuperacion pasaba por Etapa 1 · Inicial, la del puntaje base.
        UUID postulante = fixtura.usuario();
        puntaje(postulante, "420.00", fixtura.modeloVigente(), "v1");

        var etapas = etapasComoServicio(List.of(postulante));

        assertThat(etapas).hasSize(1);
        assertThat(etapas.get(0).usuarioId()).isEqualTo(postulante);
        assertThat(etapas.get(0).tieneHistorial()).isTrue();
        assertThat(etapas.get(0).etapa().codigo()).isEqualTo("RECUPERACION");
        assertThat(etapas.get(0).etapa().nombre()).isEqualTo("En recuperación");
        assertThat(etapas.get(0).etapa().desde()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "etapa · varias personas: sin historial es la del puntaje base, el repetido sale una vez y se respeta el orden")
    void servicioVariasPersonas() {
        UUID ana = fixtura.usuario();
        UUID beto = fixtura.usuario();
        UUID nueva = fixtura.usuario();
        UUID sinCuenta = UUID.randomUUID();
        puntaje(ana, "812.50", fixtura.modeloVigente(), "v1");
        puntaje(beto, "550.00", fixtura.modeloVigente(), "v1");

        var etapas = etapasComoServicio(List.of(beto, nueva, ana, beto, sinCuenta));

        assertThat(etapas)
                .extracting(ConsultarEtapasDePersonas.EtapaDePersona::usuarioId)
                .containsExactly(beto, nueva, ana, sinCuenta);
        assertThat(etapas)
                .extracting(e -> e.etapa().codigo())
                .containsExactly("EN_MARCHA", "INICIAL", "SOLIDO", "INICIAL");
        assertThat(etapas)
                .extracting(ConsultarEtapasDePersonas.EtapaDePersona::tieneHistorial)
                .containsExactly(true, false, true, false);
    }

    @Test
    @DisplayName("etapa · inválido: ninguna persona o más de 60 distintas se rechaza antes de leer")
    void servicioFueraDelContrato() {
        var sesentaYUna =
                java.util.stream.Stream.generate(UUID::randomUUID).limit(61).toList();
        var sesenta =
                java.util.stream.Stream.generate(UUID::randomUUID).limit(60).toList();

        assertThatThrownBy(() -> etapasComoServicio(List.of()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(() -> etapasComoServicio(sesentaYUna))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(etapasComoServicio(sesenta)).as("60 es el borde de arriba").hasSize(60);
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
        "499.99, RECUPERACION, 1.0000, INICIAL",
        "500.00, INICIAL, 0.0000, EN_MARCHA",
        "549.99, INICIAL, 0.9998, EN_MARCHA",
        "550.00, EN_MARCHA, 0.0000, CONSTANTE",
        "1000.00, PILAR, 1.0000, ''",
        "0.00, RECUPERACION, 0.0000, INICIAL",
    })
    @DisplayName("puntaje · bordes: desde incluido, hasta excluido, y la última incluye el máximo")
    void bordes(String valor, String codigo, String progreso, String siguiente) {
        UUID usuario = fixtura.usuario();
        puntaje(usuario, valor, fixtura.modeloVigente(), "v1");

        var p = consultar(usuario);

        assertThat(p.puntaje()).isEqualByComparingTo(valor);
        assertThat(p.etapa().etapa().codigo()).isEqualTo(codigo);
        assertThat(p.etapa().progreso().toPlainString()).isEqualTo(progreso);
        if (siguiente.isEmpty()) {
            assertThat(p.etapa().siguiente()).isNull();
        } else {
            assertThat(p.etapa().siguiente().codigo()).isEqualTo(siguiente);
        }
    }

    @Test
    @DisplayName("puntaje · sin historial: la etapa del puntaje BASE (Etapa 1 · Inicial), nunca la del cero")
    void sinHistorial() {
        UUID nueva = fixtura.usuario();

        var p = consultar(nueva);

        assertThat(p.tieneHistorial()).isFalse();
        assertThat(p.nivelDeConfianza()).isEqualTo("SIN_HISTORIAL");
        assertThat(p.puntaje()).as("el contrato del puntaje no cambia").isEqualByComparingTo("0");
        assertThat(p.etapa().etapa().codigo()).isEqualTo("INICIAL");
        assertThat(p.etapa().progreso().toPlainString()).isEqualTo("0.0000");
        assertThat(p.etapa().siguiente().codigo()).isEqualTo("EN_MARCHA");
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
    @DisplayName("puntaje · un modelo sin etapas deja la etapa ausente, no una inventada")
    void modeloSinEtapas() {
        UUID modelo = transaccion.execute(s -> dsl.fetchOne(
                        """
                        INSERT INTO transparencia.modelo_scoring
                            (version, descripcion, puntaje_base, puntaje_minimo, puntaje_maximo,
                             factor_decaimiento_mensual, ventana_historica_meses, min_eventos_para_score,
                             vigente_desde, es_produccion)
                        VALUES ('prueba-sin-etapas', 'Modelo de prueba sin catalogo de etapas', 500, 0, 1000,
                                0.98, 36, 3, now(), FALSE)
                        ON CONFLICT (version) DO UPDATE SET descripcion = EXCLUDED.descripcion
                        RETURNING id
                        """)
                .get(0, UUID.class));
        UUID usuario = fixtura.usuario();
        puntaje(usuario, "700.00", modelo, "prueba-sin-etapas");

        var p = consultar(usuario);

        assertThat(p.tieneHistorial()).isTrue();
        assertThat(p.etapa()).isNull();
    }

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
