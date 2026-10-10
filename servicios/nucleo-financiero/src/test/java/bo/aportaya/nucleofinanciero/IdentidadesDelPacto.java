package bo.aportaya.nucleofinanciero;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Lo que rodea al servidor real en los pactos: una identidad que firma tokens con la forma que emite identidad
 * (ADR-024) y publica su JWKS por HTTP, y un doble de {@code aportes}. No es identidad: es la forma de su token.
 */
final class IdentidadesDelPacto implements AutoCloseable {

    static final String ESTADO_DE_APORTES =
            "{\"alDia\":true,\"totalAportado\":\"0.00\",\"deudaVigente\":\"0.00\",\"porAportar\":\"0.00\","
                    + "\"obligacionesAbiertas\":0,\"moneda\":\"BOB\"}";

    private final RSAKey claveDeIdentidad;
    private final RSAKey claveAjena;
    private final HttpServer jwks;
    private final HttpServer aportes;

    IdentidadesDelPacto() throws Exception {
        claveDeIdentidad =
                new RSAKeyGenerator(2048).keyID("identidad-del-pacto").generate();
        // Mismo kid, otra clave: la firma no verifica aunque el kid coincida.
        claveAjena = new RSAKeyGenerator(2048).keyID("identidad-del-pacto").generate();
        jwks = servir("/jwks.json", new JWKSet(claveDeIdentidad.toPublicJWK()).toString());
        aportes = servir("/aportes/participantes/", ESTADO_DE_APORTES);
    }

    String urlDelJwks() {
        return "http://127.0.0.1:" + jwks.getAddress().getPort() + "/jwks.json";
    }

    String urlDeAportes() {
        return "http://127.0.0.1:" + aportes.getAddress().getPort();
    }

    @Override
    public void close() {
        jwks.stop(0);
        aportes.stop(0);
    }

    private static HttpServer servir(String ruta, String cuerpo) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext(ruta, e -> {
            byte[] raw = cuerpo.getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().add("Content-Type", "application/json");
            e.sendResponseHeaders(200, raw.length);
            e.getResponseBody().write(raw);
            e.close();
        });
        s.start();
        return s;
    }

    /** Un token con la forma que emite identidad: rol = piso de la sesion, permisos = lo que puede. */
    String token(boolean firmaIdentidad, UUID sujeto, String rol, List<String> permisos, Instant vence) {
        RSAKey firmante = firmaIdentidad ? claveDeIdentidad : claveAjena;
        try {
            var claims = new JWTClaimsSet.Builder()
                    .subject(sujeto.toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(Date.from(vence))
                    .claim("rol", rol)
                    .claim("permisos", permisos)
                    .claim("dispositivo", "dispositivo-del-pacto")
                    .build();
            var jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(firmante.getKeyID())
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(firmante));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    String delServicio(String servicio) {
        return token(
                true,
                UUID.randomUUID(),
                "BACKOFFICE",
                List.of(servicio),
                Instant.now().plusSeconds(600));
    }

    String delTitular(UUID titular) {
        return token(
                true,
                titular,
                "PARTICIPANTE",
                List.of("BILLETERA_OPERAR", "BILLETERA_VER"),
                Instant.now().plusSeconds(600));
    }
}
