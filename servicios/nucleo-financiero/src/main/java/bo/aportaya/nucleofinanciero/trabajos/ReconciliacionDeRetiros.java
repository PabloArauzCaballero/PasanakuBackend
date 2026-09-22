package bo.aportaya.nucleofinanciero.trabajos;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * H4.S2.M3 · resuelve las ordenes que {@code instruirPago} dejo {@code EN_PROCESO} sin
 * saber que paso de verdad.
 *
 * <p>{@code ProveedorDeRetiro.Estado.TIMEOUT} (ver {@link ProveedorDeRetiro}) es
 * deliberadamente distinto de {@code RECHAZADO}: significa "no se sabe todavia", no
 * "no salio". Asumir un rechazo ahi de vuelta la plata a disponible mientras el
 * proveedor la esta pagando de verdad — doble pago en potencia. Este job es la unica
 * pieza del sistema autorizada a cerrar esa incertidumbre, preguntandole al proveedor
 * por cada referencia pendiente y aplicando la respuesta con el mismo camino que usa
 * cualquier otra confirmacion o rechazo (invariante 6: la llamada de red, fuera de la
 * transaccion de cada orden; cada orden resuelta en SU PROPIA transaccion, para que una
 * que falle no le pise la reconciliacion a las demas).
 *
 * <p><b>No decide nada nuevo</b>: llama a {@link CU11RetirarSaldo#confirmarPago} o
 * {@link CU11RetirarSaldo#rechazar}, exactamente lo que un webhook del proveedor
 * hubiera disparado si hubiera llegado a tiempo. Si el proveedor sigue sin saber
 * (`TIMEOUT` de nuevo), la orden queda como esta — la proxima corrida vuelve a
 * preguntar.
 */
@Component
public class ReconciliacionDeRetiros {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliacionDeRetiros.class);

    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000011");

    private final ProveedorDeRetiro proveedor;
    private final CU11RetirarSaldo retiroCU;

    public ReconciliacionDeRetiros(ProveedorDeRetiro proveedor, CU11RetirarSaldo retiroCU) {
        this.proveedor = proveedor;
        this.retiroCU = retiroCU;
    }

    @Scheduled(cron = "${aportaya.retiro.reconciliacion.cron:0 */5 * * * *}", zone = "America/La_Paz")
    public void correr() {
        ContextoSesion sistema =
                ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
        List<OrdenRetiroRepositorio.Orden> pendientes = retiroCU.ordenesEnProceso(sistema);

        int resueltas = 0;
        int siguenPendientes = 0;
        for (OrdenRetiroRepositorio.Orden orden : pendientes) {
            String referencia = orden.referenciaProveedor().orElse(null);
            if (referencia == null) {
                // No debería pasar (pasarAEnProceso siempre la guarda), pero una orden
                // EN_PROCESO sin referencia no tiene con qué preguntar: se declara y se
                // salta, no se rechaza a ciegas.
                LOG.warn("CU-11 reconciliacion · orden {} esta EN_PROCESO sin referencia_proveedor", orden.id());
                continue;
            }
            ProveedorDeRetiro.Estado estado = proveedor.consultar(referencia);
            try {
                switch (estado) {
                    case ACEPTADO -> {
                        retiroCU.confirmarPago(orden.id(), sistema);
                        resueltas++;
                    }
                    case RECHAZADO -> {
                        retiroCU.rechazar(orden.id(), "Reconciliacion: el proveedor confirmo el rechazo", sistema);
                        resueltas++;
                    }
                    case TIMEOUT -> siguenPendientes++;
                }
            } catch (RuntimeException fallo) {
                // Una orden que otro proceso ya resolvio entre el SELECT y este punto
                // (carrera con un webhook que sí llegó) no puede tumbar la corrida
                // entera: se registra y se sigue con las demás.
                LOG.warn("CU-11 reconciliacion · orden {} no se pudo resolver: {}", orden.id(), fallo.getMessage());
            }
        }
        if (resueltas > 0 || siguenPendientes > 0) {
            LOG.info(
                    "CU-11 reconciliacion · {} orden(es) resuelta(s), {} siguen EN_PROCESO (TIMEOUT del proveedor)",
                    resueltas,
                    siguenPendientes);
        }
    }
}
