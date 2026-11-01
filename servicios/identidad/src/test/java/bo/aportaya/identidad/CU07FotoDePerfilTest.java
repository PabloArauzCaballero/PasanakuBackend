package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU07FotoDePerfil;
import bo.aportaya.identidad.aplicacion.ConsultarMiPerfil;
import bo.aportaya.identidad.aplicacion.FotoDePerfilEnLaCuenta;
import bo.aportaya.plataforma.archivos.AlmacenDeArchivos;
import bo.aportaya.plataforma.archivos.AmbitoArchivo;
import bo.aportaya.plataforma.archivos.ArchivoGuardado;
import bo.aportaya.plataforma.archivos.ClaveObjeto;
import bo.aportaya.plataforma.archivos.ContenidoAlmacenado;
import bo.aportaya.plataforma.archivos.ContenidoEntrante;
import bo.aportaya.plataforma.archivos.DestinoDeObjeto;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Foto de perfil (CU-07) contra la base real: correcto (sube y se ve, y el perfil lo dice), limite (sin foto,
 * relleno que no es una clave, cambiarla deja la ultima) e invalido (el almacen la rechaza: la fila no se toca).
 *
 * <p>El almacen es un DOBLE en memoria ({@link AlmacenEnMemoria}): la regla de tipo y tamano que aplica es la del
 * {@link AmbitoArchivo} real, pero MinIO no se ejercita aca. La subida contra el MinIO de verdad se prueba en TEST.
 */
class CU07FotoDePerfilTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 9};

    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static AlmacenEnMemoria almacen;
    private static CU07FotoDePerfil foto;
    private static ConsultarMiPerfil perfil;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        almacen = new AlmacenEnMemoria();
        foto = new CU07FotoDePerfil(almacen, new FotoDePerfilEnLaCuenta(new Datos(dsl)));
        perfil = new ConsultarMiPerfil(new Datos(dsl));
    }

    private static String telefono() {
        return "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
    }

    private static ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private static void subir(UUID usuario, byte[] bytes) {
        tx.executeWithoutResult(s -> foto.cambiar(ctx(usuario), new ByteArrayInputStream(bytes), bytes.length, "yo"));
    }

    private static String urlAvatar(UUID usuario) {
        return dsl.fetchOne("SELECT url_avatar FROM identidad.usuario WHERE id = ?", usuario)
                .get(0, String.class);
    }

    @Test
    void subeLaFotoYSeVeConSusBytes() throws IOException {
        UUID usuario = fixtura.usuario(telefono());

        subir(usuario, JPEG);

        assertThat(urlAvatar(usuario))
                .startsWith("s3://perfil/" + usuario + "/foto-")
                .endsWith(".jpg");
        var vista = tx.execute(s -> foto.ver(ctx(usuario))).orElseThrow();
        assertThat(vista.tipoMime()).isEqualTo("image/jpeg");
        assertThat(vista.datos().readAllBytes()).isEqualTo(JPEG);
        assertThat(tx.execute(s -> perfil.ejecutar(ctx(usuario))).tieneFotoDePerfil())
                .isTrue();
    }

    @Test
    void sinFotoNoHayNadaQueVer() {
        UUID usuario = fixtura.usuario(telefono());

        java.util.Optional<ContenidoAlmacenado> vista = tx.execute(s -> foto.ver(ctx(usuario)));
        assertThat(vista).isEmpty();
        assertThat(tx.execute(s -> perfil.ejecutar(ctx(usuario))).tieneFotoDePerfil())
                .isFalse();
    }

    @Test
    void unRellenoQueNoEsClaveDePerfilCuentaComoSinFoto() {
        UUID usuario = fixtura.usuario(telefono());
        dsl.execute("UPDATE identidad.usuario SET url_avatar = 'https://ejemplo.test/a.png' WHERE id = ?", usuario);

        java.util.Optional<ContenidoAlmacenado> vista = tx.execute(s -> foto.ver(ctx(usuario)));
        assertThat(vista).isEmpty();
        assertThat(tx.execute(s -> perfil.ejecutar(ctx(usuario))).tieneFotoDePerfil())
                .isFalse();
    }

    @Test
    void cambiarlaDejaVigenteLaUltimaSinPisarLaAnterior() {
        UUID usuario = fixtura.usuario(telefono());
        subir(usuario, JPEG);
        String primera = urlAvatar(usuario);

        subir(usuario, PNG);

        assertThat(urlAvatar(usuario)).isNotEqualTo(primera).endsWith(".png");
        assertThat(almacen.objetos).containsKey(primera);
        assertThat(tx.execute(s -> foto.ver(ctx(usuario))).orElseThrow().tipoMime())
                .isEqualTo("image/png");
    }

    @Test
    void siElAlmacenLaRechazaLaFilaNoSeToca() {
        UUID usuario = fixtura.usuario(telefono());
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};

        assertThatThrownBy(() -> subir(usuario, pdf)).isInstanceOf(ErrorDeDominio.class);
        assertThat(urlAvatar(usuario)).isNull();
    }

    @Test
    void cadaUnoSubeLaSuya() {
        UUID ana = fixtura.usuario(telefono());
        UUID beto = fixtura.usuario(telefono());

        subir(ana, JPEG);

        assertThat(urlAvatar(ana)).contains(ana.toString());
        assertThat(urlAvatar(beto)).isNull();
    }

    /** Doble del almacen: guarda en memoria, con la clave y la validacion del ambito reales. */
    static final class AlmacenEnMemoria implements AlmacenDeArchivos {
        final Map<String, byte[]> objetos = new HashMap<>();
        private final Map<String, String> tipos = new HashMap<>();

        @Override
        public ArchivoGuardado guardar(ContenidoEntrante contenido, AmbitoArchivo ambito, DestinoDeObjeto destino) {
            byte[] datos;
            try {
                datos = contenido.datos().readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            ambito.exigirQueQuepa(datos.length);
            String tipo =
                    datos[0] == (byte) 0xFF ? "image/jpeg" : datos[0] == (byte) 0x89 ? "image/png" : "application/pdf";
            ambito.exigirTipoAdmitido(tipo);
            var clave = new ClaveObjeto(
                    "s3",
                    "%s/%s/%s-%s%s"
                            .formatted(
                                    ambito.prefijo(),
                                    destino.carpeta(),
                                    destino.etiqueta(),
                                    UUID.randomUUID(),
                                    tipo.equals("image/png") ? ".png" : ".jpg"));
            objetos.put(clave.toString(), datos);
            tipos.put(clave.toString(), tipo);
            return new ArchivoGuardado(clave, "sha", tipo, datos.length);
        }

        @Override
        public List<ClaveObjeto> listar(AmbitoArchivo ambito, String carpeta) {
            return List.of();
        }

        @Override
        public ContenidoAlmacenado leer(ClaveObjeto clave) {
            byte[] datos = objetos.get(clave.toString());
            return new ContenidoAlmacenado(new ByteArrayInputStream(datos), datos.length, tipos.get(clave.toString()));
        }

        @Override
        public String urlTemporal(ClaveObjeto clave, Duration vigencia) {
            throw new UnsupportedOperationException("la foto de perfil no usa enlaces");
        }

        @Override
        public void marcarDeBaja(ClaveObjeto clave, String motivo) {
            throw new UnsupportedOperationException("la foto de perfil no se da de baja");
        }
    }
}
