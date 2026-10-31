package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.aplicacion.ConsultarPuntaje;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import bo.aportaya.transparencia.infraestructura.ReputacionRepositorio;
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
 * 1000 · 0 y sin historial) e invalido (el puntaje ajeno se niega; un modelo sin etapas deja la
 * etapa ausente). Datos sinteticos.
 *
 * <p>Este archivo cubre el puntaje propio y su etapa. La etapa de otras personas vive en
 * {@link EtapasDePersonasRepositorioTest}; el catalogo, la semilla y lo que la base rechaza, en
 * {@link EtapaCatalogoRepositorioTest}.
 */
class EtapaRepositorioTest extends BaseDeEtapas {

    private ConsultarPuntaje puntajes;

    @BeforeEach
    void preparar() {
        fixtura.catalogoDeReputacion();
        puntajes = new ConsultarPuntaje(
                new Datos(dsl),
                new ReputacionRepositorio(),
                new ModeloRepositorio(),
                new EtapaRepositorio(),
                Reloj.delSistema());
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
}
