package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.SegundoFactor;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El adaptador de DESARROLLO del segundo factor: **deniega salvo que se lo encienda a
 * proposito, y solo existe en {@code local}/{@code test}**.
 *
 * <p>H2.S1 (patron {@code identidad/infraestructura/DesafioDeDesarrollo.java}):
 * {@code @Profile({"local","test"})} saca este bean del classpath en cualquier otro
 * perfil. Antes de H2 la propiedad {@code aportaya.mfa.exigido=false} podia apagar la
 * exigencia de MFA en CUALQUIER entorno con solo cambiar una variable — sin el
 * `@Profile`, un {@code false} puesto por error en produccion habilita retiros sin
 * segundo factor, que es justo lo que R-BIL-09 prohibe. Con el bean confinado, ese
 * error ya no es posible: no hay bean que apagar, y {@link SegundoFactorStepUp}
 * (H2.S2) es el unico {@code SegundoFactor} que existe fuera de local/test.
 *
 * <p>{@code aportaya.mfa.doble-local=true} lo enciende, y solo tiene sentido en
 * desarrollo manual y en pruebas de punta a punta que no arman la evidencia real. El
 * valor por omision es seguir denegando incluso en local: nadie deberia necesitar
 * tocar esta bandera salvo para un caso puntual.
 */
@Component
@Profile({"local", "test"})
public class SegundoFactorLocal implements SegundoFactor {

    private final boolean dobleLocal;

    public SegundoFactorLocal(@Value("${aportaya.mfa.doble-local:false}") boolean dobleLocal) {
        this.dobleLocal = dobleLocal;
    }

    @Override
    public boolean verificado(UUID usuarioId, String factor) {
        if (dobleLocal) {
            // Con el doble encendido, cualquier sesion autenticada alcanza. Se sigue
            // pidiendo que el campo venga: un cliente que no lo manda no esta
            // preparado para el entorno donde si se exige la evidencia real.
            return factor != null && !factor.isBlank();
        }
        return false;
    }
}
