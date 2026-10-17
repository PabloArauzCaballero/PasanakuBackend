package bo.aportaya.aportes.trabajos;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaRecargos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El disparador diario de los recargos por mora de CU-21. <b>No decide nada</b>: arma el
 * contexto de sistema y llama a {@link CU21CobrarAporte#generarRecargos}.
 *
 * <p>Hasta este trabajo el metodo existia y se probaba, pero no lo llamaba nadie: ningun
 * atraso generaba recargo y el tablero de intereses por mora cobrados daba siempre vacio.
 *
 * <p><b>El lock ahorra trabajo, no da la correccion.</b> Lo que impide dos recargos por el
 * mismo atraso es que {@code vencidasSinRecargo} excluye las obligaciones que ya tienen
 * uno: una segunda replica, o un segundo disparo del mismo dia, no genera nada.
 *
 * <p>El contexto es {@code ContextoSesion.deSistema}: un trabajo sin contexto corre sin RLS,
 * y eso no falla — devuelve filas de todos.
 */
@Component
@ConditionalOnProperty(name = "aportaya.aportes.trabajos.habilitados", havingValue = "true", matchIfMissing = true)
public class RecargosDeMoraDiarios {

    private static final Logger LOG = LoggerFactory.getLogger(RecargosDeMoraDiarios.class);

    /** Identifica al proceso en la bitacora. Es el mismo siempre: es el mismo control. */
    static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000021");

    private final CU21CobrarAporte cobro;

    public RecargosDeMoraDiarios(CU21CobrarAporte cobro) {
        this.cobro = cobro;
    }

    @Scheduled(cron = "${aportaya.aportes.trabajos.recargos.cron:0 30 1 * * *}", zone = "America/La_Paz")
    @SchedulerLock(name = "aportes-recargos-mora", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void correr() {
        try {
            SalidaRecargos salida = cobro.generarRecargos(ContextoSesion.deSistema(
                    PROCESO, new Traza(UUID.randomUUID().toString())));
            if (salida.generados() > 0) {
                // Se registra solo cuando hubo algo: ids y conteo, nunca montos ni personas (regla 90.2.1).
                LOG.info("CU-21 · {} recargo(s) por mora generados", salida.generados());
            }
        } catch (RuntimeException fallo) {
            // El dia siguiente retoma: las vencidas sin recargo siguen siendo vencidas sin recargo.
            LOG.warn(
                    "CU-21 · los recargos por mora fallaron: {}",
                    fallo.getClass().getSimpleName());
        }
    }
}
