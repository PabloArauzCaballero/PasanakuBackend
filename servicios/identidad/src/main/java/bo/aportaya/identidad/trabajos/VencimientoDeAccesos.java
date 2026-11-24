package bo.aportaya.identidad.trabajos;

import bo.aportaya.identidad.aplicacion.CU08VencerAccesos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El disparador diario del vencimiento de accesos temporales (CU-08 flujo 6). <b>No decide nada</b>: arma el
 * contexto de sistema y llama al caso de uso por lotes (patron {@code ControlDePlanesVencidos} de cumplimiento).
 *
 * <p><b>Sin lock de planificador, y es correcto igual</b>: las asignaciones se toman {@code FOR UPDATE SKIP
 * LOCKED} y la escritura exige {@code revocada_en IS NULL}, asi que dos replicas se reparten el trabajo y
 * ninguna vence dos veces la misma asignacion.
 */
@Component
public class VencimientoDeAccesos {

    private static final Logger LOG = LoggerFactory.getLogger(VencimientoDeAccesos.class);

    /** Identifica al proceso en la bitacora. Es el mismo siempre: es el mismo control. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000008");

    private final CU08VencerAccesos vencer;
    private final int lote;

    public VencimientoDeAccesos(
            CU08VencerAccesos vencer, @Value("${identidad.accesos.lote-de-vencimiento:500}") int lote) {
        this.vencer = vencer;
        this.lote = lote;
    }

    @Scheduled(cron = "${identidad.accesos.cron-vencimiento:0 5 0 * * *}", zone = "America/La_Paz")
    public void correr() {
        int total = 0;
        int enEsteLote;
        do {
            enEsteLote = vencer.ejecutar(ContextoSesion.deSistema(
                    PROCESO, new Traza(UUID.randomUUID().toString())));
            total += enEsteLote;
        } while (enEsteLote >= lote);
        if (total > 0) {
            // Solo cuando hubo algo: un control que anuncia todos los dias que no encontro nada entrena a no leerlo.
            LOG.warn("CU-08 · {} acceso(s) temporal(es) vencido(s) y cerrado(s)", total);
        }
    }
}
