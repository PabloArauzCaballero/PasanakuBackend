package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.BorradorOnboardingDatos;
import bo.aportaya.identidad.infraestructura.VerificacionCorreoRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Guarda o actualiza el avance de los formularios del alta incompleto. */
@Service
public class GuardarBorradorOnboarding {

    private final VerificacionCorreoRepositorio repositorio;
    private final Reloj reloj;

    public GuardarBorradorOnboarding(VerificacionCorreoRepositorio repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public BorradorOnboardingDatos ejecutar(UUID verificacionId, BorradorOnboardingDatos borrador, ContextoSesion ctx) {
        return repositorio.guardarBorrador(
                verificacionId, borrador, reloj.ahora().atOffset(ZoneOffset.UTC), ctx);
    }
}
