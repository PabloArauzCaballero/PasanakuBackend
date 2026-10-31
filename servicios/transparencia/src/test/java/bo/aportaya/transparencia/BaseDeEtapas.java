package bo.aportaya.transparencia;

import java.util.UUID;

/**
 * Lo comun a las pruebas de etapas que necesitan un puntaje escrito: lo deja como CU-71, con sus
 * componentes cuadrando (R-REP-03). Las pruebas viven en {@link EtapaRepositorioTest} (el puntaje
 * propio y sus bordes) y {@link EtapasDePersonasTest} (la etapa de otras personas, para grupos).
 */
abstract class BaseDeEtapas extends BaseDeTransparencia {

    /**
     * Un puntaje vigente de UN componente que lo explica entero, en una transaccion: el
     * disparador diferido de R-REP-03 exige que el total cuadre al confirmar.
     */
    protected void puntaje(UUID usuario, String valor, UUID modeloId, String version) {
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
}
