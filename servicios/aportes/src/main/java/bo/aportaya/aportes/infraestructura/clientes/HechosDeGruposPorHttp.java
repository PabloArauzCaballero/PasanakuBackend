package bo.aportaya.aportes.infraestructura.clientes;

import bo.aportaya.aportes.aplicacion.HechosDeGrupos;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a `grupos` por su contrato, con el token de quien paga (el destino aplica SU politica de
 * fila). Si grupos no contesta, vacio: para dinero eso se rechaza, no se asume que si.
 */
@Component
public class HechosDeGruposPorHttp implements HechosDeGrupos {
    private final ClienteDeServicio grupos;

    public HechosDeGruposPorHttp(RestClient.Builder constructor, @Value("${aportaya.servicios.grupos}") String url) {
        this.grupos = new ClienteDeServicio(constructor, url, "grupos");
    }

    @Override
    public Optional<Admisibilidad> admisibilidad(UUID participanteId, UUID periodoId) {
        return grupos.consultar(
                        "/grupos/obligaciones/admisibilidad?participanteId=" + participanteId + "&periodoId="
                                + periodoId,
                        Respuesta.class)
                .map(r -> new Admisibilidad(r.esDelUsuario(), r.periodoAbierto()));
    }

    private record Respuesta(boolean esDelUsuario, boolean periodoAbierto) {}
}
