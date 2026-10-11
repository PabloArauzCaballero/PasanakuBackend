package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H9.S1.M4/M6 · Carreras reales (hilos y conexiones distintas) en el mercado.
 *
 * <p>Cada prueba se repite varias rondas: una carrera que depende de quien llego primero tiene que
 * dar un resultado COHERENTE sea quien sea el que gane.
 */
class CU130CarrerasTest extends BaseDeMercado {

    private static final int RONDAS = 6;

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private List<Object> enParalelo(List<Callable<Object>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch listos = new CountDownLatch(tareas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        for (Callable<Object> t : tareas) {
            futuros.add(pool.submit(() -> {
                listos.countDown();
                largada.await();
                try {
                    return t.call();
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        listos.await();
        largada.countDown();
        List<Object> r = new ArrayList<>();
        for (var f : futuros) {
            r.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return r;
    }

    private static boolean esRechazo(Object r, String codigo) {
        return r instanceof ErrorDeNegocio e && codigo.equals(e.codigo().valor());
    }

    @Test
    @DisplayName(
            "Dados dos compradores que compran a la vez la misma oferta · Cuando corren · Entonces UNO gana y el otro recibe AP-CU130-05; hay una sola cesion viva y una sola retencion (sin retenciones huerfanas)")
    void dosCompradores() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            var c = caso();
            var segundo = fixtura.miembro(c.e().grupoId());
            gruposDoble.miembro(c.e().grupoId(), segundo.usuarioId(), segundo.participanteId(), true);
            fondosDoble.saldo(segundo.usuarioId(), bob("10000.00"));
            var oferta = publicar(c);
            fondosDoble.reiniciar();
            fondosDoble.saldo(c.comprador().usuarioId(), bob("10000.00"));
            fondosDoble.saldo(segundo.usuarioId(), bob("10000.00"));
            var total = fondosDoble.totalDelSistema();

            var r = enParalelo(List.of(
                    () -> compraCU.comprar(oferta.ofertaId(), "A-" + c.e().turnoId(), c.ctxComprador()),
                    () -> compraCU.comprar(
                            oferta.ofertaId(), "B-" + c.e().turnoId(), contextoDe(segundo.usuarioId()))));

            assertThat(r.stream().filter(x -> !(x instanceof Exception)).count())
                    .as("ronda %d", ronda)
                    .isEqualTo(1);
            assertThat(r.stream().filter(x -> esRechazo(x, "AP-CU130-05")).count())
                    .as("ronda %d", ronda)
                    .isEqualTo(1);
            assertThat(contar(
                            "SELECT count(*)::int FROM entregas.cesion_derecho WHERE turno_id = ? AND estado <> 'FALLIDA'",
                            c.e().turnoId()))
                    .isEqualTo(1);
            assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
            assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
            assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
            assertThat(fondosDoble.totalDelSistema()).isEqualTo(total);
            assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("VENDIDA");
        }
    }

    @Test
    @DisplayName(
            "Dada la cancelacion del vendedor y la compra a la vez · Cuando corren · Entonces gana una sola y el resultado es coherente: o queda cancelada sin retencion, o queda reservada y la cancelacion se rechaza")
    void cancelacionContraCompra() throws Exception {
        int canceladas = 0;
        int compradas = 0;
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            var c = caso();
            var oferta = publicar(c);
            fondosDoble.reiniciar();
            fondosDoble.saldo(c.comprador().usuarioId(), bob("10000.00"));

            var r = enParalelo(List.of(
                    () -> ofertasCU.cancelar(oferta.ofertaId(), c.ctxVendedor()),
                    () -> compraCU.comprar(oferta.ofertaId(), "X-" + c.e().turnoId(), c.ctxComprador())));

            boolean cancelo = !(r.get(0) instanceof Exception);
            boolean compro = !(r.get(1) instanceof Exception);
            assertThat(cancelo ^ compro)
                    .as("ronda %d: exactamente una gana", ronda)
                    .isTrue();
            if (cancelo) {
                canceladas++;
                assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("CANCELADA");
                assertThat(esRechazo(r.get(1), "AP-CU130-05")).isTrue();
                assertThat(fondosDoble.retencionesNuevas.get()).isZero();
            } else {
                compradas++;
                assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("VENDIDA");
                assertThat(esRechazo(r.get(0), "AP-CU130-05")).isTrue();
            }
            assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
        }
        assertThat(canceladas + compradas).isEqualTo(RONDAS);
    }

    @Test
    @DisplayName(
            "Dado el fondeo del pozo y la compra del derecho a la vez · Cuando corren · Entonces hay UNA entrega y su beneficiario es coherente con la cesion: o el comprador (cesion viva) o el vendedor (cesion fallida y saldo intacto)")
    void fondeoContraVenta() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            var c = caso();
            var oferta = publicar(c);
            fondosDoble.reiniciar();
            fondosDoble.saldo(c.comprador().usuarioId(), bob("10000.00"));
            aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                    c.e().periodoId(),
                    c.e().grupoId(),
                    OffsetDateTime.now(),
                    bob("6000.00"),
                    bob("6000.00"),
                    bob("0.00")));

            var r = enParalelo(List.of(
                    () -> compraCU.comprar(oferta.ofertaId(), "V-" + c.e().turnoId(), c.ctxComprador()),
                    () -> pozoCU.fondear(
                            new CU22EntregarPozoCompleto.Entrada(
                                    c.e().grupoId(),
                                    c.e().periodoId(),
                                    c.e().turnoId(),
                                    c.e().cupoId(),
                                    c.e().participanteId(),
                                    "BILLETERA_MOVIL",
                                    LocalDate.now(),
                                    "F-" + c.e().turnoId()),
                            c.ctxVendedor())));

            assertThat(r.get(1))
                    .as("el fondeo nunca se pierde (ronda %d)", ronda)
                    .isNotInstanceOf(Exception.class);
            assertThat(contar("SELECT count(*)::int FROM entregas.entrega_fondo WHERE turno_id = ?", c.e().turnoId()))
                    .isEqualTo(1);
            UUID beneficiario = dsl.fetchOne(
                            "SELECT beneficiario_participante_id FROM entregas.entrega_fondo WHERE turno_id = ?",
                            c.e().turnoId())
                    .get(0, UUID.class);
            int vivas = contar(
                    "SELECT count(*)::int FROM entregas.cesion_derecho WHERE turno_id = ? AND estado IN ('TITULO_ASIGNADO','LIQUIDADA')",
                    c.e().turnoId());
            if (beneficiario.equals(c.comprador().participanteId())) {
                assertThat(vivas)
                        .as("ronda %d: cobra el comprador => hay titulo", ronda)
                        .isEqualTo(1);
            } else {
                assertThat(beneficiario).isEqualTo(c.e().participanteId());
                assertThat(vivas)
                        .as("ronda %d: cobra el vendedor => no hay titulo", ronda)
                        .isZero();
                assertThat(fondosDoble.disponible(c.comprador().usuarioId())).isEqualTo(bob("10000.00"));
                assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
            }
        }
    }
}
