package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-69 · El enlace del grupo (2026-10-19): un enlace para cualquiera; cada pedido lo aprueba el organizador. */
class CU69EnlaceDeGrupoTest extends BaseDeCU68 {
    private CU69EnlaceDeGrupo enlaces;
    private UUID grupo;
    private ContextoSesion organizador;

    @BeforeEach
    void preparar() {
        enlaces = new CU69EnlaceDeGrupo(
                new Datos(dsl),
                new AdmisionRepositorio(),
                new EmparejamientoRepositorio(),
                postularCU,
                Reloj.delSistema(),
                7);
        grupo = grupoConCupoLibre();
        criterioVigente();
        UUID usuarioOrganizador = dslFixtura
                .fetchOne(
                        """
                        UPDATE grupos.participante SET es_organizador = true
                         WHERE id = (SELECT id FROM grupos.participante WHERE grupo_id = ? AND estado = 'ACTIVO'
                                      ORDER BY id LIMIT 1)
                        RETURNING usuario_id
                        """,
                        grupo)
                .get(0, UUID.class);
        organizador = ContextoSesion.de(
                usuarioOrganizador, "ORGANIZADOR", new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("Dado el organizador de un grupo con cupos libres · Cuando genera el enlace del grupo · "
            + "Entonces recibe un código que vence a los 7 días y la base guarda solo su hash")
    void generaElEnlace() {
        var g = transaccion.execute(s -> enlaces.generar(grupo, organizador));

        assertThat(g.codigo()).matches("^[0-9a-f-]{36}\\.[0-9a-f]{64}$").startsWith(g.enlaceId() + ".");
        assertThat(g.expiraEn())
                .isBetween(
                        OffsetDateTime.now().plusDays(7).minusMinutes(5),
                        OffsetDateTime.now().plusDays(7).plusMinutes(5));
        String guardado = dslFixtura
                .fetchOne("SELECT token_hash FROM grupos.enlace_grupo WHERE id = ?", g.enlaceId())
                .get(0, String.class);
        String secreto = g.codigo().substring(g.codigo().indexOf('.') + 1);
        assertThat(guardado).hasSize(64).isNotEqualTo(secreto);
        assertThat(contar("SELECT count(*)::int FROM grupos.enlace_grupo WHERE token_hash = ?", secreto))
                .isZero();
    }

    @Test
    @DisplayName("Dado un grupo con un enlace activo · Cuando el organizador genera otro · "
            + "Entonces el anterior queda ANULADO y deja de servir")
    void generarOtroAnulaElAnterior() {
        var primero = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        var segundo = transaccion.execute(s -> enlaces.generar(grupo, organizador));

        assertThat(estadoDe(primero.enlaceId())).isEqualTo("ANULADO");
        assertThat(estadoDe(segundo.enlaceId())).isEqualTo("ACTIVO");
        assertThatThrownBy(() -> transaccion.execute(s -> enlaces.consultar(primero.codigo(), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no sirve");
        assertThat(transaccion
                        .execute(s -> enlaces.consultar(segundo.codigo(), contexto()))
                        .grupoId())
                .isEqualTo(grupo);
    }

    @Test
    @DisplayName("Dado un enlace activo del grupo · Cuando una persona verificada pide unirse · "
            + "Entonces queda una solicitud PENDIENTE con su nombre visible para el organizador")
    void pedirUnirseDejaUnaSolicitudPendienteConNombre() {
        var g = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        var detalle = transaccion.execute(s -> enlaces.consultar(g.codigo(), contexto()));
        assertThat(detalle.cuposLibres()).isEqualTo(1);

        var s1 = transaccion.execute(s -> enlaces.solicitar(g.codigo(), entrada(true), "Rosa Q.", contexto()));

        var fila = dslFixtura.fetchOne(
                "SELECT estado, nombre_visible, enlace_grupo_id FROM grupos.solicitud_ingreso WHERE id = ?",
                s1.solicitudId());
        assertThat(fila.get("estado", String.class)).isEqualTo("PENDIENTE");
        assertThat(fila.get("nombre_visible", String.class)).isEqualTo("Rosa Q.");
        assertThat(fila.get("enlace_grupo_id", UUID.class)).isEqualTo(g.enlaceId());
        // Pedir no da entrada: nadie se vuelve participante sin la decision del organizador.
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.participante WHERE grupo_id = ? AND usuario_id = ?",
                        grupo,
                        contexto().usuarioId()))
                .isZero();
    }

    @Test
    @DisplayName("Dada una persona con una solicitud PENDIENTE por el enlace · Cuando vuelve a pedir unirse · "
            + "Entonces recibe la misma solicitud y no se crea otra")
    void repetirElPedidoNoDuplica() {
        var g = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        var uno = transaccion.execute(s -> enlaces.solicitar(g.codigo(), entrada(true), "Rosa Q.", contexto()));
        var dos = transaccion.execute(s -> enlaces.solicitar(g.codigo(), entrada(true), "Rosa Q.", contexto()));

        assertThat(dos.solicitudId()).isEqualTo(uno.solicitudId());
        assertThat(contar("SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE grupo_id = ?", grupo))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un enlace vencido o anulado · Cuando alguien pide unirse · "
            + "Entonces se rechaza con ENLACE_INVALIDO")
    void enlaceVencidoOAnuladoNoSirve() {
        var vencido = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        dslFixtura.execute(
                "UPDATE grupos.enlace_grupo SET fecha_expiracion = now() - interval '1 second' WHERE id = ?",
                vencido.enlaceId());
        assertThatThrownBy(() ->
                        transaccion.execute(s -> enlaces.solicitar(vencido.codigo(), entrada(true), null, contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-11"));

        var anulado = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        transaccion.executeWithoutResult(s -> enlaces.anular(grupo, organizador));
        assertThatThrownBy(() ->
                        transaccion.execute(s -> enlaces.solicitar(anulado.codigo(), entrada(true), null, contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-11"));
        assertThat(contar("SELECT count(*)::int FROM grupos.solicitud_ingreso WHERE grupo_id = ?", grupo))
                .isZero();
    }

    @Test
    @DisplayName("Dado un participante que no administra el grupo · Cuando intenta generar o anular su enlace · "
            + "Entonces se rechaza con EMISOR_NO_HABILITADO")
    void soloElAdministradorManejaElEnlace() {
        assertThatThrownBy(() -> transaccion.execute(s -> enlaces.generar(grupo, contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-07"));
        assertThatThrownBy(() -> transaccion.executeWithoutResult(s -> enlaces.anular(grupo, contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-07"));
        assertThat(contar("SELECT count(*)::int FROM grupos.enlace_grupo WHERE grupo_id = ?", grupo))
                .isZero();
    }

    @Test
    void rechazaUnCodigoAlteradoAunqueElIdExista() {
        var g = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        String alterado = g.enlaceId() + "." + "0".repeat(64);
        assertThatThrownBy(() -> transaccion.execute(s -> enlaces.consultar(alterado, contexto())))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(s -> enlaces.consultar("no-es-un-codigo", contexto())))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void rechazaGenerarSiElGrupoYaNoTieneCupos() {
        UUID lleno = grupoSinCuposLibres();
        UUID admin = dslFixtura
                .fetchOne(
                        "UPDATE grupos.participante SET es_organizador = true WHERE id = (SELECT id FROM grupos.participante WHERE grupo_id = ? ORDER BY id LIMIT 1) RETURNING usuario_id",
                        lleno)
                .get(0, UUID.class);
        var ctx = ContextoSesion.de(
                admin, "ORGANIZADOR", new Traza(UUID.randomUUID().toString()));
        assertThatThrownBy(() -> transaccion.execute(s -> enlaces.generar(lleno, ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-01"));
    }

    @Test
    void rechazaAQuienYaParticipaDelGrupo() {
        var g = transaccion.execute(s -> enlaces.generar(grupo, organizador));
        assertThatThrownBy(
                        () -> transaccion.execute(s -> enlaces.solicitar(g.codigo(), entrada(true), null, organizador)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-03"));
    }

    private String estadoDe(UUID enlaceId) {
        return dslFixtura
                .fetchOne("SELECT estado FROM grupos.enlace_grupo WHERE id = ?", enlaceId)
                .get(0, String.class);
    }

    private CU68Postular.EntradaPostulacion entrada(boolean kyc) {
        return new CU68Postular.EntradaPostulacion(
                grupo, (short) 1, null, false, BigDecimal.ZERO, kyc, 50, 0, uno(), uno(), uno(), uno());
    }
}
