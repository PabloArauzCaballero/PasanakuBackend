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
 * <p><b>Una carpeta por persona.</b> Las cinco fotos caen en
 * {@code identidad/<usuarioId>/} con el nombre de la cara adelante
 * ({@code anverso-…}, {@code reverso-…}, {@code selfie-…}, {@code perfil-izquierdo-…},
 * {@code perfil-derecho-…}): el expediente de alguien se ve listando una carpeta, sin
 * leer dos tablas para juntar cinco claves sueltas.
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
                DestinoDeObjeto.deExpediente(usuarioId, cara.etiqueta()));
        anotar.ejecutar(usuarioId, cara, guardado, trazaId);
        return guardado;
    }

    /**
     * Las cinco fotos del expediente. El anverso y el reverso van a
     * {@code documento_identidad}; la selfie y los dos perfiles van a
     * {@code verificacion_kyc} — son la prueba de vida, no el papel.
     */
    public enum Cara {
        ANVERSO,
        REVERSO,
        SELFIE,
        PERFIL_IZQUIERDO,
        PERFIL_DERECHO;

        /**
         * El nombre como tramo de ruta: {@link DestinoDeObjeto} rechaza el guion
         * bajo, asi que {@code PERFIL_IZQUIERDO} se escribe {@code perfil-izquierdo}.
         */
        public String etiqueta() {
            return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        }
    }
}
