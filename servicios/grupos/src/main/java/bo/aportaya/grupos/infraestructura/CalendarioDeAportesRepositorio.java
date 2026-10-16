package bo.aportaya.grupos.infraestructura;

import bo.aportaya.grupos.dominio.CalendarioDeAportes;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Lee el periodo, el aporte del grupo y los cupos ocupados: el hecho que `aportes` pide para generar
 * obligaciones (H8.S1.M1). Solo lectura. El llamador decide con que rol se lee: la politica de fila deja a
 * cada titular ver lo suyo y este hecho enumera a todos, asi que lo lee el proceso de sistema detras del
 * permiso del endpoint.
 */
@Component
public class CalendarioDeAportesRepositorio {

    /** Vacio si el periodo no existe. Los cupos que no estan OCUPADOS (libres, reservados, en traspaso) no aportan. */
    public Optional<CalendarioDeAportes> deLosCupos(DSLContext dsl, UUID periodoId) {
        var periodo = dsl.fetchOne(
                """
                SELECT p.id, p.grupo_id, p.numero, p.estado, p.fecha_limite_pago, p.fecha_fin_gracia,
                       p.fecha_entrega_prevista, g.monto_aporte, g.moneda
                  FROM grupos.periodo p
                  JOIN grupos.grupo g ON g.id = p.grupo_id
                 WHERE p.id = ?
                """,
                periodoId);
        if (periodo == null) {
            return Optional.empty();
        }
        UUID grupoId = periodo.get("grupo_id", UUID.class);
        BigDecimal aporte = periodo.get("monto_aporte", BigDecimal.class);
        Moneda moneda = Moneda.valueOf(periodo.get("moneda", String.class).strip());
        var cupos = dsl.fetch(
                        """
                        SELECT c.id, c.participante_id, c.fraccion
                          FROM grupos.cupo c
                         WHERE c.grupo_id = ? AND c.estado = 'OCUPADO' AND c.participante_id IS NOT NULL
                         ORDER BY c.numero
                        """,
                        grupoId)
                .map(f -> new CalendarioDeAportes.Cupo(
                        f.get("id", UUID.class),
                        f.get("participante_id", UUID.class),
                        // Aporte por fraccion del cupo; half-even a la escala del dinero (ambiguedad abierta
                        // para fracciones distintas de 1: ver evidencia/carril-B3).
                        Dinero.de(
                                aporte.multiply(f.get("fraccion", BigDecimal.class))
                                        .setScale(Dinero.ESCALA, RoundingMode.HALF_EVEN),
                                moneda)));
        return Optional.of(new CalendarioDeAportes(
                periodoId,
                grupoId,
                periodo.get("numero", Integer.class),
                periodo.get("estado", String.class),
                periodo.get("fecha_limite_pago", LocalDate.class),
                periodo.get("fecha_fin_gracia", LocalDate.class),
                periodo.get("fecha_entrega_prevista", LocalDate.class),
                cupos));
    }
}
