package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor;
import bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor.Entrada;
import bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar.EntradaCaso;
import bo.aportaya.cumplimiento.infraestructura.CasoLftRepositorio;
import bo.aportaya.cumplimiento.infraestructura.MonitoreoLftRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H5.S2 · la decision del oficial por la puerta del backoffice.
 *
 * <p>Revisor = sesion, analista = el guardado en el caso, alertas = las enlazadas al caso, y una sola
 * decision por caso aunque lleguen dos a la vez.
 */
class CU44DecisionTest extends BaseDeCumplimiento {

    private static final String CONCLUSION =
            "Se revisaron las operaciones del periodo y responden al giro declarado del titular.";

    private UUID regla;
    private CU44DecidirComoRevisor decisionCU;

    @BeforeEach
    void escenario() {
        regla = gobiernoFixtura.reglaDeMonitoreo(
                "RM-" + UUID.randomUUID().toString().substring(0, 8), "ALTA", true);
        decisionCU = new CU44DecidirComoRevisor(
                new Datos(dsl),
                new CasoLftRepositorio(),
                new MonitoreoLftRepositorio(),
                new Outbox("cumplimiento"),
                Reloj.delSistema());
    }

    private static ContextoSesion sesionDe(UUID id) {
        return ContextoSesion.de(id, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private record Expediente(UUID casoId, UUID analista, List<UUID> alertas) {}

    private Expediente casoConDosAlertas() {
        UUID titular = fixtura.usuario();
        UUID analista = fixtura.usuario();
        var ahora = OffsetDateTime.now(ZoneOffset.UTC);
        List<UUID> alertas = List.of(
                gobiernoFixtura.alerta(regla, titular, "ALTA", "5000.00", ahora),
                gobiernoFixtura.alerta(regla, titular, "MEDIA", "900.00", ahora));
        var caso = transaccion.execute(t -> casoCU.abrirCaso(
                new EntradaCaso(alertas, analista, "Fraccionamiento detectado en el periodo"), sesionDe(analista)));
        return new Expediente(caso.casoId(), analista, alertas);
    }

    private CU44DecidirComoRevisor.Decision decidir(UUID casoId, String decision, UUID oficial) {
        return transaccion.execute(
                t -> decisionCU.decidir(new Entrada(casoId, decision, CONCLUSION, null), sesionDe(oficial)));
    }

    private int eventos(UUID casoId) {
        return contar(
                "SELECT count(*)::int FROM cumplimiento.evento_dominio WHERE tipo = 'cumplimiento.caso_decidido'"
                        + " AND agregado_id = ?",
                casoId);
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName("Dado un caso abierto por un analista · Cuando un oficial distinto lo decide DESCARTAR · Entonces el"
            + " caso queda CERRADO con el oficial de la sesion como revisor y sus alertas se descartan con conclusion")
    void oficialDecide() {
        Expediente e = casoConDosAlertas();
        UUID oficial = fixtura.usuario();

        var decision = decidir(e.casoId(), "DESCARTAR", oficial);

        assertThat(decision.revisadoPor()).isEqualTo(oficial);
        assertThat(decision.alertasCerradas()).isEqualTo(2);
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft"
                                + " WHERE id = ? AND estado = 'CERRADO' AND revisado_por = ? AND decision = 'DESCARTAR'",
                        e.casoId(),
                        oficial))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft"
                                + " WHERE caso_id = ? AND estado = 'DESCARTADA' AND conclusion IS NOT NULL",
                        e.casoId()))
                .isEqualTo(2);
        assertThat(eventos(e.casoId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Dados dos casos abiertos · Cuando el oficial decide uno · Entonces las alertas del otro siguen"
            + " abiertas: la decision solo cierra las alertas de su propio caso")
    void soloCierraLasDelCaso() {
        Expediente decidido = casoConDosAlertas();
        Expediente ajeno = casoConDosAlertas();

        decidir(decidido.casoId(), "MONITOREO_REFORZADO", fixtura.usuario());

        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft WHERE caso_id = ? AND estado = 'EN_ANALISIS'",
                        ajeno.casoId()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Dado un caso ya decidido por un oficial · Cuando el mismo oficial repite la misma decision · Entonces"
            + " recibe el mismo resultado y no hay un segundo evento")
    void reintentoIdentico() {
        Expediente e = casoConDosAlertas();
        UUID oficial = fixtura.usuario();

        var primera = decidir(e.casoId(), "DESCARTAR", oficial);
        var repetida = decidir(e.casoId(), "DESCARTAR", oficial);

        assertThat(repetida.casoId()).isEqualTo(primera.casoId());
        assertThat(repetida.revisadoPor()).isEqualTo(oficial);
        assertThat(repetida.alertasCerradas()).isEqualTo(2);
        assertThat(eventos(e.casoId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un caso recien abierto con dos alertas · Cuando se reintenta abrirlo con las mismas alertas y"
            + " el mismo analista · Entonces devuelve el mismo caso y no abre otro vacio")
    void aperturaReintentada() {
        Expediente e = casoConDosAlertas();
        int antes = contar("SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft");

        var otraVez = transaccion.execute(t -> casoCU.abrirCaso(
                new EntradaCaso(e.alertas(), e.analista(), "Fraccionamiento detectado en el periodo"),
                sesionDe(e.analista())));

        assertThat(otraVez.casoId()).isEqualTo(e.casoId());
        assertThat(contar("SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft"))
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("rechaza abrir un caso con una alerta que ya esta en otro: AP-CU44-08 y ningun caso nuevo")
    void alertaEnOtroCaso() {
        Expediente e = casoConDosAlertas();
        UUID otroAnalista = fixtura.usuario();
        int antes = contar("SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft");

        assertThatThrownBy(() -> transaccion.execute(t -> casoCU.abrirCaso(
                        new EntradaCaso(
                                List.of(e.alertas().get(0)), otroAnalista, "Otro intento sobre la misma alerta"),
                        sesionDe(otroAnalista))))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU44-08"));
        assertThat(contar("SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft"))
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("rechaza que el analista del caso se revise a si mismo: AP-CU44-02 contra el analista GUARDADO")
    void autoRevision() {
        Expediente e = casoConDosAlertas();

        assertThatThrownBy(() -> decidir(e.casoId(), "DESCARTAR", e.analista()))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU44-02"));
        assertThat(eventos(e.casoId())).isZero();
    }

    @Test
    @DisplayName("rechaza una segunda decision sobre el mismo caso: AP-CU44-07 y un solo evento")
    void segundaDecision() {
        Expediente e = casoConDosAlertas();
        decidir(e.casoId(), "DESCARTAR", fixtura.usuario());

        assertThatThrownBy(() -> decidir(e.casoId(), "MONITOREO_REFORZADO", fixtura.usuario()))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU44-07"));
        assertThat(eventos(e.casoId())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza un caso inexistente con AP-CU44-06, y REPORTAR sin ROS con AP-CU44-03")
    void inexistenteYSinRos() {
        assertThatThrownBy(() -> decidir(UUID.randomUUID(), "DESCARTAR", fixtura.usuario()))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU44-06"));
        Expediente e = casoConDosAlertas();
        assertThatThrownBy(() -> decidir(e.casoId(), "REPORTAR", fixtura.usuario()))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU44-03"));
    }

    @Test
    @DisplayName("concurrencia: dos oficiales deciden a la vez, uno gana, el otro recibe AP-CU44-07 y hay un evento")
    void dosOficialesALaVez() throws Exception {
        Expediente e = casoConDosAlertas();
        List<UUID> oficiales = List.of(fixtura.usuario(), fixtura.usuario());
        var barrera = new CyclicBarrier(2);
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());
        List<Thread> hilos = new ArrayList<>();
        for (UUID oficial : oficiales) {
            hilos.add(new Thread(() -> {
                try {
                    barrera.await();
                    decidir(e.casoId(), "DESCARTAR", oficial);
                } catch (Exception x) {
                    errores.add(x);
                }
            }));
        }
        hilos.forEach(Thread::start);
        for (Thread h : hilos) {
            h.join();
        }

        assertThat(errores).hasSize(1);
        assertThat(codigo(errores.get(0))).isEqualTo("AP-CU44-07");
        assertThat(eventos(e.casoId())).isEqualTo(1);
    }
}
