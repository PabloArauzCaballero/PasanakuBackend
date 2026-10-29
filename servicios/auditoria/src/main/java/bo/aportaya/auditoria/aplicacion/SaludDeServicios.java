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
            BigDecimal objetivoSlo) {

        /** Sin nada medido: el estado de ahora solo. */
        public Salida(Instant consultadoEn, List<SaludDeServicio> servicios) {
            this(consultadoEn, servicios, Map.of(), OBJETIVO_POR_OMISION);
        }
    }

    /** El SLO acordado el 2026-10-29: 99,5 % en 30 dias para todos los servicios. */
    static final BigDecimal OBJETIVO_POR_OMISION = new BigDecimal("99.5");

    private final SondaDeSalud sonda;
    private final RegistroDeDisponibilidad registro;
    private final Reloj reloj;
    private final BigDecimal objetivoSlo;
    private Salida ultima;

    public SaludDeServicios(
            SondaDeSalud sonda,
            RegistroDeDisponibilidad registro,
            Reloj reloj,
            @Value("${aportaya.salud.objetivo-slo:99.5}") BigDecimal objetivoSlo) {
        if (objetivoSlo.signum() <= 0 || objetivoSlo.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("aportaya.salud.objetivo-slo fuera de (0, 100): " + objetivoSlo);
        }
        this.sonda = sonda;
        this.registro = registro;
        this.reloj = reloj;
        this.objetivoSlo = objetivoSlo;
    }

    public synchronized Salida consultar() {
        Instant ahora = reloj.ahora();
        if (ultima == null || !ahora.isBefore(ultima.consultadoEn().plus(VIGENCIA))) {
            // Primero la red y despues la base: la lectura de lo medido abre su propia transaccion corta.
            List<SaludDeServicio> servicios = sonda.consultar();
            ultima = new Salida(ahora, servicios, registro.enLaVentana(ahora), objetivoSlo);
        }
        return ultima;
    }
}
