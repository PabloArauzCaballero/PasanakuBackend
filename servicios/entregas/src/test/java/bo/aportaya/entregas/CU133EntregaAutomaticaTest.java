package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.aplicacion.AplicarTurnosAsignados;
import bo.aportaya.entregas.aplicacion.AplicarTurnosAsignados.TurnosAsignados;
import bo.aportaya.entregas.aplicacion.EntregaAutomatica;
import bo.aportaya.entregas.aplicacion.EntregaAutomatica.PeriodoRecaudado;
import bo.aportaya.entregas.aplicacion.EntregaAutomatica.Resultado;
import bo.aportaya.entregas.aplicacion.PasosDeEntregaAutomatica;
import bo.aportaya.entregas.infraestructura.SagaDeEntregaAutomaticaRepositorio;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio.Turno;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F3.L2.M01 · la entrega automatica contra la base REAL. Los productores ({@code aportes.periodo_recaudado},
 * {@code grupos.turnos_asignados}) no existen todavia: sus eventos se construyen segun los pactos, y {@code aportes}
 * es el doble de {@link BaseDeEntregas} (regla 65). No cubre el broker ni la caida real del contenedor de aportes.
 */
class CU133EntregaAutomaticaTest extends BaseDePozoCompleto {

    private AplicarTurnosAsignados turnosCU;
    private EntregaAutomatica automatica;

    @BeforeEach
    void armarLaSaga() {
        Datos datos = new Datos(dsl);
        // Spring abre la transaccion en @Transactional; aca se abre a mano alrededor de cada paso LOCAL.
        var pasos =
                new PasosDeEntregaAutomatica(
                        datos, new SagaDeEntregaAutomaticaRepositorio(), new TurnoDelGrupoRepositorio()) {
                    @Override
                    public boolean avisar(UUID evento, UUID correlacion, UUID periodo, UUID grupo, String motivo) {
                        return transaccion.execute(t -> super.avisar(evento, correlacion, periodo, grupo, motivo));
                    }

                    @Override
                    public Optional<Pendiente> pendiente(UUID periodo) {
                        return transaccion.execute(t -> super.pendiente(periodo));
                    }

                    @Override
                    public boolean cerrar(UUID periodo, String estado, int paso, String correlacion) {
                        return transaccion.execute(t -> super.cerrar(periodo, estado, paso, correlacion));
                    }

                    @Override
                    public void postergar(UUID periodo, String correlacion) {
                        transaccion.executeWithoutResult(t -> super.postergar(periodo, correlacion));
                    }

                    @Override
                    public List<UUID> iniciadas(OffsetDateTime antesDe, int limite) {
                        return transaccion.execute(t -> super.iniciadas(antesDe, limite));
                    }
                };
        turnosCU = new AplicarTurnosAsignados(datos, new TurnoDelGrupoRepositorio(), Reloj.delSistema()) {
            @Override
            public Salida procesar(TurnosAsignados evento) {
                return transaccion.execute(t -> super.procesar(evento));
            }
        };
        // DOBLE del metodo de desembolso (DECISION_REQUIRED): la propuesta, con una cuenta verificada de billetera.
        automatica = new EntregaAutomatica(
                pasos, pozoCU, Reloj.delSistema(), participante -> Optional.of("BILLETERA_MOVIL"));
    }

    private TurnosAsignados turnos(Caso c, int version) {
        return new TurnosAsignados(
                UUID.randomUUID(),
                UUID.randomUUID(),
                c.e().grupoId(),
                List.of(new Turno(
                        c.e().turnoId(),
                        c.e().grupoId(),
                        c.e().periodoId(),
                        c.e().cupoId(),
                        c.e().participanteId(),
                        version)));
    }

    private static PeriodoRecaudado aviso(Caso c, String motivo) {
        return new PeriodoRecaudado(UUID.randomUUID(), UUID.randomUUID(), c.e().periodoId(), c.e().grupoId(), motivo);
    }

    private String estadoDeLaSaga(Caso c) {
        return dsl.fetchValue(
                        "SELECT estado FROM entregas.estado_saga WHERE tipo_saga = 'entrega-automatica' AND clave_negocio = ?",
                        c.e().periodoId().toString())
                .toString();
    }

