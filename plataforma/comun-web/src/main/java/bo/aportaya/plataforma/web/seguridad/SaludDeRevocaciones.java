package bo.aportaya.plataforma.web.seguridad;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Sonda de preparacion: una replica cuya lista de revocaciones todavia no esta al dia rechaza todo token con sesion
 * (ADR-030 §3), asi que no deberia recibir trafico. Igual que {@code SaludDeLaBase}, el cuerpo no lleva causas: es
 * publico dentro del cluster.
 */
public class SaludDeRevocaciones implements HealthIndicator {

    private final ListaDeRevocaciones lista;

    public SaludDeRevocaciones(ListaDeRevocaciones lista) {
        this.lista = lista;
    }

    @Override
    public Health health() {
        return lista.estaAlDia()
                ? Health.up().build()
                : Health.down()
                        .withDetail("motivo", "rehidratando la lista de sesiones cerradas")
                        .build();
    }
}
