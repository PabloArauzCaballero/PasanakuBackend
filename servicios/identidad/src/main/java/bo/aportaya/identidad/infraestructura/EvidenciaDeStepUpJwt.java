package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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
 * <p>El {@code jti} se consume en {@code identidad.evidencia_step_up_consumida} con {@code UNIQUE (jti)} (decision
 * de Pablo 2026-11-24): un solo uso entre replicas y reinicios, no en memoria. El consumo va en la transaccion del
 * acto: si el acto revierte, la evidencia no se gasto.
 */
@Component
public class EvidenciaDeStepUpJwt implements EvidenciaDeStepUp {

    private static final Logger BITACORA = LoggerFactory.getLogger(EvidenciaDeStepUpJwt.class);
    private static final String ISS = "aportaya-identidad";
    private static final String PROPOSITO_ADMIN = "ADMIN";
    private static final String ACR_MFA = "mfa";

    private final JwtDecoder decodificador;
    private final EvidenciaStepUpConsumidaRepositorio consumidas;
    private final String audiencia;

    public EvidenciaDeStepUpJwt(
            JwtDecoder decodificador,
            EvidenciaStepUpConsumidaRepositorio consumidas,
            @Value("${aportaya.mfa.audiencia-evidencia:aportaya-nucleo-financiero}") String audiencia) {
        this.decodificador = decodificador;
        this.consumidas = consumidas;
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
        // Ultimo paso a proposito: todo lo anterior no escribe; el consumo es lo unico con efecto.
        return consumidas.consumir(
                jti,
                actorId,
                PROPOSITO_ADMIN,
                ahora.atOffset(ZoneOffset.UTC),
                jwt.getExpiresAt().atOffset(ZoneOffset.UTC));
    }
}
