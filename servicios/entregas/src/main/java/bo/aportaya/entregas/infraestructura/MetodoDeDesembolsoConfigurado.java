package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.MetodoDeDesembolso;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El adaptador PROVISORIO de {@link MetodoDeDesembolso}: un solo metodo para todas las entregas automaticas, por
 * configuracion. Se reemplaza cuando Pablo decida (DECISION_REQUIRED). Encendida la entrega automatica sin un metodo
 * valido, el proceso NO levanta (regla 90.3.4).
 */
@Component
@ConditionalOnProperty(name = "aportaya.entrega-automatica.habilitada", havingValue = "true")
public class MetodoDeDesembolsoConfigurado implements MetodoDeDesembolso {

    private final String metodo;

    public MetodoDeDesembolsoConfigurado(@Value("${aportaya.entrega-automatica.metodo-desembolso:}") String metodo) {
        if (!METODOS.contains(metodo)) {
            throw new IllegalStateException("aportaya.entrega-automatica.metodo-desembolso tiene que ser uno de "
                    + METODOS + " cuando la entrega automatica esta encendida (DECISION_REQUIRED)");
        }
        this.metodo = metodo;
    }

    @Override
    public Optional<String> para(UUID beneficiarioParticipanteId) {
        return Optional.of(metodo);
    }
}
