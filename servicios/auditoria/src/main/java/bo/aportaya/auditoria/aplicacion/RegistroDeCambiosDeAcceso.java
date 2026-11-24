package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio.CambioDeAcceso;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GU-07 (P-5) · del lado de {@code auditoria}: cada cambio de acceso que publica identidad (alta, edicion, roles,
 * desbloqueo, asignaciones y revocaciones de CU-08) queda en la bitacora inmutable {@code comun.bitacora_evento}, la
 * misma que sirve {@code GET /auditoria/bitacora}.
 *
 * <p><b>Una vez por evento.</b> La entrega es al menos una vez (ADR-018): el {@code event_id} se anota en
 * {@code auditoria.evento_consumido} en la MISMA transaccion que la fila de la bitacora. Si el insert falla, tampoco
 * queda anotado y el reintento lo vuelve a intentar; si ya estaba, no se escribe nada (regla 96.7.3).
 */
@Service
public class RegistroDeCambiosDeAcceso {

    public static final String CONSUMIDOR = "auditoria.cambios-de-acceso";

    /** El «usuario» del proceso en {@code app.usuario_id}: un consumidor no es una persona. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000007");

    public enum Resultado {
        REGISTRADO,
        DUPLICADO
    }

    private final Datos datos;
    private final BitacoraRepositorio bitacora;
    private final Consumidos consumidos = new Consumidos("auditoria");

    public RegistroDeCambiosDeAcceso(Datos datos, BitacoraRepositorio bitacora) {
        this.datos = datos;
        this.bitacora = bitacora;
    }

    @Transactional
    public Resultado registrar(UUID eventoId, CambioDeAcceso cambio) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(cambio.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, eventoId, CONSUMIDOR)) {
                return Resultado.DUPLICADO;
            }
            bitacora.registrarCambioDeAcceso(dsl, cambio);
            return Resultado.REGISTRADO;
        });
    }
}
