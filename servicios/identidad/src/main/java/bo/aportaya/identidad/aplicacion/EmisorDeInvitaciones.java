package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import bo.aportaya.identidad.dominio.puertos.CorreoDeCuenta;
import bo.aportaya.identidad.infraestructura.TokensDeCuentaRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite la invitacion de un solo uso del operador y la manda a su correo institucional DESPUES del commit
 * (P-1, P-4). Lo usan el alta y el trabajo de recuperacion, dentro de su propia transaccion.
 */
@Service
public class EmisorDeInvitaciones implements InvitadorDeOperador {

    private final TokensDeCuentaRepositorio tokens;
    private final CorreoDeCuenta correo;
    private final BitacoraDeCuenta bitacora;
    private final Reloj reloj;
    private final String urlDelBackoffice;

    public EmisorDeInvitaciones(
            TokensDeCuentaRepositorio tokens,
            CorreoDeCuenta correo,
            BitacoraDeCuenta bitacora,
            Reloj reloj,
            @Value("${aportaya.acceso.url-backoffice:http://localhost:4200}") String urlDelBackoffice) {
        this.tokens = tokens;
        this.correo = correo;
        this.bitacora = bitacora;
        this.reloj = reloj;
        this.urlDelBackoffice = urlDelBackoffice.endsWith("/")
                ? urlDelBackoffice.substring(0, urlDelBackoffice.length() - 1)
                : urlDelBackoffice;
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
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
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

    @Override
    public void invitar(DSLContext dsl, UUID operadorId, ContextoSesion quienLoDioDeAlta, String motivo) {
        emitirYEnviar(
                dsl,
                operadorId,
                AlcanceDeCuenta.ALTA,
                BitacoraDeCuenta.Accion.INVITACION_EMITIDA,
                new BitacoraDeCuenta.Actor(
                        quienLoDioDeAlta.usuarioId(), quienLoDioDeAlta.rol(), BitacoraDeCuenta.ORIGEN_PANEL),
                motivo,
                UUID.fromString(quienLoDioDeAlta.traza().id()),
                "0.0.0.0",
                "panel-de-administracion");
    }
}
