package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jooq.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-69 · El grupo visto por uno de sus integrantes (2026-10-20): un resumen y una fila por cupo con el nombre
 * visible de quien lo ocupa, si firmo y su turno. Solo para integrantes; sin telefono ni correo de nadie
 * (decision del dueño). Lo lee el proceso interno porque la politica de fila no deja ver a los demas
 * participantes, y por eso la pertenencia se comprueba aca antes de devolver nada.
 */
@Service
public class ConsultarDetalleDelGrupo {

    private final Datos datos;

    public ConsultarDetalleDelGrupo(Datos datos) {
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public Detalle consultar(UUID grupoId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> {
            Record yo = dsl.fetchOne(
                    """
                    SELECT id, estado, es_organizador FROM grupos.participante
                     WHERE grupo_id = ? AND usuario_id = ?
                       AND estado NOT IN ('RETIRADO', 'EXPULSADO', 'REEMPLAZADO', 'POSTULANTE', 'INVITADO')
                     ORDER BY fecha_ingreso DESC LIMIT 1
                    """,
                    grupoId,
                    ctx.usuarioId());
            if (yo == null) {
                throw new ErrorDeNegocio(CodigoError.de(69, 14), "No sos integrante de este grupo.");
            }
            Record g = dsl.fetchOne(
                    """
                    SELECT nombre, estado, monto_aporte, moneda, periodicidad, cupos_totales, cupos_ocupados,
                           modalidad_turnos, reputacion_minima,
                           (SELECT count(*) FROM grupos.cupo c
                             WHERE c.grupo_id = g.id AND c.estado = 'OCUPADO')::int AS firmados,
                           (SELECT count(*) FROM grupos.cupo c
                             WHERE c.grupo_id = g.id AND c.estado = 'RESERVADO')::int AS reservados
                      FROM grupos.grupo g WHERE id = ?
                    """,
                    grupoId);
            Record reglamento = dsl.fetchOne(
                    "SELECT id, version, contenido, hash_contenido FROM grupos.reglamento_grupo WHERE grupo_id = ? ORDER BY version DESC LIMIT 1",
                    grupoId);
            Record sorteo = dsl.fetchOne(
                    """
                    SELECT id, estado FROM grupos.sorteo_turnos
                     WHERE grupo_id = ? AND anulado_en IS NULL AND estado IN ('COMPROMETIDO', 'REVELADO')
                     ORDER BY fecha_compromiso DESC LIMIT 1
                    """,
                    grupoId);
            List<Integrante> integrantes = LecturasDelDetalle.integrantes(dsl, grupoId, ctx.usuarioId());
            boolean soyOrganizador = Boolean.TRUE.equals(yo.get("es_organizador", Boolean.class));
            String estado = g.get("estado", String.class);
            UUID vigente = reglamento == null ? null : reglamento.get("id", UUID.class);
            // Reglamento al achicar (2026-10-30): iniciar publica una version nueva y nadie la tiene aceptada al nacer.
            // El sorteo espera a que la acepten todos los que ocupan un cupo del ciclo.
            boolean todosAceptaron =
                    vigente == null || LecturasDelDetalle.sinAceptarLaVigente(dsl, grupoId, vigente) == 0;
            boolean puedeSortear = soyOrganizador
                    && "CONFORMADO".equals(estado)
                    && "SORTEO_ALEATORIO".equals(g.get("modalidad_turnos", String.class))
                    && sorteo == null
                    && todosAceptaron;
            // Tambien firma quien quedo ACTIVO sin cupo mientras el grupo recibe integrantes: firmar le da uno.
            boolean sinCupo = !LecturasDelDetalle.tieneCupo(dsl, yo.get("id", UUID.class));
            boolean debeFirmar = "ACEPTADO_PENDIENTE_FIRMA".equals(yo.get("estado", String.class))
                    || ("ACTIVO".equals(yo.get("estado", String.class))
                            && "ABIERTO_A_INSCRIPCION".equals(estado)
                            && sinCupo);
            // Distinto de debeFirmar (la primera firma, con el cupo RESERVADO): ya es ACTIVO y le falta aceptar la
            // version vigente.
            // Solo con el grupo ya iniciado: la version nueva la publica el inicio (CU69IniciarGrupo).
            boolean pendienteDeAceptar = !debeFirmar
                    && !"ABIERTO_A_INSCRIPCION".equals(estado)
                    && "ACTIVO".equals(yo.get("estado", String.class))
                    && vigente != null
                    && !LecturasDelDetalle.acepto(dsl, vigente, yo.get("id", UUID.class));
            return new Detalle(
                    grupoId,
                    g.get("nombre", String.class),
                    estado,
                    g.get("monto_aporte", BigDecimal.class),
                    g.get("moneda", String.class).trim(),
                    g.get("periodicidad", String.class),
                    g.get("cupos_totales", Integer.class),
                    g.get("cupos_ocupados", Integer.class),
                    soyOrganizador,
                    debeFirmar,
                    puedeSortear,
                    reglamento == null ? null : reglamento.get("contenido", String.class),
                    reglamento == null ? null : reglamento.get("hash_contenido", String.class),
                    sorteo == null ? null : sorteo.get("id", UUID.class),
                    sorteo == null ? null : sorteo.get("estado", String.class),
                    integrantes,
                    new Inicio(
                            g.get("reputacion_minima", BigDecimal.class),
                            g.get("firmados", Integer.class),
                            g.get("reservados", Integer.class)),
                    reglamento == null
                            ? null
                            : reglamento.get("version", Short.class).intValue(),
                    pendienteDeAceptar);
        });
    }

