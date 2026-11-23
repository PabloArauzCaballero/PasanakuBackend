package bo.aportaya.transparencia.infraestructura.clientes;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import bo.aportaya.transparencia.dominio.puertos.VerificacionDeIdentidad;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * El nivel de KYC del titular, preguntado a {@code identidad} ({@code GET /usuarios/{id}/kyc}, el mismo contrato que
 * usa {@code grupos}). Cada quien lee el suyo: el token de quien llama viaja en la llamada. Verificada = un nivel del
 * catalogo por encima de {@code NINGUNO}. Sin respuesta, 4xx, 5xx, cuerpo ilegible o timeout: {@code false}.
 */
@Component
public class IdentidadPorHttp implements VerificacionDeIdentidad {

    private static final Set<String> VERIFICADOS = Set.of("BASICO", "INTERMEDIO", "COMPLETO");

    private final ClienteDeServicio identidad;

    public IdentidadPorHttp(
            RestClient.Builder constructor, @Value("${aportaya.servicios.identidad}") String urlIdentidad) {
        this.identidad = new ClienteDeServicio(constructor, urlIdentidad, "identidad");
    }

    @Override
    public boolean verificada(UUID usuarioId) {
        try {
            return identidad
                    .consultar("/usuarios/" + usuarioId + "/kyc", NivelKyc.class)
                    .map(n -> n.nivel() != null && VERIFICADOS.contains(n.nivel()))
                    .orElse(false);
        } catch (ErrorDeDominio | RestClientException identidadFallo) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NivelKyc(String nivel) {}
}
