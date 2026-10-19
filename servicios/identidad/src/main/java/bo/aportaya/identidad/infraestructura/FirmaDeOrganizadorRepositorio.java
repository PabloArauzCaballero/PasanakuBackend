package bo.aportaya.identidad.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Los pasos de identidad del contrato de organizador Aprendiz, cada uno en su propia
 * transacción: el token de firma tiene que estar CONFIRMADO antes de que organizador lo
 * referencie ({@code fk_contrato_organizador_token_firma_id}), así que no puede vivir en la
 * misma transacción que la llamada HTTP.
 *
 * <p>Escriben como sistema ({@link Datos#comoSistema}): las políticas de fila no dejan a la
 * persona escribir sus propios roles ni tokens. Quien llama ya comprobó que es ella misma.
 */
@Repository
public class FirmaDeOrganizadorRepositorio {

    public static final String PROPOSITO = "FIRMA_CONTRATO";

    private final Datos datos;
    private final TransactionTemplate transaccion;

    public FirmaDeOrganizadorRepositorio(Datos datos, TransactionTemplate transaccion) {
        this.datos = datos;
        this.transaccion = transaccion;
    }

    /** Quien aprobó su KYC (el otorgante de su rol PARTICIPANTE), o vacío si todavía no se aprobó. */
    public Optional<UUID> otorganteDelKyc(ContextoSesion ctx) {
        return transaccion.execute(t -> datos.conContexto(
                ctx,
                dsl -> Datos.comoSistema(dsl, () -> dsl.fetchOptional(
                                """
                                SELECT a.otorgada_por
                                  FROM identidad.asignacion_rol a
                                  JOIN identidad.rol r ON r.id = a.rol_id
                                 WHERE a.usuario_id = ? AND r.codigo = 'PARTICIPANTE'
                                   AND a.revocada_en IS NULL
                                 ORDER BY a.otorgada_en
                                 LIMIT 1
                                """,
                                ctx.usuarioId())
                        .map(f -> f.get(0, UUID.class)))));
    }

    /**
     * La evidencia de la firma: un token FIRMA_CONTRATO ya consumido (la persona aceptó el texto
     * dentro de la app con su sesión). El hash ata usuario, contrato y texto; no hay secreto que
     * guardar. Idempotente por contrato: repetirlo devuelve el mismo token.
     */
    public UUID tokenDeFirma(
            ContextoSesion ctx, UUID contratoId, String contenidoHash, String ip, String agente, OffsetDateTime ahora) {
        String clave = "firma-org:" + contratoId;
        String hash = sha256(ctx.usuarioId() + "|" + contratoId + "|" + contenidoHash);
        return transaccion.execute(t -> datos.conContexto(
                ctx,
                dsl -> Datos.comoSistema(dsl, () -> {
                    dsl.execute(
                            """
                    INSERT INTO identidad.token_verificacion
                      (usuario_id, politica_id, tipo_token, proposito, hash_token, algoritmo_hash,
                       canal_entrega, destino_enmascarado, estado, emitido_en, expira_en, consumido_en,
                       max_intentos, reenvios, ip_origen, agente_usuario, correlation_id, clave_idempotencia)
                    SELECT ?, p.id, 'ENLACE', ?, ?, 'sha256',
                           'APP', 'app', 'CONSUMIDO', ?::timestamptz,
                           ?::timestamptz + make_interval(secs => p.ttl_segundos), ?::timestamptz,
                           1, 0, ?::inet, ?, ?, ?
                      FROM identidad.politica_token p
                     WHERE p.proposito = ? AND p.vigente_desde <= ?::timestamptz
                     ORDER BY p.vigente_desde DESC
                     LIMIT 1
                    ON CONFLICT DO NOTHING
                    """,
                            ctx.usuarioId(),
                            PROPOSITO,
                            hash,
                            ahora,
                            ahora,
                            ahora,
                            ip,
                            agente,
                            UUID.fromString(ctx.traza().id()),
                            clave,
                            PROPOSITO,
                            ahora);
                    return dsl.fetchOptional(
                                    "SELECT id FROM identidad.token_verificacion WHERE usuario_id = ? AND clave_idempotencia = ?",
                                    ctx.usuarioId(),
                                    clave)
                            .map(f -> f.get(0, UUID.class))
                            .orElseThrow(() -> new IllegalStateException("No hay politica vigente para " + PROPOSITO));
                })));
    }

    /** Concede ORGANIZADOR una sola vez. @return {@code true} si se concedió ahora. */
    public boolean asignarOrganizador(ContextoSesion ctx, UUID otorgante, OffsetDateTime ahora) {
        return Boolean.TRUE.equals(transaccion.execute(t -> datos.conContexto(
                ctx,
                dsl -> Datos.comoSistema(
                        dsl,
                        () -> dsl.execute(
                                        """
                        INSERT INTO identidad.asignacion_rol
                          (usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en)
                        SELECT ?, r.id, 'GLOBAL', NULL, ?, ?::timestamptz
                          FROM identidad.rol r
                         WHERE r.codigo = 'ORGANIZADOR'
                           AND NOT EXISTS (
                               SELECT 1 FROM identidad.asignacion_rol a
                                WHERE a.usuario_id = ? AND a.rol_id = r.id AND a.revocada_en IS NULL)
                        """,
                                        ctx.usuarioId(),
                                        otorgante,
                                        ahora,
                                        ctx.usuarioId())
                                == 1))));
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }
}
