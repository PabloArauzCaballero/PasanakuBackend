package bo.aportaya.aportes.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * El calendario de obligaciones periodicas de un periodo: crear la de cada cupo UNA vez y leerlas.
 *
 * <p>La unicidad la decide la base (R-GRP-03: {@code uq_obligacion_periodo_cupo}), no un SELECT previo:
 * entre el SELECT y el INSERT entra el duplicado. {@code ON CONFLICT DO NOTHING} deja pasar al
 * segundo pedido sin error y sin segunda fila.
 */
@Component
public class CalendarioDeObligacionesRepositorio {

    public record Nueva(
            UUID grupoId,
            UUID periodoId,
            UUID cupoId,
            UUID participanteId,
            Optional<UUID> politicaMoraId,
            Dinero monto,
            LocalDate vencimiento,
            LocalDate finDeGracia) {}

    public record Fila(
            UUID obligacionId,
            UUID cupoId,
            UUID participanteId,
            Dinero monto,
            String estado,
            LocalDate vencimiento,
            LocalDate finDeGracia) {}

    /** El id de la obligacion creada, o vacio si ese cupo ya tenia la suya en este periodo. */
    public Optional<UUID> crear(DSLContext dsl, Nueva n) {
        return dsl.fetchOptional(
                        """
                        INSERT INTO aportes.obligacion_aporte
                            (id, grupo_id, periodo_id, cupo_id, participante_id, politica_mora_id, tipo,
                             monto_esperado, moneda, monto_pagado, monto_recargo, monto_condonado,
                             monto_cubierto_garantia, estado, fecha_vencimiento, fecha_fin_gracia, dias_mora, version)
                        VALUES (?, ?, ?, ?, ?, ?, 'APORTE_PERIODICO',
                                ?, ?, 0, 0, 0, 0, 'PENDIENTE', ?, ?, 0, 0)
                        ON CONFLICT DO NOTHING
                        RETURNING id
                        """,
                        DSL.val(UUID.randomUUID(), SQLDataType.UUID),
                        DSL.val(n.grupoId(), SQLDataType.UUID),
                        DSL.val(n.periodoId(), SQLDataType.UUID),
                        DSL.val(n.cupoId(), SQLDataType.UUID),
                        DSL.val(n.participanteId(), SQLDataType.UUID),
                        DSL.val(n.politicaMoraId().orElse(null), SQLDataType.UUID),
                        DSL.val(n.monto().monto(), SQLDataType.NUMERIC(14, 2)),
                        DSL.val(n.monto().moneda().name(), SQLDataType.CHAR(3)),
                        DSL.val(n.vencimiento(), SQLDataType.LOCALDATE),
                        DSL.val(n.finDeGracia(), SQLDataType.LOCALDATE))
                .map(r -> r.get(0, UUID.class));
    }

    /** Todas las obligaciones periodicas vigentes del periodo, en un orden estable. */
    public List<Fila> delPeriodo(DSLContext dsl, UUID periodoId) {
        return dsl.fetch(
                        """
                        SELECT id, cupo_id, participante_id, monto_esperado, moneda, estado,
                               fecha_vencimiento, fecha_fin_gracia
                          FROM aportes.obligacion_aporte
                         WHERE periodo_id = ? AND tipo = 'APORTE_PERIODICO' AND estado <> 'ANULADO'
                         ORDER BY cupo_id, id
                        """,
                        periodoId)
                .map(r -> new Fila(
                        r.get("id", UUID.class),
                        r.get("cupo_id", UUID.class),
                        r.get("participante_id", UUID.class),
                        Dinero.de(
                                r.get("monto_esperado", BigDecimal.class),
                                Moneda.valueOf(r.get("moneda", String.class))),
                        r.get("estado", String.class),
                        r.get("fecha_vencimiento", LocalDate.class),
                        r.get("fecha_fin_gracia", LocalDate.class)));
    }
}
