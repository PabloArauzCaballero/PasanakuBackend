package bo.aportaya.nucleofinanciero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * El punto de arranque de nucleo-financiero. No tiene logica: si aparece un if sobre una
 * regla del pasanaku aca, esta mal ubicado — va a aplicacion/.
 *
 * La configuracion se valida al arrancar: si falta una clave, el proceso NO levanta
 * y dice cual (planes/01 §0.7).
 *
 * <p>{@code @EnableScheduling} (H4.S2.M3): lo necesita {@code ReconciliacionDeRetiros},
 * el job que consulta al proveedor por las ordenes que se quedaron EN_PROCESO — mismo
 * patron que {@code cumplimiento/Aplicacion.java}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class Aplicacion {

    public static void main(String[] argumentos) {
        SpringApplication.run(Aplicacion.class, argumentos);
    }
}
