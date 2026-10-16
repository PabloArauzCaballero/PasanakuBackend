package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.RevisionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-02 · permite al titular completar o corregir su fecha mientras la revisión sigue abierta. */
@Service
public class CU02ActualizarFechaExpiracion {

    private final RevisionRepositorio revisiones;
    private final Datos datos;

    public CU02ActualizarFechaExpiracion(RevisionRepositorio revisiones, Datos datos) {
        this.revisiones = revisiones;
        this.datos = datos;
    }

    @Transactional
    public void ejecutar(UUID usuarioId, LocalDate fecha, ContextoSesion actor, LocalDate hoy) {
        if (fecha == null || fecha.isBefore(hoy)) {
            throw new ErrorDeNegocio(CodigoError.de(1, 11), "Ingresá una fecha de vencimiento vigente.");
        }
        datos.conContexto(actor, dsl -> {
            revisiones.actualizarFechaExpiracion(dsl, usuarioId, fecha);
            return null;
        });
    }
}
