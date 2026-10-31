package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.puertos.PrimerAporteDelCupo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link PrimerAporteDelCupo} preguntado a `aportes` por su contrato ({@code GET /aportes/grupos/{grupoId}/primera-cuota}),
 * con el token de quien sortea, tiempo maximo y circuit breaker ({@link ClienteConCircuito}).
 *
 * <p>Fallo cerrado: si aportes no contesta, el circuito esta abierto o la respuesta no se entiende, devuelve
 * {@code empty} y CU-60 deniega (AP-CU60-12). Nunca se completa la lista con «seguro que pagaron».
 */
@Component
public class PrimerAportePorHttp implements PrimerAporteDelCupo {

    private final ClienteConCircuito aportes;

    /** El de produccion: con dos constructores, Spring necesita que se le diga cual. */
    @Autowired
    public PrimerAportePorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.aportes}") String urlAportes,
            ConfiguracionDeCircuito circuito) {
        this.aportes = new ClienteConCircuito(constructor, urlAportes, "aportes", circuito);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    PrimerAportePorHttp(RestClient.Builder constructor, String urlAportes) {
        this(constructor, urlAportes, ConfiguracionDeCircuito.porOmision());
    }

    @Override
    public Optional<Set<UUID>> cuposConPrimeraCuotaPagada(UUID grupoId) {
        return aportes.consultar("/aportes/grupos/" + grupoId + "/primera-cuota", PrimeraCuota.class)
                .filter(r -> r.cuposPagados() != null && r.cuposPendientes() != null)
                .map(r -> Set.copyOf(r.cuposPagados()));
    }

    /** El cuerpo del contrato de aportes; lo que no se usa aca se ignora. */
    private record PrimeraCuota(UUID grupoId, UUID periodoId, List<UUID> cuposPagados, List<UUID> cuposPendientes) {}
}
