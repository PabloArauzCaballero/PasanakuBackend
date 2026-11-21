package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.ProveedorDeSecretoWebhook;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * F1.L1.M05 (ANF-SEG-001) · el secreto con que la pasarela firma su webhook, leido del entorno
 * ({@code APORTES_WEBHOOK_SECRETO}) en TODOS los perfiles.
 *
 * <p>Antes habia dos beans por perfil: uno de desarrollo con un secreto literal por omision en
 * {@code src/main}, y uno que fallaba cerrado fuera de {@code local}/{@code test}. TEST corria con
 * {@code local}, asi que el secreto literal era el de un entorno desplegado. Ahora no hay literal ni valor
 * por omision: si la variable falta o es corta, el servicio NO arranca y el mensaje nombra la variable
 * (regla 90.3.4). Un secreto mal configurado se ve al desplegar, no cuando un webhook legitimo rebota.
 *
 * <p>Un solo secreto para todos los proveedores mientras haya uno (P-15: todavia no hay pasarela
 * contratada). El segundo proveedor (tanda 8) trae su propia variable.
 */
@Component
public class SecretoWebhookDeEntorno implements ProveedorDeSecretoWebhook {

    /** HMAC-SHA256: menos de 32 caracteres es una clave adivinable, no un secreto. */
    static final int LARGO_MINIMO = 32;

    private final String secreto;

    public SecretoWebhookDeEntorno(@Value("${aportaya.aportes.webhook.secreto}") String secreto) {
        this.secreto = exigir(secreto);
    }

    static String exigir(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException(
                    "Falta APORTES_WEBHOOK_SECRETO (aportaya.aportes.webhook.secreto): sin el, aportes no puede"
                            + " verificar la firma de la pasarela y no arranca.");
        }
        if (secreto.length() < LARGO_MINIMO) {
            throw new IllegalStateException("APORTES_WEBHOOK_SECRETO tiene menos de " + LARGO_MINIMO
                    + " caracteres: no es un secreto, es una clave adivinable.");
        }
        return secreto;
    }

    @Override
    public Optional<String> secretoDe(String proveedorCodigo) {
        return Optional.of(secreto);
    }
}