    /** Integrante de un grupo: esta y no se fue. El mismo criterio para el detalle y para {@link #comparten}. */
    private static final String ES_INTEGRANTE =
            """
            SELECT EXISTS (SELECT 1 FROM grupos.participante
                            WHERE grupo_id = ? AND usuario_id = ?
                              AND estado NOT IN ('RETIRADO', 'EXPULSADO', 'REEMPLAZADO', 'POSTULANTE', 'INVITADO')) AS si
            """;

    /**
     * Perfil del integrante (2026-10-21): si quien pregunta (el token, nunca la peticion) y otra persona son
     * integrantes del mismo grupo. Lo consultan identidad y transparencia antes de mostrar telefono o reputacion.
     * Responde solo si o no: ni el estado ni el cupo del otro.
     */
    @Transactional(readOnly = true)
    public boolean comparten(UUID grupoId, UUID otroUsuarioId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(
                interno,
                dsl -> esIntegrante(dsl, grupoId, ctx.usuarioId()) && esIntegrante(dsl, grupoId, otroUsuarioId));
    }

    private static boolean esIntegrante(org.jooq.DSLContext dsl, UUID grupoId, UUID usuarioId) {
        return Boolean.TRUE.equals(
                dsl.fetchOne(ES_INTEGRANTE, grupoId, usuarioId).get("si", Boolean.class));
    }

    /** LIBRE si nadie lo ocupa; FIRMADO si quien lo ocupa ya firmo (cupo OCUPADO); si no, PENDIENTE_FIRMA. */
    static String estadoDelCupo(String estadoCupo, String estadoParticipante) {
        if (estadoParticipante == null || "LIBRE".equals(estadoCupo)) return "LIBRE";
        return "OCUPADO".equals(estadoCupo) ? "FIRMADO" : "PENDIENTE_FIRMA";
    }

    public record Integrante(
            int numero,
            UUID cupoId,
            UUID usuarioId,
            String nombre,
            String estado,
            boolean esOrganizador,
            boolean soyYo,
            Integer turno) {
        public Integrante(
                int numero,
                UUID cupoId,
                String nombre,
                String estado,
                boolean esOrganizador,
                boolean soyYo,
                Integer turno) {
            this(numero, cupoId, null, nombre, estado, esOrganizador, soyYo, turno);
        }
    }

    public record Detalle(
            UUID grupoId,
            String nombre,
            String estado,
            BigDecimal montoAporte,
            String moneda,
            String periodicidad,
            int cuposTotales,
            int cuposOcupados,
            boolean soyOrganizador,
            boolean debeFirmar,
            boolean puedeSortear,
            String reglamentoTexto,
            String reglamentoHash,
            UUID sorteoId,
            String sorteoEstado,
            List<Integrante> integrantes,
            Inicio inicio,
            Integer reglamentoVersion,
            boolean reglamentoPendienteDeAceptar) {

        /** Sin la version del reglamento ni su aceptacion pendiente: la forma de la ronda 1 del 2026-10-30. */
        @SuppressWarnings("checkstyle:ParameterNumber")
        public Detalle(
                UUID grupoId,
                String nombre,
                String estado,
                BigDecimal montoAporte,
                String moneda,
                String periodicidad,
                int cuposTotales,
                int cuposOcupados,
                boolean soyOrganizador,
                boolean debeFirmar,
                boolean puedeSortear,
                String reglamentoTexto,
                String reglamentoHash,
                UUID sorteoId,
                String sorteoEstado,
                List<Integrante> integrantes,
                Inicio inicio) {
            this(
                    grupoId,
                    nombre,
                    estado,
                    montoAporte,
                    moneda,
                    periodicidad,
                    cuposTotales,
                    cuposOcupados,
                    soyOrganizador,
                    debeFirmar,
                    puedeSortear,
                    reglamentoTexto,
                    reglamentoHash,
                    sorteoId,
                    sorteoEstado,
                    integrantes,
                    inicio,
                    null,
                    false);
        }

        /** Sin los datos del inicio: la forma de antes del 2026-10-30. */
        @SuppressWarnings("checkstyle:ParameterNumber")
        public Detalle(
                UUID grupoId,
                String nombre,
                String estado,
                BigDecimal montoAporte,
                String moneda,
                String periodicidad,
                int cuposTotales,
                int cuposOcupados,
                boolean soyOrganizador,
                boolean debeFirmar,
                boolean puedeSortear,
                String reglamentoTexto,
                String reglamentoHash,
                UUID sorteoId,
                String sorteoEstado,
                List<Integrante> integrantes) {
            this(
                    grupoId,
                    nombre,
                    estado,
                    montoAporte,
                    moneda,
                    periodicidad,
                    cuposTotales,
                    cuposOcupados,
                    soyOrganizador,
                    debeFirmar,
                    puedeSortear,
                    reglamentoTexto,
                    reglamentoHash,
                    sorteoId,
                    sorteoEstado,
                    integrantes,
                    null);
        }
    }

    /**
     * Lo que hace falta para el inicio al 75 % y la etapa minima (2026-10-30): el {@code desde} de la etapa minima, los
     * cupos OCUPADO (firmados) y los RESERVADO (aceptados sin firmar). {@code cuposOcupados} cuenta los dos juntos.
     */
    public record Inicio(BigDecimal reputacionMinima, int firmados, int reservados) {}
}
