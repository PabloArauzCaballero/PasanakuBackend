package bo.aportaya.identidad.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Persistencia y reglas atomicas del OTP que confirma el correo del alta. */
@Component
public class VerificacionCorreoRepositorio {

    private static final String PROPOSITO = "VERIFICACION_CORREO";

    private final Datos datos;
    private final Ids ids;
    private final ProteccionDeVerificacionCorreo proteccion;
    private final SecureRandom azar = new SecureRandom();

    public VerificacionCorreoRepositorio(Datos datos, Ids ids, ProteccionDeVerificacionCorreo proteccion) {
        this.datos = datos;
        this.ids = ids;
        this.proteccion = proteccion;
    }

    @Transactional
    public Preparada preparar(
            String correo, UUID claveIdempotencia, String ip, String agente, OffsetDateTime ahora, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            String huella = proteccion.huellaDestino(correo);
            Record existente = dsl.fetchOne(
                    """
                    SELECT id, destino_enmascarado, expira_en, estado, firma_hmac
                      FROM identidad.token_verificacion
                     WHERE usuario_id IS NULL AND clave_idempotencia = ?
                    """,
                    claveIdempotencia.toString());
            if (existente != null) {
                if (!proteccion.coincideDestino(existente.get("firma_hmac", String.class), correo)) {
                    throw new ErrorDeNegocio(CodigoError.de(1, 7), "La solicitud anterior corresponde a otro correo.");
                }
                String estado = existente.get("estado", String.class);
                if (!"EMITIDO".equals(estado) && !"ENVIADO".equals(estado)) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(1, 7), "La solicitud anterior ya no es util. Pedi un codigo nuevo.");
                }
                return new Preparada(
                        new Solicitada(
                                existente.get("id", UUID.class),
                                existente.get("destino_enmascarado", String.class),
                                existente.get("expira_en", OffsetDateTime.class)),
                        null,
                        false);
            }

            dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", huella);

            Record politica = dsl.fetchOne(
                    """
                    SELECT id, ttl_segundos, longitud_codigo::int AS longitud_codigo, max_intentos_validacion,
                           max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia
                      FROM identidad.politica_token
                     WHERE proposito = ? AND vigente_desde <= ?::timestamptz
                     ORDER BY vigente_desde DESC
                     LIMIT 1
                    """,
                    PROPOSITO,
                    ahora);
            if (politica == null || politica.get("longitud_codigo", Integer.class) != 6) {
                throw new ErrorDeNegocio(CodigoError.de(1, 7), "No hay una politica vigente para el codigo de correo.");
            }

            Integer emisiones = (Integer) dsl.fetchValue(
                    """
                    SELECT count(*)::int FROM identidad.token_verificacion
                     WHERE proposito = ? AND firma_hmac = ? AND emitido_en >= ?::timestamptz
                    """,
                    PROPOSITO,
                    huella,
                    ahora.minusDays(1));
            int maximas = politica.get("max_emisiones_por_dia", Short.class);
            if (emisiones != null && emisiones >= maximas) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 8), "Alcanzaste el limite diario de codigos para este correo.");
            }

            Integer emisionesEnLaHora = (Integer) dsl.fetchValue(
                    """
                    SELECT count(*)::int FROM identidad.token_verificacion
                     WHERE proposito = ? AND firma_hmac = ? AND emitido_en >= ?::timestamptz
                    """,
                    PROPOSITO,
                    huella,
                    ahora.minusHours(1));
            int reenviosMaximos = politica.get("max_reenvios_por_hora", Short.class);
            if (emisionesEnLaHora != null && emisionesEnLaHora >= reenviosMaximos + 1) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 8), "Alcanzaste el limite de codigos por hora para este correo.");
            }

            OffsetDateTime ultima = (OffsetDateTime) dsl.fetchValue(
                    """
                    SELECT max(emitido_en) FROM identidad.token_verificacion
                     WHERE proposito = ? AND firma_hmac = ?
                    """,
                    PROPOSITO,
                    huella);
            int espera = politica.get("cooldown_reenvio_segundos", Integer.class);
            if (ultima != null && ultima.plusSeconds(espera).isAfter(ahora)) {
                throw new ErrorDeNegocio(CodigoError.de(1, 8), "Espera un momento antes de pedir otra combinacion.");
            }

            dsl.execute(
                    """
                    UPDATE identidad.token_verificacion
                       SET estado = 'INVALIDADO', invalidado_en = ?::timestamptz,
                           motivo_invalidacion = 'Reemplazado por un codigo nuevo'
                     WHERE proposito = ? AND firma_hmac = ? AND estado IN ('EMITIDO', 'ENVIADO')
                    """,
                    ahora,
                    PROPOSITO,
                    huella);

            String codigo = String.format(Locale.ROOT, "%06d", azar.nextInt(1_000_000));
            UUID id = ids.nuevo();
            OffsetDateTime expira = ahora.plusSeconds(politica.get("ttl_segundos", Integer.class));
            dsl.execute(
                    """
                    INSERT INTO identidad.token_verificacion
                        (id, usuario_id, politica_id, tipo_token, proposito, hash_token,
                         algoritmo_hash, canal_entrega, destino_enmascarado, estado,
                         emitido_en, expira_en, intentos_fallidos, max_intentos, reenvios,
                         ip_origen, agente_usuario, correlation_id, clave_idempotencia,
                         longitud, firma_hmac, uso_unico)
                    VALUES (?, NULL, ?, 'OTP', ?, ?, 'HMAC-SHA256', 'CORREO', ?, 'EMITIDO',
                            ?::timestamptz, ?::timestamptz, 0, ?, 0, ?::inet, ?, ?, ?, 6, ?, true)
                    """,
                    id,
                    politica.get("id", UUID.class),
                    PROPOSITO,
                    proteccion.hashCodigo(correo, codigo),
                    TextoDeVerificacion.enmascarar(correo),
                    ahora,
                    expira,
                    politica.get("max_intentos_validacion", Short.class),
                    ip,
                    TextoDeVerificacion.limitar(agente, 255),
                    UUID.fromString(ctx.traza().id()),
                    claveIdempotencia.toString(),
                    huella);
            return new Preparada(new Solicitada(id, TextoDeVerificacion.enmascarar(correo), expira), codigo, true);
        });
    }

    @Transactional
    public void marcarEnviada(UUID id, OffsetDateTime ahora, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            dsl.execute(
                    "UPDATE identidad.token_verificacion SET estado = 'ENVIADO', enviado_en = ?::timestamptz WHERE id = ? AND estado = 'EMITIDO'",
                    ahora,
                    id);
            return null;
        });
    }

    @Transactional
    public void invalidarPorFalloDeEnvio(UUID id, OffsetDateTime ahora, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            dsl.execute(
                    """
                    UPDATE identidad.token_verificacion
                       SET estado = 'INVALIDADO', invalidado_en = ?::timestamptz, motivo_invalidacion = 'Fallo el proveedor de correo'
                     WHERE id = ? AND estado = 'EMITIDO'
                    """,
                    ahora,
                    id);
            return null;
        });
    }

    @Transactional(noRollbackFor = ErrorDeNegocio.class)
    public void confirmar(
            UUID id, String correo, String codigo, String ip, String agente, OffsetDateTime ahora, ContextoSesion ctx) {
        datos.conContexto(ctx, dsl -> {
            Record token = dsl.fetchOne(
                    """
                    SELECT hash_token, firma_hmac, estado, expira_en, intentos_fallidos, max_intentos
                      FROM identidad.token_verificacion
                     WHERE id = ? AND proposito = ?
                     FOR UPDATE
                    """,
                    id,
                    PROPOSITO);
            if (token == null) {
                throw new ErrorDeNegocio(CodigoError.de(1, 9), "El codigo no existe.");
            }
            String huella = proteccion.huellaDestino(correo);
            if (!java.security.MessageDigest.isEqual(
                    token.get("firma_hmac", String.class).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    huella.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                intento(dsl, id, "CANAL_NO_COINCIDE", ip, agente, ahora);
                throw new ErrorDeNegocio(CodigoError.de(1, 9), "El codigo no corresponde a este correo.");
            }
            boolean coincide = proteccion.coincide(token.get("hash_token", String.class), correo, codigo);
            if ("CONSUMIDO".equals(token.get("estado", String.class))) {
                if (coincide) return null;
                throw new ErrorDeNegocio(CodigoError.de(1, 9), "El codigo ya fue utilizado.");
            }
            if (!"ENVIADO".equals(token.get("estado", String.class))) {
                throw new ErrorDeNegocio(CodigoError.de(1, 9), "El codigo ya no esta disponible.");
            }
            if (!token.get("expira_en", OffsetDateTime.class).isAfter(ahora)) {
                dsl.execute(
                        "UPDATE identidad.token_verificacion SET estado = 'EXPIRADO', invalidado_en = ?::timestamptz WHERE id = ?",
                        ahora,
                        id);
                intento(dsl, id, "EXPIRADO", ip, agente, ahora);
                throw new ErrorDeNegocio(CodigoError.de(1, 10), "El codigo vencio. Pedi uno nuevo.");
            }
            int fallidos = token.get("intentos_fallidos", Short.class);
            int maximos = token.get("max_intentos", Short.class);
            if (!coincide) {
                int nuevos = fallidos + 1;
                dsl.execute(
                        """
                        UPDATE identidad.token_verificacion
                           SET intentos_fallidos = ?, estado = CASE WHEN ? >= max_intentos THEN 'INVALIDADO' ELSE estado END,
                               invalidado_en = CASE WHEN ? >= max_intentos THEN ?::timestamptz ELSE invalidado_en END,
                               motivo_invalidacion = CASE WHEN ? >= max_intentos THEN 'Demasiados intentos fallidos' ELSE motivo_invalidacion END
                         WHERE id = ?
                        """,
                        nuevos,
                        nuevos,
                        nuevos,
                        ahora,
                        nuevos,
                        id);
                intento(dsl, id, nuevos >= maximos ? "BLOQUEADO_POR_INTENTOS" : "CODIGO_INCORRECTO", ip, agente, ahora);
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 9),
                        nuevos >= maximos ? "El codigo fue bloqueado. Pedi uno nuevo." : "El codigo no es correcto.");
            }
            dsl.execute(
                    "UPDATE identidad.token_verificacion SET estado = 'CONSUMIDO', consumido_en = ?::timestamptz WHERE id = ?",
                    ahora,
                    id);
            intento(dsl, id, "VALIDO", ip, agente, ahora);
            return null;
        });
    }

    /** Se ejecuta dentro de la misma transaccion que crea al usuario. */
    public void vincularAlAlta(
            DSLContext dsl, UUID verificacionId, String correo, UUID usuarioId, OffsetDateTime ahora) {
        int filas = dsl.execute(
                """
                UPDATE identidad.token_verificacion
                   SET usuario_id = ?
                 WHERE id = ? AND proposito = ? AND estado = 'CONSUMIDO'
                   AND usuario_id IS NULL AND expira_en > ?::timestamptz AND firma_hmac = ?
                """,
                usuarioId,
                verificacionId,
                PROPOSITO,
                ahora,
                proteccion.huellaDestino(correo));
        if (filas != 1) {
            throw new ErrorDeNegocio(
                    CodigoError.de(1, 11), "Confirma el codigo correcto de este correo antes de crear la cuenta.");
        }
    }

    private void intento(
            DSLContext dsl, UUID tokenId, String resultado, String ip, String agente, OffsetDateTime ahora) {
        TextoDeVerificacion.registrarIntento(dsl, ids.nuevo(), tokenId, resultado, ip, agente, ahora);
    }

    public record Solicitada(UUID verificacionId, String destinoEnmascarado, OffsetDateTime expiraEn) {}

    public record Preparada(Solicitada salida, String codigo, boolean nueva) {}
}
