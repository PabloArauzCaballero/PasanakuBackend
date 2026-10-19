package bo.aportaya.identidad.infraestructura.clientes;

import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lo que identidad le pide a {@code organizador} para el contrato Aprendiz (CU-90 vía básica).
 * Se pregunta, no se lee su esquema (98.1); el token de la persona viaja en la llamada.
 * Ante la duda (sin respuesta, 4xx) la respuesta es «no».
 */
@Component
public class OrganizadorPorHttp {

    private final ClienteDeServicio organizador;

    public OrganizadorPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.organizador:http://organizador:8080}") String url) {
        this.organizador = new ClienteDeServicio(constructor, url, "organizador");
    }

    /** Pide a organizador que firme el contrato con el token de firma que emitió identidad. */
    public boolean firmar(UUID contratoId, UUID tokenFirmaId) {
        return organizador
                .pedir(
                        "/organizadores/yo/habilitacion-basica/firma",
                        Map.of("contratoId", contratoId.toString(), "tokenFirmaId", tokenFirmaId.toString()),
                        Firma.class)
                .isPresent();
    }

    /** {@code true} solo si organizador responde que la persona está habilitada con contrato firmado. */
    public boolean habilitadoConContrato() {
        return organizador
                .consultar("/organizadores/yo/habilitacion-basica", Estado.class)
                .map(Estado::habilitado)
                .orElse(false);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Firma(boolean firmadoAhora) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Estado(boolean habilitado) {}
}
