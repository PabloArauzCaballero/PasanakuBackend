package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.PrimeraCuotaDelGrupo;
import bo.aportaya.aportes.infraestructura.PrimeraCuotaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que este servicio le contesta a {@code grupos} sobre la cuota 1 de un grupo.
 *
 * <p>No es un caso de uso de la boveda: es la consulta que hace posible que CU-60 exija la primera cuota pagada
 * (R-GRP-20) sin leer este esquema (invariante 11) y sin que el dato lo afirme quien quiere sortear.
 *
 * <p>Solo lee, y aun asi abre transaccion: el contexto viaja con {@code SET LOCAL} dentro de ella.
 */
@Service
public class ConsultarPrimeraCuota {

    private final Datos datos;
    private final PrimeraCuotaRepositorio primeraCuota;

    public ConsultarPrimeraCuota(Datos datos, PrimeraCuotaRepositorio primeraCuota) {
        this.datos = datos;
        this.primeraCuota = primeraCuota;
    }

    @Transactional(readOnly = true)
    public PrimeraCuotaDelGrupo delGrupo(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> primeraCuota.delGrupo(dsl, grupoId));
    }
}
