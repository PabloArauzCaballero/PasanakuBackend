package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import bo.aportaya.identidad.dominio.PoliticaDeClave;
import bo.aportaya.identidad.dominio.puertos.CorreoDeCuenta;
import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.infraestructura.CredencialRepositorio;
import bo.aportaya.identidad.infraestructura.FactorTotpRepositorio;
import bo.aportaya.identidad.infraestructura.LlavesDeFactor;
import bo.aportaya.identidad.infraestructura.RecuperacionesRepositorio;
import bo.aportaya.identidad.infraestructura.SemillaTotp;
import bo.aportaya.identidad.infraestructura.TokensDeCuentaRepositorio;
import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.identidad.infraestructura.VerificadorTotp;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CifradorSimetrico;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La invitacion de un solo uso del operador: primer acceso (P-1) y los dos cierres de la recuperacion
 * asistida (P-4). CU-09 §10-§11.
 *
 * <p>Tres operaciones publicas, todas con el token de la persona y ninguna con sesion:
 * {@link #consultar} (no consume), {@link #iniciarFactor} (no consume) y {@link #completar} (consume, en
 * una sola transaccion con la clave, el factor, el corte de sesiones y la bitacora). Si algo falla, el
 * token no se consume; lo que si se cuenta es el intento fallido de un codigo o de la clave actual, y por
 * eso {@code completar} no lanza para rechazar: devuelve el resultado y el controlador lo traduce DESPUES
 * del commit (mismo patron que CU-04).
 *
 * <p>Vencido, usado, falso o de otro proposito responden lo mismo (AP-CU09-04): no se distingue.
 */
@Service
public class InvitacionesDeCuenta {

    /** El «usuario» del proceso en {@code app.usuario_id}: un endpoint sin sesion no es una persona. */
    static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-0000000000c1");

    private final Datos datos;
    private final TokensDeCuentaRepositorio tokens;
    private final UsuarioRepositorio usuarios;
    private final CredencialRepositorio credenciales;
    private final RecuperacionesRepositorio recuperaciones;
    private final FactorTotpRepositorio factores;
    private final LlavesDeFactor llaves;
    private final VerificadorTotp verificador;
    private final HasheadorDeCredencial hasheador;
    private final CorreoDeCuenta correo;
    private final BitacoraDeCuenta bitacora;
    private final Reloj reloj;
    private final PoliticaDeClave politica;
    private final String urlDelBackoffice;
    private final SecureRandom azar = new SecureRandom();

    public InvitacionesDeCuenta(
            Datos datos,
            TokensDeCuentaRepositorio tokens,
            UsuarioRepositorio usuarios,
            CredencialRepositorio credenciales,
            RecuperacionesRepositorio recuperaciones,
            FactorTotpRepositorio factores,
            LlavesDeFactor llaves,
            VerificadorTotp verificador,
            HasheadorDeCredencial hasheador,
            CorreoDeCuenta correo,
            BitacoraDeCuenta bitacora,
            Reloj reloj,
            @Value("${identidad.clave.largo-minimo:8}") int largoMinimoDeClave,
            @Value("${identidad.clave.no-se-repiten:5}") int clavesQueNoSeRepiten,
            @Value("${aportaya.acceso.url-backoffice:http://localhost:4200}") String urlDelBackoffice) {
        this.datos = datos;
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.recuperaciones = recuperaciones;
        this.factores = factores;
        this.llaves = llaves;
        this.verificador = verificador;
        this.hasheador = hasheador;
        this.correo = correo;
        this.bitacora = bitacora;
        this.reloj = reloj;
        this.politica = new PoliticaDeClave(largoMinimoDeClave, clavesQueNoSeRepiten);
        this.urlDelBackoffice = urlDelBackoffice.endsWith("/")
                ? urlDelBackoffice.substring(0, urlDelBackoffice.length() - 1)
                : urlDelBackoffice;
    }

    public record Consulta(AlcanceDeCuenta alcance, String nombres, OffsetDateTime venceEn) {}

    public record Entrada(String token, String claveNueva, String claveActual, UUID factorId, String codigo) {
        @Override
        public String toString() {
            return "Entrada[REDACTADA]";
        }
    }

    /** El resultado de completar: o termino, o hay un rechazo para traducir despues del commit. */
    public record Resultado(AlcanceDeCuenta alcance, int sesionesCerradas, ErrorDeNegocio rechazo) {
        public boolean exito() {
            return rechazo == null;
        }
    }

    public record Origen(String ip, String agente, Traza traza) {}

