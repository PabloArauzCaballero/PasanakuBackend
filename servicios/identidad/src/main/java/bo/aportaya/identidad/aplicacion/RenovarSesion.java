package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El refresh de sesion del backoffice (ADR-010): se emite al abrir la sesion web y se
 * rota en cada uso.
 *
 * <p>El modelo ya estaba en la base y este caso de uso solo lo usa: el token vive en
 * {@code token_verificacion} con {@code tipo_token = 'REFRESCO'} y una {@code familia_id}
 * que lo ata a su sesion ({@code sesion.refresco_familia_id}); {@code uq_token_refresco_vivo}
 * deja un solo token vivo por familia, y el trigger {@code fn_seg_detectar_reuso_refresco}
 * (R-SEG-09) es el que revoca la familia y la sesion cuando alguien consume un token que ya
 * no estaba {@code EMITIDO}. La revocacion la hace la base, no este codigo: asi no depende
 * de que nadie se acuerde de llamarla.
 *
 * <p><b>Se guarda el hash, no el token.</b> El valor en claro sale una sola vez, en la
 * cookie; nunca se loguea.
 */
@Service
public class RenovarSesion {

    private static final String PROPOSITO = "REFRESCO_SESION";

    /** Treinta y dos bytes de azar: un refresh adivinable es una sesion para cualquiera. */
    private static final int BYTES_DE_AZAR = 32;

    /**
     * La carrera que ADR-010 pide resolver: dos pestanas que recargan a la vez mandan el
     * mismo refresh. La segunda llega con el token ya consumido; dentro de esta ventana se
     * la rechaza SIN revocar, porque es el mismo navegador y no un robo.
     */
    static final Duration GRACIA = Duration.ofSeconds(10);

    private final Datos datos;
    private final UsuarioRepositorio usuarios;
    private final Reloj reloj;
    private final Ids ids;
    private final SecureRandom azar = new SecureRandom();

    public RenovarSesion(Datos datos, UsuarioRepositorio usuarios, Reloj reloj, Ids ids) {
        this.datos = datos;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.ids = ids;
    }

