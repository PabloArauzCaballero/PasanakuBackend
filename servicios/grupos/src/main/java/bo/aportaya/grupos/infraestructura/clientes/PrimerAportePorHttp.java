package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.puertos.PrimerAporteDelCupo;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
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
 * con la identidad de SERVICIO de grupos (F1.L1.M07, regla 98.6: aportes no le contesta a la sesion de quien sortea),
 * tiempo maximo y circuit breaker ({@link ClienteConCircuito}). Sin credencial de servicio no sale la peticion.
 *
 * <p>Fallo cerrado: si aportes no contesta, el circuito esta abierto o la respuesta no se entiende, devuelve
 * {@code empty} y CU-60 deniega (AP-CU60-12). Nunca se completa la lista con «seguro que pagaron».
 */
@Component
public class PrimerAportePorHttp implements PrimerAporteDelCupo {

    private final ClienteConCircuito aportes;
    private final CredencialDeServicio servicio;

    /** El de produccion: con dos constructores, Spring necesita que se le diga cual. */
    @Autowired
    public PrimerAportePorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.aportes}") String urlAportes,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio) {
        this.aportes = new ClienteConCircuito(constructor, urlAportes, "aportes", circuito);
        this.servicio = servicio;
    }

    /** Con los parametros del plan de resiliencia y una credencial a medida: la usan las pruebas del adaptador. */
    PrimerAportePorHttp(RestClient.Builder constructor, String urlAportes, CredencialDeServicio servicio) {
        this(constructor, urlAportes, ConfiguracionDeCircuito.porOmision(), servicio);
    }

    @Override
    public Optional<Set<UUID>> cuposConPrimeraCuotaPagada(UUID grupoId) {
        return servicio.vigente()
                .flatMap(token -> aportes.consultarComoServicio(
                        "/aportes/grupos/" + grupoId + "/primera-cuota", PrimeraCuota.class, token))
                .filter(r -> r.cuposPagados() != null && r.cuposPendientes() != null)
                .map(r -> Set.copyOf(r.cuposPagados()));
    }

    /** El cuerpo del contrato de aportes; lo que no se usa aca se ignora. */
    private record PrimeraCuota(UUID grupoId, UUID periodoId, List<UUID> cuposPagados, List<UUID> cuposPendientes) {}
}
