package bo.aportaya.inversiones.trabajos;

import bo.aportaya.inversiones.aplicacion.OperacionesProgramables;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.LocalDate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Devenga los DPF abiertos de la fecha de hoy (hora de La Paz), una vez al dia, de madrugada.
 * <b>No decide nada</b>: llama a {@link OperacionesProgramables#devengar}.
 *
 * <p>La clave de la corrida es la fecha: una segunda replica, o un segundo disparo del mismo dia,
 * recibe el resultado guardado y no corre otra vez. Si un dia falla (aliado, base), la corrida
 * queda {@code FALLIDA} con su error y se retoma con la misma clave en el siguiente disparo; si
 * el servicio estuvo caido varios dias, el devengo del dia en que vuelve cubre la diferencia (es
 * la resta de dos acumulados), de modo que no se pierde un centavo.
 */
@Component
@ConditionalOnProperty(name = "aportaya.inversiones.trabajos.habilitados", havingValue = "true", matchIfMissing = true)
public class DevengoDiario {

    private static final Logger LOG = LoggerFactory.getLogger(DevengoDiario.class);

    private final OperacionesProgramables operaciones;
    private final Reloj reloj;

    public DevengoDiario(OperacionesProgramables operaciones, Reloj reloj) {
        this.operaciones = operaciones;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${aportaya.inversiones.trabajos.devengo.cron:0 30 0 * * *}", zone = "America/La_Paz")
    @SchedulerLock(name = "inversiones.devengo-diario", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void correr() {
        LocalDate hoy = reloj.hoy();
        try {
            var r = operaciones.devengar(hoy, "DEV:" + hoy, OperacionesProgramables.PROGRAMADO, operaciones.sistema());
            LOG.info(
                    "inversiones · devengo del {}: {} nuevo(s), {} ya devengado(s)",
                    hoy,
                    r.get("devengadas"),
                    r.get("yaDevengadas"));
        } catch (RuntimeException fallo) {
            LOG.warn(
                    "inversiones · el devengo del {} fallo: {}",
                    hoy,
                    fallo.getClass().getSimpleName());
        }
    }
}
