package bo.aportaya.garantia.infraestructura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Los parametros financieros del modelo A (ADR-056 §6, F3.L2.M14): obligatorios, sin valor por defecto, en decimal
 * exacto.
 *
 * <p>Solo existe con el modelo A encendido ({@code aportaya.modelo-a.habilitado=true}, apagado por omision): asi
 * garantia arranca como hoy mientras el modelo espera a Cumplimiento (F3.S3.M7). Encendido, el proceso NO ARRANCA si
 * falta uno o si no es valido, y el error nombra la clave. Los VALORES no estan en el codigo ni en el
 * {@code application.yml} principal: los fijan Pablo y Cumplimiento (F3.S8.M3) y los sirve el registro central
 * (propuesto en {@code tarifas}, contrato en {@code docs/trabajo/2026-11-21-tanda2-l2/evidencia}). Las pruebas usan
 * un perfil SINTETICO.
 *
 * <p>Las unidades que ADR-056 no fija (tope del recargo y los dos umbrales) se declaran junto al valor: este servicio
 * no elige si son un porcentaje o un monto.
 */
@Component
@ConditionalOnProperty(name = "aportaya.modelo-a.habilitado", havingValue = "true")
public class ParametrosDelModeloA {

    static final String RAIZ = "aportaya.modelo-a.parametros.";
    private static final Set<String> UNIDADES_CON_MONEDA = Set.of("MONTO");

    /** Un valor con su unidad declarada; la moneda ISO 4217 es obligatoria si la unidad es un monto (regla 91.1.2). */
    public record Medida(BigDecimal valor, String unidad, String moneda) {}

    private final Map<String, BigDecimal> porcentajeDelFondoPorNivel;
    private final int diasDeGracia;
    private final BigDecimal tasaDeRecargo;
    private final Medida topeDelRecargo;
    private final Medida umbralDeSolvencia;
    private final Medida umbralDeDisolucionAutomatica;

    public ParametrosDelModeloA(Environment entorno) {
        var binder = Binder.get(entorno);
        List<String> faltan = new ArrayList<>();

        Map<String, String> porNivel = binder.bind(
                        RAIZ + "porcentaje-del-fondo-por-nivel", Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        Map<String, BigDecimal> niveles = new TreeMap<>();
        if (porNivel.isEmpty()) {
            faltan.add(RAIZ + "porcentaje-del-fondo-por-nivel (al menos el nivel inicial)");
        }
        porNivel.forEach((nivel, valor) -> {
            var p = porcentaje(RAIZ + "porcentaje-del-fondo-por-nivel." + nivel, valor, faltan);
            if (p != null) {
                niveles.put(nivel, p);
            }
        });
        this.porcentajeDelFondoPorNivel = Map.copyOf(niveles);

        String gracia = entorno.getProperty(RAIZ + "dias-de-gracia");
        this.diasDeGracia = entero(RAIZ + "dias-de-gracia", gracia, faltan);
        this.tasaDeRecargo =
                porcentaje(RAIZ + "tasa-de-recargo", entorno.getProperty(RAIZ + "tasa-de-recargo"), faltan);
        this.topeDelRecargo = medida(entorno, RAIZ + "tope-del-recargo", faltan);
        this.umbralDeSolvencia = medida(entorno, RAIZ + "umbral-de-solvencia", faltan);
        this.umbralDeDisolucionAutomatica = medida(entorno, RAIZ + "umbral-de-disolucion-automatica", faltan);

        if (!faltan.isEmpty()) {
            throw new IllegalStateException("El modelo A esta encendido y faltan o no son validos estos parametros "
                    + "financieros (ADR-056 §6, sin valor por defecto): " + String.join("; ", faltan));
        }
    }

    private static BigDecimal decimal(String clave, String crudo, List<String> faltan) {
        if (crudo == null || crudo.isBlank()) {
            faltan.add(clave);
            return null;
        }
        try {
            var valor = new BigDecimal(crudo.trim());
            if (valor.signum() < 0) {
                faltan.add(clave + " (no puede ser negativo)");
                return null;
            }
            return valor;
        } catch (NumberFormatException e) {
            faltan.add(clave + " (no es un decimal: «" + crudo + "»)");
            return null;
        }
    }

    /** Puntos porcentuales. Sin tope inventado: ADR-056 no fija un maximo, y ponerlo aca seria decidir un umbral. */
    private static BigDecimal porcentaje(String clave, String crudo, List<String> faltan) {
        return decimal(clave, crudo, faltan);
    }

    private static int entero(String clave, String crudo, List<String> faltan) {
        var valor = decimal(clave, crudo, faltan);
        if (valor == null) {
            return -1;
        }
        if (valor.stripTrailingZeros().scale() > 0) {
            faltan.add(clave + " (son dias enteros)");
            return -1;
        }
        return valor.intValueExact();
    }

    private static Medida medida(Environment entorno, String clave, List<String> faltan) {
        var valor = decimal(clave + ".valor", entorno.getProperty(clave + ".valor"), faltan);
        String unidad = entorno.getProperty(clave + ".unidad");
        if (unidad == null || unidad.isBlank()) {
            faltan.add(clave + ".unidad");
            return null;
        }
        String moneda = entorno.getProperty(clave + ".moneda");
        if (UNIDADES_CON_MONEDA.contains(unidad.trim())
                && (moneda == null || !moneda.trim().matches("[A-Z]{3}"))) {
            faltan.add(clave + ".moneda (ISO 4217, obligatoria con unidad MONTO)");
            return null;
        }
        return valor == null ? null : new Medida(valor, unidad.trim(), moneda == null ? null : moneda.trim());
    }

    public Map<String, BigDecimal> porcentajeDelFondoPorNivel() {
        return porcentajeDelFondoPorNivel;
    }

    public int diasDeGracia() {
        return diasDeGracia;
    }

    public BigDecimal tasaDeRecargo() {
        return tasaDeRecargo;
    }

    public Medida topeDelRecargo() {
        return topeDelRecargo;
    }

    public Medida umbralDeSolvencia() {
        return umbralDeSolvencia;
    }

    public Medida umbralDeDisolucionAutomatica() {
        return umbralDeDisolucionAutomatica;
    }
}
