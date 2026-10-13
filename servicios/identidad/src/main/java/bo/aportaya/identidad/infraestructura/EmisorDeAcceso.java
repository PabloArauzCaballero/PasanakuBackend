package bo.aportaya.identidad.infraestructura;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El unico emisor de tokens del sistema (ADR-024).
 *
 * <p>RS256 y no HS256: con clave simetrica los catorce servicios tendrian que conocer
 * el secreto de firma, y entonces cualquiera de ellos podria emitir un token de
 * administrador. Con clave asimetrica **solo identidad firma** y los otros trece solo
 * verifican.
 *
 * <p>Los reclamos son los que fija el ADR: {@code sub}, {@code rol}, {@code permisos},
 * {@code nivel_diligencia}, {@code dispositivo}, {@code exp} y {@code jti}. Ni uno mas:
 * un token es un dato que viaja por catorce procesos y queda en logs y trazas, asi que
 * lo que no hace falta para autorizar no entra.
 *
 * <p><b>La clave.</b> Viene por configuracion en formato JWK. Si no viene, se genera una
 * al arrancar y **queda solo en memoria**: sirve para desarrollo y para las pruebas de
 * punta a punta, y tiene el precio de que al reiniciar identidad los tokens vivos dejan
 * de validar. En cualquier entorno con datos reales se configura, porque una clave que
 * cambia sola no es una clave.
 *
 * <p><b>Rotacion sin cortar sesiones.</b> Rotar {@code aportaya.jwt.clave-firma} con una sola
 * clave publicada invalida de golpe todo token vivo (la clave vieja desaparece del JWKS y los
 * otros catorce servicios dejan de poder verificarlos). Por eso el JWKS publica DOS claves:
 * la vigente, con la que se firma, y opcionalmente {@code aportaya.jwt.clave-firma-anterior},
 * que solo se publica para que los tokens firmados antes sigan validando hasta que venzan
 * (15 minutos) — nunca firma. Rotacion: pasar la vigente a «anterior», poner la nueva como
 * vigente, reiniciar; pasada la vida del token, vaciar la «anterior». Cada token lleva el
 * {@code kid} de la clave que lo firmo, que es lo que permite elegir sin probar una por una.
 */
@Component
public class EmisorDeAcceso {

    /** ADR-024: vida corta, porque un token firmado no se puede revocar. */
    private static final org.slf4j.Logger BITACORA = org.slf4j.LoggerFactory.getLogger(EmisorDeAcceso.class);

    private static final Duration VIGENCIA = Duration.ofMinutes(15);

    private static final int TAMANO_DE_CLAVE = 2048;

    private final RSAKey clave;

    /** Solo se publica, nunca firma: es la ventana de solapamiento de una rotacion. */
    private final RSAKey claveAnterior;

    public EmisorDeAcceso(
            @Value("${aportaya.jwt.clave-firma:}") String claveJwk,
            @Value("${aportaya.jwt.clave-firma-anterior:}") String claveAnteriorJwk) {
        boolean generada = claveJwk == null || claveJwk.isBlank();
        this.clave = generada ? generar() : leer(claveJwk, "aportaya.jwt.clave-firma");
        this.claveAnterior = anterior(generada, claveAnteriorJwk, clave);
        if (generada) {
            // **No es un detalle de desarrollo.** ADR-037 pide dos replicas como
            // minimo, y con la clave generada en memoria cada replica firma con la
            // suya y publica su propio JWKS: un token emitido por la primera lo
            // rechaza la segunda, y la sesion se cae sola en cuanto el balanceador
            // cambia de instancia. Sirve para una maquina de desarrollo y para las
            // pruebas, y para nada mas.
            BITACORA.warn("aportaya.jwt.clave-firma vacia: se genero una clave EN MEMORIA. "
                    + "Con mas de una replica los tokens de una no valen en la otra. "
                    + "En cualquier entorno compartido, inyectala desde el almacen de secretos.");
        }
    }

