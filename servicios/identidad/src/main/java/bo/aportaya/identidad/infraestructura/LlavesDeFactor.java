package bo.aportaya.identidad.infraestructura;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Las llaves con que se cifra el secreto TOTP en {@code factor_mfa.secreto_cifrado}, por
 * version ({@code factor_mfa.version_llave}).
 *
 * <p>El secreto TOTP no se puede guardar como hash: verificar un codigo exige el secreto en
 * claro. Por eso va <b>cifrado</b> (AES-256-GCM, {@code CifradorSimetrico}) con una llave
 * que vive fuera de la base, y con version para poder rotarla sin perder los factores ya
 * enrolados: se descifra con la version de la fila y se cifra siempre con la vigente.
 *
 * <p><b>Falla cerrada al arrancar</b> (regla 90.3.4): sin llaves, con una llave corta o con
 * una version vigente que no esta en la lista, el proceso no levanta. No hay valor por
 * omision: un default seria una llave publicada en el repositorio.
 *
 * <p>Formato de {@code aportaya.mfa.llaves}: {@code <version>:<llave>[,<version>:<llave>...]}.
 */
@Component
public class LlavesDeFactor {

    /** Largo minimo de una llave: la llave AES se deriva por SHA-256, pero la entropia la pone esto. */
    static final int LARGO_MINIMO = 32;

    private final Map<Short, String> llaves;
    private final short vigente;

    public LlavesDeFactor(
            @Value("${aportaya.mfa.llaves}") String llaves, @Value("${aportaya.mfa.version-vigente}") String vigente) {
        this.llaves = Map.copyOf(leer(llaves));
        this.vigente = version(vigente);
        if (!this.llaves.containsKey(this.vigente)) {
            throw new IllegalStateException(
                    "aportaya.mfa.version-vigente=" + this.vigente + " no esta en aportaya.mfa.llaves");
        }
    }

    public short vigente() {
        return vigente;
    }

    public String llaveVigente() {
        return llaves.get(vigente);
    }

    /** La llave de una version; vacio si esa version ya no esta configurada. */
    public Optional<String> llave(short version) {
        return Optional.ofNullable(llaves.get(version));
    }

    private static Map<Short, String> leer(String configuradas) {
        if (configuradas == null || configuradas.isBlank()) {
            throw new IllegalStateException(
                    "aportaya.mfa.llaves es obligatoria (MFA_LLAVES): no hay valor por omision");
        }
        Map<Short, String> salida = new HashMap<>();
        for (String par : configuradas.split(",")) {
            int separador = par.indexOf(':');
            if (separador <= 0) {
                throw new IllegalStateException("aportaya.mfa.llaves: cada entrada es <version>:<llave>");
            }
            short version = version(par.substring(0, separador));
            String llave = par.substring(separador + 1).trim();
            if (llave.length() < LARGO_MINIMO) {
                // No se dice cual es la llave ni su largo exacto: solo la version.
                throw new IllegalStateException("aportaya.mfa.llaves: la llave de la version " + version
                        + " tiene menos de " + LARGO_MINIMO + " caracteres");
            }
            if (salida.put(version, llave) != null) {
                throw new IllegalStateException("aportaya.mfa.llaves: version " + version + " repetida");
            }
        }
        return salida;
    }

    private static short version(String texto) {
        try {
            short v = Short.parseShort(texto.trim());
            if (v < 1) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException | NullPointerException e) {
            // ck_factor_mfa_version_llave: version_llave >= 1.
            throw new IllegalStateException("version de llave MFA invalida: tiene que ser un entero >= 1");
        }
    }
}
