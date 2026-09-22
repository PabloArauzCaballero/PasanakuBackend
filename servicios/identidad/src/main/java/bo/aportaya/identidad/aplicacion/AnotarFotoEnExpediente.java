package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.ExpedienteRepositorio;
import bo.aportaya.plataforma.archivos.ArchivoGuardado;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota en la fila la clave del objeto ya subido.
 *
 * <p>Es un bean aparte por una razon concreta: `@Transactional` lo aplica el proxy de
 * Spring, y un metodo anotado que se llama desde la MISMA clase no pasa por el proxy
 * — la transaccion no se abre y `conContexto` falla con «SET LOCAL fuera de una
 * transaccion no fija nada». Ademas asi la transaccion dura lo que dura el UPDATE, y
 * no lo que tarda en subir una foto por datos moviles.
 */
@Service
public class AnotarFotoEnExpediente {

    private final ExpedienteRepositorio expedientes;
    private final Datos datos;

    public AnotarFotoEnExpediente(ExpedienteRepositorio expedientes, Datos datos) {
        this.expedientes = expedientes;
        this.datos = datos;
    }

    @Transactional
    public void ejecutar(
            UUID usuarioId, CU02GuardarFotoDelExpediente.Cara cara, ArchivoGuardado archivo, String trazaId) {
        ContextoSesion ctx =
                ContextoSesion.deSistema(CU02GuardarFotoDelExpediente.PROCESO_EXPEDIENTE, new Traza(trazaId));
        datos.conContexto(ctx, dsl -> {
            switch (cara) {
                case ANVERSO -> expedientes.anotarAnverso(dsl, usuarioId, archivo);
                case REVERSO -> expedientes.anotarReverso(dsl, usuarioId, archivo);
                case SELFIE -> expedientes.anotarSelfie(dsl, usuarioId, archivo);
            }
            return null;
        });
    }
}
