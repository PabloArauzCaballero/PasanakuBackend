package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vacío confirma el contador de intentos antes de que el controlador responda 422.
 *
 * <p>Cada intento sobre un token existente deja una fila append-only en
 * {@code intento_validacion_token} (resultado, instante, origen); nunca el secreto. Los
 * intentos sobre un token inexistente no tienen fila posible (la FK exige el token) y quedan
 * solo en la bitácora de aplicación, sin el secreto. Una invitación revocada se audita como
 * NO_ENCONTRADO: el catálogo de resultados no distingue la revocación, y distinguirla hacia
 * afuera contaría el estado del enlace a quien no tiene por qué saberlo.
 */
@Service
public class ConsumirInvitacion {
    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumirInvitacion.class);
    private final Datos datos;
    private final Reloj reloj;
    private final SecretoDeInvitacion secretos;

    public ConsumirInvitacion(Datos datos, Reloj reloj, SecretoDeInvitacion secretos) {
        this.datos = datos;
        this.reloj = reloj;
        this.secretos = secretos;
    }

    @Transactional
    public Optional<Consumo> ejecutar(
            UUID tokenId, UUID grupoId, UUID clave, String token, Origen origen, ContextoSesion ctx) {
        if (tokenId == null || grupoId == null || clave == null || token == null || !token.matches("[0-9a-f]{64}"))
            return Optional.empty();
        return datos.conContexto(ctx, dsl -> {
            var fila = dsl.fetchOne(
                    """
                SELECT t.*, a.grupo_destino_id, a.telefono_destino, a.clave_consumo,a.consumidor_id
                FROM identidad.token_verificacion t JOIN identidad.alcance_invitacion a ON a.token_id=t.id
                WHERE t.id=? AND a.grupo_destino_id=? FOR UPDATE OF t,a
                """,
                    tokenId,
                    grupoId);
            var ahora = reloj.ahora().atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
            if (fila == null) {
                BITACORA.warn("canje de invitacion sobre token inexistente o de otro grupo tokenId={}", tokenId);
                return Optional.empty();
            }
            var usuario = dsl.fetchOne("SELECT telefono_e164 FROM identidad.usuario WHERE id=?", ctx.usuarioId());
            if (usuario == null || !usuario.get(0, String.class).equals(fila.get("telefono_destino", String.class))) {
                intento(dsl, tokenId, "CANAL_NO_COINCIDE", origen, ahora);
                return Optional.empty();
            }
            boolean coincide = MessageDigest.isEqual(
                    secretos.firmar("hash", token).getBytes(StandardCharsets.UTF_8),
                    fila.get("hash_token", String.class).getBytes(StandardCharsets.UTF_8));
            String estado = fila.get("estado", String.class);
            if ("CONSUMIDO".equals(estado)) {
                intento(dsl, tokenId, "YA_CONSUMIDO", origen, ahora);
                return coincide
                                && clave.equals(fila.get("clave_consumo", UUID.class))
                                && ctx.usuarioId().equals(fila.get("consumidor_id", UUID.class))
                        ? Optional.of(new Consumo(
                                tokenId,
                                grupoId,
                                ctx.usuarioId(),
                                clave,
                                fila.get("consumido_en", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC)))
                        : Optional.empty();
            }
            if (!"EMITIDO".equals(estado)) {
                intento(
                        dsl,
                        tokenId,
                        "BLOQUEADO_POR_INTENTOS".equals(estado) ? "BLOQUEADO_POR_INTENTOS" : "NO_ENCONTRADO",
                        origen,
                        ahora);
                return Optional.empty();
            }
            if (!ahora.isBefore(fila.get("expira_en", OffsetDateTime.class))) {
                intento(dsl, tokenId, "EXPIRADO", origen, ahora);
                return Optional.empty();
            }
            if (!coincide) {
                dsl.execute(
                        """
                    UPDATE identidad.token_verificacion SET intentos_fallidos=intentos_fallidos+1,
                    estado=CASE WHEN intentos_fallidos+1>=max_intentos THEN 'BLOQUEADO_POR_INTENTOS' ELSE estado END
                    WHERE id=?
                    """,
                        tokenId);
                intento(dsl, tokenId, "CODIGO_INCORRECTO", origen, ahora);
                return Optional.empty();
            }
            dsl.execute(
                    "UPDATE identidad.token_verificacion SET estado='CONSUMIDO',consumido_en=?::timestamptz WHERE id=?",
                    ahora,
                    tokenId);
            dsl.execute(
                    "UPDATE identidad.alcance_invitacion SET clave_consumo=?,consumidor_id=? WHERE token_id=?",
                    clave,
                    ctx.usuarioId(),
                    tokenId);
            intento(dsl, tokenId, "VALIDO", origen, ahora);
            BITACORA.info("invitacion consumida tokenId={} grupoId={}", tokenId, grupoId);
            return Optional.of(new Consumo(tokenId, grupoId, ctx.usuarioId(), clave, ahora));
        });
    }

    private void intento(DSLContext dsl, UUID tokenId, String resultado, Origen origen, OffsetDateTime ahora) {
        dsl.execute(
                """
            INSERT INTO identidad.intento_validacion_token (token_id,fecha_hora,resultado,ip_origen,agente_usuario)
            VALUES (?,?::timestamptz,?,?::inet,?)
            """,
                tokenId,
                ahora,
                resultado,
                origen.ip(),
                origen.agente().length() <= 255
                        ? origen.agente()
                        : origen.agente().substring(0, 255));
    }

    /**
     * Revoca el enlace; repetir la revocación del mismo emisor sigue respondiendo verdadero
     * (el cliente puede reintentar sin saber si la red perdió la respuesta). Un enlace ya
     * consumido, o de otro emisor, no se revoca.
     */
    @Transactional
    public boolean revocar(UUID tokenId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            int filas = dsl.execute(
                    """
                UPDATE identidad.token_verificacion t SET estado='INVALIDADO',invalidado_en=?::timestamptz,
                motivo_invalidacion='REVOCADA_POR_EMISOR' FROM identidad.alcance_invitacion a
                WHERE t.id=a.token_id AND t.id=? AND a.emisor_id=? AND t.estado IN ('EMITIDO','ENVIADO')
                """,
                    reloj.ahora().atOffset(ZoneOffset.UTC),
                    tokenId,
                    ctx.usuarioId());
            if (filas == 1) {
                BITACORA.info("invitacion revocada tokenId={} emisorId={}", tokenId, ctx.usuarioId());
                return true;
            }
            return dsl.fetchOne(
                            """
                SELECT 1 FROM identidad.token_verificacion t JOIN identidad.alcance_invitacion a ON a.token_id=t.id
                WHERE t.id=? AND a.emisor_id=? AND t.estado='INVALIDADO'
                AND t.motivo_invalidacion='REVOCADA_POR_EMISOR'
                """,
                            tokenId,
                            ctx.usuarioId())
                    != null;
        });
    }

    /** Origen del intento: dirección y agente que observó el borde (nunca el secreto). */
    public record Origen(String ip, String agente) {}

    public record Consumo(UUID tokenId, UUID grupoId, UUID usuarioId, UUID clave, OffsetDateTime consumidoEn) {}
}
