package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-69 · El enlace del grupo (2026-10-19): un enlace —y su QR— que el organizador comparte para que CUALQUIER
 * persona pida unirse. No da entrada: cada pedido es una solicitud PENDIENTE que el organizador aprueba (CU-68),
 * decision del dueño. Sirve hasta llenarse los cupos o vencer (7 dias), o hasta que el organizador lo anule.
 *
 * <p>El codigo es {@code <enlaceId>.<64 hex>}; la base guarda solo el SHA-256 de la parte secreta. Un grupo tiene
 * un solo enlace ACTIVO: generar otro anula el anterior en la misma transaccion, tomando primero la fila del grupo.
 */
@Service
public class CU69EnlaceDeGrupo {

    private static final Pattern CODIGO = Pattern.compile("^([0-9a-f-]{36})\\.([0-9a-f]{64})$");
    private static final SecureRandom AZAR = new SecureRandom();

    private final Datos datos;
    private final AdmisionRepositorio admision;
    private final EmparejamientoRepositorio emparejamientos;
    private final CU68Postular postulaciones;
    private final Reloj reloj;
    private final Duration vigencia;

    public CU69EnlaceDeGrupo(
            Datos datos,
            AdmisionRepositorio admision,
            EmparejamientoRepositorio emparejamientos,
            CU68Postular postulaciones,
            Reloj reloj,
            @Value("${aportaya.grupos.enlace-del-grupo.vigencia-dias:7}") long vigenciaDias) {
        this.datos = datos;
        this.admision = admision;
        this.emparejamientos = emparejamientos;
        this.postulaciones = postulaciones;
        this.reloj = reloj;
        this.vigencia = Duration.ofDays(vigenciaDias);
    }

