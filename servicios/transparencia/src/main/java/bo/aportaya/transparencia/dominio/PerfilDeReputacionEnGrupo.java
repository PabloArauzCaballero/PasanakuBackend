package bo.aportaya.transparencia.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * La reputacion de un integrante tal como la ve otro integrante del mismo grupo (2026-10-21). Sin montos. Si no hay
 * puntaje, {@code factores} esta vacio: quien no tiene historial no tiene ceros.
 *
 * <p>Vive en el dominio y no junto al repositorio que la lee, por la misma razon que {@link IndicadoresDeReputacion}:
 * la capa web la traduce y no puede depender de {@code infraestructura} ({@code ArquitecturaTest}).
 */
public record PerfilDeReputacionEnGrupo(Optional<Factores> factores, List<Insignia> insignias) {

    /**
     * El mismo perfil sin el puntaje exacto, para quien no es su dueño (decision del dueño 2026-10-30, «Privacidad del
     * puntaje»; regla 90.2.8). El resto de las cifras y las insignias siguen: son las que el perfil del integrante
     * muestra desde el 2026-10-21.
     */
    public PerfilDeReputacionEnGrupo sinPuntaje() {
        return new PerfilDeReputacionEnGrupo(factores.map(Factores::sinPuntaje), insignias);
    }

    public record Factores(
            BigDecimal puntaje,
            String nivelConfianza,
            BigDecimal indicePuntualidad,
            int gruposCompletados,
            int gruposAbandonados,
            int incumplimientosAbiertos,
            int antiguedadMeses) {

        /** {@code puntaje} en null: el contrato lo admite ({@code [string, 'null']}) y la salida lo omite. */
        public Factores sinPuntaje() {
            return new Factores(
                    null,
                    nivelConfianza,
                    indicePuntualidad,
                    gruposCompletados,
                    gruposAbandonados,
                    incumplimientosAbiertos,
                    antiguedadMeses);
        }
    }

    public record Insignia(String codigo, String nombre, String descripcion, OffsetDateTime otorgadaEn) {}
}
