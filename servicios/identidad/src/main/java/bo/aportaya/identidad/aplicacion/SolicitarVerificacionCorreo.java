package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import bo.aportaya.identidad.infraestructura.VerificacionCorreoRepositorio;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Prepara y entrega por Gmail el OTP de correo previo al alta. */
@Service
public class SolicitarVerificacionCorreo {

    private final VerificacionCorreoRepositorio repositorio;
    private final CorreoDeVerificacion correo;
    private final Reloj reloj;

    public SolicitarVerificacionCorreo(
            VerificacionCorreoRepositorio repositorio, CorreoDeVerificacion correo, Reloj reloj) {
        this.repositorio = repositorio;
        this.correo = correo;
        this.reloj = reloj;
    }

    public Resultado ejecutar(String destino, UUID idempotencia, String ip, String agente, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        var preparada = repositorio.preparar(destino, idempotencia, ip, agente, ahora, ctx);
        if (!preparada.nueva()) {
            return resultado(preparada.salida());
        }
        Duration vigencia = Duration.between(ahora, preparada.salida().expiraEn());
        try {
            correo.enviarCodigo(destino, preparada.codigo(), vigencia);
            repositorio.marcarEnviada(preparada.salida().verificacionId(), ahora, ctx);
            return resultado(preparada.salida());
        } catch (RuntimeException fallo) {
            repositorio.invalidarPorFalloDeEnvio(preparada.salida().verificacionId(), ahora, ctx);
            throw new ErrorDeNegocio(CodigoError.de(1, 7), "No pudimos enviar el correo. Intenta nuevamente.");
        }
    }

    private static Resultado resultado(VerificacionCorreoRepositorio.Solicitada salida) {
        return new Resultado(salida.verificacionId(), salida.destinoEnmascarado(), salida.expiraEn());
    }

    public record Resultado(UUID verificacionId, String destinoEnmascarado, OffsetDateTime expiraEn) {}
}
