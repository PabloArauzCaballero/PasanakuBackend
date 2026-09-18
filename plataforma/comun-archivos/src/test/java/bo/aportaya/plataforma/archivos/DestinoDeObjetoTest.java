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
    @DisplayName("La ruta del objeto es legible: identidad/<usuarioId>/anverso-…")
    void claveLegible() {
        var destino = DestinoDeObjeto.deExpediente(USUARIO, "ANVERSO");
        var clave = ClaveObjeto.de("s3://%s/%s/%s-%s.jpg"
                .formatted(AmbitoArchivo.IDENTIDAD.prefijo(), destino.carpeta(), destino.etiqueta(), UUID.randomUUID()));
        assertThat(clave.ruta()).startsWith("identidad/" + USUARIO + "/anverso-");
    }
}
