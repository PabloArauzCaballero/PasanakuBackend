package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * El segundo factor de VERDAD: valida el JWT de evidencia step-up que emite
 * {@code identidad} (H2.S2, `docs/auditoria-produccion/contratos/step-up-jwt.md`).
 *
 * <p>{@code @Profile("!local & !test")}: es el {@code SegundoFactor} de
 * {@code staging}/{@code production} — activo en TODO perfil salvo esos dos, donde
 * vive {@link SegundoFactorLocal} en su lugar. Sin esta exclusion, Spring encontraria
 * DOS beans {@code SegundoFactor} en local/test y {@code BilleteraController} no
 * sabria cual inyectar. Es lo que hace que {@code ArranqueProduccionTest} pase una vez
 * que este bean existe: en {@code production} es el UNICO candidato.
 *
 * <p><b>Que valida, en orden</b> (denegar por omision ante cualquier duda —
 * invariante 9):
 *
 * <ol>
 *   <li>Firma y vigencia ({@code exp}/{@code nbf}): las hace {@link JwtDecoder}, con
 *       el MISMO JWKS que valida el token de acceso (ADR-024) — la evidencia la firma
 *       la misma clave.
 *   <li>{@code iss = aportaya-identidad}.
 *   <li>{@code aud} contiene {@code aportaya-nucleo-financiero} (el token de acceso
 *       trae {@code aportaya}; son audiencias DISTINTAS a proposito).
 *   <li>{@code sub} coincide con el usuario de la sesion que pide el retiro — una
 *       evidencia valida de OTRO usuario no sirve para esta operacion.
 *   <li>{@code proposito = RETIRO}.
 *   <li>{@code acr = mfa}.
 *   <li>{@code jti} sin consumir — {@link ConsumoDeEvidenciaMfa}, un solo uso.
 * </ol>
 */
@Component
@Profile("!local & !test")
public class SegundoFactorStepUp implements SegundoFactor {

    private static final Logger BITACORA = LoggerFactory.getLogger(SegundoFactorStepUp.class);

    private static final String ISS = "aportaya-identidad";
    private static final String AUD = "aportaya-nucleo-financiero";
    private static final String PROPOSITO_RETIRO = "RETIRO";
    private static final String ACR_MFA = "mfa";

    private final JwtDecoder decodificador;
    private final ConsumoDeEvidenciaMfa consumo;

    public SegundoFactorStepUp(JwtDecoder decodificador, ConsumoDeEvidenciaMfa consumo) {
        this.decodificador = decodificador;
        this.consumo = consumo;
    }

    @Override
    public boolean verificado(UUID usuarioId, String factor) {
        if (factor == null || factor.isBlank()) {
            return false;
        }

        Jwt jwt;
        try {
            jwt = decodificador.decode(factor);
        } catch (JwtException firmaOVigenciaInvalida) {
            // Nunca el JWT en el log: es evidencia de identidad, dato sensible
            // (regla 90/`data-privacy-financial`). Solo el motivo de rechazo.
            BITACORA.warn("Evidencia MFA rechazada: {}", firmaOVigenciaInvalida.getMessage());
            return false;
        }

        // El JwtDecoder comun aplica el skew de reloj de ADR-024 (PT60S,
        // aportaya.jwt.tolerancia) pensado para el token de ACCESO. La evidencia
        // step-up es prueba de un instante, no una sesion larga: el contrato fija el
        // limite exacto en `exp` sin margen (exp=+1s acepta, exp=-1s rechaza), asi
        // que se revalida a mano, mas estricto que lo que el decodificador ya dejo
        // pasar.
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
            BITACORA.warn("Evidencia MFA rechazada: vencida (fuera del margen de step-up, no del de sesion)");
            return false;
        }

        // getClaimAsString y no getIssuer(): `iss` acá es un identificador plano
        // (`aportaya-identidad`), no una URL, y getIssuer() fuerza esa conversion.
        if (!ISS.equals(jwt.getClaimAsString("iss"))) {
            return false;
        }
        List<String> audiencia = jwt.getAudience();
        if (audiencia == null || !audiencia.contains(AUD)) {
            return false;
        }
        if (!usuarioId.toString().equals(jwt.getSubject())) {
            return false;
        }
        if (!PROPOSITO_RETIRO.equals(jwt.getClaimAsString("proposito"))) {
            return false;
        }
        if (!ACR_MFA.equals(jwt.getClaimAsString("acr"))) {
            return false;
        }

        String jtiCrudo = jwt.getId();
        if (jtiCrudo == null || jtiCrudo.isBlank()) {
            return false;
        }
        UUID jti;
        try {
            jti = UUID.fromString(jtiCrudo);
        } catch (IllegalArgumentException jtiNoEsUuid) {
            return false;
        }

        // Ultimo paso a proposito: todo lo anterior es gratis (sin escritura); el
        // consumo es lo unico con efecto, y solo se paga si el resto ya paso.
        return consumo.consumir(jti, usuarioId, PROPOSITO_RETIRO);
    }
}
