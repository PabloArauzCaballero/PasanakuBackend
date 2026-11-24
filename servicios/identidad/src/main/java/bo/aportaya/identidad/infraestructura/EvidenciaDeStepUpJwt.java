package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * La evidencia step-up de VERDAD, en todos los perfiles: el servidor TEST corre con el perfil {@code local}
 * y un doble que acepta cualquier cosa ahi es una puerta abierta (mismo criterio que {@link DesafioPorTipo}).
 *
 * <p>Valida, en orden y denegando ante cualquier duda (patron {@code SegundoFactorStepUp} de
 * nucleo-financiero): firma y vigencia con el mismo JWKS del token de acceso, {@code exp} sin margen,
 * {@code iss}, audiencia, {@code sub} = quien actua, {@code proposito = ADMIN}, {@code acr = mfa} y por
 * ultimo el {@code jti} sin usar.
 *
 * <p><b>Riesgo residual declarado</b> (REPORTE de GU-B, ambiguedad 5): el {@code jti} consumido vive en
 * memoria de la replica, igual que los desafios abiertos de {@code CU04StepUp}. Con mas de una replica, la
 * misma evidencia podria usarse una vez por replica dentro de su vida (5 min). El arreglo es una tabla de
 * evidencias consumidas en {@code identidad}, que es un cambio de modelo pendiente de decision.
 */
@Component
public class EvidenciaDeStepUpJwt implements EvidenciaDeStepUp {

    private static final Logger BITACORA = LoggerFactory.getLogger(EvidenciaDeStepUpJwt.class);
    private static final String ISS = "aportaya-identidad";
    private static final String PROPOSITO_ADMIN = "ADMIN";
    private static final String ACR_MFA = "mfa";

    private final JwtDecoder decodificador;
    private final String audiencia;
    private final Map<UUID, Instant> consumidas = new ConcurrentHashMap<>();

    public EvidenciaDeStepUpJwt(
            JwtDecoder decodificador,
            @Value("${aportaya.mfa.audiencia-evidencia:aportaya-nucleo-financiero}") String audiencia) {
        this.decodificador = decodificador;
        this.audiencia = audiencia;
    }

    @Override
    public boolean consumir(UUID actorId, String evidencia) {
        if (evidencia == null || evidencia.isBlank()) {
            return false;
        }
        Jwt jwt;
        try {
            jwt = decodificador.decode(evidencia);
        } catch (JwtException firmaOVigenciaInvalida) {
            // Nunca el JWT en el log: es evidencia de identidad. Solo el motivo.
            BITACORA.warn("Evidencia step-up de gobierno rechazada: {}", firmaOVigenciaInvalida.getMessage());
            return false;
        }
        Instant ahora = Instant.now();
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(ahora)) {
            return false;
        }
        List<String> aud = jwt.getAudience();
        boolean valida = ISS.equals(jwt.getClaimAsString("iss"))
                && aud != null
                && aud.contains(audiencia)
                && actorId.toString().equals(jwt.getSubject())
                && PROPOSITO_ADMIN.equals(jwt.getClaimAsString("proposito"))
                && ACR_MFA.equals(jwt.getClaimAsString("acr"));
        if (!valida) {
            return false;
        }
        UUID jti;
        try {
            jti = UUID.fromString(String.valueOf(jwt.getId()));
        } catch (IllegalArgumentException jtiNoEsUuid) {
            return false;
        }
        consumidas.values().removeIf(vence -> vence.isBefore(ahora));
        // putIfAbsent es la barrera: dos actos simultaneos con la misma evidencia, uno solo gana.
        return consumidas.putIfAbsent(jti, jwt.getExpiresAt()) == null;
    }
}
