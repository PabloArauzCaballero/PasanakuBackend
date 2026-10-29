package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.archivos.AlmacenDeArchivos;
import bo.aportaya.plataforma.archivos.AmbitoArchivo;
import bo.aportaya.plataforma.archivos.ContenidoAlmacenado;
import bo.aportaya.plataforma.archivos.ContenidoEntrante;
import bo.aportaya.plataforma.archivos.DestinoDeObjeto;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * CU-07 · la foto de perfil de la propia cuenta: subirla y verla.
 *
 * <p><b>Primero el archivo, despues la fila</b>, igual que {@link CU02GuardarFotoDelExpediente}:
 * si el almacen falla no queda {@code url_avatar} apuntando a un objeto que no existe; si la base
 * falla queda un objeto huerfano, que el barrido encuentra sin referencia.
 *
 * <p>Siempre es de quien tiene la sesion: no hay id en la ruta, asi que nadie sube ni mira la
 * foto de otra persona por esta via.
 */
@Service
public class CU07FotoDePerfil {

    private final AlmacenDeArchivos almacen;
    private final FotoDePerfilEnLaCuenta cuenta;

    public CU07FotoDePerfil(AlmacenDeArchivos almacen, FotoDePerfilEnLaCuenta cuenta) {
        this.almacen = almacen;
        this.cuenta = cuenta;
    }

    public void cambiar(ContextoSesion ctx, InputStream contenido, long bytes, String nombre) {
        var guardado = almacen.guardar(
                new ContenidoEntrante(contenido, bytes, nombre),
                AmbitoArchivo.PERFIL,
                DestinoDeObjeto.deFotoDePerfil(ctx.usuarioId()));
        cuenta.anotar(ctx, guardado.clave());
    }

    /** Vacio si la cuenta no tiene foto: la app muestra las iniciales. */
    public Optional<ContenidoAlmacenado> ver(ContextoSesion ctx) {
        return cuenta.vigente(ctx).map(almacen::leer);
    }

    /** Para {@code MiPerfil.tieneFotoDePerfil}, sin abrir el objeto. */
    public boolean tiene(ContextoSesion ctx) {
        return cuenta.vigente(ctx).isPresent();
    }
}
