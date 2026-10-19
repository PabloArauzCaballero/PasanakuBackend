package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.infraestructura.SondaDeSalud;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * operacion/estado · la salud de cada servicio, consultada AHORA (reemplaza la constante «arriba» de la pantalla).
 * La ultima consulta se reusa {@link #VIGENCIA}: abrir la pantalla muchas veces seguidas no multiplica las catorce
 * llamadas internas. Lo que vuelve lleva la hora en que se pregunto, para que la pantalla diga de cuando es.
 */
@Service
public class SaludDeServicios {

    static final Duration VIGENCIA = Duration.ofSeconds(10);

    public record Salida(Instant consultadoEn, List<SaludDeServicio> servicios) {}

    private final SondaDeSalud sonda;
    private final Reloj reloj;
    private Salida ultima;

    public SaludDeServicios(SondaDeSalud sonda, Reloj reloj) {
        this.sonda = sonda;
        this.reloj = reloj;
    }

    public synchronized Salida consultar() {
        Instant ahora = reloj.ahora();
        if (ultima == null || !ahora.isBefore(ultima.consultadoEn().plus(VIGENCIA))) {
            ultima = new Salida(ahora, sonda.consultar());
        }
        return ultima;
    }
}
