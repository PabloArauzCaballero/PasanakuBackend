package bo.aportaya.entregas.trabajos;

import bo.aportaya.entregas.aplicacion.EntregaAutomatica;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retoma las sagas de entrega automatica que quedaron esperando: el turno llego despues del aviso, o {@code aportes}
 * no respondio. Una sola replica a la vez (ShedLock, ADR-018); aunque corrieran dos, el fondeo es unico por turno.
 */
@Component
@ConditionalOnProperty(name = "aportaya.entrega-automatica.habilitada", havingValue = "true")
public class BarridoDeEntregaAutomatica {

    private final EntregaAutomatica entregaAutomatica;
    private final Duration espera;
    private final int tanda;

    public BarridoDeEntregaAutomatica(
            EntregaAutomatica entregaAutomatica,
            @Value("${aportaya.entrega-automatica.espera-entre-intentos}") Duration espera,
            @Value("${aportaya.entrega-automatica.tanda}") int tanda) {
        this.entregaAutomatica = entregaAutomatica;
        this.espera = espera;
        this.tanda = tanda;
    }

    @Scheduled(fixedDelayString = "${aportaya.entrega-automatica.intervalo-del-barrido}")
    @SchedulerLock(name = "entregas.entrega-automatica.barrido", lockAtMostFor = "PT5M")
    public void barrer() {
        entregaAutomatica.barrer(espera, tanda);
    }
}
