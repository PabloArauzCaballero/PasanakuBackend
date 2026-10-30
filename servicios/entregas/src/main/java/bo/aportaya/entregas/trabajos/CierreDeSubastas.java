package bo.aportaya.entregas.trabajos;

import bo.aportaya.entregas.aplicacion.SubastaDelCupo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El disparador del cierre de las subastas del cupo. <b>No decide nada</b>: arma el contexto de sistema y llama al
 * caso de uso, que cierra las vencidas, sigue las que quedaron a mitad (traspaso, pago) y devuelve lo superado.
 *
 * <p>Sin lock de planificador, y es correcto igual: cada paso es una transicion condicionada al estado y a la version
 * con la subasta bloqueada, y cada llamada remota lleva una clave que sale de la subasta o de la puja. Dos replicas que
 * corren a la vez se pisan el trabajo, no el dinero.
 */
@Component
public class CierreDeSubastas {

    private static final Logger LOG = LoggerFactory.getLogger(CierreDeSubastas.class);

    /** Identifica al proceso en la bitacora. Es el mismo siempre: es el mismo cierre. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000136");

    private final SubastaDelCupo subasta;

    public CierreDeSubastas(SubastaDelCupo subasta) {
        this.subasta = subasta;
    }

    @Scheduled(
            fixedDelayString = "${aportaya.subastas.cierre-cada:PT1M}",
            initialDelayString = "${aportaya.subastas.cierre-cada:PT1M}")
    public void correr() {
        int avanzadas = subasta.correrCierres(
                ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString())));
        if (avanzadas > 0) {
            // Solo cuando hubo algo: un cierre que anuncia cada minuto que no encontro nada entrena a que nadie lo lea.
            LOG.info("subastas: {} avanzada(s)", avanzadas);
        }
    }
}
