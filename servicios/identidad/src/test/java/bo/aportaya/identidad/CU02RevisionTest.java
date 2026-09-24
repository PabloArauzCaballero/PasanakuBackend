package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.identidad.aplicacion.CU02RevisarExpediente;
import bo.aportaya.identidad.infraestructura.RevisionRepositorio;
import bo.aportaya.plataforma.archivos.AlmacenDeArchivos;
import bo.aportaya.plataforma.archivos.ClaveObjeto;
import bo.aportaya.plataforma.archivos.ContenidoAlmacenado;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

class CU02RevisionTest {
    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static RevisionRepositorio repositorio;
    private static FixturaDeIdentidad fixtura;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        repositorio = new RevisionRepositorio();
        fixtura = new FixturaDeIdentidad(dsl);
    }

    @Test
    void colaDistingueFotosCargadasDeRellenosYFiltraPorEstado() {
        UUID usuario = usuario();
        UUID verificacion = expediente(usuario, "PENDIENTE", true);
        String reverso = clave(usuario, "reverso");
        dsl.execute(
                "UPDATE identidad.documento_identidad SET url_anverso = ?, url_reverso = ? WHERE usuario_id = ?",
                "local://documentos/relleno",
                reverso,
                usuario);
        String selfie = clave(usuario, "selfie");
        dsl.execute("UPDATE identidad.verificacion_kyc SET url_selfie = ? WHERE id = ?", selfie, verificacion);
        UUID sinDocumento = usuario();
        UUID otraVerificacion = expediente(sinDocumento, "EN_REVISION", false);

        var pendientes = repositorio.enEstado(dsl, "PENDIENTE");
        var fila = pendientes.stream()
                .filter(it -> it.verificacionId().equals(verificacion))
                .findFirst()
                .orElseThrow();
        assertThat(fila.nombreCompleto()).isEqualTo("Ana Quispe");
        assertThat(fila.documento()).isEqualTo("CI LP");
        assertThat(fila.fotos()).containsExactly("REVERSO", "SELFIE");
        assertThat(pendientes).noneMatch(it -> it.verificacionId().equals(otraVerificacion));

        var todos = repositorio.enEstado(dsl, null);
        assertThat(todos)
                .anyMatch(it -> it.verificacionId().equals(otraVerificacion)
                        && it.documento() == null
                        && it.fotos().isEmpty());
        assertThatThrownBy(() -> repositorio.claveDeFoto(dsl, verificacion, "ANVERSO"))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("no esta cargada");
        assertThat(repositorio.claveDeFoto(dsl, verificacion, "REVERSO")).isEqualTo(reverso);
        assertThat(repositorio.claveDeFoto(dsl, verificacion, "SELFIE")).isEqualTo(selfie);
        assertThatThrownBy(() -> repositorio.claveDeFoto(dsl, verificacion, "LADO"))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("no existe");
        assertThatThrownBy(() -> repositorio.claveDeFoto(dsl, UUID.randomUUID(), "SELFIE"))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("expediente no existe");
    }

    @Test
    void fotoYContenidoSoloLeenLaClaveVerificadaDelExpediente() {
        UUID usuario = usuario();
        UUID verificacion = expediente(usuario, "PENDIENTE", true);
        String anverso = clave(usuario, "anverso");
        dsl.execute("UPDATE identidad.documento_identidad SET url_anverso = ? WHERE usuario_id = ?", anverso, usuario);
        ClaveObjeto objeto = ClaveObjeto.de(anverso);
        AlmacenDeArchivos almacen = mock(AlmacenDeArchivos.class);
        when(almacen.urlTemporal(objeto, Duration.ofMinutes(10))).thenReturn("https://foto-temporal");
        when(almacen.leer(objeto))
                .thenReturn(new ContenidoAlmacenado(new ByteArrayInputStream(new byte[] {1}), 1, "image/jpeg"));
        var caso = new CU02RevisarExpediente(
                repositorio, almacen, new Datos(dsl), Reloj.fijo(Instant.parse("2026-09-24T12:00:00Z")));
        ContextoSesion contexto = contexto(usuario());

        var enlace = transaccion.execute(estado -> caso.foto(verificacion, "ANVERSO", contexto));
        assertThat(enlace.url()).isEqualTo("https://foto-temporal");
        assertThat(enlace.vigenteHasta().toInstant()).isEqualTo(Instant.parse("2026-09-24T12:10:00Z"));
        var contenido = transaccion.execute(estado -> caso.contenido(verificacion, "ANVERSO", contexto));
        assertThat(contenido.bytes()).isEqualTo(1);
        verify(almacen).leer(objeto);
        assertThatThrownBy(() -> transaccion.execute(estado -> caso.foto(verificacion, "REVERSO", contexto)))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("no esta cargada");
    }

    @Test
    void resolverRegistraRevisorYMotivoYRechazaDecisionesIncompletas() {
        UUID titular = usuario();
        UUID verificacion = expediente(titular, "PENDIENTE", false);
        UUID revisor = usuario();
        ContextoSesion contexto = contexto(revisor);
        var caso = new CU02RevisarExpediente(
                repositorio,
                mock(AlmacenDeArchivos.class),
                new Datos(dsl),
                Reloj.fijo(Instant.parse("2026-09-24T12:00:00Z")));

        assertThatThrownBy(() -> caso.resolver(verificacion, "RECHAZAR", null, contexto))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("decir por que");
        assertThatThrownBy(() -> caso.resolver(verificacion, "RECHAZAR", " ", contexto))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("decir por que");
        transaccion.execute(estado -> {
            caso.resolver(verificacion, "RECHAZAR", "Foto ilegible", contexto);
            return null;
        });
        var rechazado = repositorio.enEstado(dsl, "RECHAZADA").stream()
                .filter(it -> it.verificacionId().equals(verificacion))
                .findFirst()
                .orElseThrow();
        assertThat(rechazado.motivoRechazo()).isEqualTo("Foto ilegible");
        assertThat(dsl.fetchOne("SELECT revisada_por FROM identidad.verificacion_kyc WHERE id = ?", verificacion)
                        .get(0, UUID.class))
                .isEqualTo(revisor);

        transaccion.execute(estado -> {
            caso.resolver(verificacion, "APROBAR", "Se ignora", contexto);
            return null;
        });
        assertThat(dsl.fetchValue("SELECT motivo_rechazo FROM identidad.verificacion_kyc WHERE id = ?", verificacion))
                .isNull();
        assertThat(dsl.fetchValue("SELECT estado FROM identidad.verificacion_kyc WHERE id = ?", verificacion))
                .isEqualTo("APROBADA");
        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    caso.resolver(UUID.randomUUID(), "APROBAR", null, contexto);
                    return null;
                }))
                .isInstanceOf(ErrorDeDominio.class)
                .hasMessageContaining("expediente no existe");
    }

    private static UUID usuario() {
        return fixtura.usuario(
                "+59177" + String.format("%06d", Math.abs(UUID.randomUUID().hashCode() % 1_000_000)));
    }

    private static UUID expediente(UUID usuario, String estado, boolean conDocumento) {
        UUID verificacion = UUID.randomUUID();
        UUID documento = null;
        if (conDocumento) {
            documento = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO identidad.documento_identidad
                      (id, usuario_id, tipo, numero_cifrado, version_llave, hash_numero,
                       lugar_expedicion, pais_emision, estado)
                    VALUES (?, ?, 'CI', 'cifrado', 1, ?, 'LP', 'BO', 'EN_REVISION')
                    """,
                    documento,
                    usuario,
                    UUID.randomUUID().toString());
        }
        dsl.execute(
                """
                INSERT INTO identidad.verificacion_kyc
                  (id, usuario_id, documento_id, nivel_solicitado, estado, iniciada_en)
                VALUES (?, ?, ?, 'BASICO', ?, now())
                """,
                verificacion,
                usuario,
                documento,
                estado);
        return verificacion;
    }

    private static String clave(UUID usuario, String cara) {
        return "s3://identidad/" + usuario + "/" + cara + "-" + UUID.randomUUID() + ".jpg";
    }

    private static ContextoSesion contexto(UUID revisor) {
        return ContextoSesion.deSistema(revisor, new Traza(UUID.randomUUID().toString()));
    }
}
