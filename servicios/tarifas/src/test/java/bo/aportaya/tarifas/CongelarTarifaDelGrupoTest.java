package bo.aportaya.tarifas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.tarifas.aplicacion.CongelarTarifaDelGrupo;
import bo.aportaya.tarifas.aplicacion.CongelarTarifaDelGrupo.GrupoCreado;
import bo.aportaya.tarifas.aplicacion.CongelarTarifaDelGrupo.Resultado;
import bo.aportaya.tarifas.infraestructura.TarifaCongeladaRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-20 · {@code tarifas} congela el precio del grupo al consumir {@code grupos.grupo_creado} y responde
 * {@code tarifas.tarifa_congelada} (R-TAR-07). El productor ({@code grupos}) es un doble de CONTRATO: el evento se arma con
 * los campos que emite {@code CU20CrearGrupo} (grupoId, tarifarioId). La cadena real, con Kafka, la ejercita el
 * recorrido de integracion.
 */
class CongelarTarifaDelGrupoTest extends BaseDeTarifas {

    private static CongelarTarifaDelGrupo congelar;

    @BeforeAll
    static void armarCongelamiento() {
        congelar = new CongelarTarifaDelGrupo(
                new Datos(dsl), new TarifaCongeladaRepositorio(), new Outbox("tarifas"), Reloj.delSistema());
    }

    private String codigo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Un tarifario VIGENTE con dos conceptos activos. */
    private UUID tarifarioConDosConceptos() {
        UUID tarifario = fixtura.tarifarioBorrador("CONG-" + codigo());
        UUID uno = fixtura.hechoGenerador("H1-" + codigo());
        UUID dos = fixtura.hechoGenerador("H2-" + codigo());
        fixtura.conceptoPorcentual(
                tarifario, uno, null, null, "C1-" + codigo(), "1.5000", "1.00", "50.00", false, false);
        fixtura.conceptoPorcentual(
                tarifario, dos, null, null, "C2-" + codigo(), "2.0000", "2.00", "80.00", false, false);
        fixtura.activar(tarifario);
        return tarifario;
    }

    private GrupoCreado evento(UUID grupo, UUID tarifario) {
        return new GrupoCreado(UUID.randomUUID(), UUID.randomUUID(), grupo, tarifario);
    }

    private Resultado procesar(GrupoCreado evento) {
        return transaccion.execute(e -> congelar.procesar(evento));
    }

    private int tarifasDe(UUID grupo) {
        return contar("SELECT count(*)::int FROM tarifas.tarifa_congelada_grupo WHERE grupo_id = ?", grupo);
    }

    private int respuestasDe(UUID grupo) {
        return contar(
                "SELECT count(*)::int FROM tarifas.evento_dominio WHERE agregado_id = ? AND tipo = 'tarifas.tarifa_congelada'",
                grupo);
    }

    @Test
    @DisplayName(
            "congela el precio con el snapshot de los conceptos y un hash verificable, y responde tarifa_congelada")
    void congelaYResponde() {
        UUID grupo = escenario.grupo();
        UUID tarifario = tarifarioConDosConceptos();

        assertThat(procesar(evento(grupo, tarifario))).isEqualTo(Resultado.CONGELADA);

        var fila = dsl.fetchOne(
                """
                SELECT tarifario_id, hash_snapshot, jsonb_array_length(snapshot_conceptos -> 'conceptos') AS conceptos,
                       snapshot_conceptos ->> 'tarifarioId' AS en_snapshot,
                       encode(digest(snapshot_conceptos::text, 'sha256'), 'hex') AS recalculado
                  FROM tarifas.tarifa_congelada_grupo WHERE grupo_id = ?
                """,
                grupo);
        assertThat(fila.get("tarifario_id", UUID.class)).isEqualTo(tarifario);
        assertThat(fila.get("conceptos", Integer.class)).isEqualTo(2);
        assertThat(fila.get("en_snapshot", String.class)).isEqualTo(tarifario.toString());
        // El hash se verifica con una consulta: no es una cifra que haya que creerle a quien la escribio.
        assertThat(fila.get("hash_snapshot", String.class))
                .hasSize(64)
                .isEqualTo(fila.get("recalculado", String.class));

        var respuesta = dsl.fetchOne(
                """
                SELECT payload ->> 'grupoId' AS g, payload ->> 'tarifarioId' AS t, payload ->> 'hashSnapshot' AS h
                  FROM tarifas.evento_dominio WHERE agregado_id = ? AND tipo = 'tarifas.tarifa_congelada'
                """,
                grupo);
        assertThat(respuesta.get("g", String.class)).isEqualTo(grupo.toString());
        assertThat(respuesta.get("t", String.class)).isEqualTo(tarifario.toString());
        assertThat(respuesta.get("h", String.class)).isEqualTo(fila.get("hash_snapshot", String.class));
    }

