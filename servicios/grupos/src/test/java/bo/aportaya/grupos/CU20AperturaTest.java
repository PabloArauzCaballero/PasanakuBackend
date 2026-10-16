package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.AbrirInscripcionDelGrupo;
import bo.aportaya.grupos.aplicacion.AbrirInscripcionDelGrupo.PrecioCongelado;
import bo.aportaya.grupos.aplicacion.AbrirInscripcionDelGrupo.Resultado;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-20 · el grupo recien creado (BORRADOR) se abre a inscripciones cuando {@code tarifas} congela su precio.
 *
 * <p>El productor ({@code tarifas.tarifa_congelada}) es un doble de CONTRATO: el evento se arma con los campos exactos que
 * emite {@code CongelarTarifaDelGrupo} (grupoId, tarifarioId). La cadena real, con Kafka y los dos servicios, la ejercita
 * el recorrido de integracion.
 */
class CU20AperturaTest extends BaseDeCU20 {

    private static AbrirInscripcionDelGrupo apertura;

    @BeforeAll
    static void armarApertura() {
        apertura = new AbrirInscripcionDelGrupo(new Datos(dsl), Reloj.delSistema());
    }

    private PrecioCongelado precio(UUID grupo) {
        return new PrecioCongelado(UUID.randomUUID(), UUID.randomUUID(), grupo, UUID.randomUUID());
    }

    private Resultado procesar(PrecioCongelado evento) {
        return transaccion.execute(e -> apertura.procesar(evento));
    }

    private int historiales(UUID grupo) {
        return dsl.fetchOne(
                        "SELECT count(*)::int FROM grupos.historial_estado_grupo WHERE grupo_id = ? AND estado_nuevo = 'ABIERTO_A_INSCRIPCION'",
                        grupo)
                .get(0, Integer.class);
    }

    @Test
    @DisplayName(
            "Dado un grupo recién creado en BORRADOR · Cuando tarifas congela su precio y responde tarifa.congelada · Entonces el grupo pasa a ABIERTO_A_INSCRIPCION y queda el historial de su estado")
    void criterioApertura() {
        var grupo = crear(UUID.randomUUID()).grupoId();
        assertThat(estadoDelGrupo(grupo)).isEqualTo("BORRADOR");

        assertThat(procesar(precio(grupo))).isEqualTo(Resultado.ABIERTO);

        assertThat(estadoDelGrupo(grupo)).isEqualTo("ABIERTO_A_INSCRIPCION");
        var historial = dsl.fetchOne(
                "SELECT estado_anterior, ejecutado_por, motivo FROM grupos.historial_estado_grupo WHERE grupo_id = ?",
                grupo);
        assertThat(historial.get("estado_anterior", String.class)).isEqualTo("BORRADOR");
        assertThat(historial.get("ejecutado_por", UUID.class)).isEqualTo(creador);
        assertThat(historial.get("motivo", String.class)).contains("tarifas congelo el precio");
    }

    @Test
    @DisplayName(
            "Dado un grupo que ya salió de BORRADOR · Cuando la respuesta de tarifas llega otra vez o fuera de orden · Entonces el estado no cambia y no se escribe otro historial")
    void criterioIdempotente() {
        var grupo = crear(UUID.randomUUID()).grupoId();
        var primero = precio(grupo);
        procesar(primero);

        // Evento duplicado (el broker entrega al menos una vez): no hace nada.
        assertThat(procesar(primero)).isEqualTo(Resultado.DUPLICADO);
        // Otro evento para el mismo grupo ya abierto: no hace nada.
        assertThat(procesar(precio(grupo))).isEqualTo(Resultado.YA_ABIERTO);

        // Fuera de orden: el grupo ya avanzo (se completo, se cancelo) y la respuesta de tarifas llega tarde.
        dsl.execute("UPDATE grupos.grupo SET estado = 'CONFORMADO' WHERE id = ?", grupo);
        assertThat(procesar(precio(grupo))).isEqualTo(Resultado.YA_ABIERTO);
        assertThat(estadoDelGrupo(grupo)).isEqualTo("CONFORMADO");

        dsl.execute("UPDATE grupos.grupo SET estado = 'CANCELADO' WHERE id = ?", grupo);
        assertThat(procesar(precio(grupo))).isEqualTo(Resultado.YA_ABIERTO);
        assertThat(estadoDelGrupo(grupo)).isEqualTo("CANCELADO");

        assertThat(historiales(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza un grupo que no existe y no marca el evento como consumido: el reintento lo aplica")
    void rechazaGrupoInexistente() {
        var evento = precio(UUID.randomUUID());

        assertThatThrownBy(() -> procesar(evento))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no existe");

        assertThat(dsl.fetchOne(
                                "SELECT count(*)::int FROM grupos.evento_consumido WHERE id_evento = ?",
                                evento.eventoId())
                        .get(0, Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("concurrencia: dos respuestas distintas de tarifas para el mismo grupo abren una sola vez")
    void concurrencia() throws Exception {
        var grupo = crear(UUID.randomUUID()).grupoId();
        var inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var a = ejecutor.submit(() -> {
                inicio.await();
                return procesar(precio(grupo));
            });
            var b = ejecutor.submit(() -> {
                inicio.await();
                return procesar(precio(grupo));
            });
            inicio.countDown();
            var resultados = java.util.List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(Resultado.ABIERTO, Resultado.YA_ABIERTO);
        }
        assertThat(estadoDelGrupo(grupo)).isEqualTo("ABIERTO_A_INSCRIPCION");
        assertThat(historiales(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rls: la apertura funciona con el rol real del servicio (svc_grupos), no solo con el dueno de la base")
    void rolRealDelServicio() {
        var grupo = crear(UUID.randomUUID()).grupoId();
        var evento = precio(grupo);

        var resultado = transaccion.execute(e -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return apertura.procesar(evento);
        });

        assertThat(resultado).isEqualTo(Resultado.ABIERTO);
        assertThat(estadoDelGrupo(grupo)).isEqualTo("ABIERTO_A_INSCRIPCION");
    }
}
