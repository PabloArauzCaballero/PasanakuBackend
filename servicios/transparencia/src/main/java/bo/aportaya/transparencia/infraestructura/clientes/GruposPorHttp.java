package bo.aportaya.transparencia.infraestructura.clientes;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Lo que transparencia le pregunta a {@code grupos} para el perfil del integrante (2026-10-21): si quien llama y otra
 * persona estan en el mismo grupo. Se pregunta, no se lee su esquema (98.1); el token de quien llama viaja en la
 * llamada y grupos decide con el. Ante la duda (sin respuesta, 4xx, 5xx, cuerpo ilegible, timeout) la respuesta es «no».
 */
@Component
public class GruposPorHttp {

    private final ClienteDeServicio grupos;

    public GruposPorHttp(RestClient.Builder constructor, @Value("${aportaya.servicios.grupos}") String urlGrupos) {
        this.grupos = new ClienteDeServicio(constructor, urlGrupos, "grupos");
    }

    /** {@code true} solo si grupos responde que quien llama y {@code usuarioId} son integrantes de {@code grupoId}. */
    public boolean compartenGrupo(UUID grupoId, UUID usuarioId) {
        try {
            return grupos.consultar("/grupos/" + grupoId + "/integrantes/" + usuarioId + "/comparte", Pertenencia.class)
                    .map(p -> Boolean.TRUE.equals(p.comparten()))
                    .orElse(false);
        } catch (ErrorDeDominio | RestClientException grupoFallo) {
            // 5xx o cuerpo ilegible: no se sabe, y no saber es «no» (falla cerrado). El perfil responde 403.
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Pertenencia(Boolean comparten) {}
}
