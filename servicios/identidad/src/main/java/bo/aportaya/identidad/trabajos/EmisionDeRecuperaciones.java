package bo.aportaya.identidad.trabajos;

import bo.aportaya.identidad.aplicacion.RecuperacionesDeAcceso;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Emite la invitacion de las recuperaciones aprobadas cuyo enfriamiento ya se cumplio (P-4, CU-09 §11).
 *
 * <p>Idempotente y seguro con varias replicas: cada fila se toma con {@code FOR UPDATE SKIP LOCKED} y la
 * transicion {@code APROBADA -> EMITIDA} lleva su precondicion en el {@code UPDATE}. Correrlo dos veces no
 * emite dos invitaciones.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.acceso.recuperacion.emision-habilitada",
        havingValue = "true",
        matchIfMissing = true)
public class EmisionDeRecuperaciones {

    private final RecuperacionesDeAcceso recuperaciones;

    public EmisionDeRecuperaciones(RecuperacionesDeAcceso recuperaciones) {
        this.recuperaciones = recuperaciones;
    }

    @Scheduled(fixedDelayString = "${aportaya.acceso.recuperacion.intervalo:PT1M}")
    public void emitir() {
        recuperaciones.emitirLasQueYaPuedenSalir(new Traza(UUID.randomUUID().toString()));
    }
}
