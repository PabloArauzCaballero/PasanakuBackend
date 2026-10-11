package bo.aportaya.garantia;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.DSLContext;

/**
 * Las filas del respaldo empresarial. Todo lo que dice «tope» es SINTETICO: el tope real
 * es una decision de gobierno que nadie ha tomado (ambiguedad A2 del plan del carril C).
 */
class FixturaDeRespaldo {

    private static final AtomicInteger NUMERO_DE_CUPO = new AtomicInteger(100);
    private static final AtomicInteger ORDEN = new AtomicInteger(100);
    private static final AtomicInteger NUMERO_DE_PERIODO = new AtomicInteger(100);

    private final DSLContext dsl;
    private final FixturaDeGarantia base;

    FixturaDeRespaldo(DSLContext dsl, FixturaDeGarantia base) {
        this.dsl = dsl;
        this.base = base;
    }

    /**
     * La capacidad GENERAL en BOB con exactamente `holgura` libre.
     *
     * <p>Las reservas y los movimientos son append-only y no se borran entre pruebas, asi
     * que el comprometido de la fila compartida crece. En vez de fingir una base virgen,
     * cada prueba fija el tope en `comprometido + holgura`: lo que la prueba mide es el
     * disponible, y ese queda determinista.
     */
    UUID capacidad(String holgura) {
        boolean existe = dsl.fetchExists(
                org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("garantia", "capacidad_respaldo")),
                org.jooq
                        .impl
                        .DSL
                        .field("ambito")
                        .eq("GENERAL")
                        .and(org.jooq.impl.DSL.field("moneda").eq("BOB")));
        if (!existe) {
            dsl.execute(
                    """
                    INSERT INTO garantia.capacidad_respaldo
                        (id, ambito, moneda, monto_tope, monto_comprometido, responsable_id, vigente_desde, version)
                    VALUES (gen_random_uuid(), 'GENERAL', 'BOB', 0, 0, ?, now(), 0)
                    """,
                    base.usuario());
        }
        dsl.execute(
                "UPDATE garantia.capacidad_respaldo SET monto_tope = monto_comprometido + ? WHERE ambito = 'GENERAL' AND moneda = 'BOB'",
                new BigDecimal(holgura));
        return dsl.fetchOne("SELECT id FROM garantia.capacidad_respaldo WHERE ambito = 'GENERAL' AND moneda = 'BOB'")
                .get(0, UUID.class);
    }

    BigDecimal comprometido() {
        return dsl.fetchOne(
                        "SELECT monto_comprometido FROM garantia.capacidad_respaldo WHERE ambito = 'GENERAL' AND moneda = 'BOB'")
                .get(0, BigDecimal.class);
    }

    /**
     * Un turno del grupo. Hay uno por periodo (uq_turno_periodo): el primero usa el periodo
     * del escenario y los siguientes traen el suyo.
     */
    UUID turno(FixturaDeGarantia.Escenario e) {
        boolean ocupado = dsl.fetchExists(
                org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("grupos", "turno")),
                org.jooq.impl.DSL.field("periodo_id", UUID.class).eq(e.periodoId()));
        UUID periodo = e.periodoId();
        if (ocupado) {
            periodo = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO grupos.periodo
                        (id, grupo_id, numero, fecha_inicio, fecha_limite_pago, fecha_fin_gracia,
                         fecha_entrega_prevista, estado, monto_objetivo, monto_recaudado, cupos_morosos)
                    VALUES (?, ?, ?, current_date - 60, current_date - 30, current_date - 27,
                            current_date - 25, 'ABIERTO', 1500.00, 1000.00, 1)
                    """,
                    periodo,
                    e.grupoId(),
                    (short) NUMERO_DE_PERIODO.incrementAndGet());
        }
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO grupos.turno
                    (id, grupo_id, periodo_id, cupo_id, orden_asignado, estado, criterio_asignacion,
                     monto_estimado_cobro)
                VALUES (?, ?, ?, ?, ?, 'PROGRAMADO', 'SORTEO', 1500.00)
                """,
                id,
                e.grupoId(),
                periodo,
                e.cupoId(),
                (short) ORDEN.incrementAndGet());
        return id;
    }

    /** Otra obligacion impaga del mismo periodo: un cupo y un participante propios. */
    UUID otraObligacion(FixturaDeGarantia fixtura, FixturaDeGarantia.Escenario e, String monto) {
        UUID participante = fixtura.otroParticipante(e.grupoId());
        UUID cupo = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                VALUES (?, ?, ?, ?, 'OCUPADO', 1.0, now())
                """,
                cupo,
                e.grupoId(),
                (short) NUMERO_DE_CUPO.incrementAndGet(),
                participante);
        UUID obligacion = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO aportes.obligacion_aporte
                    (id, grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado,
                     moneda, monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia,
                     estado, fecha_vencimiento, fecha_fin_gracia, dias_mora, version)
                VALUES (?, ?, ?, ?, ?, 'APORTE_PERIODICO', ?, 'BOB', 0, 0, 0, 0,
                        'EN_MORA', current_date - 30, current_date - 27, 30, 0)
                """,
                obligacion,
                e.grupoId(),
                e.periodoId(),
                cupo,
                participante,
                new BigDecimal(monto));
        return obligacion;
    }

    /** Un pago acreditado: lo que el aporte tardio trae. Vive en `aportes`. */
    UUID pago(UUID obligacionId, String monto) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO aportes.pago
                    (id, obligacion_id, monto, moneda, monto_comision_proveedor, monto_neto_acreditado,
                     canal, estado, fecha_hora_pago, fecha_hora_acreditacion, referencia_proveedor,
                     es_manual, clave_idempotencia)
                VALUES (?, ?, ?, 'BOB', 0, ?, 'QR_INTEROPERABLE', 'ACREDITADO', now(), now(), ?, false, ?)
                """,
                id,
                obligacionId,
                new BigDecimal(monto),
                new BigDecimal(monto),
                "ref-" + id,
                "pago-" + id);
        return id;
    }
}