    @Test
    @DisplayName("correcto: con el turno conocido, el aviso de recaudo fondea una entrega sin pedido manual")
    void avisoFondea() {
        var c = caso();
        turnosCU.procesar(turnos(c, 1));
        aportesConfirma(c, "6000.00");

        assertThat(automatica.recibir(aviso(c, "COMPLETO"))).isEqualTo(Resultado.FONDEADO);

        assertThat(entregasDelTurno(c)).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.fondeo_entrega WHERE clave_idempotencia = ?",
                        "fondeo-auto-" + c.e().periodoId()))
                .isEqualTo(1);
        assertThat(estadoDeLaSaga(c)).isEqualTo("COMPLETADA");
    }

    @Test
    @DisplayName("limite: el mismo aviso dos veces deja una sola entrega y no vuelve a preguntar a aportes")
    void avisoDuplicado() {
        var c = caso();
        turnosCU.procesar(turnos(c, 1));
        aportesConfirma(c, "6000.00");
        var aviso = aviso(c, "COMPLETO");

        automatica.recibir(aviso);
        int consultas = aportesDoble.consultas.get();

        assertThat(automatica.recibir(aviso)).isEqualTo(Resultado.DUPLICADO);
        assertThat(aportesDoble.consultas).hasValue(consultas);
        assertThat(entregasDelTurno(c)).isEqualTo(1);
    }

    @Test
    @DisplayName("limite: el aviso antes que los turnos espera, y el barrido fondea una sola vez cuando llegan")
    void fueraDeOrden() {
        var c = caso();
        aportesConfirma(c, "6000.00");

        assertThat(automatica.recibir(aviso(c, "COMPLETO"))).isEqualTo(Resultado.ESPERA_TURNO);
        assertThat(entregasDelTurno(c)).isZero();

        turnosCU.procesar(turnos(c, 1));
        automatica.barrer(Duration.ofMinutes(-1), 10);
        automatica.barrer(Duration.ofMinutes(-1), 10);

        assertThat(entregasDelTurno(c)).isEqualTo(1);
        assertThat(estadoDeLaSaga(c)).isEqualTo("COMPLETADA");
    }

    @Test
    @DisplayName("limite: gracia vencida con faltante y despues completo terminan en el MISMO fondeo, destrabado")
    void graciaVencidaYDespuesCompleto() {
        var c = caso();
        turnosCU.procesar(turnos(c, 1));
        aportesConfirma(c, "5000.00");
        assertThat(automatica.recibir(aviso(c, "GRACIA_VENCIDA"))).isEqualTo(Resultado.CON_PENDIENTE);
        assertThat(estadoDeLaSaga(c)).isEqualTo("EN_CURSO");

        aportesConfirma(c, "6000.00");

        assertThat(automatica.recibir(aviso(c, "COMPLETO"))).isEqualTo(Resultado.FONDEADO);
        assertThat(entregasDelTurno(c)).isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM entregas.fondeo_entrega WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(1);
        assertThat(dsl.fetchValue("SELECT estado FROM entregas.entrega_fondo WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo("PROGRAMADA");
    }

    @Test
    @DisplayName("invalido: con aportes caido no se escribe nada, y el reintento fondea cuando vuelve")
    void aportesCaidoYVuelve() {
        var c = caso();
        turnosCU.procesar(turnos(c, 1));
        aportesDoble.seCae();

        assertThat(automatica.recibir(aviso(c, "COMPLETO"))).isEqualTo(Resultado.ESPERA_APORTES);
        assertThat(entregasDelTurno(c)).isZero();
        assertThat(estadoDeLaSaga(c)).isEqualTo("INICIADA");

        aportesConfirma(c, "6000.00");
        automatica.barrer(Duration.ofMinutes(-1), 10);

        assertThat(entregasDelTurno(c)).isEqualTo(1);
    }

    @Test
    @DisplayName("limite: la copia de turnos aplica una version mayor y descarta una vieja o repetida")
    void versionesDeTurnos() {
        var c = caso();
        var v2 = turnos(c, 2);

        assertThat(turnosCU.procesar(v2).aplicados()).isEqualTo(1);
        assertThat(turnosCU.procesar(v2).resultado()).isEqualTo(AplicarTurnosAsignados.Resultado.DUPLICADO);
        assertThat(turnosCU.procesar(turnos(c, 1)).descartados()).isEqualTo(1);
        assertThat(dsl.fetchValue("SELECT version FROM entregas.turno_del_grupo WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(2);
    }
}
