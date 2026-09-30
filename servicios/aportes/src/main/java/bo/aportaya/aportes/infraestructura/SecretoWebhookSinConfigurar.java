package bo.aportaya.aportes.infraestructura;

import bo.aportaya.aportes.dominio.ProveedorDeSecretoWebhook;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail closed fuera de {@code local}/{@code test}: no hay adaptador de vault real en
 * el repo (ver {@link ProveedorDeSecretoWebhook}), y NO ARRANCAR el servicio entero
 * por esa falta seria desproporcionado — el resto de {@code aportes} no depende del
 * webhook. En vez de eso, este bean es el unico del puerto fuera de
 * {@code local}/{@code test} y siempre devuelve {@link Optional#empty()}: todo
 * webhook real se verifica contra un secreto ausente, que nunca coincide, y por lo
 * tanto cada webhook se rechaza con 401 hasta que exista el adaptador real.
 *
 * <p>{@code DECISION_REQUIRED}: adoptar un cliente de vault/HSM real es una decision
 * de arquitectura (Pablo) que este carril de seguridad transversal no toma por su
 * cuenta (regla 00) — se documenta en {@code carriles/PR4-seguridad.md} §H1.S2.
 */
@Component
@Profile("!local & !test")
public class SecretoWebhookSinConfigurar implements ProveedorDeSecretoWebhook {

    @Override
    public Optional<String> secretoDe(String proveedorCodigo) {
        return Optional.empty();
    }
}
