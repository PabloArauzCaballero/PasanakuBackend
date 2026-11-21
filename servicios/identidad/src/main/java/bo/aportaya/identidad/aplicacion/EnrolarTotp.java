package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.FactorTotpRepositorio;
import bo.aportaya.identidad.infraestructura.LlavesDeFactor;
import bo.aportaya.identidad.infraestructura.SemillaTotp;
import bo.aportaya.identidad.infraestructura.VerificadorTotp;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CifradorSimetrico;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-04 · Enrolar el segundo factor TOTP del titular de la sesion (F1.S3.M2 / IDCU-HUECO-077).
 *
 * <p>Dos pasos, los dos con la sesion del propio titular (el usuario sale del token, nunca
 * del cuerpo): {@link #iniciar} genera la semilla, la guarda <b>cifrada</b> con la llave
 * vigente y la devuelve <b>una sola vez</b> para que la app la lea; {@link #confirmar} la
 * activa solo si el titular presenta un codigo valido calculado con esa semilla. Un factor
 * sin confirmar no autentica.
 *
 * <p>Si ya hay un TOTP confirmado, no se reemplaza por aca: quien robo una sesion no puede
 * cambiar el segundo factor de la victima. Rotarlo es otro flujo (con step-up), pendiente.
 */
@Service
public class EnrolarTotp {

    static final String EMISOR = "AportaYa";

    private final Datos datos;
    private final FactorTotpRepositorio factores;
    private final LlavesDeFactor llaves;
    private final VerificadorTotp verificador;
    private final Outbox outbox;
    private final Reloj reloj;
    private final SecureRandom azar = new SecureRandom();

    public EnrolarTotp(
            Datos datos,
            FactorTotpRepositorio factores,
            LlavesDeFactor llaves,
            VerificadorTotp verificador,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.factores = factores;
        this.llaves = llaves;
        this.verificador = verificador;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** La semilla en claro sale SOLO en esta respuesta; no se loguea ni se vuelve a mostrar. */
    public record Inicio(UUID factorId, String semillaBase32, String uriOtpauth) {}

    public record Confirmacion(UUID factorId, OffsetDateTime confirmadoEn) {}

    @Transactional
    public Inicio iniciar(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            if (factores.tieneTotpConfirmado(dsl, ctx.usuarioId())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(4, 9), "Ya tenes un segundo factor activo. Para cambiarlo, pedilo a soporte.");
            }
            String semilla = SemillaTotp.nueva(azar);
            String cifrado = CifradorSimetrico.cifrar(semilla, llaves.llaveVigente());
            UUID id = factores.iniciar(dsl, ctx.usuarioId(), cifrado, llaves.vigente());
            String cuenta = factores.codigoPublicoDe(dsl, ctx.usuarioId())
                    .orElse(ctx.usuarioId().toString());
            return new Inicio(id, semilla, uri(cuenta, semilla));
        });
    }

    @Transactional
    public Confirmacion confirmar(UUID factorId, String codigo, ContextoSesion ctx) {
        Instant ahora = reloj.ahora();
        return datos.conContexto(ctx, dsl -> {
            var pendiente = factores.pendiente(dsl, ctx.usuarioId(), factorId)
                    .orElseThrow(() -> new ErrorDeNegocio(
                            CodigoError.de(4, 8), "Ese enrolamiento no existe, no es tuyo o ya se confirmo."));
            Optional<Instant> paso = verificador
                    .semillaDe(pendiente.id(), pendiente.secretoCifrado(), pendiente.versionLlave())
                    .flatMap(semilla -> VerificadorTotp.pasoAceptado(semilla, codigo, ahora, Optional.empty()));
            if (paso.isEmpty()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(4, 4), "Ese codigo no coincide. Revisa la hora del telefono y proba de nuevo.");
            }
            OffsetDateTime confirmadoEn = ahora.atOffset(ZoneOffset.UTC);
            if (!factores.confirmar(
                    dsl, ctx.usuarioId(), factorId, paso.get().atOffset(ZoneOffset.UTC), confirmadoEn)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(4, 8), "Ese enrolamiento no existe, no es tuyo o ya se confirmo.");
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.factor_enrolado",
                            "factor_mfa",
                            factorId,
                            // Identificadores, no contenidos: el consumidor pregunta el tipo a su dueno.
                            Map.of("usuarioId", ctx.usuarioId().toString()),
                            UUID.fromString(ctx.traza().id())));
            return new Confirmacion(factorId, confirmadoEn);
        });
    }

    public static String uri(String cuenta, String semilla) {
        String etiqueta =
                URLEncoder.encode(EMISOR + ":" + cuenta, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + etiqueta + "?secret=" + semilla + "&issuer=" + EMISOR
                + "&algorithm=SHA1&digits=6&period=30";
    }
}
