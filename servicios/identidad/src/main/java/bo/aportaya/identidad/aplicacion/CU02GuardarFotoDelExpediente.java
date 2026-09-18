package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.archivos.AlmacenDeArchivos;
import bo.aportaya.plataforma.archivos.AmbitoArchivo;
import bo.aportaya.plataforma.archivos.ArchivoGuardado;
import bo.aportaya.plataforma.archivos.ContenidoEntrante;
import bo.aportaya.plataforma.archivos.DestinoDeObjeto;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-02 · guarda una foto del expediente: el anverso, el reverso o la prueba de vida.
 *
 * <p><b>Primero el archivo, despues la fila.</b> Se sube al almacen y recien con la
 * clave y el hash en la mano se toca la base. Al reves, un fallo del almacen dejaria
 * una fila apuntando a un objeto que no existe — y esa fila es evidencia legal.
 *
 * <p>Si el almacen guarda y la base falla, queda un objeto huerfano: eso es
 * recuperable (el barrido de retencion lo encuentra sin referencia) y una fila rota no.
 *
 * <p><b>Una carpeta por persona.</b> Las tres fotos caen en
 * {@code identidad/<usuarioId>/} con el nombre de la cara adelante
 * ({@code anverso-…}, {@code reverso-…}, {@code selfie-…}): el expediente de alguien
 * se ve listando una carpeta, sin leer dos tablas para juntar tres claves sueltas.
 */
@Service
public class CU02GuardarFotoDelExpediente {

    /** El proceso del sistema que corre esto: el alta todavia no tiene sesion. */
    public static final UUID PROCESO_EXPEDIENTE = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private final AlmacenDeArchivos almacen;
    private final AnotarFotoEnExpediente anotar;

    public CU02GuardarFotoDelExpediente(AlmacenDeArchivos almacen, AnotarFotoEnExpediente anotar) {
        this.almacen = almacen;
        this.anotar = anotar;
    }

    public ArchivoGuardado ejecutar(
            UUID usuarioId, Cara cara, InputStream contenido, long bytes, String nombre, String trazaId) {
        ArchivoGuardado guardado = almacen.guardar(
                new ContenidoEntrante(contenido, bytes, nombre),
                AmbitoArchivo.IDENTIDAD,
                DestinoDeObjeto.deExpediente(usuarioId, cara.name()));
        anotar.ejecutar(usuarioId, cara, guardado, trazaId);
        return guardado;
    }

    /** Las tres fotos del expediente. La selfie va a `verificacion_kyc`, no al documento. */
    public enum Cara {
        ANVERSO,
        REVERSO,
        SELFIE
    }

    /** La carpeta del expediente de alguien, para listarla o para nombrarla. */
    public static String carpetaDe(UUID usuarioId) {
        return AmbitoArchivo.IDENTIDAD.prefijo() + "/" + usuarioId + "/";
    }
}
