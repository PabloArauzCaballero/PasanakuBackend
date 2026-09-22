package bo.aportaya.plataforma.archivos;

import java.time.Duration;

/**
 * El puerto de archivos (ADR-034). Lo que devuelve al guardar es una
 * {@link ClaveObjeto}, no una URL: la columna guarda la clave y el binario lo entrega
 * el servicio dueño de la tabla, que valida sesion, permisos y politicas de fila.
 *
 * <p>Los objetos <b>no se sobrescriben ni se borran</b>. Reemplazar es guardar uno
 * nuevo y apuntar la fila; dar de baja es logico, y el unico que borra bytes es el
 * barrido de retencion cuando vence el plazo.
 */
public interface AlmacenDeArchivos {

    /**
     * Guarda y devuelve la clave y el hash. Valida tamaño y tipo contra el ambito.
     *
     * <p>El {@link DestinoDeObjeto} decide en que carpeta cae: para el expediente de
     * identidad, una carpeta por persona, para que las tres fotos de alguien esten
     * juntas y se puedan listar sin leer la base.
     */
    ArchivoGuardado guardar(ContenidoEntrante contenido, AmbitoArchivo ambito, DestinoDeObjeto destino);

    /** Para lo que no pertenece a nadie en particular: reparto por fecha. */
    default ArchivoGuardado guardar(ContenidoEntrante contenido, AmbitoArchivo ambito) {
        return guardar(contenido, ambito, DestinoDeObjeto.porFecha());
    }

    /** Lo que hay guardado en la carpeta de alguien, para revisar un expediente entero. */
    java.util.List<ClaveObjeto> listar(AmbitoArchivo ambito, String carpeta);

    /** El binario, para que el servicio dueño lo entregue con sus reglas. */
    ContenidoAlmacenado leer(ClaveObjeto clave);

    /**
     * Un enlace de vigencia corta, para cuando el binario lo tiene que ver un humano
     * en el backoffice sin que el servidor proxie megabytes de imagen.
     */
    String urlTemporal(ClaveObjeto clave, Duration vigencia);

    /** Baja logica: el objeto queda, marcado. Los bytes los borra la retencion. */
    void marcarDeBaja(ClaveObjeto clave, String motivo);
}
