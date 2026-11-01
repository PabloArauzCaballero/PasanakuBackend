package bo.aportaya.auditoria.trabajos;

import bo.aportaya.auditoria.aplicacion.RegistroDeDisponibilidad;
import bo.aportaya.auditoria.infraestructura.SondaDeSalud;
import bo.aportaya.plataforma.dominio.Reloj;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El disparador del muestreo de disponibilidad. <b>No decide nada</b>: pregunta el readiness de cada servicio y le
 * pasa lo que contestaron al registro, una vez por minuto.
 *
 * <p>El candado evita que dos instancias de auditoria pregunten a la vez; si igual llegaran a registrar el mismo
 * minuto, la escritura condicionada lo cuenta una sola vez.
 */
@Component
public class MuestreoDeDisponibilidad {

    private final SondaDeSalud sonda;
    private final RegistroDeDisponibilidad registro;
    private final Reloj reloj;

    public MuestreoDeDisponibilidad(SondaDeSalud sonda, RegistroDeDisponibilidad registro, Reloj reloj) {
        this.sonda = sonda;
        this.registro = registro;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${aportaya.salud.muestreo-cron:0 * * * * *}", zone = "UTC")
    @SchedulerLock(name = "auditoria.salud.muestreo", lockAtMostFor = "PT50S")
    public void correr() {
        // La hora se toma antes de preguntar: el minuto es el que se midio, no el que termino la consulta.
        var minuto = reloj.ahora();
        registro.registrar(sonda.consultar(), minuto);
    }
}