    @Transactional(readOnly = true)
    public Consulta consultar(String token, Origen origen) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(deSistema(origen), dsl -> {
            var vigente = tokens.vigente(dsl, token, ahora, false).orElseThrow(InvitacionesDeCuenta::tokenInvalido);
            return new Consulta(
                    vigente.alcance(), tokens.nombresDe(dsl, vigente.usuarioId()).orElse(""), vigente.expiraEn());
        });
    }

    /** La semilla en claro sale SOLO en esta respuesta y no se loguea. No consume el token. */
    @Transactional
    public EnrolarTotp.Inicio iniciarFactor(String token, Origen origen) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(deSistema(origen), dsl -> {
            var vigente = tokens.vigente(dsl, token, ahora, false).orElseThrow(InvitacionesDeCuenta::tokenInvalido);
            if (!vigente.alcance().pideFactor()) {
                throw tokenInvalido();
            }
            String semilla = SemillaTotp.nueva(azar);
            String cifrado = CifradorSimetrico.cifrar(semilla, llaves.llaveVigente());
            UUID factorId = factores.iniciar(dsl, vigente.usuarioId(), cifrado, llaves.vigente());
            String cuenta = factores.codigoPublicoDe(dsl, vigente.usuarioId())
                    .orElse(vigente.usuarioId().toString());
            return new EnrolarTotp.Inicio(factorId, semilla, EnrolarTotp.uri(cuenta, semilla));
        });
    }

    @Transactional
    public Resultado completar(Entrada entrada, Origen origen) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(deSistema(origen), dsl -> {
            var vigente = tokens.vigente(dsl, entrada.token(), ahora, true).orElse(null);
            if (vigente == null) {
                return rechazo(null, tokenInvalido());
            }
            AlcanceDeCuenta alcance = vigente.alcance();
            UUID usuario = vigente.usuarioId();

            if (alcance.pideClaveActual()) {
                Optional<String> hash = usuarios.hashDeCredencial(dsl, usuario);
                boolean coincide = entrada.claveActual() != null
                        && hash.isPresent()
                        && hasheador.coincide(entrada.claveActual().toCharArray(), hash.get());
                if (!coincide) {
                    tokens.registrarIntentoFallido(dsl, vigente.tokenId(), ahora);
                    return rechazo(alcance, new ErrorDeNegocio(CodigoError.de(9, 1), "La clave actual no coincide."));
                }
            }
            if (alcance.pideClaveNueva()) {
                ErrorDeNegocio debil = evaluarClave(dsl, usuario, entrada.claveNueva());
                if (debil != null) {
                    return rechazo(alcance, debil);
                }
            }
            Instant pasoAceptado = null;
            if (alcance.pideFactor()) {
                if (entrada.factorId() == null || entrada.codigo() == null) {
                    return rechazo(alcance, new ErrorDeNegocio(CodigoError.de(4, 4), "Escribi el codigo de tu app."));
                }
                var pendiente = factores.pendiente(dsl, usuario, entrada.factorId());
                if (pendiente.isEmpty()) {
                    return rechazo(
                            alcance,
                            new ErrorDeNegocio(
                                    CodigoError.de(4, 8), "Ese enrolamiento no existe o ya se confirmo. Genera otro."));
                }
                Optional<Instant> paso = verificador
                        .semillaDe(
                                pendiente.get().id(),
                                pendiente.get().secretoCifrado(),
                                pendiente.get().versionLlave())
                        .flatMap(semilla -> VerificadorTotp.pasoAceptado(
                                semilla, entrada.codigo(), ahora.toInstant(), Optional.empty()));
                if (paso.isEmpty()) {
                    tokens.registrarIntentoFallido(dsl, vigente.tokenId(), ahora);
                    return rechazo(
                            alcance,
                            new ErrorDeNegocio(
                                    CodigoError.de(4, 4),
                                    "Ese codigo no coincide. Revisa la hora del telefono y proba de nuevo."));
                }
                pasoAceptado = paso.get();
            }

            // Desde aca todo se escribe o nada: cualquier excepcion revierte la transaccion entera.
            if (alcance.pideClaveNueva()) {
                String anterior = usuarios.hashDeCredencial(dsl, usuario).orElseThrow();
                credenciales.archivar(dsl, usuario, anterior, ahora);
                credenciales.reemplazar(dsl, usuario, hasheador.hashear(entrada.claveNueva().toCharArray()), ahora);
                dsl.execute(
                        "UPDATE identidad.usuario_interno SET debe_cambiar_contrasena = false WHERE usuario_id = ?",
                        usuario);
            }
            if (alcance.pideFactor()) {
                // El factor viejo se apaga ANTES de confirmar el nuevo: nunca hay dos principales.
                dsl.execute(
                        """
                        UPDATE identidad.factor_mfa SET activo = false, es_principal = false
                        WHERE usuario_id = ? AND tipo = 'TOTP' AND confirmado_en IS NOT NULL
                        """,
                        usuario);
                OffsetDateTime inicioDelPaso = pasoAceptado.atOffset(ZoneOffset.UTC);
                if (!factores.confirmar(dsl, usuario, entrada.factorId(), inicioDelPaso, ahora)) {
                    throw new ErrorDeNegocio(CodigoError.de(4, 8), "Ese enrolamiento ya no esta disponible.");
                }
            }
            // R-SEG-11: lo que colgaba de la credencial o del factor viejo se corta, sin excepcion.
            int cerradas = credenciales.cerrarSesiones(dsl, usuario, Optional.empty(), motivoDelCorte(alcance), ahora);
            credenciales.quitarConfianza(dsl, usuario);
            if (!tokens.consumir(dsl, vigente.tokenId(), ahora)) {
                throw tokenInvalido();
            }
            recuperaciones.marcarCompletadaPorToken(dsl, vigente.tokenId(), ahora);

            BitacoraDeCuenta.Accion accion =
                    switch (alcance) {
                        case ALTA -> BitacoraDeCuenta.Accion.INVITACION_ACEPTADA;
                        case CLAVE -> BitacoraDeCuenta.Accion.CLAVE_RESTABLECIDA;
                        case FACTOR -> BitacoraDeCuenta.Accion.FACTOR_REINSCRITO;
                    };
            bitacora.registrar(
                    dsl,
                    accion,
                    usuario,
                    new BitacoraDeCuenta.Actor(
                            usuario, BitacoraDeCuenta.ROL_DEL_TITULAR, BitacoraDeCuenta.ORIGEN_PUBLICO),
                    "Cumplimiento del enlace de un solo uso",
                    Map.of("alcance", alcance.name(), "sesionesCerradas", cerradas),
                    correlacion(origen),
                    ahora);
            return new Resultado(alcance, cerradas, null);
        });
    }

    /**
     * Emite el token y manda el correo al correo institucional DESPUES del commit. Lo llama el alta (P-1)
     * y el trabajo de recuperacion (P-4) dentro de su propia transaccion.
     *
     * @return el id del token emitido (para enlazarlo al expediente); nunca su valor
     */
    public UUID emitirYEnviar(
            DSLContext dsl,
            UUID usuarioId,
            AlcanceDeCuenta alcance,
            BitacoraDeCuenta.Accion accion,
            BitacoraDeCuenta.Actor actor,
            String motivo,
            UUID correlacion,
            String ip,
            String agente) {
        OffsetDateTime ahora = ahora();
        String destino = dsl.fetchOptional("SELECT correo FROM identidad.usuario WHERE id = ?", usuarioId)
                .map(fila -> fila.get(0, String.class))
                .filter(c -> c != null && !c.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "El operador " + usuarioId + " no tiene un correo institucional registrado"));
        var emitido = tokens.emitir(
                dsl, usuarioId, alcance, TokensDeCuentaRepositorio.enmascarar(destino), ahora, ip, agente, correlacion);
        bitacora.registrar(
                dsl,
                accion,
                usuarioId,
                actor,
                motivo,
                Map.of("alcance", alcance.name(), "venceEn", emitido.expiraEn().toString()),
                correlacion,
                ahora);
        String enlace = urlDelBackoffice + "/acceso/invitacion#token=" + emitido.token();
        Duration vigencia = Duration.between(ahora, emitido.expiraEn());
        DespuesDelCommit.ejecutar(
                "invitacion " + alcance, () -> correo.enviarInvitacion(destino, enlace, alcance, vigencia));
        return emitido.tokenId();
    }

    private ErrorDeNegocio evaluarClave(DSLContext dsl, UUID usuario, String claveNueva) {
        if (claveNueva == null || claveNueva.isBlank()) {
            return new ErrorDeNegocio(CodigoError.de(9, 2), "Elegi una clave.");
        }
        return politica.evaluar(
                        claveNueva.toCharArray(),
                        credenciales.historialDe(dsl, usuario),
                        hasheador::coincide,
                        credenciales.telefonoDe(dsl, usuario).orElse(null),
                        null)
                .map(motivo -> switch (motivo) {
                    case DEMASIADO_CORTA -> new ErrorDeNegocio(CodigoError.de(9, 2), "Esa clave es demasiado corta.");
                    case DERIVADA_DE_DATOS_PERSONALES -> new ErrorDeNegocio(
                            CodigoError.de(9, 2), "No uses tu telefono ni tu documento dentro de la clave.");
                    case REUTILIZADA -> new ErrorDeNegocio(
                            CodigoError.de(9, 3), "Ya usaste esa clave antes. Elegi una nueva.");
                })
                .orElse(null);
    }

    private static String motivoDelCorte(AlcanceDeCuenta alcance) {
        return switch (alcance) {
            case ALTA -> "R-SEG-11: primer acceso del operador";
            case CLAVE -> "R-SEG-11: credencial de operador restablecida";
            case FACTOR -> "R-SEG-11: factor de operador reinscrito";
        };
    }

    private static Resultado rechazo(AlcanceDeCuenta alcance, ErrorDeNegocio error) {
        return new Resultado(alcance, 0, error);
    }

    static ErrorDeNegocio tokenInvalido() {
        return new ErrorDeNegocio(
                CodigoError.de(9, 4), "Ese enlace ya no sirve. Pedi otra invitacion o recupera tu acceso.");
    }

    private static ContextoSesion deSistema(Origen origen) {
        return ContextoSesion.deSistema(PROCESO, origen.traza());
    }

    private static UUID correlacion(Origen origen) {
        return UUID.fromString(origen.traza().id());
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}
