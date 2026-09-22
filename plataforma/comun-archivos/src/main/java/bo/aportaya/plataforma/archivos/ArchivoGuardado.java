package bo.aportaya.plataforma.archivos;

/**
 * El resultado de guardar: la clave que va a la columna y el SHA-256 del contenido.
 *
 * <p>El hash es obligatorio al guardar (ADR-034, regla 5) y va a `hash_archivo` en las
 * siete tablas que la tienen. En las que no, igual se calcula y viaja en la respuesta
 * — pero no se inventa columna.
 */
public record ArchivoGuardado(ClaveObjeto clave, String hashSha256, String tipoMime, long bytes) {}