    /** Primer refresh de la familia, al abrir la sesion web. */
    @Transactional
    public Emitido emitir(UUID usuarioId, UUID sesionId, String ip, String agente, String trazaId) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(contexto(trazaId), dsl -> {
            UUID familia = ids.nuevo();
            dsl.execute(
                    "UPDATE identidad.sesion SET refresco_familia_id = ? WHERE id = ? AND usuario_id = ?",
                    familia,
                    sesionId,
                    usuarioId);
            return insertar(dsl, usuarioId, sesionId, familia, null, ip, agente, trazaId, ahora);
        });
    }

    /**
     * Consume el refresh de la cookie y entrega el siguiente de la misma familia.
     *
     * <p>No lanza: un rechazo vuelve como resultado para que la transaccion haga
     * {@code COMMIT}. Si el rechazo es por reuso, lo que se tiene que guardar es justamente
     * la revocacion que hizo el trigger; un {@code ROLLBACK} la desharia.
     */
    @Transactional
    public Renovacion renovar(String enClaro, String ip, String agente, String trazaId) {
        if (enClaro == null || enClaro.isBlank()) {
            return Renovacion.rechazada();
        }
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(contexto(trazaId), dsl -> {
            var fila = dsl.fetchOne(
                    """
                    SELECT t.id, t.usuario_id, t.familia_id, t.estado, t.consumido_en, t.expira_en,
                           s.id AS sesion_id, s.revocada_en, s.expira_en AS sesion_expira_en
                      FROM identidad.token_verificacion t
                      JOIN identidad.sesion s ON s.refresco_familia_id = t.familia_id
                     WHERE t.tipo_token = 'REFRESCO'
                       AND t.hash_token = ?
                       FOR UPDATE OF t, s
                    """,
                    sha256(enClaro));
            if (fila == null) {
                return Renovacion.rechazada();
            }
            UUID tokenId = fila.get("id", UUID.class);
            String estado = fila.get("estado", String.class);
            OffsetDateTime consumidoEn = fila.get("consumido_en", OffsetDateTime.class);

            if ("CONSUMIDO".equals(estado)) {
                if (consumidoEn != null && consumidoEn.isAfter(ahora.minus(GRACIA))) {
                    return Renovacion.rechazada(); // carrera del mismo navegador: no se revoca
                }
                // Reuso fuera de la ventana: se vuelve a consumir y el trigger R-SEG-09
                // invalida la familia y revoca la sesion. Queda escrito con el COMMIT.
                dsl.execute("UPDATE identidad.token_verificacion SET estado = 'CONSUMIDO' WHERE id = ?", tokenId);
                return Renovacion.rechazada();
            }

            boolean vivo = "EMITIDO".equals(estado)
                    && fila.get("expira_en", OffsetDateTime.class).isAfter(ahora)
                    && fila.get("revocada_en", OffsetDateTime.class) == null
                    && fila.get("sesion_expira_en", OffsetDateTime.class).isAfter(ahora);
            if (!vivo) {
                return Renovacion.rechazada();
            }

            // La precondicion va en la escritura: si otro lo consumio entre el SELECT y
            // aca, este UPDATE no toca filas y no se emite un segundo hijo.
            int consumidos = dsl.execute(
                    """
                    UPDATE identidad.token_verificacion
                       SET estado = 'CONSUMIDO', consumido_en = ?::timestamptz
                     WHERE id = ? AND estado = 'EMITIDO'
                    """,
                    ahora,
                    tokenId);
            if (consumidos != 1) {
                return Renovacion.rechazada();
            }
            UUID usuarioId = fila.get("usuario_id", UUID.class);
            UUID sesionId = fila.get("sesion_id", UUID.class);
            dsl.execute(
                    "UPDATE identidad.sesion SET ultima_actividad_en = ?::timestamptz WHERE id = ?", ahora, sesionId);
            Emitido siguiente = insertar(
                    dsl, usuarioId, sesionId, fila.get("familia_id", UUID.class), tokenId, ip, agente, trazaId, ahora);
            boolean esOperador = usuarios.perfilDe(dsl, usuarioId).esOperador();
            return new Renovacion(Optional.of(usuarioId), esOperador, Optional.of(siguiente));
        });
    }

    private Emitido insertar(
            DSLContext dsl,
            UUID usuarioId,
            UUID sesionId,
            UUID familia,
            UUID rotadoDe,
            String ip,
            String agente,
            String trazaId,
            OffsetDateTime ahora) {
        var politica =
                dsl.fetchOne("SELECT id, ttl_segundos FROM identidad.politica_token WHERE proposito = ?", PROPOSITO);
        if (politica == null) {
            // Denegar por omision: sin politica sembrada no se inventa una vigencia.
            throw new ErrorDeNegocio(CodigoError.de(4, 8), "No hay politica de token para renovar la sesion.");
        }
        byte[] bytes = new byte[BYTES_DE_AZAR];
        azar.nextBytes(bytes);
        String enClaro = HexFormat.of().formatHex(bytes);
        UUID id = ids.nuevo();
        OffsetDateTime expira = ahora.plusSeconds(politica.get("ttl_segundos", Integer.class));
        dsl.execute(
                """
                INSERT INTO identidad.token_verificacion
                    (id, usuario_id, politica_id, dispositivo_id, tipo_token, proposito, hash_token,
                     algoritmo_hash, canal_entrega, destino_enmascarado, estado, emitido_en, expira_en,
                     intentos_fallidos, max_intentos, reenvios, uso_unico, clicks, familia_id,
                     rotado_de_id, ip_origen, agente_usuario, correlation_id, clave_idempotencia)
                SELECT ?, ?, ?, s.dispositivo_id, 'REFRESCO', ?, ?,
                       'SHA-256', 'COOKIE_HTTPONLY', 'navegador', 'EMITIDO', ?::timestamptz, ?::timestamptz,
                       0, 1, 0, true, 0, ?, ?, ?::inet, ?, ?, ?
                  FROM identidad.sesion s
                 WHERE s.id = ?
                """,
                id,
                usuarioId,
                politica.get("id", UUID.class),
                PROPOSITO,
                sha256(enClaro),
                ahora,
                expira,
                familia,
                rotadoDe,
                ip,
                agente.length() > 255 ? agente.substring(0, 255) : agente,
                trazaUuid(trazaId),
                id.toString(),
                sesionId);
        return new Emitido(enClaro, expira);
    }

    /**
     * El SHA-256 en hexadecimal, calculado aca y no con {@code digest()} de pgcrypto: la
     * extension vive en {@code public}, que no esta en el {@code search_path} del rol del
     * servicio ({@code svc_identidad}), y en runtime la funcion no existe para el. Mismo
     * formato que {@code encode(digest(x, 'sha256'), 'hex')}.
     */
    static String sha256(String enClaro) {
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(enClaro.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 es obligatorio en toda JVM", e);
        }
    }

    private static ContextoSesion contexto(String trazaId) {
        // Igual que el ingreso: todavia no hay token de acceso valido, y las politicas de
        // fila del rol `sistema` son las que dejan leer la sesion por su familia.
        return ContextoSesion.deSistema(EntradaAutenticacion.PROCESO_INGRESO, new Traza(trazaId));
    }

    private static UUID trazaUuid(String trazaId) {
        try {
            return UUID.fromString(trazaId);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(trazaId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /** El refresh en claro sale UNA vez (a la cookie); de la base solo se recupera su hash. */
    public record Emitido(String token, OffsetDateTime expiraEn) {}

    /** Renovada (con titular, si es operador y el refresh siguiente) o rechazada (todo vacio). */
    public record Renovacion(Optional<UUID> usuarioId, boolean esOperador, Optional<Emitido> siguiente) {
        static Renovacion rechazada() {
            return new Renovacion(Optional.empty(), false, Optional.empty());
        }

        public boolean renovada() {
            return siguiente.isPresent();
        }
    }
}
