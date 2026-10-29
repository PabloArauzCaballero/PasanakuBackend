package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.archivos.ClaveObjeto;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La columna {@code usuario.url_avatar} de la cuenta de la sesion: anotar la clave de la foto
 * recien subida y leer cual es la vigente.
 *
 * <p>Bean aparte de {@link CU07FotoDePerfil} por lo mismo que {@link AnotarFotoEnExpediente}:
 * {@code @Transactional} lo aplica el proxy, y asi la transaccion dura lo que dura el UPDATE y
 * no lo que tarda en subir una foto por datos moviles. La fila se toca con el contexto de la
 * sesion: la politica {@code pol_usuario_titular} solo deja escribir la propia.
 */
@Service
public class FotoDePerfilEnLaCuenta {

    private final Datos datos;

    public FotoDePerfilEnLaCuenta(Datos datos) {
        this.datos = datos;
    }

    @Transactional
    public void anotar(ContextoSesion ctx, ClaveObjeto clave) {
        datos.conContexto(ctx, dsl -> dsl.execute(
                "UPDATE identidad.usuario SET url_avatar = ? WHERE id = ?", clave.toString(), ctx.usuarioId()));
    }

    /**
     * La clave de la foto vigente, si hay una subida de verdad. Un valor que no es una clave del
     * ambito de perfil —un relleno de semilla, una URL vieja— cuenta como «sin foto»: la app
     * muestra las iniciales en vez de un error por un dato que la persona no cargo.
     */
    @Transactional(readOnly = true)
    public Optional<ClaveObjeto> vigente(ContextoSesion ctx) {
        String texto = datos.conContexto(ctx, dsl -> {
            var fila = dsl.fetchOne("SELECT url_avatar FROM identidad.usuario WHERE id = ?", ctx.usuarioId());
            return fila == null ? null : fila.get(0, String.class);
        });
        if (texto == null || !texto.startsWith(PREFIJO)) {
            return Optional.empty();
        }
        return Optional.of(ClaveObjeto.de(texto));
    }

    /** Lo que deja {@link CU07FotoDePerfil} en la columna: {@code s3://perfil/<usuarioId>/foto-…}. */
    static final String PREFIJO = "s3://" + bo.aportaya.plataforma.archivos.AmbitoArchivo.PERFIL.prefijo() + "/";
}
