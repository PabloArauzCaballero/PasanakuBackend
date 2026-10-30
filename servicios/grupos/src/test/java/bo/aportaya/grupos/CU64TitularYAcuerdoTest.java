package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo.EntradaTraspaso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-64 como lo pidio Pablo (2026-10-30): traspasa quien tiene el cupo, el traspaso SIEMPRE se vota
 * y la votacion es sobre ESE cupo y se usa una sola vez.
 */
class CU64TitularYAcuerdoTest extends BaseDeCU64 {

    @Test
    @DisplayName("el titular, con la votacion de SU cupo aprobada, traspasa y el acuerdo queda EJECUTADO")
    void titularConAcuerdoDeSuCupo() {
        Escenario escenario = escenarioConTurno();
        UUID acuerdo = acuerdo(escenario.grupo(), escenario.cupo(), "APROBADO");

        ejecutar(escenario, Optional.of(acuerdo), escenario.grupo(), escenario.saliente());

        assertThat(titularDelCupo(escenario.cupo())).isEqualTo(escenario.entrante());
        assertThat(estadoDelAcuerdo(acuerdo)).isEqualTo("EJECUTADO");
    }

    @Test
    @DisplayName("sin acuerdo no hay traspaso: el grupo siempre lo vota (AP-CU64-05)")
    void sinAcuerdoNoHayTraspaso() {
        Escenario escenario = escenarioConTurno();

        assertThatThrownBy(() -> ejecutar(escenario, Optional.empty(), escenario.grupo(), escenario.saliente()))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo())
                .isEqualTo(CodigoError.de(64, 5));
        assertThat(titularDelCupo(escenario.cupo())).isEqualTo(escenario.saliente());
    }

    @Test
    @DisplayName("la votacion aprobada de OTRO cupo no autoriza este (AP-CU64-05)")
    void acuerdoDeOtroCupo() {
        Escenario escenario = escenarioConTurno();
        UUID otroCupo = cuposDe(escenario.grupo()).get(1);
        UUID acuerdo = acuerdo(escenario.grupo(), otroCupo, "APROBADO");

        assertThatThrownBy(() -> ejecutar(escenario, Optional.of(acuerdo), escenario.grupo(), escenario.saliente()))
                .extracting(e -> ((ErrorDeNegocio) e).codigo())
                .isEqualTo(CodigoError.de(64, 5));
    }

    @Test
    @DisplayName("un acuerdo ya EJECUTADO no vuelve a autorizar (AP-CU64-05)")
    void acuerdoYaUsado() {
        Escenario escenario = escenarioConTurno();
        UUID acuerdo = acuerdo(escenario.grupo(), escenario.cupo(), "EJECUTADO");

        assertThatThrownBy(() -> ejecutar(escenario, Optional.of(acuerdo), escenario.grupo(), escenario.saliente()))
                .extracting(e -> ((ErrorDeNegocio) e).codigo())
                .isEqualTo(CodigoError.de(64, 5));
    }

    @Test
    @DisplayName("quien no tiene el cupo no lo traspasa, aunque haya votacion aprobada (AP-CU64-06)")
    void soloElTitular() {
        Escenario escenario = escenarioConTurno();
        UUID acuerdo = acuerdo(escenario.grupo(), escenario.cupo(), "APROBADO");
        UUID otroParticipante = titularDelCupo(cuposDe(escenario.grupo()).get(1));

        assertThatThrownBy(() -> ejecutar(escenario, Optional.of(acuerdo), escenario.grupo(), otroParticipante))
                .extracting(e -> ((ErrorDeNegocio) e).codigo())
                .isEqualTo(CodigoError.de(64, 6));
        assertThat(titularDelCupo(escenario.cupo())).isEqualTo(escenario.saliente());
        assertThat(estadoDelAcuerdo(acuerdo)).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("el cupo tiene que ser del grupo de la ruta (AP-CU64-01)")
    void cupoDeOtroGrupo() {
        Escenario escenario = escenarioConTurno();
        UUID acuerdo = acuerdo(escenario.grupo(), escenario.cupo(), "APROBADO");

        assertThatThrownBy(() -> ejecutar(escenario, Optional.of(acuerdo), UUID.randomUUID(), escenario.saliente()))
                .extracting(e -> ((ErrorDeNegocio) e).codigo())
                .isEqualTo(CodigoError.de(64, 1));
    }

    @Test
    @DisplayName("concurrencia: dos pedidos simultaneos con el MISMO acuerdo y distinto entrante traspasan UNA vez")
    void mismoAcuerdoEnParalelo() throws Exception {
        Escenario escenario = escenarioConTurno();
        UUID acuerdo = acuerdo(escenario.grupo(), escenario.cupo(), "APROBADO");
        UUID otroEntrante = fixtura.participanteSuelto(escenario.grupo());
        var hilos = Executors.newFixedThreadPool(2);
        try {
            var listo = new CountDownLatch(1);
            Callable<String> a = () -> intento(listo, escenario, escenario.entrante(), acuerdo);
            Callable<String> b = () -> intento(listo, escenario, otroEntrante, acuerdo);
            var fa = hilos.submit(a);
            var fb = hilos.submit(b);
            listo.countDown();

            assertThat(List.of(fa.get(), fb.get())).containsExactlyInAnyOrder("TRASPASADO", "AP-CU64-05");
        } finally {
            hilos.shutdownNow();
        }
        assertThat(dsl.fetchCount(
                        DSL.table("grupos.traspaso_cupo"), DSL.field("cupo_id").eq(escenario.cupo())))
                .isEqualTo(1);
        assertThat(titularDelCupo(escenario.cupo())).isIn(escenario.entrante(), otroEntrante);
        assertThat(estadoDelAcuerdo(acuerdo)).isEqualTo("EJECUTADO");
    }

    private String intento(CountDownLatch listo, Escenario escenario, UUID entrante, UUID acuerdo) throws Exception {
        listo.await();
        try {
            ejecutar(escenario, entrante, Optional.of(acuerdo), escenario.grupo(), escenario.saliente());
            return "TRASPASADO";
        } catch (ErrorDeNegocio e) {
            return e.codigo().toString();
        }
    }

    private UUID ejecutar(Escenario escenario, Optional<UUID> acuerdo, UUID grupoDeLaRuta, UUID quienLlama) {
        return ejecutar(escenario, escenario.entrante(), acuerdo, grupoDeLaRuta, quienLlama);
    }

    private UUID ejecutar(
            Escenario escenario, UUID entrante, Optional<UUID> acuerdo, UUID grupoDeLaRuta, UUID quienLlama) {
        return transaccion.execute(e -> traspasar.ejecutar(
                new EntradaTraspaso(
                        escenario.cupo(),
                        entrante,
                        "Me mudo a otra ciudad",
                        true,
                        "COMPLETO",
                        kycMinimoDe(escenario.grupo()),
                        100,
                        0,
                        false,
                        true,
                        acuerdo,
                        Optional.of(grupoDeLaRuta),
                        Optional.of(quienLlama)),
                contexto()));
    }

    private UUID acuerdo(UUID grupo, UUID cupo, String estado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO grupos.acuerdo
                    (id, grupo_id, tipo, descripcion, propuesto_por, quorum_requerido, votos_a_favor,
                     votos_en_contra, abstenciones, estado, referencia_afectada_id, abierto_en, cierra_en)
                VALUES (?, ?, 'ADMISION_REEMPLAZO', 'traspaso de prueba', ?, 0.600, 3, 0, 0, ?, ?,
                        now() - interval '2 days', now() - interval '1 day')
                """,
                id,
                grupo,
                fixtura.usuario(),
                estado,
                cupo);
        return id;
    }

    private String estadoDelAcuerdo(UUID acuerdo) {
        return dsl.fetchOne("SELECT estado FROM grupos.acuerdo WHERE id = ?", acuerdo)
                .get(0, String.class);
    }
}
