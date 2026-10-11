package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emisión idempotente vinculada al emisor, grupo y teléfono; secreto fuera de la base.
 *
 * <p>Límite de emisión: {@code max_emisiones_por_dia} de la política vigente, contado por emisor
 * en las últimas 24 horas y bajo bloqueo por emisor (dos emisiones simultáneas no lo superan).
 * Reemisión: {@code invalida_anteriores} de la política decide, de forma explícita, si la
 * emisión nueva revoca las anteriores vivas del mismo grupo y teléfono. Nada se revoca por accidente.
 */
@Service
public class EmitirTokenDeInvitacion {
    private static final Logger BITACORA = LoggerFactory.getLogger(EmitirTokenDeInvitacion.class);
    private final Datos datos;
    private final Reloj reloj;
    private final Ids ids;
    private final SecretoDeInvitacion secretos;

    public EmitirTokenDeInvitacion(Datos datos, Reloj reloj, Ids ids, SecretoDeInvitacion secretos) {
        this.datos = datos;
        this.reloj = reloj;
        this.ids = ids;
        this.secretos = secretos;
    }

    @Transactional
    public Emitido ejecutar(Entrada e, ContextoSesion ctx) {
        if (e.clave() == null
                || e.grupoId() == null
                || e.telefono() == null
                || !e.telefono().matches("\\+591[0-9]{8}")
                || e.canal() == null) {
            throw invalida();
        }
        String huella = secretos.firmar(
                "solicitud", ctx.usuarioId() + "|" + e.grupoId() + "|" + e.telefono() + "|" + e.canal());
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        return datos.conContexto(ctx, dsl -> {
            dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "invitacion:" + e.clave());
            var anterior = dsl.fetchOne(
                    """
                SELECT a.*, t.expira_en, t.estado FROM identidad.alcance_invitacion a
                JOIN identidad.token_verificacion t ON t.id=a.token_id WHERE a.clave_emision=?
                """,
                    e.clave());
            if (anterior != null) {
                if (!huella.equals(anterior.get("huella_solicitud", String.class))) throw invalida();
                // Un enlace revocado o reemitido no se "recupera": devolverlo daria un enlace muerto.
                if ("INVALIDADO".equals(anterior.get("estado", String.class))) throw invalida();
                UUID id = anterior.get("token_id", UUID.class);
                return new Emitido(
                        id,
                        secretos.firmar("enlace", id + ":" + anterior.get("nonce", String.class)),
                        anterior.get("expira_en", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC));
            }
            var politica = dsl.fetchOne(
                    """
                SELECT * FROM identidad.politica_token WHERE proposito='INVITACION_GRUPO'
                AND vigente_desde<=?::timestamptz ORDER BY vigente_desde DESC LIMIT 1
                """,
                    ahora);
            if (politica == null
                    || !Arrays.asList(politica.get("canales_permitidos", String.class)
                                    .split("[,|;\\s]+"))
                            .contains(e.canal())) throw invalida();
            dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "invitacion-emisor:" + ctx.usuarioId());
            var recientes = dsl.fetchOne(
                    """
                SELECT count(*) FROM identidad.alcance_invitacion a
                JOIN identidad.token_verificacion t ON t.id=a.token_id
                WHERE a.emisor_id=? AND t.emitido_en > ?::timestamptz
                """,
                    ctx.usuarioId(),
                    ahora.minusDays(1));
            if (recientes.get(0, Long.class) >= politica.get("max_emisiones_por_dia", Short.class)) {
                BITACORA.warn("limite diario de invitaciones alcanzado emisorId={}", ctx.usuarioId());
                throw new ErrorDeNegocio(
                        CodigoError.de(69, 8), "Alcanzaste el limite diario de invitaciones. Proba manana.");
            }
            int revocadas = Boolean.TRUE.equals(politica.get("invalida_anteriores", Boolean.class))
                    ? revocarVivas(dsl, e.grupoId(), e.telefono(), ahora)
                    : 0;
            UUID id = ids.nuevo();
            String nonce = secretos.nonce();
            String token = secretos.firmar("enlace", id + ":" + nonce);
            var expira = ahora.plusSeconds(politica.get("ttl_segundos", Integer.class));
            dsl.execute(
                    """
                INSERT INTO identidad.token_verificacion
                (id,usuario_id,politica_id,tipo_token,proposito,hash_token,algoritmo_hash,
                 canal_entrega,destino_enmascarado,estado,emitido_en,expira_en,intentos_fallidos,
                 max_intentos,reenvios,uso_unico,clicks,ip_origen,agente_usuario,correlation_id,clave_idempotencia)
                VALUES (?, ?, ?, 'ENLACE','INVITACION_GRUPO',?,'HMAC-SHA256',?,?,'EMITIDO',
                        ?::timestamptz,?::timestamptz,0,?,0,true,0,?::inet,?,?,?)
                """,
                    id,
                    ctx.usuarioId(),
                    politica.get("id", UUID.class),
                    secretos.firmar("hash", token),
                    e.canal(),
                    "+591****" + e.telefono().substring(e.telefono().length() - 4),
                    ahora,
                    expira,
                    politica.get("max_intentos_validacion", Short.class),
                    e.ip(),
                    e.agente(),
                    UUID.fromString(ctx.traza().id()),
                    e.clave().toString());
            dsl.execute(
                    """
                INSERT INTO identidad.alcance_invitacion
                (id,token_id,grupo_destino_id,emisor_id,telefono_destino,nonce,clave_emision,huella_solicitud)
                VALUES (?,?,?,?,?,?,?,?)
                """,
                    ids.nuevo(),
                    id,
                    e.grupoId(),
                    ctx.usuarioId(),
                    e.telefono(),
                    nonce,
                    e.clave(),
                    huella);
            BITACORA.info(
                    "invitacion emitida tokenId={} grupoId={} emisorId={} revocadasPorReemision={}",
                    id,
                    e.grupoId(),
                    ctx.usuarioId(),
                    revocadas);
            return new Emitido(id, token, expira);
        });
    }

    private int revocarVivas(org.jooq.DSLContext dsl, UUID grupoId, String telefono, OffsetDateTime ahora) {
        return dsl.execute(
                """
            UPDATE identidad.token_verificacion t SET estado='INVALIDADO',invalidado_en=?::timestamptz,
            motivo_invalidacion='REEMITIDA_POR_NUEVA_EMISION' FROM identidad.alcance_invitacion a
            WHERE t.id=a.token_id AND a.grupo_destino_id=? AND a.telefono_destino=?
            AND t.estado IN ('EMITIDO','ENVIADO')
            """,
                ahora,
                grupoId,
                telefono);
    }

    private ErrorDeNegocio invalida() {
        return new ErrorDeNegocio(CodigoError.de(69, 5), "No se pudo emitir esa invitacion.");
    }

    public record Entrada(UUID clave, UUID grupoId, String telefono, String canal, String ip, String agente) {}

    public record Emitido(UUID tokenId, String token, OffsetDateTime expiraEn) {
        @Override
        public String toString() {
            return "Emitido[tokenId=" + tokenId + ", token=REDACTADO]";
        }
    }
}
