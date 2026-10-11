package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.AplicarCesionDeDerecho;
import bo.aportaya.grupos.aplicacion.AplicarCesionDeDerecho.Cesion;
import bo.aportaya.grupos.aplicacion.AplicarCesionDeDerecho.Resultado;
import bo.aportaya.grupos.infraestructura.TraspasoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril I (quinta vuelta) · {@code entregas.derecho_cedido}: al venderse un turno el cupo cambia de titular en
 * {@code grupos}. Antes nadie consumia el evento (brecha hallada por el carril G contra el stack real: «el cupo del turno 2
 * sigue de P2, el vendedor»). El productor (entregas) es un doble de CONTRATO: el evento se arma con los campos exactos
 * que {@code TransicionesDeCesion.liquidar} emite. El recorrido con Kafka y entregas reales vive en
 * {@code despliegue/ensayos/integracion}.
 */
class CUCesionDeDerechoTest extends BaseDeCU64 {

    private static AplicarCesionDeDerecho aplicar;

    @BeforeAll
    static void armarConsumo() {
        aplicar = new AplicarCesionDeDerecho(
                new Datos(dsl), new TraspasoRepositorio(), new Outbox("grupos"), Reloj.delSistema());
    }

    /** Un grupo con tres participantes ACTIVOS, cada uno con su cupo y su turno. */
    private record Mundo(UUID grupo, List<UUID> participantes, List<UUID> cupos) {}

    private Mundo mundo() {
        UUID grupo = fixtura.grupoConformado(3);
        List<UUID> participantes = fixtura.participantesConCupo(grupo, 3);
        List<UUID> periodos = fixtura.periodos(grupo, 3, new BigDecimal("1500.00"));
        List<UUID> cupos = cuposDe(grupo);
        fixtura.turnos(grupo, periodos, cupos);
        return new Mundo(grupo, participantes, cupos);
    }

