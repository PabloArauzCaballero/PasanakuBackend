package bo.aportaya.plataforma.mensajeria;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.List;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tableros (CU-98) · el trabajo de calculo que faltaba, del lado de cada servicio dueño: corre sus
 * {@link CalculadorDeIndicadores} sobre los periodos CERRADOS y publica cada valor por outbox
 * ({@code <esquema>.indicador_calculado}); {@code auditoria} lo guarda en {@code indicador_kpi}.
 *
 * <p>Existe solo en los servicios que declaran un calculador (lo registra {@link ConfiguracionMensajeria}). Una
 * transaccion por periodo: si uno falla, los demas se publican igual y el que fallo se reintenta el dia siguiente o
 * en el proximo relleno (en {@code auditoria}, repetir un periodo no pisa nada).
 */
public class PublicacionDeIndicadores {

    private static final Logger LOG = LoggerFactory.getLogger(PublicacionDeIndicadores.class);
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-0000000000c9");

    private final Datos datos;
    private final TransactionTemplate transaccion;
    private final PublicadorDeIndicadores publicador;
    private final List<CalculadorDeIndicadores> calculadores;
    private final Reloj reloj;
    private final int mesesDeRelleno;
    private final int diasDeRelleno;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public PublicacionDeIndicadores(
            Datos datos,
            TransactionTemplate transaccion,
            PublicadorDeIndicadores publicador,
            List<CalculadorDeIndicadores> calculadores,
            Reloj reloj,
            int mesesDeRelleno,
            int diasDeRelleno) {
        this.datos = datos;
        this.transaccion = transaccion;
        this.publicador = publicador;
        this.calculadores = List.copyOf(calculadores);
        this.reloj = reloj;
        this.mesesDeRelleno = mesesDeRelleno;
        this.diasDeRelleno = diasDeRelleno;
    }

    @Scheduled(cron = "${aportaya.indicadores.cron:0 15 2 * * *}", zone = "America/La_Paz")
    @SchedulerLock(name = "indicadores-diarios", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void diario() {
        publicarTodos(PeriodoDeTablero.cerradosAl(reloj.hoy()));
    }

    /** Relleno al arrancar (0 por omision): en dev y TEST, para que el tablero tenga serie con la base sembrada. */
    @EventListener(ApplicationReadyEvent.class)
    public void rellenar() {
        if (mesesDeRelleno > 0 || diasDeRelleno > 0) {
            publicarTodos(PeriodoDeTablero.relleno(reloj.hoy(), mesesDeRelleno, diasDeRelleno));
        }
    }

    /** Publica un periodo. Devuelve cuantos indicadores emitio (los que no tienen valor no se emiten). */
    public int publicar(PeriodoDeTablero periodo) {
        ContextoSesion ctx =
                ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
        Integer emitidos = transaccion.execute(estado -> datos.conContexto(ctx, dsl -> {
            int n = 0;
            for (CalculadorDeIndicadores calculador : calculadores) {
                for (CalculadorDeIndicadores.Calculado c : calculador.calcular(dsl, periodo)) {
                    // Periodos cerrados: definitivos (ambiguedad A7 del trabajo tableros-por-rol).
                    if (publicador.publicar(
                            dsl,
                            c.indicador(),
                            c.nombre(),
                            periodo.codigo(),
                            false,
                            UUID.fromString(ctx.traza().id()))) {
                        n++;
                    }
                }
            }
            return n;
        }));
        return emitidos == null ? 0 : emitidos;
    }

    private void publicarTodos(List<PeriodoDeTablero> periodos) {
        int total = 0;
        for (PeriodoDeTablero p : periodos) {
            try {
                total += publicar(p);
            } catch (RuntimeException fallo) {
                // Codigos y periodos, nunca valores ni personas (regla 90.2.1).
                LOG.warn(
                        "indicadores del periodo {} no publicados: {}",
                        p.codigo(),
                        fallo.getClass().getSimpleName());
            }
        }
        LOG.info("indicadores: {} emitidos en {} periodo(s)", total, periodos.size());
    }
}
