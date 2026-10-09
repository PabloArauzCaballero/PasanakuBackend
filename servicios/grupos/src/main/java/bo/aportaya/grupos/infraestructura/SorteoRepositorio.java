package bo.aportaya.grupos.infraestructura;

import static bo.aportaya.grupos.generado.Tables.CUPO;
import static bo.aportaya.grupos.generado.Tables.SORTEO_TURNOS;
import static bo.aportaya.grupos.generado.Tables.TURNO;

import bo.aportaya.grupos.dominio.SnapshotDeSorteo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/** El sorteo y sus turnos. */
@Component
public class SorteoRepositorio {

    /** Los cupos ocupados, ordenados por numero: el punto de partida del sorteo. */
    public List<UUID> cuposPorNumero(DSLContext dsl, UUID grupoId) {
        return dsl.select(CUPO.ID)
                .from(CUPO)
                .where(CUPO.GRUPO_ID.eq(grupoId))
                .and(CUPO.ESTADO.eq("OCUPADO"))
                .orderBy(CUPO.NUMERO.asc())
                .fetch(CUPO.ID);
    }

    /** Un grupo a medio armar no se sortea: quedan cupos libres. */
    public boolean estaConformado(DSLContext dsl, UUID grupoId) {
        return dsl.fetchCount(CUPO, CUPO.GRUPO_ID.eq(grupoId).and(CUPO.ESTADO.ne("OCUPADO"))) == 0
                && dsl.fetchCount(CUPO, CUPO.GRUPO_ID.eq(grupoId)) > 0;
    }

    /** Cada participante con su reglamento aceptado, o no hay sorteo. */
    public boolean todosAceptaronElReglamento(DSLContext dsl, UUID grupoId) {
        Integer sinAceptar = (Integer) dsl.fetchOne(
                        """
                        SELECT count(*)::int FROM grupos.cupo c
                         WHERE c.grupo_id = ? AND c.participante_id IS NOT NULL
                           AND NOT EXISTS (
                             SELECT 1 FROM grupos.aceptacion_reglamento a
                              WHERE a.participante_id = c.participante_id)
                        """,
                        grupoId)
                .get(0);
        return sinAceptar != null && sinAceptar == 0;
    }

    public boolean yaHuboSorteo(DSLContext dsl, UUID grupoId) {
        return dsl.fetchExists(dsl.selectFrom(SORTEO_TURNOS)
                .where(SORTEO_TURNOS.GRUPO_ID.eq(grupoId))
                .and(SORTEO_TURNOS.ANULADO_EN.isNull()));
    }

    /** La fila es append-only: el hash publicado no admite {@code UPDATE}. */
    public UUID comprometer(
            DSLContext dsl,
            UUID grupoId,
            String hash,
            String algoritmo,
            UUID ejecutadoPor,
            OffsetDateTime ahora,
            Optional<OffsetDateTime> fechaPrevista,
            List<String> entropiasComprometidas) {
        return dsl.insertInto(SORTEO_TURNOS)
                .set(SORTEO_TURNOS.GRUPO_ID, grupoId)
                .set(SORTEO_TURNOS.ALGORITMO, algoritmo)
                .set(SORTEO_TURNOS.ESTADO, "COMPROMETIDO")
                .set(SORTEO_TURNOS.HASH_SEMILLA_PREVIO, hash)
                .set(SORTEO_TURNOS.FECHA_COMPROMISO, ahora)
                .set(SORTEO_TURNOS.FECHA_REVELADO_PREVISTA, fechaPrevista.orElse(null))
                .set(SORTEO_TURNOS.APORTES_ENTROPIA, org.jooq.JSONB.valueOf(comoJson(entropiasComprometidas)))
                .set(SORTEO_TURNOS.SEMILLA_PUBLICA, "")
                .set(SORTEO_TURNOS.RESULTADO, org.jooq.JSONB.valueOf("[]"))
                .set(SORTEO_TURNOS.EJECUTADO_POR, ejecutadoPor)
                .set(SORTEO_TURNOS.FECHA_EJECUCION, ahora)
                .returning(SORTEO_TURNOS.ID)
                .fetchOne(SORTEO_TURNOS.ID);
    }

