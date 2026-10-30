package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta;
import bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.Impedimento;
import bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.NoElegible;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.grupos.infraestructura.SubastaDeCupoRepositorio;
import bo.aportaya.grupos.infraestructura.TraspasoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-64 por subasta · lo que {@code grupos} contesta y hace para que el cupo pase al que mas ofrece. */
class CU64SubastaTest extends BaseDeCU64 {

    private CU64TraspasoPorSubasta conMaximo(int maximoDeGrupos) {
        return new CU64TraspasoPorSubasta(
                new Datos(dsl),
                new TraspasoRepositorio(),
                new SubastaDeCupoRepositorio(),
                new EmparejamientoRepositorio(maximoDeGrupos),
                new Outbox("grupos"),
                Reloj.delSistema());
    }

    private final CU64TraspasoPorSubasta subasta = conMaximo(Integer.MAX_VALUE);

    private ContextoSesion como(UUID usuario) {
        return ContextoSesion.de(usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private ContextoSesion servicio() {
        return ContextoSesion.de(UUID.randomUUID(), "SERVICIO_ENTREGAS", new Traza(UUID.randomUUID().toString()));
    }

    private CU64TraspasoPorSubasta.Traspaso adjudicar(Escenario e, UUID subastaId, UUID ganador) {
        return transaccion.execute(
                t -> subasta.traspasar(e.grupo(), e.cupo(), subastaId, ganador, e.saliente(), servicio()));
    }

    private UUID usuarioDe(UUID participante) {
        return (UUID) dsl.fetchOne("SELECT usuario_id FROM grupos.participante WHERE id = ?", participante)
                .get(0);
    }

    private Optional<NoElegible> elegibilidadEnTx(
            CU64TraspasoPorSubasta caso, UUID grupo, boolean restriccion, boolean kyc, boolean rep, ContextoSesion ctx) {
        return transaccion.execute(t -> caso.elegibilidad(grupo, restriccion, kyc, rep, ctx));
    }

    private String codigoDe(Throwable t) {
        return ((ErrorDeNegocio) t).codigo().toString();
    }

    @Test
    @DisplayName("Dado el titular al día · Cuando pregunta si puede subastar su cupo · Entonces es subastable y ve el aporte del grupo")
    void elTitularPuedeSubastar() {
        Escenario e = escenarioConTurno();
        var s = transaccion.execute(t -> subasta.subastabilidad(
                e.grupo(), e.cupo(), Optional.of(e.saliente()), true, como(usuarioDe(e.saliente()))));

        assertThat(s.impedimento()).isEmpty();
        assertThat(s.participanteId()).isEqualTo(e.saliente());
        assertThat(s.montoAporte()).isPositive();
        assertThat(s.moneda()).hasSize(3);
    }

    @Test
    @DisplayName("Dado otro integrante · Cuando pregunta por un cupo que no es suyo · Entonces AP-CU64-06 y no se dice de quién es")
    void otroNoPuedeSubastarloAjeno() {
        Escenario e = escenarioConTurno();
        assertThatThrownBy(() -> transaccion.execute(t -> subasta.subastabilidad(
                        e.grupo(), e.cupo(), Optional.of(e.entrante()), true, como(usuarioDe(e.entrante())))))
                .satisfies(x -> assertThat(codigoDe(x)).isEqualTo("AP-CU64-06"));
    }

    @Test
    @DisplayName("Dado un titular con deuda o un turno ya cobrado · Cuando pregunta · Entonces no es subastable y dice por qué")
    void conDeudaOConTurnoCobradoNoSeSubasta() {
        Escenario e = escenarioConTurno();
        var conDeuda = transaccion.execute(t -> subasta.subastabilidad(
                e.grupo(), e.cupo(), Optional.of(e.saliente()), false, como(usuarioDe(e.saliente()))));
        assertThat(conDeuda.impedimento()).contains(Impedimento.SALIENTE_CON_DEUDA);

        dslFixtura.execute("UPDATE grupos.turno SET estado = 'COBRADO' WHERE cupo_id = ?", e.cupo());
        var cobrado = transaccion.execute(t -> subasta.subastabilidad(
                e.grupo(), e.cupo(), Optional.of(e.saliente()), true, como(usuarioDe(e.saliente()))));
        assertThat(cobrado.impedimento()).contains(Impedimento.CUPO_NO_TRASPASABLE);
    }

    @Test
    @DisplayName("Dado un ganador de afuera del grupo · Cuando entregas pide el traspaso · Entonces entra ACTIVO, el cupo es suyo, el saliente queda RETIRADO y el turno no se mueve")
    void elGanadorDeAfueraEntraConElCupo() {
        Escenario e = escenarioConTurno();
        UUID ganador = fixtura.usuario();
        short ordenAntes = ordenDelCupo(e.cupo());
        UUID subastaId = UUID.randomUUID();

        var t = adjudicar(e, subastaId, ganador);

        assertThat(titularDelCupo(e.cupo())).isEqualTo(t.participanteEntranteId());
        assertThat(usuarioDe(t.participanteEntranteId())).isEqualTo(ganador);
        assertThat(estadoDelParticipante(t.participanteEntranteId())).isEqualTo("ACTIVO");
        assertThat(estadoDelParticipante(e.saliente())).isEqualTo("RETIRADO");
        assertThat(ordenDelCupo(e.cupo())).isEqualTo(ordenAntes);
        assertThat(deudaTransferida(e.cupo())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dsl.fetchOne(
                                "SELECT payload ->> 'subastaId' FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = 'grupos.cupo_traspasado'",
                                t.traspasoId())
                        .get(0))
                .isEqualTo(subastaId.toString());
    }

    @Test
    @DisplayName("Dado un traspaso por subasta ya aplicado · Cuando entregas lo repite · Entonces devuelve el mismo traspaso y no hay un segundo")
    void repetirNoTraspasaDosVeces() {
        Escenario e = escenarioConTurno();
        UUID ganador = fixtura.usuario();
        UUID subastaId = UUID.randomUUID();

        var primero = adjudicar(e, subastaId, ganador);
        var segundo = adjudicar(e, subastaId, ganador);

        assertThat(segundo).isEqualTo(primero);
        assertThat(dsl.fetchCount(
                        org.jooq.impl.DSL.table("grupos.traspaso_cupo"),
                        org.jooq.impl.DSL.field("cupo_id").eq(e.cupo())))
                .isEqualTo(1);
        assertThat(dsl.fetchCount(
                        org.jooq.impl.DSL.table("grupos.participante"),
                        org.jooq.impl.DSL.field("grupo_id")
                                .eq(e.grupo())
                                .and(org.jooq.impl.DSL.field("usuario_id").eq(ganador))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un cupo que ya no es del saliente · Cuando entregas pide el traspaso · Entonces AP-CU64-01 y nada cambia")
    void siElCupoYaNoEsDelSalienteSeRechaza() {
        Escenario e = escenarioConTurno();
        UUID ganador = fixtura.usuario();
        var otroSaliente = new Escenario(e.grupo(), e.cupo(), e.entrante(), e.entrante());

        assertThatThrownBy(() -> adjudicar(otroSaliente, UUID.randomUUID(), ganador))
                .satisfies(x -> assertThat(codigoDe(x)).isEqualTo("AP-CU64-01"));
        assertThat(titularDelCupo(e.cupo())).isEqualTo(e.saliente());
    }

    @Test
    @DisplayName("Dados dos cierres simultáneos del mismo cupo con ganadores distintos · Entonces uno solo se lleva el cupo y el otro recibe AP-CU64-01")
    void dosCierresALaVezUnSoloGanador() throws Exception {
        Escenario e = escenarioConTurno();
        List<UUID> ganadores = List.of(fixtura.usuario(), fixtura.usuario());
        var largada = new CountDownLatch(1);
        var hilos = Executors.newFixedThreadPool(2);
        List<Future<String>> resultados = new ArrayList<>();
        for (UUID g : ganadores) {
            resultados.add(hilos.submit(() -> {
                largada.await();
                try {
                    adjudicar(e, UUID.randomUUID(), g);
                    return "OK";
                } catch (ErrorDeNegocio rechazo) {
                    return rechazo.codigo().toString();
                }
            }));
        }
        largada.countDown();
        List<String> salidas = new ArrayList<>();
        for (var r : resultados) {
            salidas.add(r.get());
        }
        hilos.shutdown();

        assertThat(salidas).containsExactlyInAnyOrder("OK", "AP-CU64-01");
        assertThat(dsl.fetchCount(
                        org.jooq.impl.DSL.table("grupos.traspaso_cupo"),
                        org.jooq.impl.DSL.field("cupo_id").eq(e.cupo())))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado quien quiere pujar · Cuando tiene restricción, KYC o reputación insuficiente · Entonces no es elegible y se dice el motivo, en ese orden")
    void elegibilidadEnElOrdenDeLaPostulacion() {
        Escenario e = escenarioConTurno();
        var ctx = como(fixtura.usuario());
        assertThat(subasta.elegibilidad(e.grupo(), true, false, false, ctx)).contains(NoElegible.RESTRICCION_VIGENTE);
        assertThat(subasta.elegibilidad(e.grupo(), false, false, false, ctx)).contains(NoElegible.KYC_INSUFICIENTE);
        assertThat(subasta.elegibilidad(e.grupo(), false, true, false, ctx))
                .contains(NoElegible.REPUTACION_INSUFICIENTE);
        assertThat(elegibilidadEnTx(subasta, e.grupo(), false, true, true, ctx))
                .isEmpty();
    }

    @Test
    @DisplayName("Dado alguien que ya llegó al máximo de grupos · Cuando quiere pujar en otro · Entonces MAXIMO_DE_GRUPOS; si ya está en este grupo, puede")
    void maximoDeGrupos() {
        Escenario e = escenarioConTurno();
        var conTope = conMaximo(1);
        UUID integrante = usuarioDe(e.saliente());

        assertThat(elegibilidadEnTx(conTope, e.grupo(), false, true, true, como(integrante)))
                .isEmpty();

        Escenario otro = escenarioConTurno();
        assertThat(elegibilidadEnTx(conTope, otro.grupo(), false, true, true, como(integrante)))
                .contains(NoElegible.MAXIMO_DE_GRUPOS);
    }
}
