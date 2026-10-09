package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * La implementacion de {@link HechosDeGrupos} mientras {@code grupos} no exponga el contrato.
 *
 * <p>Contesta SIEMPRE «no se pudo saber», y por eso el mercado deniega todo: ni se publica, ni se
 * lista, ni se compra. Es a proposito. Inventar un adaptador contra rutas de {@code grupos} que no
 * existen seria presentar como hecho lo que es una pregunta abierta (regla 00); y suplir el dato
 * con lo que diga el cliente dejaria vender turnos ajenos. Cuando {@code grupos} publique el
 * contrato, se reemplaza este bean por un adaptador HTTP con timeout, y las pruebas existentes de
 * tres niveles —que ya corren contra un doble— pasan a correr tambien contra el.
 */
@Component
public class GruposNoDisponible implements HechosDeGrupos {

    @Override
    public Optional<Turno> turno(UUID turnoId) {
        return Optional.empty();
    }

    @Override
    public Optional<Miembro> miembro(UUID grupoId, UUID usuarioId) {
        return Optional.empty();
    }

    @Override
    public Optional<Set<UUID>> gruposActivosDe(UUID usuarioId) {
        return Optional.empty();
    }
}
