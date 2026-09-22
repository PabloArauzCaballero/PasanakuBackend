package bo.aportaya.nucleofinanciero.infraestructura;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * El doble de {@code identidad} para el contrato de evidencia step-up (§3 del
 * encargo, `docs/auditoria-produccion/contratos/step-up-jwt.md`).
 *
 * <p><b>Por que un doble y no esperar a Richard.</b> Regla 65: un contrato ajeno que
 * todavia no esta en {@code dev} se simula en tres niveles bajo {@code src/test}, y el
 * hito se cierra contra el doble — declarado, no escondido. Cuando el PR de Richard
 * llegue a {@code dev}, la integracion real (H2.S4.M2) reemplaza el JWKS de este doble
 * por el real; {@code SegundoFactorStepUp} no cambia una linea porque valida contra
 * CUALQUIER JWKS que {@code aportaya.jwt.jwks-uri} le apunte.
 *
 * <p><b>Los tres niveles</b> (`test-case-design-techniques`):
 *
 * <ul>
 *   <li><b>correcto</b>: {@link #correcto} — todos los claims del contrato, vigente.
 *   <li><b>limite</b>: {@link #expiraEnUnSegundo}, {@link #expiroHaceUnSegundo},
 *       {@link #mismoJti} — el umbral exacto de vigencia, y el reuso del {@code jti}.
 *   <li><b>invalido</b>: {@link #firmadaConOtraClave}, {@link #otroSujeto},
 *       {@link #otroProposito}, {@link #otraAudiencia}, {@link #sinAcrMfa} — cada
 *       claim que el validador tiene que rechazar, uno a la vez.
 * </ul>
 */
public final class EmisorDeEvidenciaDePrueba implements AutoCloseable {

    public static final String ISS = "aportaya-identidad";
    public static final String AUD = "aportaya-nucleo-financiero";
    public static final String KID = "kid-prueba-step-up";

    private final RSAKey clave;
    private final RSAKey claveAjena;
    private final HttpServer servidor;

    public EmisorDeEvidenciaDePrueba() {
        try {
            this.clave = new RSAKeyGenerator(2048).keyID(KID).generate();
            // Una segunda clave, para firmar evidencia "de otra identidad" en el
            // nivel invalido: el JWKS de este servidor solo publica la primera.
            this.claveAjena = new RSAKeyGenerator(2048).keyID("kid-ajeno").generate();
            this.servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/.well-known/jwks.json", intercambio -> {
                byte[] cuerpo = jwksJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, cuerpo.length);
                try (var salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            });
            servidor.setExecutor(Executors.newSingleThreadExecutor());
            servidor.start();
        } catch (JOSEException | IOException e) {
            throw new IllegalStateException("No se pudo armar el doble de identidad", e);
        }
    }

    /** {@code aportaya.jwt.jwks-uri} de este doble. */
    public URI jwksUri() {
        return URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/.well-known/jwks.json");
    }

    @Override
    public void close() {
        servidor.stop(0);
    }

    // ---- nivel correcto -----------------------------------------------------------

    /** Todos los claims del contrato, vigente, {@code jti} nuevo. */
    public String correcto(UUID usuarioId) {
        return firmar(constructorBase(usuarioId, UUID.randomUUID()), clave);
    }

    // ---- nivel limite ---------------------------------------------------------------

    /** {@code exp} = ahora + 1s: todavia vigente. */
    public String expiraEnUnSegundo(UUID usuarioId) {
        Instant ahora = Instant.now();
        return firmar(
                constructorBase(usuarioId, UUID.randomUUID())
                        .issueTime(Date.from(ahora.minus(4, ChronoUnit.MINUTES).minus(59, ChronoUnit.SECONDS)))
                        .expirationTime(Date.from(ahora.plusSeconds(1))),
                clave);
    }

    /** {@code exp} = ahora - 1s: mas alla de la tolerancia, vencida. */
    public String expiroHaceUnSegundo(UUID usuarioId) {
        Instant ahora = Instant.now();
        return firmar(
                constructorBase(usuarioId, UUID.randomUUID())
                        .issueTime(Date.from(ahora.minus(5, ChronoUnit.MINUTES).minus(1, ChronoUnit.SECONDS)))
                        .expirationTime(Date.from(ahora.minusSeconds(1))),
                clave);
    }

    /** El mismo {@code jti} para dos evidencias: la segunda tiene que rechazarse. */
    public String conJti(UUID usuarioId, UUID jti) {
        return firmar(constructorBase(usuarioId, jti), clave);
    }

    // ---- nivel invalido -------------------------------------------------------------

    /** Firmada con una clave que el JWKS de este doble NO publica. */
    public String firmadaConOtraClave(UUID usuarioId) {
        return firmar(constructorBase(usuarioId, UUID.randomUUID()), claveAjena);
    }

    /** {@code sub} distinto del usuario de la sesion que pide el retiro. */
    public String otroSujeto() {
        return firmar(constructorBase(UUID.randomUUID(), UUID.randomUUID()), clave);
    }

    /** {@code proposito=ADMIN} en vez de {@code RETIRO}. */
    public String otroProposito(UUID usuarioId) {
        Instant ahora = Instant.now();
        return firmar(
                new JWTClaimsSet.Builder()
                        .issuer(ISS)
                        .audience(AUD)
                        .subject(usuarioId.toString())
                        .jwtID(UUID.randomUUID().toString())
                        .issueTime(Date.from(ahora))
                        .expirationTime(Date.from(ahora.plus(5, ChronoUnit.MINUTES)))
                        .claim("proposito", "ADMIN")
                        .claim("desafio_id", UUID.randomUUID().toString())
                        .claim("acr", "mfa")
                        .claim("amr", List.of("totp")),
                clave);
    }

    /** {@code aud} sin {@code aportaya-nucleo-financiero}. */
    public String otraAudiencia(UUID usuarioId) {
        Instant ahora = Instant.now();
        return firmar(
                new JWTClaimsSet.Builder()
                        .issuer(ISS)
                        .audience("aportaya-otro-servicio")
                        .subject(usuarioId.toString())
                        .jwtID(UUID.randomUUID().toString())
                        .issueTime(Date.from(ahora))
                        .expirationTime(Date.from(ahora.plus(5, ChronoUnit.MINUTES)))
                        .claim("proposito", "RETIRO")
                        .claim("desafio_id", UUID.randomUUID().toString())
                        .claim("acr", "mfa")
                        .claim("amr", List.of("totp")),
                clave);
    }

    /** Sin {@code acr=mfa}. */
    public String sinAcrMfa(UUID usuarioId) {
        Instant ahora = Instant.now();
        return firmar(
                new JWTClaimsSet.Builder()
                        .issuer(ISS)
                        .audience(AUD)
                        .subject(usuarioId.toString())
                        .jwtID(UUID.randomUUID().toString())
                        .issueTime(Date.from(ahora))
                        .expirationTime(Date.from(ahora.plus(5, ChronoUnit.MINUTES)))
                        .claim("proposito", "RETIRO")
                        .claim("desafio_id", UUID.randomUUID().toString())
                        .claim("amr", List.of("totp")),
                clave);
    }

    // ---- interno ----------------------------------------------------------------------

    private JWTClaimsSet.Builder constructorBase(UUID usuarioId, UUID jti) {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISS)
                .audience(AUD)
                .subject(usuarioId.toString())
                .jwtID(jti.toString())
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plus(5, ChronoUnit.MINUTES)))
                .claim("proposito", "RETIRO")
                .claim("desafio_id", UUID.randomUUID().toString())
                .claim("acr", "mfa")
                .claim("amr", List.of("totp"));
    }

    private String firmar(JWTClaimsSet.Builder claims, RSAKey conQue) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(conQue.getKeyID())
                            .build(),
                    claims.build());
            jwt.sign(new RSASSASigner(conQue));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar la evidencia de prueba", e);
        }
    }

    private String jwksJson() {
        // Un JWKS es {"keys":[ ... ]}; RSAKey.toPublicJWK().toJSONString() ya da el
        // JSON de UNA clave, asi que envolverlo a mano evita atarse a que tipo de
        // JSONObject trae nimbus-jose-jwt transitivamente.
        return "{\"keys\":[" + clave.toPublicJWK().toJSONString() + "]}";
    }
}
