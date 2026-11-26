package bo.aportaya.aportes.infraestructura.pasarela.simulada;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail-closed (regla 90.3.4): la pasarela SIMULADA solo es legitima en {@code local} y {@code test}. En cualquier
 * otro perfil (staging, production, sin perfil) un modo {@code simulada} es un error de despliegue — daria por
 * «cobradas» ordenes que ningun banco vio — y el arranque se niega diciendo que propiedad cambiar.
 */
@Component
@Profile("!local & !test")
@ConditionalOnProperty(name = "aportaya.aportes.pasarela.modo", havingValue = "simulada", matchIfMissing = true)
public class GuardiaDePasarelaSimulada {

    public GuardiaDePasarelaSimulada() {
        throw new IllegalStateException(
                "aportaya.aportes.pasarela.modo=simulada fuera de los perfiles local/test: la pasarela simulada no cobra"
                        + " dinero real. Use aportaya.aportes.pasarela.modo=real (adaptador en infraestructura/pasarela/real)"
                        + " o ejecute con el perfil local/test.");
    }
}