    public Optional<Compromiso> compromisoDe(DSLContext dsl, UUID sorteoId) {
        Record fila = dsl.select(SORTEO_TURNOS.HASH_SEMILLA_PREVIO, SORTEO_TURNOS.ESTADO, SORTEO_TURNOS.GRUPO_ID)
                .from(SORTEO_TURNOS)
                .where(SORTEO_TURNOS.ID.eq(sorteoId))
                .fetchOne();
        return fila == null
                ? Optional.empty()
                : Optional.of(new Compromiso(
                        fila.get(SORTEO_TURNOS.HASH_SEMILLA_PREVIO),
                        fila.get(SORTEO_TURNOS.ESTADO),
                        fila.get(SORTEO_TURNOS.GRUPO_ID)));
    }

    public void revelar(DSLContext dsl, UUID sorteoId, String semilla, String resultadoJson, OffsetDateTime ahora) {
        dsl.update(SORTEO_TURNOS)
                .set(SORTEO_TURNOS.ESTADO, "REVELADO")
                .set(SORTEO_TURNOS.SEMILLA_SERVIDOR, semilla)
                .set(SORTEO_TURNOS.SEMILLA_PUBLICA, semilla)
                .set(SORTEO_TURNOS.RESULTADO, org.jooq.JSONB.valueOf(resultadoJson))
                .set(SORTEO_TURNOS.FECHA_EJECUCION, ahora)
                .where(SORTEO_TURNOS.ID.eq(sorteoId))
                .execute();
    }

    public void anular(DSLContext dsl, UUID sorteoId, OffsetDateTime ahora) {
        dsl.update(SORTEO_TURNOS)
                .set(SORTEO_TURNOS.ESTADO, "ANULADO")
                .set(SORTEO_TURNOS.ANULADO_EN, ahora)
                .where(SORTEO_TURNOS.ID.eq(sorteoId))
                .execute();
    }

    /**
     * Un turno por periodo, y en el orden del sorteo.
     *
     * <p>{@code uq_turno_periodo} es unico sobre {@code periodo_id} a secas: cada
     * periodo tiene UN beneficiario, que es lo que hace que un pasanaku sea un
     * pasanaku. Por eso recibe tantos periodos como cupos.
     */
    public void crearTurnos(
            DSLContext dsl,
            UUID grupoId,
            List<UUID> periodosEnOrden,
            List<UUID> cuposEnOrden,
            BigDecimal montoEstimado) {
        if (periodosEnOrden.size() != cuposEnOrden.size()) {
            throw new IllegalArgumentException("Cada cupo necesita su periodo: %d periodos para %d cupos"
                    .formatted(periodosEnOrden.size(), cuposEnOrden.size()));
        }
        for (int i = 0; i < cuposEnOrden.size(); i++) {
            dsl.insertInto(TURNO)
                    .set(TURNO.GRUPO_ID, grupoId)
                    .set(TURNO.PERIODO_ID, periodosEnOrden.get(i))
                    .set(TURNO.CUPO_ID, cuposEnOrden.get(i))
                    .set(TURNO.ORDEN_ASIGNADO, (short) (i + 1))
                    .set(TURNO.ESTADO, "PROGRAMADO")
                    .set(TURNO.CRITERIO_ASIGNACION, "SORTEO")
                    .set(TURNO.MONTO_ESTIMADO_COBRO, montoEstimado)
                    .execute();
        }
    }

    public int turnosDe(DSLContext dsl, UUID grupoId) {
        return dsl.fetchCount(TURNO, TURNO.GRUPO_ID.eq(grupoId));
    }

    /** Los cupos ocupados, con su participante, en orden de numero: el plantel que se congela. */
    public List<SnapshotDeSorteo.CupoCongelado> plantelOcupado(DSLContext dsl, UUID grupoId) {
        return dsl.fetch(
                        "SELECT numero, id, participante_id FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO' ORDER BY numero",
                        grupoId)
                .map(f -> new SnapshotDeSorteo.CupoCongelado(
                        f.get("numero", Integer.class), f.get("id", UUID.class), f.get("participante_id", UUID.class)));
    }

    /** El calendario del grupo, en orden de periodo. */
    public List<SnapshotDeSorteo.PeriodoCongelado> calendarioDelGrupo(DSLContext dsl, UUID grupoId) {
        return dsl.fetch(
                        "SELECT numero, id, fecha_limite_pago FROM grupos.periodo WHERE grupo_id = ? ORDER BY numero",
                        grupoId)
                .map(f -> new SnapshotDeSorteo.PeriodoCongelado(
                        f.get("numero", Integer.class),
                        f.get("id", UUID.class),
                        f.get("fecha_limite_pago", java.time.LocalDate.class)));
    }

