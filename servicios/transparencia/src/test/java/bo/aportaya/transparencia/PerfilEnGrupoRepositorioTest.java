package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.transparencia.aplicacion.ConsultarPerfilEnGrupo;
import bo.aportaya.transparencia.dominio.PerfilDeReputacionEnGrupo;
import bo.aportaya.transparencia.infraestructura.PerfilEnGrupoRepositorio;
import bo.aportaya.transparencia.infraestructura.clientes.GruposPorHttp;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Perfil del integrante (2026-10-21): la reputacion de alguien contra PostgreSQL real, detras de la respuesta de grupos.
 * Grupos es un DOBLE declarado ({@code compartenGrupo}); la forma de su contrato la cubre
 * {@code GruposPorHttpPactoContratoTest}. Las insignias y el modelo salen de la semilla de la boveda. Datos sinteticos.
 */
class PerfilEnGrupoRepositorioTest extends BaseDeTransparencia {

    private PerfilEnGrupoRepositorio repositorio;

    @BeforeEach
    void preparar() {
        fixtura.catalogoDeReputacion();
        repositorio = new PerfilEnGrupoRepositorio(new Datos(dsl));
    }

    /** Grupos como doble: responde lo que la prueba diga, sin red. */
    private static final class GruposDoble extends GruposPorHttp {
        private final boolean comparten;

        GruposDoble(boolean comparten) {
            super(org.springframework.web.client.RestClient.builder(), "http://no-se-usa");
            this.comparten = comparten;
        }

        @Override
        public boolean compartenGrupo(UUID grupoId, UUID usuarioId) {
            return comparten;
        }
    }

    private static ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private PerfilDeReputacionEnGrupo consultar(boolean comparten, UUID quienPregunta, UUID otro) {
        var caso = new ConsultarPerfilEnGrupo(new GruposDoble(comparten), repositorio);
        return transaccion.execute(s -> caso.ejecutar(UUID.randomUUID(), otro, de(quienPregunta)));
    }

    /**
     * Un puntaje vigente con sus dos componentes, en UNA transaccion: R-REP-03 (disparador diferido) exige que el total
     * cuadre con la suma de las contribuciones al confirmar. Los codigos de factor son los de la semilla.
     */
    private void puntaje(UUID usuario) {
        transaccion.execute(s -> {
            UUID id = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO transparencia.puntaje_reputacion
                        (id, usuario_id, modelo_id, puntaje, nivel_confianza, indice_puntualidad,
                         tasa_incumplimiento, monto_total_aportado, grupos_completados, grupos_abandonados,
                         incumplimientos_abiertos, antiguedad_meses, eventos_considerados,
                         modelo_version, vigente_desde, calculado_en, proximo_recalculo_en)
                    VALUES (?, ?, ?, 812.50, 'CONFIABLE', 96.40, 1.20, 12000.00, 3, 1, 0, 14, 42,
                            'v1', now(), now(), now() + interval '30 days')
                    """,
                    id,
                    usuario,
                    fixtura.modeloVigente());
            for (Object[] c : new Object[][] {{"PUNTUALIDAD_DE_APORTE", "500.00"}, {"CICLOS_COMPLETADOS", "312.50"}}) {
                dsl.execute(
                        """
                        INSERT INTO transparencia.componente_score
                            (puntaje_id, codigo_factor, valor_crudo, valor_normalizado, contribucion, tendencia)
                        VALUES (?, ?, 1, 0.5, ?::numeric, 'ESTABLE')
                        """,
                        id,
                        c[0],
                        c[1]);
            }
            return null;
        });
    }

    private void insignia(UUID usuario, String codigo, boolean revocada) {
        dsl.execute(
                """
                INSERT INTO transparencia.insignia_otorgada (usuario_id, insignia_id, otorgada_en, revocada_en,
                                                             motivo_revocacion)
                VALUES (?, ?, now(), CASE WHEN ? THEN now() END, CASE WHEN ? THEN 'Prueba de revocacion' END)
                """,
                usuario,
                fixtura.insignia(codigo),
                revocada,
                revocada);
    }

    @Test
    @DisplayName(
            "perfil · con historial: puntaje, nivel, puntualidad, grupos, incumplimientos, antiguedad e insignias vigentes")
    void conHistorial() {
        UUID beto = fixtura.usuario();
        UUID ana = fixtura.usuario();
        puntaje(beto);
        insignia(beto, "PAGADOR_PUNTUAL", false);
        insignia(beto, "PRIMER_PASANAKU", true);

        var perfil = consultar(true, ana, beto);

        var f = perfil.factores().orElseThrow();
        assertThat(f.puntaje()).isEqualByComparingTo(new BigDecimal("812.50"));
        assertThat(f.nivelConfianza()).isEqualTo("CONFIABLE");
        assertThat(f.indicePuntualidad()).isEqualByComparingTo(new BigDecimal("96.40"));
        assertThat(f.gruposCompletados()).isEqualTo(3);
        assertThat(f.gruposAbandonados()).isEqualTo(1);
        assertThat(f.incumplimientosAbiertos()).isZero();
        assertThat(f.antiguedadMeses()).isEqualTo(14);
        assertThat(perfil.insignias())
                .as("la revocada no aparece")
                .extracting(PerfilDeReputacionEnGrupo.Insignia::codigo)
                .containsExactly("PAGADOR_PUNTUAL");
    }

    @Test
    @DisplayName("perfil · sin historial: no hay factores (no ceros) y las insignias de otra persona no se cuelan")
    void sinHistorial() {
        UUID nueva = fixtura.usuario();
        UUID otra = fixtura.usuario();
        insignia(otra, "TRES_CICLOS", false);

        var perfil = consultar(true, otra, nueva);

        assertThat(perfil.factores()).isEmpty();
        assertThat(perfil.insignias()).isEmpty();
    }

    @Test
    @DisplayName("rechaza · si grupos dice que no comparten (o no responde), es 403 y no se lee nada")
    void noCompartenEs403() {
        UUID beto = fixtura.usuario();
        puntaje(beto);

        assertThatThrownBy(() -> consultar(false, fixtura.usuario(), beto)).isInstanceOf(AccessDeniedException.class);
    }
}
