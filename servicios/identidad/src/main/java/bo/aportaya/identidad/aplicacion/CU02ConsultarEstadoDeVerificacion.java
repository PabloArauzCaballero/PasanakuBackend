package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.identidad.infraestructura.RevisionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-02 · el estado de la verificacion, para quien todavia no puede abrir sesion.
 *
 * <p>Corre con el contexto del sistema, igual que {@link AnotarFotoEnExpediente}: el
 * dispositivo de alguien recien registrado es nuevo y {@code ExigeSegundoFactor} le
 * pide un MFA que todavia no tiene enrolado — no hay sesion que presentar, y esta
 * consulta es publica por el mismo motivo que {@code subirDocumento}.
 *
 * <p>Devuelve el {@link ExpedienteDeIdentidad} completo porque eso es lo que ya sabe
 * mapear la pagina; la respuesta minima (sin nombre ni documento) la recorta el
 * controlador, no este caso de uso.
 */
@Service
public class CU02ConsultarEstadoDeVerificacion {

    private final RevisionRepositorio revisiones;
    private final Datos datos;

    public CU02ConsultarEstadoDeVerificacion(RevisionRepositorio revisiones, Datos datos) {
        this.revisiones = revisiones;
        this.datos = datos;
    }

    @Transactional(readOnly = true)
    public ExpedienteDeIdentidad ejecutar(UUID usuarioId, String trazaId) {
        ContextoSesion ctx =
                ContextoSesion.deSistema(CU02GuardarFotoDelExpediente.PROCESO_EXPEDIENTE, new Traza(trazaId));
        return datos.conContexto(ctx, dsl -> revisiones.deUsuario(dsl, usuarioId))
                .orElseThrow(() -> new ErrorDeDominio("Todavia no hay un expediente abierto para esa persona"));
    }
}
