package bo.aportaya.identidad.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CifradorSimetrico;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.web.clientes.CodigoTotp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifica un codigo TOTP (RFC 6238) contra el secreto enrolado en {@code factor_mfa}
 * (F1.S3.M2 / IDCU-HUECO-077). Reemplaza al desafio en memoria, que generaba un codigo al
 * azar que nunca llegaba a nadie: el TOTP lo calcula la app del titular, no el servidor.
 *
 * <ul>
 *   <li>Ventana de ±1 paso (30 s) por desfase de reloj del telefono (RFC 6238 §5.2).
 *   <li>Un codigo sirve <b>una vez</b>: se guarda el paso aceptado y se rechaza ese mismo
 *       paso o uno anterior, con la precondicion en el {@code UPDATE}.
 *   <li>No hay codigo fijo en ningun perfil.
 * </ul>
 *
 * <p>Corre en <b>su propia transaccion</b> ({@code REQUIRES_NEW}) y con el contexto del
 * sistema: lo llaman el ingreso (contexto del sistema) y el step-up (transaccion de solo
 * lectura del titular), y el uso del codigo tiene que quedar escrito aunque la operacion
 * que lo pidio se revierta. Un codigo gastado esta gastado.
 */
@Component
public class VerificadorTotp {

    private static final Logger BITACORA = LoggerFactory.getLogger(VerificadorTotp.class);
    private static final Pattern SEIS_DIGITOS = Pattern.compile("\\d{6}");
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000077");

    private final Datos datos;
    private final FactorTotpRepositorio factores;
    private final LlavesDeFactor llaves;
    private final Reloj reloj;
    private final TransactionTemplate transaccionPropia;

    public VerificadorTotp(
            Datos datos,
            FactorTotpRepositorio factores,
            LlavesDeFactor llaves,
            Reloj reloj,
            PlatformTransactionManager transacciones) {
        this.datos = datos;
        this.factores = factores;
        this.llaves = llaves;
        this.reloj = reloj;
        this.transaccionPropia = new TransactionTemplate(transacciones);
        this.transaccionPropia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** {@code true} si el codigo es el vigente del TOTP confirmado del usuario y no se uso. */
    public boolean validar(UUID usuarioId, String codigo) {
        if (codigo == null || !SEIS_DIGITOS.matcher(codigo).matches()) {
            return false;
        }
        Instant ahora = reloj.ahora();
        ContextoSesion ctx =
                ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
        Boolean aceptado = transaccionPropia.execute(estado -> datos.conContexto(ctx, dsl -> factores.activoDe(
                        dsl, usuarioId)
                .flatMap(factor -> semillaDe(factor.id(), factor.secretoCifrado(), factor.versionLlave())
                        .flatMap(semilla -> pasoAceptado(
                                semilla, codigo, ahora, factor.ultimoUso().map(OffsetDateTime::toInstant)))
                        .filter(paso -> factores.marcarUso(dsl, factor.id(), paso.atOffset(ZoneOffset.UTC))))
                .isPresent()));
        return Boolean.TRUE.equals(aceptado);
    }

    /**
     * La regla pura: el inicio del paso en que {@code codigo} es valido, dentro de ±1 paso de
     * {@code ahora}, y posterior al ultimo paso aceptado. Vacio si no hay ninguno.
     */
    public static Optional<Instant> pasoAceptado(
            String semillaBase32, String codigo, Instant ahora, Optional<Instant> ultimoUso) {
        if (codigo == null || !SEIS_DIGITOS.matcher(codigo).matches()) {
            return Optional.empty();
        }
        long pasoActual = ahora.getEpochSecond() / CodigoTotp.PASO_SEGUNDOS;
        for (long delta = -1; delta <= 1; delta++) {
            Instant inicio = Instant.ofEpochSecond((pasoActual + delta) * CodigoTotp.PASO_SEGUNDOS);
            if (ultimoUso.isPresent() && !inicio.isAfter(ultimoUso.get())) {
                continue;
            }
            // Comparacion en tiempo constante: el tiempo de respuesta no dice cuantos digitos acertaste.
            if (MessageDigest.isEqual(
                    CodigoTotp.de(semillaBase32, inicio).getBytes(StandardCharsets.US_ASCII),
                    codigo.getBytes(StandardCharsets.US_ASCII))) {
                return Optional.of(inicio);
            }
        }
        return Optional.empty();
    }

    /** Descifra la semilla; vacio si la llave de esa version ya no esta o el dato no es descifrable. */
    public Optional<String> semillaDe(UUID factorId, String secretoCifrado, short version) {
        Optional<String> llave = llaves.llave(version);
        if (llave.isEmpty()) {
            BITACORA.warn(
                    "factor {}: la version de llave {} no esta configurada; el factor no autentica", factorId, version);
            return Optional.empty();
        }
        try {
            return Optional.of(CifradorSimetrico.descifrar(secretoCifrado, llave.get()));
        } catch (RuntimeException ilegible) {
            // Solo el id y el tipo de falla: nunca el secreto ni la llave (regla 90.2.9).
            BITACORA.warn(
                    "factor {}: secreto no descifrable ({})",
                    factorId,
                    ilegible.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
