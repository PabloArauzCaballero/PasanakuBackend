package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.SustituirAdministrador;
import bo.aportaya.grupos.aplicacion.SustituirAdministrador.Entrada;
import bo.aportaya.grupos.infraestructura.SustitucionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H5.S1.M7: el cambio de administrador conserva las obligaciones del saliente. PostgreSQL real. */
class CU68SustitucionTest extends BaseDeCU68 {
    private SustituirAdministrador sustitucion;
    private UUID grupo;
    private UUID saliente;
    private UUID salienteUsuario;
    private UUID entranteUsuario;
    private UUID entrante;
    private ContextoSesion backoffice;

    @BeforeEach
    void prepararGrupo() {
        sustitucion = new SustituirAdministrador(
                new Datos(dsl), new SustitucionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        grupo = grupoSinCuposLibres();
        var filas = dslFixtura.fetch(
                "SELECT id, usuario_id FROM grupos.participante WHERE grupo_id=? ORDER BY id LIMIT 2", grupo);
        saliente = filas.get(0).get("id", UUID.class);
        salienteUsuario = filas.get(0).get("usuario_id", UUID.class);
        entrante = filas.get(1).get("id", UUID.class);
        entranteUsuario = filas.get(1).get("usuario_id", UUID.class);
        dslFixtura.execute("UPDATE grupos.participante SET es_organizador=true WHERE id=?", saliente);
        backoffice = sesion(fixtura.usuario(), "BACKOFFICE");
    }

    private ContextoSesion sesion(UUID usuario, String rol) {
        return ContextoSesion.de(usuario, rol, new Traza(UUID.randomUUID().toString()));
    }

    private Entrada pedido(UUID clave, UUID nuevo) {
        return new Entrada(grupo, nuevo, clave, "Habilitación revocada por incumplimiento documentado");
    }

    private boolean esAdministrador(UUID participante) {
        return (boolean) dsl.fetchOne("SELECT es_organizador FROM grupos.participante WHERE id=?", participante)
                .get(0);
    }

    @Test
    void cambiaQuienAdministraYElSalienteConservaParticipacionYCupos() {
        int cuposAntes = contar("SELECT count(*)::int FROM grupos.cupo WHERE participante_id=?", saliente);
        var registro =
                transaccion.execute(tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), entranteUsuario), backoffice));
        assertThat(esAdministrador(entrante)).isTrue();
        assertThat(esAdministrador(saliente)).isFalse();
        assertThat(contar("SELECT count(*)::int FROM grupos.cupo WHERE participante_id=?", saliente))
                .isEqualTo(cuposAntes)
                .isPositive();
        assertThat(dsl.fetchOne("SELECT estado FROM grupos.participante WHERE id=?", saliente)
                        .get(0))
                .isEqualTo("ACTIVO");
        assertThat(registro.obligacionesConservadas())
                .contains("participante=" + saliente)
                .contains("cupos=");
        assertThat(contar("SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND es_organizador", grupo))
                .isEqualTo(1);
    }

    @Test
    void reintentarConLaMismaClaveDevuelveLaOriginalSinDuplicarNiEmitirDeNuevo() {
        UUID clave = UUID.randomUUID();
        var primera = transaccion.execute(tx -> sustitucion.ejecutar(pedido(clave, entranteUsuario), backoffice));
        var segunda = transaccion.execute(tx -> sustitucion.ejecutar(pedido(clave, entranteUsuario), backoffice));
        assertThat(segunda).isEqualTo(primera);
        assertThat(contar("SELECT count(*)::int FROM grupos.sustitucion_administrador WHERE grupo_id=?", grupo))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.evento_dominio WHERE tipo=? AND agregado_id=?",
                        "grupos.administrador_sustituido",
                        grupo))
                .isEqualTo(1);
    }

    @Test
    void laMismaClaveParaOtroEntranteSeRechaza() {
        UUID clave = UUID.randomUUID();
        transaccion.execute(tx -> sustitucion.ejecutar(pedido(clave, entranteUsuario), backoffice));
        UUID tercero = dslFixtura
                .fetchOne(
                        "SELECT usuario_id FROM grupos.participante WHERE grupo_id=? AND id NOT IN (?,?) LIMIT 1",
                        grupo,
                        saliente,
                        entrante)
                .get(0, UUID.class);
        assertThatThrownBy(() -> transaccion.execute(tx -> sustitucion.ejecutar(pedido(clave, tercero), backoffice)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otra sustitucion");
    }

    @Test
    void rolInsuficienteNoSustituyeNiAlteraNada() {
        for (String rol : new String[] {"PARTICIPANTE", "ORGANIZADOR"}) {
            var ctx = sesion(salienteUsuario, rol);
            assertThatThrownBy(() -> transaccion.execute(
                            tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), entranteUsuario), ctx)))
                    .isInstanceOf(ErrorDeNegocio.class);
        }
        assertThat(esAdministrador(saliente)).isTrue();
        assertThat(contar("SELECT count(*)::int FROM grupos.sustitucion_administrador WHERE grupo_id=?", grupo))
                .isZero();
    }

    @Test
    void entranteQueNoEsParticipanteDelGrupoSeRechaza() {
        UUID ajeno = fixtura.usuario();
        assertThatThrownBy(() ->
                        transaccion.execute(tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), ajeno), backoffice)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("participante activo");
        assertThat(esAdministrador(saliente)).isTrue();
    }

    @Test
    void sustituirPorQuienYaAdministraSeRechaza() {
        assertThatThrownBy(() -> transaccion.execute(
                        tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), salienteUsuario), backoffice)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya es la administradora");
    }

    @Test
    void quienDecideNoPuedeQuedarComoAdministrador() {
        var autoDesignado = sesion(entranteUsuario, "BACKOFFICE");
        assertThatThrownBy(() -> transaccion.execute(
                        tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), entranteUsuario), autoDesignado)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no puede ser quien queda");
    }

    @Test
    void motivoVacioOClaveAusenteSeRechazan() {
        assertThatThrownBy(() -> transaccion.execute(tx -> sustitucion.ejecutar(
                        new Entrada(grupo, entranteUsuario, UUID.randomUUID(), "   "), backoffice)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(
                        tx -> sustitucion.ejecutar(new Entrada(grupo, entranteUsuario, null, "motivo"), backoffice)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "rechaza por R-GRP-19: la sustitución del administrador se conserva íntegra, la base no deja modificarla ni borrarla")
    void elRegistroEsAppendOnly() {
        transaccion.execute(tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), entranteUsuario), backoffice));
        assertThat(rechazaLaBase("UPDATE grupos.sustitucion_administrador SET motivo='otro'"))
                .isNotEmpty();
        assertThat(rechazaLaBase("DELETE FROM grupos.sustitucion_administrador"))
                .isNotEmpty();
    }

    @Test
    void dosSustitucionesConcurrentesSoloDejanUnAdministrador() throws Exception {
        UUID tercero = dslFixtura
                .fetchOne(
                        "SELECT usuario_id FROM grupos.participante WHERE grupo_id=? AND id NOT IN (?,?) LIMIT 1",
                        grupo,
                        saliente,
                        entrante)
                .get(0, UUID.class);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> intentar(entranteUsuario));
            var b = pool.submit(() -> intentar(tercero));
            int ok = (a.get() ? 1 : 0) + (b.get() ? 1 : 0);
            // Ambas pueden prosperar en serie (la segunda ve un solo administrador: el primero). Lo invariante es uno.
            assertThat(ok).isBetween(1, 2);
        } finally {
            pool.shutdownNow();
        }
        assertThat(contar("SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND es_organizador", grupo))
                .isEqualTo(1);
    }

    private boolean intentar(UUID nuevo) {
        try {
            transaccion.execute(tx -> sustitucion.ejecutar(pedido(UUID.randomUUID(), nuevo), backoffice));
            return true;
        } catch (ErrorDeNegocio e) {
            return false;
        }
    }
}
