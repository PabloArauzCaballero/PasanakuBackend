package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.InicioDelGrupo;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-69 · El organizador inicia el grupo al 75 % (decision del dueño 2026-10-30). Es la transicion
 * {@code ABIERTO_A_INSCRIPCION → CONFORMADO} pedida por quien organiza, con su renglon en el historial de estados: la
 * otra forma de llegar a CONFORMADO es la ultima firma con todos los cupos ocupados ({@link CU69FirmarReglamento}).
 *
 * <p>Al iniciar el grupo SE ACHICA: {@code cupos_totales} y {@code num_periodos} pasan a ser los cupos OCUPADO, y
 * desde ese momento nadie mas entra por ningun camino ({@link GrupoAbierto}, AP-CU69-17). Los cupos LIBRE dejan de
 * ser del ciclo: se renumeran por ENCIMA de los ocupados ({@code numero > cupos_totales}), porque {@code svc_grupos}
 * no puede borrar filas de {@code cupo} y el modelo no tiene un estado de cupo para «descartado». Los ocupados quedan
 * numerados de 1 a N en su orden de antes (el organizador sigue siendo el 1).
 *
 * <p>La fila del grupo se toma primero ({@code FOR UPDATE}), la misma que toman los ingresos que reservan cupo: un
 * ingreso y un inicio no pasan los dos. Ademas, la precondicion va EN la escritura (regla 96.3.2): el {@code UPDATE}
 * exige el estado, el conteo de OCUPADO y que no haya RESERVADO. Idempotente por clave: el renglon del historial lleva
 * un id derivado de grupo y clave; la misma clave devuelve el resultado original y otra clave responde AP-CU69-17.
 *
 * <p>Y en la misma transaccion publica la version nueva del reglamento con los numeros del grupo achicado, que nadie
 * tiene aceptada al nacer ({@link ReglamentoAlIniciar}).
 */
@Service
public class CU69IniciarGrupo {

    private final Datos datos;
    private final AdmisionRepositorio admision;
    private final CalendarioDelGrupoRepositorio calendario;
    private final Reloj reloj;
    private final Outbox outbox;
    private final int porcentaje;

    public CU69IniciarGrupo(
            Datos datos,
            AdmisionRepositorio admision,
            CalendarioDelGrupoRepositorio calendario,
            Reloj reloj,
            Outbox outbox,
            @Value("${aportaya.grupos.inicio.porcentaje}") int porcentaje) {
        this.datos = datos;
        this.admision = admision;
        this.calendario = calendario;
        this.reloj = reloj;
        this.outbox = outbox;
        this.porcentaje = porcentaje;
    }

