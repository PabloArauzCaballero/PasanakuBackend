package bo.aportaya.plataforma.archivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La carpeta del expediente. Lo que se prueba aca es lo unico que decide donde cae
 * una cedula: si el tramo de ruta se valida o se cuela.
 */
class DestinoDeObjetoTest {

    private static final UUID USUARIO = UUID.fromString("9f2c1e4a-0000-4000-8000-000000000001");

    @Test
    @DisplayName("Las tres fotos de alguien caen en la carpeta de esa persona, nombradas por su cara")
    void carpetaPorUsuario() {
        assertThat(DestinoDeObjeto.deExpediente(USUARIO, "ANVERSO").carpeta()).isEqualTo(USUARIO.toString());
        assertThat(DestinoDeObjeto.deExpediente(USUARIO, "ANVERSO").etiqueta()).isEqualTo("anverso");
        assertThat(DestinoDeObjeto.deExpediente(USUARIO, "SELFIE").etiqueta()).isEqualTo("selfie");
        assertThat(DestinoDeObjeto.deExpediente(USUARIO, "REVERSO").carpeta())
                .isEqualTo(DestinoDeObjeto.deExpediente(USUARIO, "ANVERSO").carpeta());
    }

    @Test
    @DisplayName("Una carpeta con barras o con .. no se sanea: se rechaza")
    void rutaQueSubeDeDirectorio() {
        assertThatThrownBy(() -> new DestinoDeObjeto("../otro", "anverso")).isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> new DestinoDeObjeto("una/dos", "anverso")).isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> new DestinoDeObjeto("carpeta", "cara/rara")).isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> new DestinoDeObjeto("  ", "anverso")).isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName("Sin usuario no hay carpeta: una foto de expediente es de alguien")
    void sinUsuario() {
        assertThatThrownBy(() -> DestinoDeObjeto.deExpediente(null, "ANVERSO")).isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName("La carpeta del expediente es la que se usa para saber si una clave es una foto de verdad")
    void carpetaDelExpediente() {
        String carpeta = DestinoDeObjeto.carpetaDeExpediente(USUARIO);
        assertThat(carpeta).isEqualTo("identidad/" + USUARIO + "/");
        // Lo que sube el uploader cae adentro…
        assertThat("s3://identidad/" + USUARIO + "/anverso-" + UUID.randomUUID() + ".jpg")
                .contains(carpeta);
        // …y el relleno que el alta escribe para poder insertar la fila, no.
        assertThat("local://documentos/" + USUARIO + "/anverso").doesNotContain(carpeta);
    }

    @Test
    @DisplayName("La ruta del objeto es legible: identidad/<usuarioId>/anverso-…")
    void claveLegible() {
        var destino = DestinoDeObjeto.deExpediente(USUARIO, "ANVERSO");
        var clave = ClaveObjeto.de("s3://%s/%s/%s-%s.jpg"
                .formatted(
                        AmbitoArchivo.IDENTIDAD.prefijo(), destino.carpeta(), destino.etiqueta(), UUID.randomUUID()));
        assertThat(clave.ruta()).startsWith("identidad/" + USUARIO + "/anverso-");
    }

    @Test
    @DisplayName("La foto de perfil cae en la carpeta de la persona, fuera del expediente")
    void fotoDePerfil() {
        var destino = DestinoDeObjeto.deFotoDePerfil(USUARIO);
        assertThat(destino.carpeta()).isEqualTo(USUARIO.toString());
        assertThat(destino.etiqueta()).isEqualTo("foto");
        assertThat(AmbitoArchivo.PERFIL.prefijo()).isEqualTo("perfil");
        assertThatThrownBy(() -> DestinoDeObjeto.deFotoDePerfil(null)).isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName("La foto de perfil admite JPEG y PNG hasta 5 MB; un PDF o un archivo mas grande no")
    void ambitoDePerfil() {
        AmbitoArchivo.PERFIL.exigirTipoAdmitido("image/jpeg");
        AmbitoArchivo.PERFIL.exigirTipoAdmitido("image/png");
        AmbitoArchivo.PERFIL.exigirQueQuepa(5 * 1024 * 1024);
        assertThatThrownBy(() -> AmbitoArchivo.PERFIL.exigirTipoAdmitido("application/pdf"))
                .isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> AmbitoArchivo.PERFIL.exigirQueQuepa(5 * 1024 * 1024 + 1))
                .isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> AmbitoArchivo.PERFIL.exigirQueQuepa(0)).isInstanceOf(ErrorDeDominio.class);
    }
}
