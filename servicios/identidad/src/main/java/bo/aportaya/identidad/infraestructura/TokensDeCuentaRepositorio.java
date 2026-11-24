package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Los tokens de un solo uso de la invitacion y la recuperacion del operador (P-1, P-4).
 *
 * <p>Solo se persiste el hash con pepper ({@link SecretoDeInvitacion}, HMAC con una llave que vive
 * fuera de la base): el valor plano existe en el correo y en la memoria de quien lo abre, nunca en
 * una columna ni en un log (regla 90.2.9). El proposito va en el token: uno de invitacion no sirve
 * para restablecer una clave, ni al reves.
 */
@Component
public class TokensDeCuentaRepositorio {

    private static final String CANAL = "CORREO";
    private static final String VIGENTE =
            """
            SELECT id, usuario_id, proposito, expira_en, intentos_fallidos FROM identidad.token_verificacion
            WHERE hash_token = ?
              AND proposito IN ('INVITACION_OPERADOR','RECUPERACION_OPERADOR_CLAVE','REINSCRIPCION_FACTOR')
              AND estado IN ('EMITIDO','ENVIADO') AND expira_en > ?::timestamptz
              AND intentos_fallidos < max_intentos
            """;
    private static final String VIGENTE_BLOQUEANDO = VIGENTE + "FOR UPDATE";

    private final SecretoDeInvitacion secretos;
    private final SecureRandom azar = new SecureRandom();

    public TokensDeCuentaRepositorio(SecretoDeInvitacion secretos) {
        this.secretos = secretos;
    }

    /** El token plano solo viaja en este valor: se manda al correo y se descarta. */
    public record Emitido(UUID tokenId, String token, OffsetDateTime expiraEn) {
        @Override
        public String toString() {
            return "Emitido[tokenId=" + tokenId + ", token=REDACTADO]";
        }
    }

    public record Vigente(
            UUID tokenId, UUID usuarioId, AlcanceDeCuenta alcance, OffsetDateTime expiraEn, int intentosFallidos) {}

    /**
     * Emite un token nuevo e invalida los vivos del mismo usuario y proposito: una sola invitacion
     * viva por operador y alcance (reemitir mata la anterior).
     */
    public Emitido emitir(
            DSLContext dsl,
            UUID usuarioId,
            AlcanceDeCuenta alcance,
            String destinoEnmascarado,
            OffsetDateTime ahora,
            String ip,
            String agente,
            UUID correlacion) {
        var politica = dsl.fetchOne(
                """
                        SELECT id, ttl_segundos, max_intentos_validacion FROM identidad.politica_token
                        WHERE proposito = ? AND vigente_desde <= ?::timestamptz
                        ORDER BY vigente_desde DESC LIMIT 1
                        """,
                alcance.proposito(),
                ahora);
        if (politica == null) {
            throw new IllegalStateException(
                    "Falta la politica de token " + alcance.proposito() + " (sembrar 13-politicas-de-token)");
        }
        dsl.execute(
                """
                        UPDATE identidad.token_verificacion SET estado = 'INVALIDADO', invalidado_en = ?::timestamptz,
                        motivo_invalidacion = 'REEMITIDA_POR_NUEVA_EMISION'
                        WHERE usuario_id = ? AND proposito = ? AND estado IN ('EMITIDO','ENVIADO')
                        """,
                ahora,
                usuarioId,
                alcance.proposito());
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id = UUID.randomUUID();
        OffsetDateTime expira = ahora.plusSeconds(politica.get("ttl_segundos", Integer.class));
        dsl.execute(
                """
                        INSERT INTO identidad.token_verificacion (id, usuario_id, politica_id, tipo_token, proposito,
                        hash_token, algoritmo_hash, canal_entrega, destino_enmascarado, estado, emitido_en,
                        expira_en, intentos_fallidos, max_intentos, reenvios, ip_origen, agente_usuario,
                        correlation_id, clave_idempotencia, uso_unico, clicks)
                        VALUES (?, ?, ?, 'ENLACE', ?, ?, 'HMAC-SHA256', ?, ?, 'EMITIDO', ?::timestamptz,
                        ?::timestamptz, 0, ?, 0, ?::inet, ?, ?, ?, true, 0)
                        """,
                id,
                usuarioId,
                politica.get("id", UUID.class),
                alcance.proposito(),
                secretos.firmar("cuenta-token", token),
                CANAL,
                destinoEnmascarado,
                ahora,
                expira,
                politica.get("max_intentos_validacion", Short.class),
                ip,
                recortar(agente, 255),
                correlacion,
                "CUENTA:" + id);
        return new Emitido(id, token, expira);
    }

    /**
     * El token vivo que corresponde a ese valor: no vencido, no consumido, no invalidado y con
     * intentos disponibles. {@code paraActualizar} toma el candado de la fila: completar y consumir
     * son una sola decision.
     */
    public Optional<Vigente> vigente(DSLContext dsl, String token, OffsetDateTime ahora, boolean paraActualizar) {
        if (token == null || token.length() < 20 || token.length() > 200) {
            return Optional.empty();
        }
        var fila = dsl.fetchOne(
                paraActualizar ? VIGENTE_BLOQUEANDO : VIGENTE,
                secretos.firmar("cuenta-token", token),
                ahora);
        if (fila == null) {
            return Optional.empty();
        }
        return AlcanceDeCuenta.dePropositoDeToken(fila.get("proposito", String.class))
                .map(alcance -> new Vigente(
                        fila.get("id", UUID.class),
                        fila.get("usuario_id", UUID.class),
                        alcance,
                        fila.get("expira_en", OffsetDateTime.class),
                        fila.get("intentos_fallidos", Short.class)));
    }

    /** Un intento fallido que NO consume el token; al tope, el token muere. */
    public void registrarIntentoFallido(DSLContext dsl, UUID tokenId, OffsetDateTime ahora) {
        dsl.execute(
                """
                        UPDATE identidad.token_verificacion SET intentos_fallidos = intentos_fallidos + 1,
                        estado = CASE WHEN intentos_fallidos + 1 >= max_intentos THEN 'BLOQUEADO_POR_INTENTOS'
                        ELSE estado END,
                        invalidado_en = CASE WHEN intentos_fallidos + 1 >= max_intentos THEN ?::timestamptz
                        ELSE invalidado_en END
                        WHERE id = ?
                        """,
                ahora,
                tokenId);
    }

    /** La precondicion va en la escritura: dos que compiten, gana una. */
    public boolean consumir(DSLContext dsl, UUID tokenId, OffsetDateTime ahora) {
        return dsl.execute(
                        """
                                UPDATE identidad.token_verificacion SET estado = 'CONSUMIDO', consumido_en = ?::timestamptz
                                WHERE id = ? AND estado IN ('EMITIDO','ENVIADO') AND expira_en > ?::timestamptz
                                """,
                        ahora,
                        tokenId,
                        ahora)
                == 1;
    }

    public Optional<String> nombresDe(DSLContext dsl, UUID usuarioId) {
        return dsl.fetchOptional("SELECT nombres FROM identidad.usuario WHERE id = ?", usuarioId)
                .map(fila -> fila.get(0, String.class));
    }

    /** {@code o***@dominio}: suficiente para reconocerlo, nada para reconstruirlo. */
    public static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }

    private static String recortar(String texto, int largo) {
        String base = texto == null || texto.isBlank() ? "desconocido" : texto;
        byte[] bytes = base.getBytes(StandardCharsets.UTF_8);
        return bytes.length <= largo ? base : new String(bytes, 0, largo, StandardCharsets.UTF_8);
    }
}
