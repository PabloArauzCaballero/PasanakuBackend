package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.DisponibilidadDelServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.infraestructura.SondaDeSalud;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * operacion/estado · la salud de cada servicio, consultada AHORA (reemplaza la constante «arriba» de la pantalla).
 * La ultima consulta se reusa {@link #VIGENCIA}: abrir la pantalla muchas veces seguidas no multiplica las catorce
 * llamadas internas. Lo que vuelve lleva la hora en que se pregunto, para que la pantalla diga de cuando es.
 *
 * <p>sistemas/servicios · junto al estado de ahora va lo medido en la ventana del SLO ({@link RegistroDeDisponibilidad})
 * y el objetivo contra el que se mide el presupuesto de error.
 */
@Service
public class SaludDeServicios {

    static final Duration VIGENCIA = Duration.ofSeconds(10);

    /** {@code disponibilidad} trae solo los servicios con algo medido; el resto no tiene dato todavia. */
    public record Salida(
            Instant consultadoEn,
            List<SaludDeServicio> servicios,
            Map<String, DisponibilidadDelServicio> disponibilidad,
            BigDecimal objetivoSlo) {}

    private final SondaDeSalud sonda;
    private final RegistroDeDisponibilidad registro;
    private final Reloj reloj;
    private final BigDecimal objetivoSlo;
    // Un candado y no `synchronized`: auditoria corre en hilos virtuales (ADR-015) y en Java 21 un hilo virtual que
    // espera dentro de `synchronized` inmoviliza su hilo portador. Con 0,5 CPU hay UN solo portador: la peticion que
    // esperaba la sonda dejaba sin donde correr a la consulta de auditoria a si misma, que vencia el plazo y salia
    // CAIDO en la pantalla («Caidos 1» con el servicio sano, medido en TEST el 2026-10-29).
    private final ReentrantLock candado = new ReentrantLock();
    private Salida ultima;

    public SaludDeServicios(
            SondaDeSalud sonda,
            RegistroDeDisponibilidad registro,
            Reloj reloj,
            // El objetivo sale de application.yml (SALUD_OBJETIVO_SLO) y de ningun otro lado: sin la propiedad el
            // servicio no arranca, en vez de arrancar con una cifra escondida en el codigo.
            @Value("${aportaya.salud.objetivo-slo}") BigDecimal objetivoSlo) {
        DisponibilidadDelServicio.exigirObjetivoValido(objetivoSlo);
        this.sonda = sonda;
        this.registro = registro;
        this.reloj = reloj;
        this.objetivoSlo = objetivoSlo;
    }

    public Salida consultar() {
        candado.lock();
        try {
            Instant ahora = reloj.ahora();
            if (ultima == null || !ahora.isBefore(ultima.consultadoEn().plus(VIGENCIA))) {
                // Primero la red y despues la base: la lectura de lo medido abre su propia transaccion corta.
                List<SaludDeServicio> servicios = sonda.consultar();
                ultima = new Salida(ahora, servicios, registro.enLaVentana(ahora), objetivoSlo);
            }
            return ultima;
        } finally {
            candado.unlock();
        }
    }
}
