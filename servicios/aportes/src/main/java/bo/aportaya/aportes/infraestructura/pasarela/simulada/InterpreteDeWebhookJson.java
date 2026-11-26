package bo.aportaya.aportes.infraestructura.pasarela.simulada;

import bo.aportaya.aportes.dominio.InterpreteDeWebhook;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El formato JSON plano que usa hoy aportes y que emite {@link PasarelaSimulada}:
 * {@code {"evento","idEvento","referenciaProveedor","monto","moneda"}}. Es el contrato del DOBLE, no el de
 * ningun proveedor real: el adaptador real trae su propio {@link InterpreteDeWebhook}.
 *
 * <p>Antes este parseo vivia dentro de CU-100; se movio aca sin cambiar su comportamiento. El monto se lee como
 * texto y se convierte a decimal exacto (nunca por {@code double}).
 */
@Component
@ConditionalOnProperty(name = "aportaya.aportes.pasarela.modo", havingValue = "simulada", matchIfMissing = true)
public class InterpreteDeWebhookJson implements InterpreteDeWebhook {

    private final ObjectMapper json;

    public InterpreteDeWebhookJson(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public EventoDePago interpretar(String cuerpoCrudo) throws PayloadIlegible {
        JsonNode nodo;
        try {
            nodo = json.readTree(cuerpoCrudo);
        } catch (Exception e) {
            throw new PayloadIlegible("cuerpo no es JSON valido");
        }
        if (nodo == null || !nodo.isObject()) {
            throw new PayloadIlegible("cuerpo no es JSON valido");
        }
        return new EventoDePago(
                textoDe(nodo, "idEvento"),
                textoDe(nodo, "evento"),
                textoDe(nodo, "referenciaProveedor"),
                montoDe(nodo));
    }

    private static String textoDe(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private static Optional<Dinero> montoDe(JsonNode nodo) {
        String monto = textoDe(nodo, "monto");
        String moneda = textoDe(nodo, "moneda");
        if (monto == null || moneda == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Dinero.de(monto, Moneda.valueOf(moneda)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
