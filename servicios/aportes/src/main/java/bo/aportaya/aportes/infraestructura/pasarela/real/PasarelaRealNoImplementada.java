package bo.aportaya.aportes.infraestructura.pasarela.real;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Marcador del adaptador real: con {@code aportaya.aportes.pasarela.modo=real} el arranque se niega mientras no
 * exista adaptador, en vez de caer en un {@code NoSuchBeanDefinitionException} opaco. Al agregar el adaptador real
 * (ver {@code docs/operacion/pasarela-real-como-enchufar.md}) se BORRA esta clase.
 */
@Component
@ConditionalOnProperty(name = "aportaya.aportes.pasarela.modo", havingValue = "real")
public class PasarelaRealNoImplementada {

    public PasarelaRealNoImplementada() {
        throw new IllegalStateException(
                "aportaya.aportes.pasarela.modo=real pero no hay adaptador real de la pasarela: implemente"
                        + " PasarelaDePagos e InterpreteDeWebhook en infraestructura/pasarela/real"
                        + " (docs/operacion/pasarela-real-como-enchufar.md).");
    }
}