    @Transactional
    public Iniciado iniciar(UUID grupoId, UUID clave, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        // `cupo` y `participante` son del proceso interno (politica de fila); quien pide se comprueba abajo.
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> {
            Record g = dsl.fetchOne(
                    """
                    SELECT estado, cupos_totales, num_periodos, fecha_inicio, nombre, monto_aporte, moneda,
                           periodicidad, dia_cobro
                      FROM grupos.grupo WHERE id = ? FOR UPDATE
                    """,
                    grupoId);
            // Un grupo ajeno y uno que no existe responden lo mismo: no se confirma la existencia de nada.
            if (g == null || !admision.esAdministrador(dsl, grupoId, ctx.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(69, 7), "Solo quien organiza el grupo puede iniciarlo.");
            }
            UUID renglon = renglonDelInicio(grupoId, clave);
            if (dsl.fetchOne("SELECT 1 FROM grupos.historial_estado_grupo WHERE id = ?", renglon) != null) {
                return iniciado(grupoId, g);
            }
            String estado = g.get("estado", String.class);
            if ("BORRADOR".equals(estado)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(69, 12), "Tu grupo todavia se esta abriendo para inscripciones.");
            }
            if (!InicioDelGrupo.ABIERTO.equals(estado)) throw GrupoAbierto.yaIniciado();

            int cuposTotales = g.get("cupos_totales", Integer.class);
            int ocupados = contar(dsl, grupoId, "OCUPADO");
            int reservados = contar(dsl, grupoId, "RESERVADO");
            if (reservados > 0) {
                throw new ErrorDeNegocio(
                        CodigoError.de(69, 19),
                        "Hay integrantes aceptados que todavia no firmaron el reglamento.",
                        Map.of("reservados", reservados));
            }
            int minimo = InicioDelGrupo.minimoParaIniciar(cuposTotales, porcentaje);
            if (ocupados < minimo) {
                throw new ErrorDeNegocio(
                        CodigoError.de(69, 16),
                        "Todavia no se llego al minimo de cupos ocupados para iniciar.",
                        Map.of("ocupados", ocupados, "minimoRequerido", minimo, "cuposTotales", cuposTotales));
            }
            achicar(dsl, grupoId, ocupados, minimo, g.get("fecha_inicio", LocalDate.class));
            dsl.execute(
                    """
                    INSERT INTO grupos.historial_estado_grupo
                      (id, grupo_id, estado_anterior, estado_nuevo, motivo, ejecutado_por, fecha_hora)
                    VALUES (?, ?, 'ABIERTO_A_INSCRIPCION', 'CONFORMADO', ?, ?, CAST(? AS timestamptz))
                    """,
                    renglon,
                    grupoId,
                    "Iniciado por el organizador con %d de %d cupos ocupados.".formatted(ocupados, cuposTotales),
                    ctx.usuarioId(),
                    ahora);
            ReglamentoAlIniciar.publicar(dsl, grupoId, g, ocupados, ctx.usuarioId(), ahora);
            calendario.generar(dsl, grupoId, ahora.toLocalDate());
            emitirInicio(dsl, grupoId, g, ocupados, ahora, ctx);
            return new Iniciado(grupoId, "CONFORMADO", ocupados, ocupados);
        });
    }

    /**
     * R-GRP-20 (decision del dueño 2026-10-30): la cuota 1 nace al iniciar. En la MISMA transaccion, con el grupo ya
     * achicado y su calendario armado, se publica por outbox {@code grupos.grupo_iniciado} con todo lo que {@code aportes}
     * necesita para abrir la obligacion de la cuota 1 de cada cupo sin preguntar nada: el periodo 1 con sus fechas y los
     * cupos OCUPADO ya renumerados 1..N con su titular y su aporte (un consumidor no tiene el token de nadie). Importes
     * como cadena de dos decimales. El reintento con la misma clave sale antes de llegar aca: un inicio, un evento.
     * Pacto: {@code contratos/pactos/grupos-aportes-grupo-iniciado.json}.
     */
    private void emitirInicio(
            DSLContext dsl, UUID grupoId, Record g, int ocupados, OffsetDateTime ahora, ContextoSesion ctx) {
        Record periodo = dsl.fetchOne(
                """
                SELECT id, fecha_inicio, fecha_limite_pago, fecha_fin_gracia, fecha_entrega_prevista
                  FROM grupos.periodo WHERE grupo_id = ? AND numero = 1
                """,
                grupoId);
        if (periodo == null) {
            // El calendario se genero recien, en esta transaccion: sin periodo 1 el inicio no es tal.
            throw new IllegalStateException("El grupo " + grupoId + " se inicio sin periodo 1.");
        }
        // NUMERIC(14,2) en la base: la escala ya es 2, UNNECESSARY lo afirma en vez de redondear en silencio (regla
        // 91).
        String montoAporte = g.get("monto_aporte", BigDecimal.class)
                .setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString();
        List<Map<String, Object>> cupos = dsl.fetch(
                        """
                        SELECT id, numero, participante_id FROM grupos.cupo
                         WHERE grupo_id = ? AND estado = 'OCUPADO' ORDER BY numero
                        """,
                        grupoId)
                .map(c -> {
                    Map<String, Object> cupo = new LinkedHashMap<>();
                    cupo.put("cupoId", c.get("id", UUID.class).toString());
                    cupo.put("numero", c.get("numero", Integer.class));
                    cupo.put(
                            "participanteId",
                            c.get("participante_id", UUID.class).toString());
                    cupo.put("montoAporte", montoAporte);
                    return cupo;
                });
        Map<String, Object> periodoUno = new LinkedHashMap<>();
        periodoUno.put("periodoId", periodo.get("id", UUID.class).toString());
        periodoUno.put("numero", 1);
        periodoUno.put(
                "fechaInicio", periodo.get("fecha_inicio", LocalDate.class).toString());
        periodoUno.put(
                "fechaLimitePago",
                periodo.get("fecha_limite_pago", LocalDate.class).toString());
        periodoUno.put(
                "fechaFinGracia",
                periodo.get("fecha_fin_gracia", LocalDate.class).toString());
        periodoUno.put(
                "fechaEntregaPrevista",
                periodo.get("fecha_entrega_prevista", LocalDate.class).toString());
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("grupoId", grupoId.toString());
        carga.put("moneda", g.get("moneda", String.class).trim());
        carga.put("cuposTotales", ocupados);
        carga.put("iniciadoEn", ahora.toString());
        carga.put("periodo", periodoUno);
        carga.put("cupos", cupos);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "grupos.grupo_iniciado",
                        "grupo",
                        grupoId,
                        carga,
                        UUID.fromString(ctx.traza().id())));
    }

    /**
     * La transicion con la precondicion en la escritura, y despues la renumeracion: primero todo el grupo se corre por
     * encima de cualquier numero en uso (la unicidad {@code (grupo_id, numero)} se mira fila a fila), y despues los
     * OCUPADO toman 1..N en su orden y los LIBRE N+1 en adelante.
     */
    private static void achicar(DSLContext dsl, UUID grupoId, int ocupados, int minimo, LocalDate fechaInicio) {
        int cambio = dsl.execute(
                """
                UPDATE grupos.grupo
                   SET estado = 'CONFORMADO', cupos_totales = ?, num_periodos = ?, cupos_ocupados = ?,
                       fecha_fin_estimada = ?
                 WHERE id = ? AND estado = 'ABIERTO_A_INSCRIPCION'
                   AND (SELECT count(*) FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO') = ?
                   AND (SELECT count(*) FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO') >= ?
                   AND NOT EXISTS (SELECT 1 FROM grupos.cupo WHERE grupo_id = ? AND estado = 'RESERVADO')
                """,
                (short) ocupados,
                (short) ocupados,
                (short) ocupados,
                // Igual que al crear: tantos meses como periodos (CreacionRepositorio).
                fechaInicio.plusMonths(ocupados),
                grupoId,
                grupoId,
                ocupados,
                grupoId,
                minimo,
                grupoId);
        if (cambio == 0) throw GrupoAbierto.yaIniciado();
        int desplazamiento = dsl.fetchOne(
                        "SELECT coalesce(max(numero), 0)::int FROM grupos.cupo WHERE grupo_id = ?", grupoId)
                .get(0, Integer.class);
        dsl.execute("UPDATE grupos.cupo SET numero = numero + ? WHERE grupo_id = ?", desplazamiento, grupoId);
        dsl.execute(
                """
                UPDATE grupos.cupo c SET numero = r.nuevo
                  FROM (SELECT id, row_number() OVER (ORDER BY estado <> 'OCUPADO', numero) AS nuevo
                          FROM grupos.cupo WHERE grupo_id = ?) r
                 WHERE c.id = r.id
                """,
                grupoId);
    }

    private static int contar(DSLContext dsl, UUID grupoId, String estado) {
        return dsl.fetchOne("SELECT count(*)::int FROM grupos.cupo WHERE grupo_id = ? AND estado = ?", grupoId, estado)
                .get(0, Integer.class);
    }

    private static Iniciado iniciado(UUID grupoId, Record g) {
        return new Iniciado(
                grupoId, "CONFORMADO", g.get("cupos_totales", Integer.class), g.get("num_periodos", Integer.class));
    }

    /** El id del renglon del historial: el mismo para el mismo grupo y la misma clave, distinto entre grupos. */
    static UUID renglonDelInicio(UUID grupoId, UUID clave) {
        return UUID.nameUUIDFromBytes(("grupos.inicio:" + grupoId + ":" + clave).getBytes(StandardCharsets.UTF_8));
    }

    public record Iniciado(UUID grupoId, String estado, int cuposTotales, int numPeriodos) {}
}
