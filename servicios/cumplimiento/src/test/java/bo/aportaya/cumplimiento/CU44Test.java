package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar.EntradaCaso;
import bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor;
import bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor.Entrada;
import bo.aportaya.cumplimiento.infraestructura.CasoLftRepositorio;
import bo.aportaya.cumplimiento.infraestructura.MonitoreoLftRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-44 · De alerta de monitoreo a reporte de operacion sospechosa. */
class CU44Test extends BaseDeCumplimiento {

    private record EntradaDecision(UUID casoId, List<UUID> alertas, UUID analistaId, UUID revisorId, String decision, String conclusion, UUID reporteSospechosoId) {}

    private CU44DecidirComoRevisor decisionCU;

    private static final String CONCLUSION =
            "Se revisaron las doce operaciones del periodo y responden al giro declarado del titular.";

    private UUID titular;
    private UUID analista;
    private UUID revisor;
    private UUID regla;
    private ContextoSesion ctx;

    @BeforeEach
    void escenario() {
        decisionCU = new CU44DecidirComoRevisor(new Datos(dsl), new CasoLftRepositorio(), new MonitoreoLftRepositorio(), new Outbox("cumplimiento"), Reloj.delSistema());
        titular = fixtura.usuario();
        analista = fixtura.usuario();
        revisor = fixtura.usuario();
        regla = gobiernoFixtura.reglaDeMonitoreo(
                "RM-" + UUID.randomUUID().toString().substring(0, 8), "ALTA", true);
        ctx = ContextoSesion.de(
                analista, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private CU44DecidirComoRevisor.Decision decidir(EntradaDecision e, ContextoSesion ignorado) {
        ContextoSesion actor = ContextoSesion.de(e.revisorId(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
        return decisionCU.decidir(new Entrada(e.casoId(), e.decision(), e.conclusion(), e.reporteSospechosoId()), actor);
    }

    private UUID alerta(String severidad) {
        return gobiernoFixtura.alerta(regla, titular, severidad, "5000.00", OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName(
            "Dada una regla de fraccionamiento activa · Cuando un cliente realiza operaciones que la satisfacen · Entonces existe una alerta_monitoreo_lft con detalle del patrón")
    void criterio1() {
        UUID alertaId = alerta("ALTA");

        // El detalle no es decorativo: es lo que el analista lee para decidir, y lo que
        // despues explica por que se abrio el caso.
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft
                         WHERE id = ? AND estado = 'ABIERTA' AND jsonb_exists(detalle, 'patron')
                        """,
                        alertaId))
                .isEqualTo(1);

        var caso = transaccion.execute(
                t -> casoCU.abrirCaso(new EntradaCaso(List.of(alertaId), "Fraccionamiento detectado"), ctx));
        assertThat(caso.plazoLimite()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName(
            "Dado un intento de cerrar una alerta sin conclusión · Cuando se guarda · Entonces la base de datos lo rechaza (R-UIF-07)")
    void criterio2() {
        UUID alertaId = alerta("MEDIA");

        // ck_alerta_conclusion exige veinte caracteres: «revisado» no le sirve a quien
        // audite ni a quien calibre la regla.
        assertThat(rechazaLaBase(
                        "UPDATE cumplimiento.alerta_monitoreo_lft SET estado = 'DESCARTADA' WHERE id = ?", alertaId))
                .contains("ck_alerta_conclusion");
        assertThat(rechazaLaBase(
                        """
                        UPDATE cumplimiento.alerta_monitoreo_lft SET estado = 'DESCARTADA', conclusion = 'ok'
                         WHERE id = ?
                        """,
                        alertaId))
                .contains("ck_alerta_conclusion");
    }

    @Test
    @DisplayName(
            "Dado un caso con decisión REPORTAR · Cuando se cierra · Entonces existe un reporte_operacion_sospechosa enlazado · Y no se generó ninguna notificación al cliente")
    void criterio3() {
        UUID alertaId = alerta("CRITICA");
        var caso = transaccion.execute(
                t -> casoCU.abrirCaso(new EntradaCaso(List.of(alertaId), "Patron inusual"), ctx));
        // El ROS vive en el esquema de auditoria y llega ya radicado: este servicio no
        // lo escribe (invariante 11).
        UUID ros = gobiernoFixtura.reporteSospechoso(titular);

        transaccion.execute(t -> decidir(
                new EntradaDecision(caso.casoId(), List.of(alertaId), analista, revisor, "REPORTAR", CONCLUSION, ros),
                ctx));

        assertThat(contar(
                        """
                        SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft
                         WHERE id = ? AND decision = 'REPORTAR' AND reporte_operacion_sospechosa_id = ?
                        """,
                        caso.casoId(),
                        ros))
                .isEqualTo(1);
        // Deber de reserva: ni una notificacion al titular. El evento lo dice explicito
        // para que ningun consumidor lo intente.
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM cumplimiento.evento_dominio
                         WHERE tipo = 'cumplimiento.caso_decidido' AND agregado_id = ?
                           AND payload->>'notificarAlTitular' = 'false'
                        """,
                        caso.casoId()))
                .isEqualTo(1);
        // Y el payload no lleva al titular: la reserva alcanza a la bandeja de eventos.
        assertThat(
                        contar(
                                """
                        SELECT count(*)::int FROM cumplimiento.evento_dominio
                         WHERE tipo = 'cumplimiento.caso_abierto' AND jsonb_exists(payload, 'usuarioId')
                        """))
                .isZero();
    }

    @Test
    @DisplayName("reintento: la misma clave de idempotencia dos veces devuelve la misma respuesta y un solo efecto")
    void reintento() {
        UUID alertaId = alerta("ALTA");
        var caso = transaccion.execute(
                t -> casoCU.abrirCaso(new EntradaCaso(List.of(alertaId), "Patron inusual"), ctx));
        var decision =
                new EntradaDecision(caso.casoId(), List.of(alertaId), analista, revisor, "DESCARTAR", CONCLUSION, null);

        transaccion.execute(t -> decidir(decision, ctx));
        // La segunda no reabre ni reescribe: el caso ya esta cerrado y la alerta tambien.
        transaccion.execute(t -> decidir(decision, ctx));

        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft WHERE id = ? AND estado = 'CERRADO'",
                        caso.casoId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft WHERE id = ? AND estado = 'DESCARTADA'",
                        alertaId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: dos transacciones sobre el mismo agregado, una gana y nunca hay doble efecto")
    void concurrencia() throws Exception {
        UUID alertaId = alerta("ALTA");
        var entrada = new EntradaCaso(List.of(alertaId), "Patron inusual");

        var barrera = new java.util.concurrent.CyclicBarrier(2);
        var errores = java.util.Collections.synchronizedList(new java.util.ArrayList<Throwable>());
        Runnable intento = () -> {
            try {
                barrera.await();
                transaccion.execute(t -> casoCU.abrirCaso(entrada, ctx));
            } catch (Exception e) {
                errores.add(e);
            }
        };
        var uno = new Thread(intento);
        var dos = new Thread(intento);
        uno.start();
        dos.start();
        uno.join();
        dos.join();

        assertThat(errores).hasSizeLessThanOrEqualTo(1);
        // La alerta queda asignada a un solo analista: asignar dos veces la pondria en
        // dos bandejas y ninguno la miraria.
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft WHERE id = ? AND asignada_a IS NOT NULL",
                        alertaId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: la suma de debitos iguala la de creditos, al centavo")
    void cuadre() {
        // El cuadre de un caso es que todas sus alertas queden cerradas con el: una
        // alerta que sobrevive al caso vuelve a la bandeja sin dueño.
        UUID a1 = alerta("ALTA");
        UUID a2 = alerta("MEDIA");
        var caso = transaccion.execute(t ->
                casoCU.abrirCaso(new EntradaCaso(List.of(a1, a2), "Dos alertas del mismo titular"), ctx));

        transaccion.execute(t -> decidir(
                new EntradaDecision(caso.casoId(), List.of(a1, a2), analista, revisor, "DESCARTAR", CONCLUSION, null),
                ctx));

        assertThat(contar(
                        """
                        SELECT count(*)::int FROM cumplimiento.alerta_monitoreo_lft
                         WHERE caso_id = ? AND cerrada_en IS NOT NULL
                        """,
                        caso.casoId()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("evento duplicado y fuera de orden: un solo efecto")
    void eventoDuplicadoYFueraDeOrden() {
        UUID alertaId = alerta("ALTA");
        var caso = transaccion.execute(
                t -> casoCU.abrirCaso(new EntradaCaso(List.of(alertaId), "Patron inusual"), ctx));
        var decision =
                new EntradaDecision(caso.casoId(), List.of(alertaId), analista, revisor, "DESCARTAR", CONCLUSION, null);

        transaccion.execute(t -> decidir(decision, ctx));
        transaccion.execute(t -> decidir(decision, ctx));
        transaccion.execute(t -> decidir(decision, ctx));

        // Tres decisiones sobre el mismo caso no son tres decisiones: el estado no se
        // reabre y la alerta no vuelve a cerrarse.
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft WHERE id = ? AND estado = 'CERRADO'",
                        caso.casoId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("compensacion: se fuerza el fallo de cada paso y el sistema queda cuadrado")
    void compensacion() {
        UUID alertaId = alerta("ALTA");
        var caso = transaccion.execute(
                t -> casoCU.abrirCaso(new EntradaCaso(List.of(alertaId), "Patron inusual"), ctx));

        // Paso fallido: cerrar sin conclusion.
        assertThatThrownBy(() -> transaccion.execute(t -> decidir(
                        new EntradaDecision(
                                caso.casoId(), List.of(alertaId), analista, revisor, "DESCARTAR", "corto", null),
                        ctx)))
                .hasMessageContaining("sin conclusion");

        // Paso fallido: el revisor es el analista.
        assertThatThrownBy(() -> transaccion.execute(t -> decidir(
                        new EntradaDecision(
                                caso.casoId(), List.of(alertaId), analista, analista, "DESCARTAR", CONCLUSION, null),
                        ctx)))
                .hasMessageContaining("independiente");

        // Paso fallido: reportar sin el ROS radicado.
        assertThatThrownBy(() -> transaccion.execute(t -> decidir(
                        new EntradaDecision(
                                caso.casoId(), List.of(alertaId), analista, revisor, "REPORTAR", CONCLUSION, null),
                        ctx)))
                .hasMessageContaining("narrativa");

        // El caso sigue abierto tras los tres fallos: nada quedo a medias.
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft WHERE id = ? AND estado = 'ABIERTO'",
                        caso.casoId()))
                .isEqualTo(1);

        transaccion.execute(t -> decidir(
                new EntradaDecision(caso.casoId(), List.of(alertaId), analista, revisor, "DESCARTAR", CONCLUSION, null),
                ctx));
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.caso_investigacion_lft WHERE id = ? AND estado = 'CERRADO'",
                        caso.casoId()))
                .isEqualTo(1);
    }
}