    /** Las reglas del grupo que el sorteo da por sentadas, en texto canonico (clave=valor por linea). */
    public String reglasDelGrupo(DSLContext dsl, UUID grupoId) {
        var g = dsl.fetchOne(
                "SELECT monto_aporte, moneda, periodicidad, dia_cobro, num_periodos, cupos_totales, fecha_inicio FROM grupos.grupo WHERE id = ?",
                grupoId);
        if (g == null) return "";
        return "monto=" + g.get("monto_aporte", BigDecimal.class).toPlainString()
                + "\nmoneda=" + g.get("moneda", String.class)
                + "\nperiodicidad=" + g.get("periodicidad", String.class)
                + "\ndiaCobro=" + g.get("dia_cobro")
                + "\nperiodos=" + g.get("num_periodos")
                + "\ncupos=" + g.get("cupos_totales")
                + "\nfechaInicio=" + g.get("fecha_inicio");
    }

    /** El sorteo con candado de fila: revelar dos veces a la vez se serializa aqui. */
    public Optional<Sorteo> bloquear(DSLContext dsl, UUID sorteoId) {
        var f = dsl.fetchOne("SELECT * FROM grupos.sorteo_turnos WHERE id = ? FOR UPDATE", sorteoId);
        return Optional.ofNullable(f).map(SorteoRepositorio::sorteo);
    }

    public Optional<Sorteo> ver(DSLContext dsl, UUID sorteoId) {
        var f = dsl.fetchOne("SELECT * FROM grupos.sorteo_turnos WHERE id = ?", sorteoId);
        return Optional.ofNullable(f).map(SorteoRepositorio::sorteo);
    }

    private static Sorteo sorteo(Record f) {
        return new Sorteo(
                f.get("id", UUID.class),
                f.get("grupo_id", UUID.class),
                f.get("estado", String.class),
                f.get("hash_semilla_previo", String.class),
                leerLista(f.get("aportes_entropia", org.jooq.JSONB.class)),
                leerLista(f.get("resultado", org.jooq.JSONB.class)),
                f.get("semilla_publica", String.class));
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    public static String comoJson(List<String> valores) {
        try {
            return JSON.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<String> leerLista(org.jooq.JSONB json) {
        if (json == null) return List.of();
        try {
            return JSON.readValue(json.data(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON de sorteo ilegible", e);
        }
    }

    /** Congela el plantel, el calendario y las reglas junto con la semilla sellada. Una sola vez por sorteo. */
    public void guardarSnapshot(
            DSLContext dsl,
            UUID sorteoId,
            UUID grupoId,
            SnapshotDeSorteo snapshot,
            String semillaSellada,
            UUID correlacion,
            OffsetDateTime ahora) {
        dsl.execute(
                """
            INSERT INTO grupos.snapshot_sorteo
            (id,sorteo_id,grupo_id,roster,periodos,reglas,hash_snapshot,semilla_sellada,congelado_en,correlacion_id)
            VALUES (gen_random_uuid(),?,?,?,?,?,?,?,?::timestamptz,?)
            """,
                sorteoId,
                grupoId,
                snapshot.rosterCanonico(),
                snapshot.periodosCanonico(),
                snapshot.reglas(),
                snapshot.hash(),
                semillaSellada,
                ahora,
                correlacion);
    }

    public Optional<Congelado> snapshotDe(DSLContext dsl, UUID sorteoId) {
        var f = dsl.fetchOne(
                "SELECT roster, periodos, reglas, hash_snapshot, semilla_sellada FROM grupos.snapshot_sorteo WHERE sorteo_id = ?",
                sorteoId);
        if (f == null) return Optional.empty();
        var snapshot = new SnapshotDeSorteo(
                SnapshotDeSorteo.leerRoster(f.get("roster", String.class)),
                SnapshotDeSorteo.leerPeriodos(f.get("periodos", String.class)),
                f.get("reglas", String.class));
        return Optional.of(
                new Congelado(snapshot, f.get("hash_snapshot", String.class), f.get("semilla_sellada", String.class)));
    }

    public record Sorteo(
            UUID id,
            UUID grupoId,
            String estado,
            String hash,
            List<String> entropias,
            List<String> resultado,
            String semillaPublica) {}

    /** El snapshot guardado y la semilla sellada; {@code toString} no muestra la semilla. */
    public record Congelado(SnapshotDeSorteo snapshot, String hash, String semillaSellada) {
        @Override
        public String toString() {
            return "Congelado[hash=" + hash + ", semilla=SELLADA]";
        }
    }

    public record Compromiso(String hash, String estado, UUID grupoId) {}
}
