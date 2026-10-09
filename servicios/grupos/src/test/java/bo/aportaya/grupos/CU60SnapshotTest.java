package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU60Sortear.Compromiso;
import bo.aportaya.grupos.aplicacion.CU60Sortear.Revelacion;
import bo.aportaya.grupos.infraestructura.ConsultasRepositorio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.SorteoVerificable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-60 · plantel, calendario y reglas congelados al comprometer; un solo resultado vigente; sin reroll.
 *
 * <p>Amenazas: cambiar quién entra después de publicar el compromiso, probar semillas hasta que salga el orden
 * conveniente, sortear dos veces, y un resultado que un tercero no puede reconstruir.
 */
class CU60SnapshotTest extends BaseDeCU60 {

    private Compromiso comprometer(UUID grupo, String... entropias) {
        return transaccion.execute(e -> sortear.comprometer(grupo, List.of(entropias), Optional.empty(), contexto()));
    }

    /** Revelar por el camino HTTP: sin semilla, con la sellada en el servidor. */
    private Revelacion revelarSellado(UUID sorteoId, List<String> entropias) {
        return transaccion.execute(
                e -> sortear.revelar(sorteoId, null, entropias, List.of(), null, Optional.empty(), contexto()));
    }

    private ErrorDeNegocio rechazo(Runnable accion) {
        var capturado = new ErrorDeNegocio[1];
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> capturado[0] = e);
        return capturado[0];
    }

    @Test
    @DisplayName(
            "Dado un grupo conformado con sus cupos ocupados y su calendario cargado · Cuando se ejecuta la fase de compromiso · Entonces queda un snapshot con el plantel, el calendario y las reglas congelados, con un hash de 64 caracteres · Y el hash del snapshot entra en las entropías junto con el aporte recibido · Y la semilla queda sellada en el servidor y no aparece en el texto del compromiso")
    void congelaAlComprometer() {
        UUID grupo = fixtura.grupoConformado(4);
        fixtura.cuposOcupados(grupo, 4);
        fixtura.periodos(grupo, 4, new BigDecimal("2000.00"));

        Compromiso compromiso = comprometer(grupo, "entropia-de-ana");

        var fila = dsl.fetchOne(
                "SELECT roster, periodos, reglas, hash_snapshot, semilla_sellada FROM grupos.snapshot_sorteo WHERE sorteo_id=?",
                compromiso.sorteoId());
        assertThat(fila.get("roster", String.class).split("\n")).hasSize(4);
        assertThat(fila.get("periodos", String.class).split("\n")).hasSize(4);
        assertThat(fila.get("reglas", String.class)).contains("monto=").contains("cupos=");
        assertThat(fila.get("hash_snapshot", String.class)).hasSize(64);
        assertThat(fila.get("semilla_sellada", String.class)).isEqualTo(compromiso.semilla());
        var entropias = dsl.fetchOne(
                        "SELECT aportes_entropia::text FROM grupos.sorteo_turnos WHERE id=?", compromiso.sorteoId())
                .get(0, String.class);
        assertThat(entropias)
                .contains("entropia-de-ana")
                .contains("snapshot:" + fila.get("hash_snapshot", String.class));
        assertThat(compromiso.toString()).doesNotContain(compromiso.semilla());
    }

    @Test
    @DisplayName("rechaza por R-GRP-18: el snapshot congelado no se reescribe ni se borra, la base lo rechaza")
    void snapshotInmutable() {
        UUID grupo = fixtura.grupoConformado(3);
        fixtura.cuposOcupados(grupo, 3);
        Compromiso compromiso = comprometer(grupo);

        assertThat(rechazaLaBase(
                        "UPDATE grupos.snapshot_sorteo SET roster='otro' WHERE sorteo_id=?", compromiso.sorteoId()))
                .isNotBlank();
        assertThat(rechazaLaBase("DELETE FROM grupos.snapshot_sorteo WHERE sorteo_id=?", compromiso.sorteoId()))
                .isNotBlank();
    }

    @Test
    @DisplayName(
            "Dado un sorteo comprometido y un cupo que deja de estar ocupado antes de la revelación · Cuando se intenta revelar · Entonces se rechaza sin crear turnos y el sorteo sigue COMPROMETIDO, sin anularse · Y al restituir el plantel congelado la revelación procede con el mismo compromiso")
    void plantelCambiadoNoSeSortea() {
        UUID grupo = fixtura.grupoConformado(4);
        fixtura.cuposOcupados(grupo, 4);
        fixtura.periodos(grupo, 4, new BigDecimal("2000.00"));
        Compromiso compromiso = comprometer(grupo);

        dsl.execute("UPDATE grupos.cupo SET estado='LIBRE' WHERE grupo_id=? AND numero=2", grupo);
        var error = rechazo(() -> revelarSellado(compromiso.sorteoId(), null));

        assertThat(error.codigo().valor()).isEqualTo("AP-CU60-06");
        assertThat(turnosDe(grupo)).isZero();
        assertThat(estadoDelSorteo(compromiso.sorteoId())).isEqualTo("COMPROMETIDO");

        // Restituido el plantel congelado, la revelacion procede: el compromiso sigue siendo el mismo.
        dsl.execute("UPDATE grupos.cupo SET estado='OCUPADO' WHERE grupo_id=? AND numero=2", grupo);
        assertThat(revelarSellado(compromiso.sorteoId(), null).verificado()).isTrue();
        assertThat(turnosDe(grupo)).isEqualTo(4);
    }

    @Test
    @DisplayName(
            "Dado un sorteo comprometido y una fecha límite de pago que cambia después del compromiso · Cuando se intenta revelar · Entonces se rechaza y no se crea ningún turno")
    void calendarioCambiadoNoSeSortea() {
        UUID grupo = fixtura.grupoConformado(3);
        fixtura.cuposOcupados(grupo, 3);
        fixtura.periodos(grupo, 3, new BigDecimal("1500.00"));
        Compromiso compromiso = comprometer(grupo);

        dsl.execute(
                "UPDATE grupos.periodo SET fecha_limite_pago = fecha_limite_pago + 3 WHERE grupo_id=? AND numero=1",
                grupo);

        assertThat(rechazo(() -> revelarSellado(compromiso.sorteoId(), null))
                        .codigo()
                        .valor())
                .isEqualTo("AP-CU60-06");
        assertThat(turnosDe(grupo)).isZero();
    }

    @Test
    @DisplayName(
            "Dado un sorteo que ya fue revelado · Cuando se intenta revelar de nuevo · Entonces se rechaza y no se sortea otra vez · Y el resultado original sigue consultable con el mismo orden y la misma semilla · Y los turnos son los del primer sorteo")
    void sinReroll() {
        UUID grupo = fixtura.grupoConformado(5);
        fixtura.cuposOcupados(grupo, 5);
        fixtura.periodos(grupo, 5, new BigDecimal("2500.00"));
        Compromiso compromiso = comprometer(grupo, "a", "b");
        Revelacion primera = revelarSellado(compromiso.sorteoId(), List.of("a", "b"));

        var segundo = rechazo(() -> revelarSellado(compromiso.sorteoId(), List.of("a", "b")));

        assertThat(segundo.codigo().valor()).isEqualTo("AP-CU60-07");
        var original = transaccion
                .execute(e -> sortear.resultadoVigente(compromiso.sorteoId(), contexto()))
                .orElseThrow();
        assertThat(original.cuposEnOrden()).isEqualTo(primera.cuposEnOrden());
        assertThat(original.semilla()).isEqualTo(primera.semilla());
        assertThat(turnosDe(grupo)).isEqualTo(5);
    }

    @Test
    @DisplayName(
            "Dado un sorteo comprometido cuya semilla todavía no se reveló · Cuando se revela y un tercero reconstruye el resultado con el paquete publicado · Entonces antes de la revelación el paquete no trae semilla ni orden · Y después la semilla revelada verifica el hash comprometido con las entropías · Y barajar el orden original con esa semilla da exactamente el orden sorteado")
    void paqueteReconstruible() {
        UUID grupo = fixtura.grupoConformado(6);
        fixtura.cuposOcupados(grupo, 6);
        fixtura.periodos(grupo, 6, new BigDecimal("3000.00"));
        Compromiso compromiso = comprometer(grupo, "entropia-uno", "entropia-dos");
        var antes = transaccion
                .execute(e -> new ConsultasRepositorio().paqueteDelSorteo(dsl, compromiso.sorteoId()))
                .orElseThrow();
        assertThat(antes.semillaRevelada()).isNull();
        assertThat(antes.ordenPublicado()).isEmpty();

        Revelacion revelacion = revelarSellado(compromiso.sorteoId(), null);

        var paquete = transaccion
                .execute(e -> new ConsultasRepositorio().paqueteDelSorteo(dsl, compromiso.sorteoId()))
                .orElseThrow();
        assertThat(paquete.semillaRevelada()).isEqualTo(revelacion.semilla());
        assertThat(SorteoVerificable.verificarCompromiso(
                        paquete.semillaRevelada(), paquete.entropias(), paquete.hashComprometido()))
                .isTrue();
        assertThat(paquete.cuposEnOrdenOriginal()).containsExactly("1", "2", "3", "4", "5", "6");
        assertThat(SorteoVerificable.barajarDeterminista(paquete.semillaRevelada(), paquete.cuposEnOrdenOriginal()))
                .isEqualTo(paquete.ordenPublicado());
    }

    @Test
    @DisplayName(
            "Dado un sorteo comprometido con la semilla sellada en el servidor · Cuando se intenta revelar con unas entropías que no son las comprometidas · Entonces se rechaza sin revelar ni anular: el sorteo sigue COMPROMETIDO y no hay turnos · Y con las entropías comprometidas la revelación sí verifica")
    void entropiasAjenasNoRevelan() {
        UUID grupo = fixtura.grupoConformado(3);
        fixtura.cuposOcupados(grupo, 3);
        fixtura.periodos(grupo, 3, new BigDecimal("1500.00"));
        Compromiso compromiso = comprometer(grupo, "las-de-verdad");

        var error = rechazo(() -> revelarSellado(compromiso.sorteoId(), List.of("otras")));

        assertThat(error.codigo().valor()).isEqualTo("AP-CU60-04");
        assertThat(estadoDelSorteo(compromiso.sorteoId())).isEqualTo("COMPROMETIDO");
        assertThat(turnosDe(grupo)).isZero();
        assertThat(revelarSellado(compromiso.sorteoId(), List.of("las-de-verdad"))
                        .verificado())
                .isTrue();
    }

    @Test
    @DisplayName(
            "Dado un sorteo comprometido en un grupo sin calendario · Cuando se intenta revelar · Entonces se rechaza pero el sorteo sigue vivo en COMPROMETIDO · Y al existir el calendario la revelación procede y se crean los turnos")
    void sinCalendario() {
        UUID grupo = fixtura.grupoConformado(3);
        fixtura.cuposOcupados(grupo, 3);
        Compromiso compromiso = comprometer(grupo);

        assertThat(rechazo(() -> revelarSellado(compromiso.sorteoId(), null))
                        .codigo()
                        .valor())
                .isEqualTo("AP-CU60-09");
        assertThat(estadoDelSorteo(compromiso.sorteoId())).isEqualTo("COMPROMETIDO");

        fixtura.periodos(grupo, 3, new BigDecimal("1500.00"));
        assertThat(revelarSellado(compromiso.sorteoId(), null).verificado()).isTrue();
        assertThat(turnosDe(grupo)).isEqualTo(3);
    }

    @Test
    @DisplayName(
            "concurrencia: dos revelaciones simultaneas: una sortea, la otra recibe AP-CU60-07 y los turnos son los de una sola")
    void revelacionesSimultaneas() throws Exception {
        UUID grupo = fixtura.grupoConformado(4);
        fixtura.cuposOcupados(grupo, 4);
        fixtura.periodos(grupo, 4, new BigDecimal("2000.00"));
        Compromiso compromiso = comprometer(grupo);
        var inicio = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> intentar(inicio, compromiso.sorteoId()));
            var b = pool.submit(() -> intentar(inicio, compromiso.sorteoId()));
            inicio.countDown();
            var codigos = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertThat(codigos).containsExactlyInAnyOrder("OK", "AP-CU60-07");
        }
        assertThat(turnosDe(grupo)).isEqualTo(4);
    }

    @Test
    @DisplayName(
            "concurrencia: dos compromisos simultaneos del mismo grupo: uno gana y el otro recibe AP-CU60-02, no un error de base")
    void compromisosSimultaneos() throws Exception {
        UUID grupo = fixtura.grupoConformado(4);
        fixtura.cuposOcupados(grupo, 4);
        var inicio = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> comprometerSimultaneo(inicio, grupo));
            var b = pool.submit(() -> comprometerSimultaneo(inicio, grupo));
            inicio.countDown();
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "AP-CU60-02");
        }
        assertThat(dsl.fetchCount(
                        org.jooq.impl.DSL.table("grupos.sorteo_turnos"),
                        org.jooq.impl.DSL.field("grupo_id").eq(grupo)))
                .isEqualTo(1);
    }

    private String intentar(CountDownLatch inicio, UUID sorteoId) throws InterruptedException {
        inicio.await();
        try {
            revelarSellado(sorteoId, null);
            return "OK";
        } catch (ErrorDeNegocio e) {
            return e.codigo().valor();
        }
    }

    private String comprometerSimultaneo(CountDownLatch inicio, UUID grupo) throws InterruptedException {
        inicio.await();
        try {
            comprometer(grupo);
            return "OK";
        } catch (ErrorDeNegocio e) {
            return e.codigo().valor();
        }
    }
}