    /**
     * El token de acceso de una sesion ya abierta.
     *
     * @param permisos los efectivos, que CU-08 calculo al asignar el rol. Vacio
     *     significa <b>no puede nada</b>: el token no es una llave maestra.
     */
    public Emitido emitir(
            UUID usuarioId,
            String rol,
            List<String> permisos,
            String nivelDiligencia,
            String dispositivo,
            UUID sesionId) {

        Instant ahora = Instant.now();
        Instant vence = ahora.plus(VIGENCIA);

        var constructor = new JWTClaimsSet.Builder()
                .subject(usuarioId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(vence))
                .claim("rol", rol)
                .claim("permisos", permisos)
                .claim("nivel_diligencia", nivelDiligencia)
                .claim("dispositivo", dispositivo);
        if (sesionId != null) constructor.claim("sesion_id", sesionId.toString());
        var reclamos = constructor.build();

        try {
            var jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(clave.getKeyID())
                            .build(),
                    reclamos);
            jwt.sign(new RSASSASigner(clave));
            return new Emitido(jwt.serialize(), vence);
        } catch (JOSEException imposible) {
            throw new IllegalStateException("No se pudo firmar el token de acceso", imposible);
        }
    }

    /** Compatibilidad para emisores de pruebas/otros flujos que no abren una sesión revocable. */
    public Emitido emitir(UUID usuarioId, String rol, List<String> permisos, String nivelDiligencia, String dispositivo) {
        return emitir(usuarioId, rol, permisos, nivelDiligencia, dispositivo, null);
    }

    /**
     * La evidencia step-up: prueba de que ESTE usuario paso un segundo factor AHORA, para UNA
     * operacion (contrato docs/auditoria-produccion/contratos/step-up-jwt.md). Firmada con la
     * misma clave que el token de acceso, pero con otra audiencia: un token de acceso no sirve como
     * evidencia ni al reves.
     *
     * @param amr como se probo el factor (`totp`, `otp`, `biometria`); informativo.
     */
    public Emitido emitirEvidencia(
            UUID usuarioId, String proposito, UUID desafioId, String amr, Duration vigencia, String audiencia) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(vigencia);
        var reclamos = new JWTClaimsSet.Builder()
                .issuer("aportaya-identidad")
                .audience(audiencia)
                .subject(usuarioId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(vence))
                .claim("proposito", proposito)
                .claim("desafio_id", desafioId.toString())
                .claim("acr", "mfa")
                .claim("amr", List.of(amr))
                .build();
        try {
            var jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(clave.getKeyID())
                            .build(),
                    reclamos);
            jwt.sign(new RSASSASigner(clave));
            return new Emitido(jwt.serialize(), vence);
        } catch (JOSEException imposible) {
            throw new IllegalStateException("No se pudo firmar la evidencia step-up", imposible);
        }
    }

    /**
     * Lo que se publica: la parte publica, y nada mas. La vigente primero; la anterior, si hay
     * una rotacion en curso.
     */
    public Map<String, Object> jwks() {
        var publicas = new java.util.ArrayList<com.nimbusds.jose.jwk.JWK>();
        publicas.add(clave.toPublicJWK());
        if (claveAnterior != null) {
            publicas.add(claveAnterior.toPublicJWK());
        }
        return new JWKSet(publicas).toJSONObject();
    }

    private static RSAKey anterior(boolean vigenteGenerada, String claveAnteriorJwk, RSAKey vigente) {
        if (claveAnteriorJwk == null || claveAnteriorJwk.isBlank()) {
            return null;
        }
        if (vigenteGenerada) {
            // Una vigente generada al arrancar no sobrevive al reinicio: publicar una «anterior»
            // al lado de ella no rota nada y esconde que la configuracion esta incompleta.
            throw new IllegalStateException(
                    "aportaya.jwt.clave-firma-anterior sin aportaya.jwt.clave-firma: no hay nada que rotar");
        }
        RSAKey previa = leer(claveAnteriorJwk, "aportaya.jwt.clave-firma-anterior");
        if (previa.getKeyID() == null || previa.getKeyID().equals(vigente.getKeyID())) {
            // Dos claves con el mismo kid hacen que el verificador elija una al azar.
            throw new IllegalStateException(
                    "aportaya.jwt.clave-firma-anterior tiene que traer un kid distinto al de la vigente");
        }
        return previa;
    }

    private static RSAKey generar() {
        try {
            return new RSAKeyGenerator(TAMANO_DE_CLAVE)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (JOSEException imposible) {
            throw new IllegalStateException("No se pudo generar la clave de firma", imposible);
        }
    }

    private static RSAKey leer(String claveJwk, String propiedad) {
        try {
            return RSAKey.parse(claveJwk);
        } catch (java.text.ParseException mal) {
            // Sin el JWK en el mensaje ni como causa: es una clave privada.
            throw new IllegalStateException(propiedad + " no es un JWK RSA valido");
        }
    }

    public record Emitido(String token, Instant expiraEn) {}
}
