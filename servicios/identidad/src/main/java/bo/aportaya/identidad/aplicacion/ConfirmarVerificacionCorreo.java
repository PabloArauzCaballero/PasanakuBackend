package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.VerificacionCorreoRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Valida el OTP exacto y lo consume para que pueda respaldar una sola alta. */
@Service
public class ConfirmarVerificacionCorreo {

    private final VerificacionCorreoRepositorio repositorio;
    private final Reloj reloj;

    public ConfirmarVerificacionCorreo(VerificacionCorreoRepositorio repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public void ejecutar(UUID id, String correo, String codigo, String ip, String agente, ContextoSesion ctx) {
        repositorio.confirmar(id, correo, codigo, ip, agente, reloj.ahora().atOffset(ZoneOffset.UTC), ctx);
    }
}
