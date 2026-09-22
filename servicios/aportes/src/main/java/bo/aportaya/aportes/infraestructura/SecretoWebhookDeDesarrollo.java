package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.ProveedorDeSecretoWebhook;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Doble de la pasarela (Q-02, DECIDIDA 2026-09-21): un secreto FIJO por entorno de
 * prueba, nunca un secreto por proveedor real ni leido de un vault.
 *
 * <p>Patron identico a {@code DesafioDeDesarrollo} de {@code identidad}
 * (confinamiento por {@code @Profile}): solo existe en {@code local}/{@code test}. En
 * cualquier otro perfil, {@link SecretoWebhookSinConfigurar} es el unico bean del
 * puerto y siempre falla cerrado.
 */
@Component
@Profile({"local", "test"})
public class SecretoWebhookDeDesarrollo implements ProveedorDeSecretoWebhook {

    private final String secreto;

    public SecretoWebhookDeDesarrollo(
            @Value("${aportaya.aportes.webhook.secreto-de-prueba:secreto-webhook-de-prueba-local}") String secreto) {
        this.secreto = secreto;
    }

    @Override
    public Optional<String> secretoDe(String proveedorCodigo) {
        return Optional.of(secreto);
    }
}
