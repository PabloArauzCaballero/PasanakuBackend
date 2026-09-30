package bo.aportaya.plataforma.archivos;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * El almacen contra un servidor S3 — en desarrollo, MinIO.
 *
 * <p>Guarda en un bucket con versionado: reemplazar un objeto crea una version nueva y
 * la anterior sigue ahi, que es lo que exige el ADR-034 para evidencia legal.
 *
 * <p>El contenido se lee entero a memoria antes de subir, y a proposito: hay que
 * mirarle los primeros bytes para decidir el tipo y calcular el SHA-256 del mismo
 * contenido que se sube. Los tamaños maximos son de megabytes, no de gigabytes.
 */
public class AlmacenS3Adaptador implements AlmacenDeArchivos {

    private static final String ESQUEMA = "s3";

    private final MinioClient cliente;
    private final String bucket;

    public AlmacenS3Adaptador(String url, String usuario, String clave, String bucket) {
        this.cliente =
                MinioClient.builder().endpoint(url).credentials(usuario, clave).build();
        this.bucket = bucket;
    }

    @Override
    public ArchivoGuardado guardar(ContenidoEntrante contenido, AmbitoArchivo ambito, DestinoDeObjeto destino) {
        byte[] datos = leerTodo(contenido.datos(), ambito);
        ambito.exigirQueQuepa(datos.length);
        String tipo = TipoPorContenido.detectar(datos);
        ambito.exigirTipoAdmitido(tipo);

        ClaveObjeto clave = nuevaClave(ambito, destino, tipo);
        try (InputStream flujo = new ByteArrayInputStream(datos)) {
            cliente.putObject(PutObjectArgs.builder().bucket(bucket).object(clave.nombreEnBucket()).stream(
                            flujo, datos.length, -1)
                    .contentType(tipo)
                    // El nombre original va como metadato, nunca como ruta.
                    .userMetadata(Map.of("nombre-original", seguro(contenido.nombreOriginal())))
                    .build());
        } catch (Exception e) {
            throw new ErrorDeDominio("No pudimos guardar el archivo. Probá de nuevo.", e);
        }
        return new ArchivoGuardado(clave, sha256(datos), tipo, datos.length);
    }

    @Override
    public ContenidoAlmacenado leer(ClaveObjeto clave) {
        try {
            var info = cliente.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(clave.nombreEnBucket())
                    .build());
            InputStream datos = cliente.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(clave.nombreEnBucket())
                    .build());
            return new ContenidoAlmacenado(datos, info.size(), info.contentType());
        } catch (Exception e) {
            throw new ErrorDeDominio("No encontramos ese archivo", e);
        }
    }

    @Override
    public String urlTemporal(ClaveObjeto clave, Duration vigencia) {
        try {
            return cliente.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(clave.nombreEnBucket())
                    .expiry((int) vigencia.toSeconds())
                    .build());
        } catch (Exception e) {
            throw new ErrorDeDominio("No pudimos preparar el enlace del archivo", e);
        }
    }

    /**
     * Lo que hay en la carpeta de alguien. Sirve para comprobar que el expediente
     * esta completo sin abrir ninguna foto — listar nombres no es mirar cedulas.
     */
    @Override
    public java.util.List<ClaveObjeto> listar(AmbitoArchivo ambito, String carpeta) {
        String prefijo = ambito.prefijo() + "/" + DestinoDeObjeto.carpetaValida(carpeta) + "/";
        var claves = new java.util.ArrayList<ClaveObjeto>();
        try {
            for (var resultado : cliente.listObjects(io.minio.ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefijo)
                    .recursive(true)
                    .build())) {
                claves.add(new ClaveObjeto(ESQUEMA, resultado.get().objectName()));
            }
        } catch (Exception e) {
            throw new ErrorDeDominio("No pudimos listar los archivos de esa carpeta", e);
        }
        return java.util.List.copyOf(claves);
    }

    @Override
    public void marcarDeBaja(ClaveObjeto clave, String motivo) {
        // Baja LOGICA: el objeto no se borra. Se le pone una etiqueta y el barrido de
        // retencion es el unico que borra bytes, cuando vence el plazo.
        try {
            cliente.setObjectTags(io.minio.SetObjectTagsArgs.builder()
                    .bucket(bucket)
                    .object(clave.nombreEnBucket())
                    .tags(Map.of("baja", "si", "motivo", seguro(motivo)))
                    .build());
        } catch (Exception e) {
            throw new ErrorDeDominio("No pudimos marcar el archivo de baja", e);
        }
    }

    /**
     * `identidad/<usuarioId>/anverso-<uuid>.jpg` — la carpeta agrupa el expediente de
     * una persona y el UUID del final evita pisar la foto anterior: los objetos no se
     * sobrescriben (ADR-034), sacarse la foto de nuevo agrega una al lado.
     */
    private static ClaveObjeto nuevaClave(AmbitoArchivo ambito, DestinoDeObjeto destino, String tipo) {
        String ruta = "%s/%s/%s-%s%s"
                .formatted(ambito.prefijo(), destino.carpeta(), destino.etiqueta(), UUID.randomUUID(), extension(tipo));
        return new ClaveObjeto(ESQUEMA, ruta);
    }

    private static String extension(String tipo) {
        return switch (tipo) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }

    private static byte[] leerTodo(InputStream entrada, AmbitoArchivo ambito) {
        try (entrada) {
            // Un byte mas que el maximo: si lo hay, el archivo no cabe y se corta ahi
            // en vez de cargarse entero para despues rechazarlo.
            return entrada.readNBytes((int) ambito.tamanoMaximo() + 1);
        } catch (IOException e) {
            throw new ErrorDeDominio("Se corto la subida del archivo", e);
        }
    }

    private static String sha256(byte[] datos) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tiene que existir en cualquier JVM", e);
        }
    }

    /** Metadato de S3: solo ASCII imprimible, y corto. */
    private static String seguro(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = new String(texto.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return limpio.length() > 80 ? limpio.substring(0, 80) : limpio;
    }
}
