package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.EmisorDeAcceso;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-04 · step-up: el segundo factor para UNA operacion sensible (retiro, cambio de cuenta, admin).
 *
 * <p>Dos pasos (docs/auditoria-produccion/contratos/step-up-jwt.md): abrir un desafio y presentar el
 * factor para recibir una evidencia firmada. El desafio es de quien lo abre, se consume en el primer
 * intento —acierte o no— y vence. La evidencia la valida {@code nucleo-financiero} con el JWKS
 * publico, sin llamar a este servicio.
 *
 * <p>Los desafios abiertos viven en memoria con vencimiento, igual que {@code DesafioLocal}: con mas
 * de una replica de identidad hay que moverlos a {@code token_verificacion} (riesgo residual del PLAN).
 */
@Service
public class CU04StepUp {
    private static final Set<String> PROPOSITOS = Set.of("RETIRO", "CAMBIO_CUENTA", "ADMIN");
    private static final Duration VIDA_DEL_DESAFIO = Duration.ofMinutes(5);

    private final Datos datos;
    private final AccesoRepositorio accesos;
    private final DesafioDeFactor desafio;
    private final EmisorDeAcceso emisor;
    private final Duration vigenciaDeLaEvidencia;
    private final String audiencia;
    private final Map<UUID, Abierto> abiertos = new ConcurrentHashMap<>();

    public CU04StepUp(
            Datos datos,
            AccesoRepositorio accesos,
            DesafioDeFactor desafio,
            EmisorDeAcceso emisor,
            @Value("${aportaya.mfa.vigencia-evidencia:PT5M}") Duration vigenciaDeLaEvidencia,
            @Value("${aportaya.mfa.audiencia-evidencia:aportaya-nucleo-financiero}") String audiencia) {
        this.datos = datos;
        this.accesos = accesos;
        this.desafio = desafio;
        this.emisor = emisor;
        this.vigenciaDeLaEvidencia = vigenciaDeLaEvidencia;
        this.audiencia = audiencia;
    }

    @Transactional(readOnly = true)
    public Desafio abrir(String proposito, ContextoSesion ctx) {
        if (!PROPOSITOS.contains(proposito)) {
            throw new ErrorDeNegocio(CodigoError.de(4, 8), "Ese proposito no admite segundo factor.");
        }
        String tipo = datos.conContexto(ctx, dsl -> accesos.factorActivo(dsl, ctx.usuarioId()))
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(4, 6), "Tenes que enrolar tu segundo factor antes de continuar."));
        limpiarVencidos();
        UUID id = desafio.emitir(ctx.usuarioId(), tipo);
        Instant vence = Instant.now().plus(VIDA_DEL_DESAFIO);
        abiertos.put(id, new Abierto(ctx.usuarioId(), proposito, tipo, vence));
        return new Desafio(id, vence);
    }

    @Transactional(readOnly = true)
    public Evidencia verificar(UUID desafioId, String valor, ContextoSesion ctx) {
        // remove: un solo uso, acierte o no. Reintentar sobre el mismo desafio es lo que la fuerza
        // bruta necesita.
        Abierto a = abiertos.remove(desafioId);
        if (a == null || !a.usuarioId().equals(ctx.usuarioId()) || Instant.now().isAfter(a.expiraEn())) {
            throw new ErrorDeNegocio(CodigoError.de(4, 8), "Ese desafio ya se uso, vencio o no es tuyo.");
        }
        if (!desafio.validar(ctx.usuarioId(), a.tipoDeFactor(), valor)) {
            throw new ErrorDeNegocio(CodigoError.de(4, 4), "Ese codigo ya no sirve. Pedi uno nuevo.");
        }
        var emitida = emisor.emitirEvidencia(
                ctx.usuarioId(), a.proposito(), desafioId, amr(a.tipoDeFactor()), vigenciaDeLaEvidencia, audiencia);
        return new Evidencia(emitida.token(), emitida.expiraEn(), jtiDe(emitida.token()));
    }

    private void limpiarVencidos() {
        Instant ahora = Instant.now();
        abiertos.values().removeIf(a -> ahora.isAfter(a.expiraEn()));
    }

    private static String amr(String tipoDeFactor) {
        return switch (tipoDeFactor) {
            case "TOTP" -> "totp";
            case "BIOMETRIA" -> "biometria";
            default -> "otp";
        };
    }

    private static UUID jtiDe(String jwt) {
        try {
            return UUID.fromString(
                    com.nimbusds.jwt.SignedJWT.parse(jwt).getJWTClaimsSet().getJWTID());
        } catch (java.text.ParseException imposible) {
            throw new IllegalStateException("La evidencia recien firmada no se puede leer", imposible);
        }
    }

    private record Abierto(UUID usuarioId, String proposito, String tipoDeFactor, Instant expiraEn) {}

    public record Desafio(UUID id, Instant expiraEn) {}

    public record Evidencia(String jwt, Instant expiraEn, UUID jti) {}
}