    @Test
    @DisplayName("evento duplicado: el mismo grupo_creado dos veces deja una sola tarifa y una sola respuesta")
    void eventoDuplicado() {
        UUID grupo = escenario.grupo();
        var mismo = evento(grupo, tarifarioConDosConceptos());

        assertThat(procesar(mismo)).isEqualTo(Resultado.CONGELADA);
        assertThat(procesar(mismo)).isEqualTo(Resultado.DUPLICADO);

        assertThat(tarifasDe(grupo)).isEqualTo(1);
        assertThat(respuestasDe(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "reintento: otro grupo_creado del mismo grupo conserva la tarifa y vuelve a responder para que el grupo abra")
    void reintentoConservaLaTarifa() {
        UUID grupo = escenario.grupo();
        UUID primero = tarifarioConDosConceptos();
        UUID otro = tarifarioConDosConceptos();

        assertThat(procesar(evento(grupo, primero))).isEqualTo(Resultado.CONGELADA);
        // R-TAR-07: una sola tarifa por grupo. Un segundo evento (aun con otro tarifario) no la reescribe...
        assertThat(procesar(evento(grupo, otro))).isEqualTo(Resultado.YA_CONGELADA);

        assertThat(tarifasDe(grupo)).isEqualTo(1);
        assertThat(dsl.fetchOne("SELECT tarifario_id FROM tarifas.tarifa_congelada_grupo WHERE grupo_id = ?", grupo)
                        .get(0, UUID.class))
                .isEqualTo(primero);
        // ...y responde con lo que QUEDO congelado, no con lo que traia el evento.
        assertThat(respuestasDe(grupo)).isEqualTo(2);
        assertThat(dsl.fetch(
                                "SELECT payload ->> 'tarifarioId' FROM tarifas.evento_dominio WHERE agregado_id = ? AND tipo = 'tarifas.tarifa_congelada'",
                                grupo)
                        .getValues(0, String.class))
                .containsOnly(primero.toString());
    }

    @Test
    @DisplayName("rechaza un tarifario que no existe: no congela un precio inventado ni marca el evento como consumido")
    void rechazaTarifarioInexistente() {
        UUID grupo = escenario.grupo();
        var evento = evento(grupo, UUID.randomUUID());

        assertThatThrownBy(() -> procesar(evento))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tarifario");

        assertThat(tarifasDe(grupo)).isZero();
        assertThat(respuestasDe(grupo)).isZero();
        assertThat(contar("SELECT count(*)::int FROM tarifas.evento_consumido WHERE id_evento = ?", evento.eventoId()))
                .isZero();
    }

    @Test
    @DisplayName("concurrencia: dos grupo_creado a la vez del mismo grupo dejan una sola tarifa congelada")
    void concurrencia() throws Exception {
        UUID grupo = escenario.grupo();
        UUID tarifario = tarifarioConDosConceptos();
        var inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var a = ejecutor.submit(() -> {
                inicio.await();
                return procesar(evento(grupo, tarifario));
            });
            var b = ejecutor.submit(() -> {
                inicio.await();
                return procesar(evento(grupo, tarifario));
            });
            inicio.countDown();
            var resultados = List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertThat(resultados).doesNotContain(Resultado.DUPLICADO);
        }
        assertThat(tarifasDe(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rls: congelar funciona con el rol real del servicio (svc_tarifas), no solo con el dueno de la base")
    void rolRealDelServicio() {
        UUID grupo = escenario.grupo();
        UUID tarifario = tarifarioConDosConceptos();
        var evento = evento(grupo, tarifario);

        var resultado = transaccion.execute(e -> {
            dsl.execute("SET LOCAL ROLE svc_tarifas");
            return congelar.procesar(evento);
        });

        assertThat(resultado).isEqualTo(Resultado.CONGELADA);
        assertThat(tarifasDe(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName("el snapshot es estable: el mismo tarifario produce el mismo texto y por eso el mismo hash")
    void snapshotEstable() {
        UUID tarifario = tarifarioConDosConceptos();
        var repositorio = new TarifaCongeladaRepositorio();

        String uno =
                transaccion.execute(e -> repositorio.snapshotDe(dsl, tarifario).orElseThrow());
        String dos =
                transaccion.execute(e -> repositorio.snapshotDe(dsl, tarifario).orElseThrow());

        assertThat(uno).isEqualTo(dos);
        assertThat(TarifaCongeladaRepositorio.hashDe(uno)).isEqualTo(TarifaCongeladaRepositorio.hashDe(dos));
        Optional<String> inexistente = transaccion.execute(e -> repositorio.snapshotDe(dsl, UUID.randomUUID()));
        assertThat(inexistente).isEmpty();
    }
}
