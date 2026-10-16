package bo.aportaya.inversiones.trabajos;

import bo.aportaya.inversiones.aplicacion.OperacionesProgramables;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada minuto reintenta las instrucciones al libro que quedaron pendientes (libro caido al
 * retener, debitar, liberar o acreditar). <b>No decide nada</b>: arma el contexto de sistema y
 * llama a {@link OperacionesProgramables#reintentarInstrucciones}, que aplica la politica de
 * espera exponencial y el tope de intentos.
 *
 * <p>Si no hay pendientes no abre corrida: una fila por minuto de "no hice nada" entrenaria a
 * que nadie mire el historial. Si hay, la corrida queda registrada con su resultado, y las
 * instrucciones que pasaron el tope se ven en el medidor {@code inversiones.instrucciones.atascadas}
 * y en el campo {@code atascadas} de la corrida.
 *
 * <p>El {@code lockAtMostFor} es mayor que el plazo maximo del barrido: un proceso que muere a
 * mitad no deja el candado tomado para siempre, y dos replicas no corren a la vez. Aunque el
 * candado fallara, repetir es seguro: el libro recibe la MISMA clave de idempotencia.
 */
@Component
@ConditionalOnProperty(name = "aportaya.inversiones.trabajos.habilitados", havingValue = "true", matchIfMissing = true)
public class ReintentoDeInstrucciones {

    private static final Logger LOG = LoggerFactory.getLogger(ReintentoDeInstrucciones.class);
    private static final DateTimeFormatter MINUTO = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final OperacionesProgramables operaciones;
    private final Reloj reloj;

    public ReintentoDeInstrucciones(OperacionesProgramables operaciones, Reloj reloj) {
        this.operaciones = operaciones;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${aportaya.inversiones.trabajos.reintento.cron:0 * * * * *}", zone = "America/La_Paz")
    @SchedulerLock(name = "inversiones.reintento-instrucciones", lockAtMostFor = "PT2M", lockAtLeastFor = "PT5S")
    public void correr() {
        if (!operaciones.hayPendientes()) {
            return;
        }
        String clave = "REINT:" + MINUTO.format(ZonedDateTime.ofInstant(reloj.ahora(), Reloj.ZONA));
        try {
            var r = operaciones.reintentarDelReloj(clave);
            if (!"0".equals(r.get("atascadas"))) {
                LOG.warn(
                        "inversiones · {} instruccion(es) al libro atascadas (pasaron el maximo de intentos); revisar el libro",
                        r.get("atascadas"));
            }
        } catch (RuntimeException fallo) {
            // Una vuelta fallida no tumba el planificador: queda en corrida_trabajo y se reintenta en la proxima.
            LOG.warn(
                    "inversiones · el reintento de instrucciones fallo: {}",
                    fallo.getClass().getSimpleName());
        }
    }
}
