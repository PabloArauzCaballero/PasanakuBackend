package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.plataforma.dominio.Dinero;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * La implementacion de {@link FondosDelComprador} mientras {@code nucleo-financiero} no defina
 * «ejecutar una retencion a favor de un tercero».
 *
 * <p>Deniega todo ({@code NO_DISPONIBLE}): nadie retiene ni paga sin una operacion real del libro.
 * Una compra queda detenida en su primer paso, sin dar el titulo, y reanudable. No hay un
 * adaptador contra rutas inexistentes (regla 00): el contrato esta propuesto en el puerto.
 */
@Component
public class FondosNoDisponibles implements FondosDelComprador {

    @Override
    public Referencia retener(UUID compradorUsuarioId, Dinero monto, String clave) {
        return new Referencia(Resultado.NO_DISPONIBLE, null);
    }

    @Override
    public Referencia pagarAlVendedor(
            UUID retencionRef, UUID vendedorUsuarioId, Dinero paraElVendedor, Dinero cargos, String clave) {
        return new Referencia(Resultado.NO_DISPONIBLE, null);
    }

    @Override
    public Resultado liberar(UUID retencionRef, String clave) {
        return Resultado.NO_DISPONIBLE;
    }
}
