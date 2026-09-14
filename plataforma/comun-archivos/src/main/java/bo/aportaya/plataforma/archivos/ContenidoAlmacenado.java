package bo.aportaya.plataforma.archivos;

import java.io.InputStream;

/** El binario que vuelve del almacen, con lo minimo para servirlo. */
public record ContenidoAlmacenado(InputStream datos, long bytes, String tipoMime) {}
