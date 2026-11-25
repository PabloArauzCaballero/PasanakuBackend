package bo.aportaya.identidad.trabajos;

import bo.aportaya.identidad.aplicacion.CU134GestionarMedida;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Procesa vencimientos de CU-134. El repositorio reparte los lotes entre réplicas. */
@Component
public class VencimientoDeMedidas {

    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000134");
    private static final int TAMANO_LOTE = 500;

    private final CU134GestionarMedida medidas;

    public VencimientoDeMedidas(CU134GestionarMedida medidas) {
        this.medidas = medidas;
    }

    @Scheduled(cron = "${identidad.medidas.cron-vencimiento:0 10 0 * * *}", zone = "America/La_Paz")
    public void correr() {
        var contexto =
                ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
        while (medidas.vencer(contexto) == TAMANO_LOTE) {
            // Cada llamada procesa un lote en una transacción propia.
        }
    }
}