    private Cesion cesion(Mundo m, int cupo, UUID origen, UUID destino) {
        return new Cesion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                m.grupo(),
                m.cupos().get(cupo),
                origen,
                destino);
    }

    private AplicarCesionDeDerecho.Salida procesar(Cesion c) {
        return transaccion.execute(t -> aplicar.procesar(c));
    }

    private int marcas(UUID evento) {
        return ((Number) dsl.fetchOne("SELECT count(*) FROM grupos.evento_consumido WHERE id_evento = ?", evento)
                        .get(0))
                .intValue();
    }

    private int traspasos(UUID cupo) {
        return ((Number) dsl.fetchOne("SELECT count(*) FROM grupos.traspaso_cupo WHERE cupo_id = ?", cupo)
                        .get(0))
                .intValue();
    }

    @Test
    @DisplayName(
            "Dado un turno vendido por P1 a P2 · Cuando grupos consume derecho_cedido · Entonces el cupo es de P2, el"
                    + " turno conserva su orden, queda el traspaso VENTA con deuda 0 y los estados no cambian")
    void elCupoCambiaDeTitular() {
        var m = mundo();
        UUID vendedor = m.participantes().get(0);
        UUID comprador = m.participantes().get(1);
        UUID cupo = m.cupos().get(0);
        short ordenAntes = ordenDelCupo(cupo);
        var c = cesion(m, 0, vendedor, comprador);

        var salida = procesar(c);

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(titularDelCupo(cupo)).isEqualTo(comprador);
        assertThat(ordenDelCupo(cupo)).isEqualTo(ordenAntes);
        var t = dsl.fetchOne(
                """
                SELECT motivo, deuda_transferida, derecho_cobro_transferido, participante_origen_id, participante_destino_id
                  FROM grupos.traspaso_cupo WHERE cupo_id = ?
                """,
                cupo);
        assertThat(t.get(0)).isEqualTo("VENTA");
        assertThat((BigDecimal) t.get(1)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(t.get(2)).isEqualTo(true);
        assertThat(t.get(3)).isEqualTo(vendedor);
        assertThat(t.get(4)).isEqualTo(comprador);
        assertThat(estadoDelParticipante(vendedor)).isEqualTo("ACTIVO");
        assertThat(estadoDelParticipante(comprador)).isEqualTo("ACTIVO");
        // Outbox: el hecho se publica en la misma transaccion (regla 98.3).
        assertThat(((Number) dsl.fetchOne(
                                        """
                                        SELECT count(*) FROM grupos.evento_dominio WHERE tipo = 'grupos.cupo_traspasado' AND agregado_id = ?
                                        """,
                                        salida.traspasoId().orElseThrow())
                                .get(0))
                        .intValue())
                .isEqualTo(1);
        System.out.println("CESION | cupo " + cupo + " | titular " + vendedor + " -> " + titularDelCupo(cupo));
    }

    @Test
    @DisplayName(
            "evento duplicado: el mismo derecho_cedido entregado dos veces cambia el cupo UNA vez y deja UN traspaso")
    void eventoDuplicado() {
        var m = mundo();
        var c = cesion(m, 0, m.participantes().get(0), m.participantes().get(1));

        var primera = procesar(c);
        var segunda = procesar(c);

        assertThat(primera.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(segunda.resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(traspasos(m.cupos().get(0))).isEqualTo(1);
        assertThat(marcas(c.eventoId())).isEqualTo(1);
        assertThat(titularDelCupo(m.cupos().get(0))).isEqualTo(m.participantes().get(1));
    }

    @Test
    @DisplayName("evento duplicado: el MISMO hecho llegado con otro id de evento no escribe un segundo traspaso"
            + " (YA_APLICADA)")
    void mismoHechoConOtroEvento() {
        var m = mundo();
        var original = cesion(m, 0, m.participantes().get(0), m.participantes().get(1));
        var reenviada = new Cesion(
                UUID.randomUUID(),
                original.correlationId(),
                original.cesionId(),
                original.turnoId(),
                original.grupoId(),
                original.cupoId(),
                original.participanteOrigenId(),
                original.participanteDestinoId());

        assertThat(procesar(original).resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(procesar(reenviada).resultado()).isEqualTo(Resultado.YA_APLICADA);

        assertThat(traspasos(m.cupos().get(0))).isEqualTo(1);
        assertThat(titularDelCupo(m.cupos().get(0))).isEqualTo(m.participantes().get(1));
    }

    @Test
    @DisplayName(
            "fuera de orden: la segunda cesion de la cadena (P2 a P3) llegada antes de la primera (P1 a P2) se rechaza"
                    + " sin marcar ni escribir, y se aplica cuando la primera ya esta")
    void fueraDeOrden() {
        var m = mundo();
        UUID p1 = m.participantes().get(0);
        UUID p2 = m.participantes().get(1);
        UUID p3 = m.participantes().get(2);
        UUID cupo = m.cupos().get(0);
        var primera = cesion(m, 0, p1, p2);
        var segunda = cesion(m, 0, p2, p3);

        assertThatThrownBy(() -> procesar(segunda))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU64-01"));
        assertThat(titularDelCupo(cupo)).isEqualTo(p1);
        assertThat(traspasos(cupo)).isZero();
        assertThat(marcas(segunda.eventoId())).isZero();

        assertThat(procesar(primera).resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(procesar(segunda).resultado()).isEqualTo(Resultado.APLICADA);

        assertThat(titularDelCupo(cupo)).isEqualTo(p3);
        assertThat(traspasos(cupo)).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza: un cupo inexistente, un cupo de OTRO grupo y un comprador que no esta ACTIVO en el grupo no"
            + " cambian nada ni marcan el evento")
    void rechazaLoQueNoCorresponde() {
        var m = mundo();
        var otro = mundo();
        UUID vendedor = m.participantes().get(0);
        UUID comprador = m.participantes().get(1);
        UUID invitado = fixtura.participanteSuelto(m.grupo());

        var inexistente = new Cesion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                m.grupo(),
                UUID.randomUUID(),
                vendedor,
                comprador);
        // El cupo existe pero es de otro grupo: la cesion lo nombra con un grupo que no es el suyo.
        var deOtroGrupo = new Cesion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                m.grupo(),
                otro.cupos().get(0),
                vendedor,
                comprador);
        var compradorNoActivo = cesion(m, 0, vendedor, invitado);

        for (var c : List.of(inexistente, deOtroGrupo, compradorNoActivo)) {
            assertThatThrownBy(() -> procesar(c))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .satisfies(e ->
                            assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU64-01"));
            assertThat(marcas(c.eventoId())).isZero();
        }
        assertThat(titularDelCupo(m.cupos().get(0))).isEqualTo(vendedor);
        assertThat(titularDelCupo(otro.cupos().get(0)))
                .isEqualTo(otro.participantes().get(0));
        assertThat(traspasos(m.cupos().get(0))).isZero();
        assertThat(traspasos(otro.cupos().get(0))).isZero();
    }

    @Test
    @DisplayName("concurrencia: dos entregas simultaneas del mismo derecho_cedido (dos replicas) dejan UN traspaso y el"
            + " cupo en manos del comprador")
    void concurrenciaDeLaMismaCesion() throws Exception {
        var m = mundo();
        var c = cesion(m, 0, m.participantes().get(0), m.participantes().get(1));
        var hilos = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var listo = new java.util.concurrent.CountDownLatch(1);
            var a = hilos.submit(() -> {
                listo.await();
                return procesar(c).resultado();
            });
            var b = hilos.submit(() -> {
                listo.await();
                return procesar(c).resultado();
            });
            listo.countDown();
            var resultados = List.of(a.get(), b.get());

            assertThat(resultados).containsExactlyInAnyOrder(Resultado.APLICADA, Resultado.DUPLICADO);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(traspasos(m.cupos().get(0))).isEqualTo(1);
        assertThat(titularDelCupo(m.cupos().get(0))).isEqualTo(m.participantes().get(1));
    }
}
