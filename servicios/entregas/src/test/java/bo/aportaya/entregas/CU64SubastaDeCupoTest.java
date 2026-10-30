package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.SubastaDelCupo;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-64 por subasta · abrir, pujar, listar y cancelar, contra Postgres real con grupos y la billetera como dobles. */
class CU64SubastaDeCupoTest extends BaseDeSubasta {

    // ------------------------------------------------------------------ abrir

    @Test
    @DisplayName(
            "Dado el titular al día · Cuando pone su cupo en subasta con base Bs 1.000 · Entonces queda ABIERTA 24 h y sale el aviso para todos")
    void abrirEmiteElAviso() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");

        assertThat(estadoDeSubasta(id)).isEqualTo("ABIERTA");
        assertThat(eventos("entregas.subasta_abierta", id)).isEqualTo(1);
        var vista = consultasCU.ver(id, contextoDe(c.saliente())).orElseThrow();
        assertThat(vista.minimo()).isEqualTo(bob("1000.00"));
        assertThat(vista.soyElSaliente()).isTrue();
        assertThat(vista.nombreDelGrupo()).isEqualTo("Pasanaku de prueba");
    }

    @Test
    @DisplayName(
            "Dada una subasta abierta · Cuando se repite la apertura con la misma clave · Entonces es la misma; con otra clave, AP-CU136-02")
    void unaSolaSubastaVivaPorCupo() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        assertThat(abrir(c, "1000.00")).isEqualTo(id);

        gruposSubasta.sesionDe(c.saliente());
        assertThatThrownBy(() -> subastaCU.abrir(
                        new SubastaDelCupo.Apertura(c.e().grupoId(), c.e().cupoId(), bob("900.00"), 48, "otra"),
                        contextoDe(c.saliente())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-02"));
    }

    @Test
    @DisplayName(
            "Inválidos al abrir: quien no es titular (01), titular con deuda (01 con motivo), plazo de 12 h (03), grupos caído (08)")
    void aperturasInvalidas() {
        var c = cupoSubastable();
        UUID otro = fixtura.usuario();
        gruposSubasta.sesionDe(otro);
        assertThatThrownBy(() -> subastaCU.abrir(
                        new SubastaDelCupo.Apertura(c.e().grupoId(), c.e().cupoId(), bob("1000.00"), 24, "k1"),
                        contextoDe(otro)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-01"));

        var conDeuda = cupoSubastable();
        gruposSubasta.cupo(
                conDeuda.e().cupoId(),
                conDeuda.saliente(),
                conDeuda.e().participanteId(),
                bob("500.00"),
                Optional.of("SALIENTE_CON_DEUDA"));
        gruposSubasta.sesionDe(conDeuda.saliente());
        assertThatThrownBy(() -> subastaCU.abrir(
                        new SubastaDelCupo.Apertura(
                                conDeuda.e().grupoId(), conDeuda.e().cupoId(), bob("1000.00"), 24, "k2"),
                        contextoDe(conDeuda.saliente())))
                .satisfies(e -> {
                    assertThat(codigo(e)).isEqualTo("AP-CU136-01");
                    assertThat(((ErrorDeNegocio) e).detalle()).containsEntry("motivo", "SALIENTE_CON_DEUDA");
                });

        gruposSubasta.sesionDe(c.saliente());
        assertThatThrownBy(() -> subastaCU.abrir(
                        new SubastaDelCupo.Apertura(c.e().grupoId(), c.e().cupoId(), bob("1000.00"), 12, "k3"),
                        contextoDe(c.saliente())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-03"));

        gruposSubasta.seCae(true);
        assertThatThrownBy(() -> subastaCU.abrir(
                        new SubastaDelCupo.Apertura(c.e().grupoId(), c.e().cupoId(), bob("1000.00"), 24, "k4"),
                        contextoDe(c.saliente())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-08"));
    }

    // ------------------------------------------------------------------ pujar

    @Test
    @DisplayName(
            "Dadas dos pujas sucesivas · Cuando B ofrece más que A · Entonces B queda VIGENTE retenida y a A se le devuelve todo su saldo al instante")
    void alSuperadoSeLeDevuelve() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        UUID b = postorConSaldo("5000.00");

        var pa = pujar(id, a, "1200.00");
        assertThat(pa.estado()).isEqualTo("VIGENTE");
        assertThat(fondosDoble.disponible(a)).isEqualTo(bob("3800.00"));

        var pb = pujar(id, b, "1500.00");
        assertThat(pb.estado()).isEqualTo("VIGENTE");
        assertThat(estadoDePuja(pa.pujaId())).isEqualTo("LIBERADA");
        assertThat(fondosDoble.disponible(a)).isEqualTo(bob("5000.00"));
        assertThat(fondosDoble.disponible(b)).isEqualTo(bob("3500.00"));
        assertThat(fondosDoble.retenidoVivo()).isEqualTo(bob("1500.00"));
        assertThat(eventos("entregas.puja_superada", id)).isEqualTo(1);

        var vista = consultasCU.ver(id, contextoDe(a)).orElseThrow();
        assertThat(vista.masAlto()).contains(bob("1500.00"));
        assertThat(vista.minimo()).isEqualTo(bob("1500.01"));
        assertThat(vista.cantidadDePujas()).isEqualTo(2);
        assertThat(vista.miPuja().orElseThrow().estado()).isEqualTo("LIBERADA");
    }

    @Test
    @DisplayName(
            "Límites de la puja: igual a la más alta (04, su saldo vuelve), debajo de la base (03), exactamente la base (VIGENTE)")
    void limitesDeLaPuja() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");
        UUID b = postorConSaldo("5000.00");

        assertThatThrownBy(() -> pujar(id, a, "999.99"))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-03"));
        assertThat(pujar(id, a, "1000.00").estado()).isEqualTo("VIGENTE");
        assertThatThrownBy(() -> pujar(id, b, "1000.00"))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-04"));
        assertThat(fondosDoble.disponible(b)).isEqualTo(bob("5000.00"));
    }

    @Test
    @DisplayName(
            "Inválidos al pujar: el saliente (06), quien no es elegible (06 con motivo), sin aceptar el reglamento (06), sin saldo (07 y la puja queda RECHAZADA)")
    void pujasInvalidas() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        fondosDoble.saldo(c.saliente(), bob("9000.00"));
        assertThatThrownBy(() -> pujar(id, c.saliente(), "2000.00"))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-06"));

        UUID sinKyc = postorConSaldo("9000.00");
        gruposSubasta.noElegible(sinKyc, "KYC_INSUFICIENTE");
        assertThatThrownBy(() -> pujar(id, sinKyc, "2000.00")).satisfies(e -> {
            assertThat(codigo(e)).isEqualTo("AP-CU136-06");
            assertThat(((ErrorDeNegocio) e).detalle()).containsEntry("motivo", "KYC_INSUFICIENTE");
        });

        UUID apurado = postorConSaldo("9000.00");
        gruposSubasta.sesionDe(apurado);
        assertThatThrownBy(() -> subastaCU.pujar(id, bob("2000.00"), false, "k", contextoDe(apurado)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-06"));

        UUID pobre = postorConSaldo("100.00");
        assertThatThrownBy(() -> pujar(id, pobre, "2000.00", "puja-pobre"))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-07"));
        assertThat(dsl.fetchOne("SELECT estado FROM entregas.puja_subasta WHERE clave_idempotencia = 'puja-pobre'")
                        .get(0, String.class))
                .isEqualTo("RECHAZADA");
        assertThat(fondosDoble.retenidoVivo()).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName(
            "Dada una puja ya hecha · Cuando se repite con la misma clave · Entonces es la misma puja y la plata se retuvo una sola vez")
    void pujaIdempotente() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID a = postorConSaldo("5000.00");

        var primera = pujar(id, a, "1200.00", "misma");
        var segunda = pujar(id, a, "1200.00", "misma");

        assertThat(segunda).isEqualTo(primera);
        assertThat(fondosDoble.retencionesNuevas.get()).isEqualTo(1);
        assertThat(fondosDoble.disponible(a)).isEqualTo(bob("3800.00"));
    }

    @Test
    @DisplayName(
            "Dados seis postores que pujan a la vez (dos por el mismo monto) · Entonces queda UNA vigente, la más alta, y el resto recupera todo: el total del sistema no cambia")
    void pujasSimultaneas() throws Exception {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        List<String> montos = List.of("1100.00", "1300.00", "1300.00", "1250.00", "1200.00", "1050.00");
        List<UUID> postores = new ArrayList<>();
        for (int i = 0; i < montos.size(); i++) {
            postores.add(postorConSaldo("2000.00"));
        }
        var totalAntes = fondosDoble.totalDelSistema();

        var largada = new CountDownLatch(1);
        var hilos = Executors.newFixedThreadPool(montos.size());
        List<Future<String>> salidas = new ArrayList<>();
        for (int i = 0; i < montos.size(); i++) {
            UUID postor = postores.get(i);
            String monto = montos.get(i);
            salidas.add(hilos.submit(() -> {
                largada.await();
                try {
                    return subastaCU
                            .pujar(id, bob(monto), true, "p-" + postor, contextoDe(postor))
                            .estado();
                } catch (ErrorDeNegocio rechazo) {
                    return codigo(rechazo);
                }
            }));
        }
        largada.countDown();
        for (var s : salidas) {
            s.get();
        }
        hilos.shutdown();
        subastaCU.devolverSuperadas(Optional.of(id), sistema());

        assertThat(dsl.fetchOne(
                                "SELECT count(*)::int FROM entregas.puja_subasta WHERE subasta_cupo_id = ? AND estado = 'VIGENTE'",
                                id)
                        .get(0, Integer.class))
                .isEqualTo(1);
        assertThat(consultasCU.ver(id, sistema()).orElseThrow().masAlto()).contains(bob("1300.00"));
        assertThat(fondosDoble.retenidoVivo()).isEqualTo(bob("1300.00"));
        assertThat(fondosDoble.totalDelSistema()).isEqualTo(totalAntes);
    }

    // ------------------------------------------------------------------ listar

    @Test
    @DisplayName(
            "La lista para todos trae las ABIERTAS en orden de cierre, sin las canceladas ni las vencidas; por cupo trae la última en cualquier estado")
    void listaDeAbiertas() {
        var primero = cupoSubastable();
        UUID antes = abrir(primero, "1000.00");
        AHORA.set(AHORA.get().plusSeconds(60));
        var segundo = cupoSubastable();
        UUID despues = abrir(segundo, "800.00");
        var tercero = cupoSubastable();
        UUID cancelada = abrir(tercero, "700.00");
        subastaCU.cancelar(cancelada, contextoDe(tercero.saliente()));
        UUID mira = fixtura.usuario();

        var ids = consultasCU.abiertas(contextoDe(mira), 50, 0).stream()
                .map(v -> v.subastaId())
                .filter(id -> id.equals(antes) || id.equals(despues) || id.equals(cancelada))
                .toList();
        assertThat(ids).containsExactly(antes, despues);
        var vista = consultasCU.abiertas(contextoDe(mira), 50, 0).stream()
                .filter(v -> v.subastaId().equals(antes))
                .findFirst()
                .orElseThrow();
        assertThat(vista.soyElSaliente()).isFalse();
        assertThat(vista.miPuja()).isEmpty();
        assertThat(consultasCU
                        .delCupo(tercero.e().cupoId(), contextoDe(mira))
                        .orElseThrow()
                        .estado())
                .isEqualTo("CANCELADA");
    }

    // ------------------------------------------------------------------ cancelar

    @Test
    @DisplayName("Cancelar: el saliente sin pujas puede; con pujas, AP-CU136-02; otro usuario, AP-CU136-05")
    void cancelar() {
        var c = cupoSubastable();
        UUID id = abrir(c, "1000.00");
        UUID otro = fixtura.usuario();
        assertThatThrownBy(() -> subastaCU.cancelar(id, contextoDe(otro)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-05"));

        var conPujas = cupoSubastable();
        UUID id2 = abrir(conPujas, "1000.00");
        pujar(id2, postorConSaldo("5000.00"), "1000.00");
        assertThatThrownBy(() -> subastaCU.cancelar(id2, contextoDe(conPujas.saliente())))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU136-02"));

        subastaCU.cancelar(id, contextoDe(c.saliente()));
        assertThat(estadoDeSubasta(id)).isEqualTo("CANCELADA");
    }
}
