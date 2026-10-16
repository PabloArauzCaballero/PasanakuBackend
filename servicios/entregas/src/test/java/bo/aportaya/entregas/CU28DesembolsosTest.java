package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso.EntradaRespuesta;
import bo.aportaya.entregas.aplicacion.ConsultarDesembolsos;
import bo.aportaya.entregas.infraestructura.BandejaDeDesembolsosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-28 · la bandeja de desembolsos de Tesoreria (backoffice carril A · H8), contra la base real.
 *
 * <p>Las ordenes salen del caso de uso real (emitir + respuesta del proveedor), no de INSERT: lo que
 * la bandeja muestra es lo que el sistema escribe. Correcto (por omision solo excepciones), limite
 * (recorrido con cursor hasta la pagina vacia) e invalido (estado desconocido, cursor a medias).
 */
class CU28DesembolsosTest extends EscenarioDeDesembolso {

    private static ConsultarDesembolsos consultas() {
        return new ConsultarDesembolsos(new Datos(dsl), new BandejaDeDesembolsosRepositorio());
    }

    private static ContextoSesion tesoreria(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Una orden emitida y contestada por el proveedor: exitosa la acredita, fallida definitiva la rechaza. */
    private UUID ordenContestada(boolean exitosa) {
        Caso c = caso(true, true);
        UUID orden = transaccion
                .execute(t -> desembolsoCU.emitir(orden(c, "des-" + UUID.randomUUID()), c.ctx()))
                .ordenId();
        transaccion.execute(t -> desembolsoCU.anotarRespuesta(
                exitosa
                        ? new EntradaRespuesta(orden, arranco(), true, "REF-" + UUID.randomUUID(), null, "OK")
                        : new EntradaRespuesta(
                                orden, arranco(), false, null, "CUENTA_INEXISTENTE", "La cuenta no existe"),
                c.ctx()));
        return orden;
    }

    @Test
    @DisplayName(
            "Dadas una orden RECHAZADA y una ACREDITADA · Cuando Tesorería abre la bandeja de desembolsos sin filtro · Entonces ve solo la rechazada, con su último código de error")
    void soloExcepcionesPorOmision() {
        UUID rechazada = ordenContestada(false);
        UUID acreditada = ordenContestada(true);
        ContextoSesion ctx = tesoreria(fixtura.usuario());

        var pagina =
                transaccion.execute(t -> consultas().listar(List.of(), Optional.empty(), Optional.empty(), 25, ctx));

        assertThat(pagina.items())
                .extracting(ConsultarDesembolsos.Desembolso::ordenId)
                .contains(rechazada)
                .doesNotContain(acreditada);
        assertThat(pagina.items())
                .filteredOn(d -> d.ordenId().equals(rechazada))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.estado()).isEqualTo("RECHAZADA");
                    assertThat(d.intentos()).isEqualTo(1);
                    assertThat(d.ultimoResultado()).contains("FALLIDO");
                    assertThat(d.ultimoCodigoError()).contains("CUENTA_INEXISTENTE");
                    assertThat(d.reintentableEn()).isEmpty();
                    assertThat(d.monto().moneda()).isEqualTo(Moneda.BOB);
                });
        // Con el filtro explicito, la acreditada si aparece.
        var acreditadas = transaccion.execute(
                t -> consultas().listar(List.of("ACREDITADA"), Optional.empty(), Optional.empty(), 25, ctx));
        assertThat(acreditadas.items())
                .extracting(ConsultarDesembolsos.Desembolso::ordenId)
                .containsExactly(acreditada);
    }

    @Test
    @DisplayName(
            "Dadas dos órdenes rechazadas · Cuando Tesorería recorre la bandeja de a una · Entonces ve cada una una sola vez, de la más reciente a la más antigua, y la última página cierra sin cursor")
    void recorridoConCursor() {
        UUID primera = ordenContestada(false);
        UUID segunda = ordenContestada(false);
        ContextoSesion ctx = tesoreria(fixtura.usuario());

        var p1 = transaccion.execute(t -> consultas().listar(List.of(), Optional.empty(), Optional.empty(), 1, ctx));
        var p2 = transaccion.execute(
                t -> consultas().listar(List.of(), p1.proximoAntesDe(), p1.proximoAntesDeId(), 1, ctx));
        var p3 = transaccion.execute(
                t -> consultas().listar(List.of(), p2.proximoAntesDe(), p2.proximoAntesDeId(), 1, ctx));

        // De la mas reciente a la mas antigua.
        assertThat(p1.items())
                .extracting(ConsultarDesembolsos.Desembolso::ordenId)
                .containsExactly(segunda);
        assertThat(p2.items())
                .extracting(ConsultarDesembolsos.Desembolso::ordenId)
                .containsExactly(primera);
        assertThat(p3.items()).isEmpty();
        assertThat(p3.proximoAntesDeId()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un estado que el contrato no conoce con AP-CU28-07")
    void rechazaEstadoDesconocido() {
        ContextoSesion ctx = tesoreria(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(
                        t -> consultas().listar(List.of("PAGADA"), Optional.empty(), Optional.empty(), 25, ctx)))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU28-07"));
    }

    @Test
    @DisplayName("rechaza un cursor a medias en la bandeja de desembolsos con AP-CU28-07")
    void rechazaCursorAMedias() {
        ContextoSesion ctx = tesoreria(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(t ->
                        consultas().listar(List.of(), Optional.of(OffsetDateTime.now()), Optional.empty(), 25, ctx)))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU28-07"));
    }
}
