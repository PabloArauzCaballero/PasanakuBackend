package bo.aportaya.organizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.organizador.aplicacion.CU90HabilitacionBasica;
import bo.aportaya.organizador.infraestructura.ContratoRepositorio;
import bo.aportaya.organizador.infraestructura.OrganizadorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-90 vía básica · aceptar el contrato Aprendiz y quedar habilitado, sin bandeja. */
class CU90HabilitacionBasicaTest extends BaseDeOrganizador {

    private static CU90HabilitacionBasica basicaCU;

    @BeforeAll
    static void armarBasica() {
        basicaCU = new CU90HabilitacionBasica(
                new Datos(dsl),
                new OrganizadorRepositorio(),
                new ContratoRepositorio(),
                new Outbox("organizador"),
                Reloj.delSistema(),
                GRUPOS_APRENDIZ,
                MONTO_APRENDIZ);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private CU90HabilitacionBasica.Resultado aceptar(ContextoSesion ctx) {
        return transaccion.execute(t -> basicaCU.habilitar(true, CU90HabilitacionBasica.VERSION, ctx));
    }

    private boolean firmar(UUID contratoId, UUID token, ContextoSesion ctx) {
        return Boolean.TRUE.equals(transaccion.execute(t -> basicaCU.firmar(contratoId, token, ctx)));
    }

    @Test
    @DisplayName(
            "Dado un participante con KYC aprobado y sin ficha de organizador · Cuando acepta el contrato Aprendiz y lo firma · Entonces queda organizador Aprendiz habilitado con el contrato firmado")
    void aceptaFirmaYQuedaHabilitado() {
        UUID usuario = fixtura.usuario();
        var ctx = contextoDe(usuario);

        var r = aceptar(ctx);

        assertThat(r.esNueva()).isTrue();
        assertThat(r.nivel()).isEqualTo("APRENDIZ");
        assertThat(r.contenidoHash()).hasSize(64);
        assertThat(contar(
                        """
                        SELECT count(*) FROM organizador.organizador WHERE id=? AND usuario_id=? AND estado='HABILITADO' AND limite_grupos_simultaneos=? AND limite_monto_administrado=?
                        """,
                        r.organizadorId(),
                        usuario,
                        GRUPOS_APRENDIZ,
                        MONTO_APRENDIZ))
                .isEqualTo(1);
        // Sin firma no hay habilitación completa (R-ORG-02).
        assertThat(transaccion.execute(t -> basicaCU.estado(ctx)).habilitado()).isFalse();

        UUID token = fixtura.tokenDeFirma(usuario);
        assertThat(firmar(r.contratoId(), token, ctx)).isTrue();

        assertThat(contar(
                        """
                        SELECT count(*) FROM organizador.contrato_organizador WHERE id=? AND firmado_en IS NOT NULL AND token_firma_id=? AND version=?
                        """,
                        r.contratoId(),
                        token,
                        CU90HabilitacionBasica.VERSION))
                .isEqualTo(1);
        Boolean puede = transaccion.execute(t -> contratoCU.puedeCrearGrupos(r.organizadorId(), ctx));
        assertThat(puede).isTrue();
        assertThat(transaccion.execute(t -> basicaCU.estado(ctx)).habilitado()).isTrue();
    }

    @Test
    @DisplayName(
            "Dado un contrato Aprendiz ya firmado · Cuando se intenta firmar otra vez · Entonces se conserva la primera firma")
    void firmaUnaSolaVez() {
        UUID usuario = fixtura.usuario();
        var ctx = contextoDe(usuario);
        var r = aceptar(ctx);
        UUID primero = fixtura.tokenDeFirma(usuario);
        firmar(r.contratoId(), primero, ctx);

        boolean otraVez = firmar(r.contratoId(), fixtura.tokenDeFirma(usuario), ctx);

        assertThat(otraVez).isFalse();
        assertThat(contar(
                        "SELECT count(*) FROM organizador.contrato_organizador WHERE id=? AND token_firma_id=?",
                        r.contratoId(),
                        primero))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el contrato Aprendiz de otra persona · Cuando alguien intenta firmarlo · Entonces se rechaza porque el contrato lo firma su titular")
    void firmaAjena() {
        UUID duenio = fixtura.usuario();
        var r = aceptar(contextoDe(duenio));
        UUID otro = fixtura.usuario();

        assertThatThrownBy(() -> firmar(r.contratoId(), fixtura.tokenDeFirma(otro), contextoDe(otro)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no un tercero");
    }

    @Test
    @DisplayName(
            "Dado un participante que ya aceptó el contrato Aprendiz · Cuando lo acepta de nuevo · Entonces recibe la misma ficha y el mismo contrato")
    void idempotente() {
        UUID usuario = fixtura.usuario();
        var ctx = contextoDe(usuario);

        var primero = aceptar(ctx);
        var segundo = aceptar(ctx);

        assertThat(segundo.organizadorId()).isEqualTo(primero.organizadorId());
        assertThat(segundo.contratoId()).isEqualTo(primero.contratoId());
        assertThat(segundo.esNueva()).isFalse();
        assertThat(contar(
                        "SELECT count(*) FROM organizador.contrato_organizador WHERE organizador_id=?",
                        primero.organizadorId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un participante que todavía no aceptó el contrato Aprendiz · Cuando consulta su habilitación básica · Entonces no figura habilitado")
    void sinAceptarNoHabilitado() {
        var ctx = contextoDe(fixtura.usuario());

        var estado = transaccion.execute(t -> basicaCU.estado(ctx));

        assertThat(estado.habilitado()).isFalse();
        assertThat(estado.organizadorId()).isNull();
    }

    @Test
    @DisplayName(
            "Dado un participante que no marcó la aceptación del contrato · Cuando pide la habilitación básica · Entonces se rechaza y no se escribe nada")
    void sinAceptacion() {
        UUID usuario = fixtura.usuario();
        var ctx = contextoDe(usuario);

        assertThatThrownBy(
                        () -> transaccion.execute(t -> basicaCU.habilitar(false, CU90HabilitacionBasica.VERSION, ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("aceptar el contrato");
        assertThat(contar("SELECT count(*) FROM organizador.organizador WHERE usuario_id=?", usuario))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un participante que leyó una versión anterior del contrato · Cuando la acepta · Entonces se rechaza para que lea la vigente")
    void versionVieja() {
        var ctx = contextoDe(fixtura.usuario());

        assertThatThrownBy(() -> transaccion.execute(t -> basicaCU.habilitar(true, "v0", ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("cambió");
    }

    @Test
    @DisplayName(
            "Dado un organizador suspendido · Cuando acepta el contrato Aprendiz · Entonces se rechaza y sigue suspendido")
    void suspendidoNoSeReactiva() {
        UUID usuario = fixtura.usuario();
        UUID organizadorId = fixtura.organizadorHabilitado(usuario);
        dslFixtura.execute("UPDATE organizador.organizador SET estado='SUSPENDIDO' WHERE id=?", organizadorId);
        var ctx = contextoDe(usuario);

        assertThatThrownBy(() -> aceptar(ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no está activa");
    }
}