    /** El organizador genera el enlace; el anterior, si habia, queda ANULADO. */
    @Transactional
    public Generado generar(UUID grupoId, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            exigirAdministrador(dsl, grupoId, ctx);
            // Un enlace de un grupo que no esta abierto nace inservible (vigente() lo rechaza): se avisa ya.
            String estado = dsl.fetchValue("SELECT estado FROM grupos.grupo WHERE id = ?", grupoId) instanceof String s
                    ? s
                    : null;
            if ("BORRADOR".equals(estado) || estado == null) {
                throw new ErrorDeNegocio(
                        CodigoError.de(69, 12),
                        "Tu grupo todavia se esta abriendo para inscripciones. Proba de nuevo en unos segundos.");
            }
            // Iniciado (2026-10-30), el grupo se achico a sus cupos ocupados: no le queda ningun cupo para compartir.
            if (!"ABIERTO_A_INSCRIPCION".equals(estado)) {
                throw new ErrorDeNegocio(CodigoError.de(69, 1), "El grupo ya inicio: no le quedan cupos libres.");
            }
            if (!emparejamientos.hayCuposLibres(dsl, grupoId)) {
                throw new ErrorDeNegocio(CodigoError.de(69, 1), "El grupo ya no tiene cupos libres.");
            }
            anularActivo(dsl, grupoId, ahora);
            UUID id = UUID.randomUUID();
            String secreto = secreto();
            OffsetDateTime expira = ahora.plus(vigencia);
            dsl.execute(
                    """
                    INSERT INTO grupos.enlace_grupo
                      (id, grupo_id, emisor_id, token_hash, estado, fecha_creacion, fecha_expiracion)
                    VALUES (?, ?, ?, ?, 'ACTIVO', CAST(? AS timestamptz), CAST(? AS timestamptz))
                    """,
                    id,
                    grupoId,
                    ctx.usuarioId(),
                    hash(secreto),
                    ahora,
                    expira);
            return new Generado(id, id + "." + secreto, expira);
        });
    }

    /** El organizador anula el enlace activo. Sin enlace activo no hay nada que hacer. */
    @Transactional
    public void anular(UUID grupoId, ContextoSesion ctx) {
        OffsetDateTime ahora = ahora();
        datos.conContexto(ctx, dsl -> {
            exigirAdministrador(dsl, grupoId, ctx);
            anularActivo(dsl, grupoId, ahora);
            return null;
        });
    }

    /** Lo que ve quien abre el enlace: el grupo, sin datos de sus integrantes. */
    @Transactional(readOnly = true)
    public Detalle consultar(String codigo, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> vigente(dsl, codigo, ahora()));
    }

    /**
     * Pide unirse. Las precondiciones de postular (KYC, restricciones, reputacion, cupos) las comprueba
     * {@link CU68Postular}; repetir devuelve la misma solicitud PENDIENTE en vez de fallar.
     */
    @Transactional
    public Solicitada solicitar(
            String codigo,
            CU68Postular.EntradaPostulacion entrada,
            HechosDeOtrosServicios.Contacto contacto,
            ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            Detalle enlace = vigente(dsl, codigo, ahora());
            if (!enlace.grupoId().equals(entrada.grupoId())) throw invalido();
            // Orden de bloqueos: grupo antes que la solicitud (igual que el canje y la decision).
            dsl.fetchOne("SELECT id FROM grupos.grupo WHERE id = ? FOR UPDATE", enlace.grupoId());
            Record participa = dsl.fetchOne(
                    """
                    SELECT 1 FROM grupos.participante
                     WHERE grupo_id = ? AND usuario_id = ?
                       AND estado IN ('ACEPTADO_PENDIENTE_FIRMA','ACTIVO','EN_MORA','SUSPENDIDO')
                    """,
                    enlace.grupoId(),
                    ctx.usuarioId());
            if (participa != null) {
                throw new ErrorDeNegocio(CodigoError.de(69, 3), "Ya participas de este grupo.");
            }
            Optional<UUID> pendiente = pendienteDe(dsl, enlace.grupoId(), ctx.usuarioId());
            if (pendiente.isPresent()) return new Solicitada(pendiente.get(), enlace.grupoId());
            UUID solicitud = postulaciones.postular(entrada, ctx).solicitudId();
            Datos.comoSistema(
                    dsl,
                    () -> dsl.execute(
                            """
                            UPDATE grupos.solicitud_ingreso
                               SET enlace_grupo_id = ?, nombre_visible = ?
                             WHERE id = ?
                            """,
                            enlace.enlaceId(),
                            // Solo el nombre completo: el organizador no ve telefono ni correo (decision del
                            // dueño 2026-10-20), y lo que no se muestra tampoco se guarda.
                            recortar(contacto == null ? null : contacto.nombre(), 60),
                            solicitud));
            return new Solicitada(solicitud, enlace.grupoId());
        });
    }

    /**
     * La solicitud PENDIENTE de quien llama en ese grupo, si ya pidio: {@link #solicitar} la devuelve en vez de crear
     * otra. Sirve para no volver a exigir la arena en ese reintento (regla 96.3.3); {@link #solicitar} lo vuelve a
     * mirar con la fila del grupo tomada.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> pendienteDe(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> pendienteDe(dsl, grupoId, ctx.usuarioId()));
    }

    /** El grupo de un codigo vigente, para armar la postulacion (con hechos de otros servicios) antes de pedir. */
    @Transactional(readOnly = true)
    public UUID grupoDe(String codigo, ContextoSesion ctx) {
        return consultar(codigo, ctx).grupoId();
    }

    /** Lo que viene de otro servicio se ajusta al largo de la columna en vez de hacer fallar el pedido. */
    private static String recortar(String valor, int largo) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim();
        return limpio.length() <= largo ? limpio : limpio.substring(0, largo);
    }

    private Detalle vigente(DSLContext dsl, String codigo, OffsetDateTime ahora) {
        var partes = CODIGO.matcher(codigo == null ? "" : codigo);
        if (!partes.matches()) throw invalido();
        UUID enlaceId;
        try {
            enlaceId = UUID.fromString(partes.group(1));
        } catch (IllegalArgumentException e) {
            throw invalido();
        }
        Record fila = dsl.fetchOne(
                """
                SELECT e.id, e.grupo_id, e.token_hash, e.fecha_expiracion,
                       g.nombre, g.monto_aporte, g.moneda, g.periodicidad,
                       CASE WHEN g.estado = 'ABIERTO_A_INSCRIPCION'
                            THEN (SELECT count(*) FROM grupos.cupo c WHERE c.grupo_id = g.id AND c.estado = 'LIBRE')
                            ELSE 0 END::int AS cupos_libres
                  FROM grupos.enlace_grupo e
                  JOIN grupos.grupo g ON g.id = e.grupo_id
                 WHERE e.id = ? AND e.estado = 'ACTIVO' AND e.fecha_expiracion > CAST(? AS timestamptz)
                   AND g.estado IN ('CONFORMADO','ABIERTO_A_INSCRIPCION')
                """,
                enlaceId,
                ahora);
        // La comparacion del hash es en tiempo constante: no se filtra cuanto del codigo era correcto.
        if (fila == null
                || !MessageDigest.isEqual(
                        hash(partes.group(2)).getBytes(StandardCharsets.US_ASCII),
                        fila.get("token_hash", String.class).getBytes(StandardCharsets.US_ASCII))) {
            throw invalido();
        }
        return new Detalle(
                fila.get("id", UUID.class),
                fila.get("grupo_id", UUID.class),
                fila.get("nombre", String.class),
                fila.get("monto_aporte", BigDecimal.class),
                fila.get("moneda", String.class).trim(),
                fila.get("periodicidad", String.class),
                Math.max(0, fila.get("cupos_libres", Integer.class)),
                fila.get("fecha_expiracion", OffsetDateTime.class));
    }

    private void exigirAdministrador(DSLContext dsl, UUID grupoId, ContextoSesion ctx) {
        dsl.fetchOne("SELECT id FROM grupos.grupo WHERE id = ? FOR UPDATE", grupoId);
        if (!admision.esAdministrador(dsl, grupoId, ctx.usuarioId())) {
            throw new ErrorDeNegocio(CodigoError.de(69, 7), "Solo quien administra el grupo maneja su enlace.");
        }
    }

    private static void anularActivo(DSLContext dsl, UUID grupoId, OffsetDateTime ahora) {
        dsl.execute(
                "UPDATE grupos.enlace_grupo SET estado = 'ANULADO', fecha_anulacion = CAST(? AS timestamptz) WHERE grupo_id = ? AND estado = 'ACTIVO'",
                ahora,
                grupoId);
    }

    private static Optional<UUID> pendienteDe(DSLContext dsl, UUID grupoId, UUID usuarioId) {
        Record fila = dsl.fetchOne(
                "SELECT id FROM grupos.solicitud_ingreso WHERE grupo_id = ? AND usuario_id = ? AND estado = 'PENDIENTE'",
                grupoId,
                usuarioId);
        return Optional.ofNullable(fila).map(f -> f.get("id", UUID.class));
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    private static String secreto() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String hash(String valor) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ErrorDeNegocio invalido() {
        return new ErrorDeNegocio(
                CodigoError.de(69, 11), "Ese enlace del grupo ya no sirve. Pedile uno nuevo al organizador.");
    }

    public record Generado(UUID enlaceId, String codigo, OffsetDateTime expiraEn) {}

    public record Detalle(
            UUID enlaceId,
            UUID grupoId,
            String nombre,
            BigDecimal montoAporte,
            String moneda,
            String periodicidad,
            int cuposLibres,
            OffsetDateTime expiraEn) {}

    public record Solicitada(UUID solicitudId, UUID grupoId) {}
}
