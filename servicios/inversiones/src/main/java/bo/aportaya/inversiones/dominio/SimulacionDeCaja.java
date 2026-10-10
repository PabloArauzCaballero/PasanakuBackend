package bo.aportaya.inversiones.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Comparacion MINIMA de caja entre dejar un importe en la billetera, un DPF y un fondo.
 *
 * <p>Es un calculo puro sobre un importe HIPOTETICO: no lee el saldo de nadie, no retiene,
 * no ordena y no toca dinero de grupos. Por eso es una simulacion "de caja separada": la
 * inversion de dinero de grupos esta desactivada y esto no es una puerta de atras, es una
 * cuenta que se puede hacer con cualquier numero.
 *
 * <p>Que dice y que no dice:
 * <ul>
 *   <li>La billetera conserva el importe y esta disponible hoy.
 *   <li>Un DPF paga lo que fija su contrato (interes simple menos retencion) y, si no se
 *       llega al vencimiento dentro del horizonte, o no admite cancelacion (no esta
 *       disponible) o se cancela con penalizacion.
 *   <li>Un fondo NO promete nada: se muestran escenarios HIPOTETICOS de variacion del valor de
 *       cuota que pone quien simula (por omision -10 %, 0 % y +10 %). La comision de exito
 *       solo grava lo que supera el valor de entrada.
 * </ul>
 * Todo con las condiciones del producto, que mientras no tengan fuente aprobada son SINTETICAS
 * ({@code DR-INV-02}, {@code DR-INV-03}).
 */
public final class SimulacionDeCaja {

    public static final String BILLETERA = "BILLETERA";
    public static final String DPF_AL_VENCIMIENTO = "DPF_AL_VENCIMIENTO";
    public static final String DPF_ANTICIPADO = "DPF_ANTICIPADO";
    public static final String DPF_NO_DISPONIBLE = "DPF_NO_DISPONIBLE";
    public static final String FONDO_ESCENARIO = "FONDO_ESCENARIO";

    private SimulacionDeCaja() {}

    /**
     * @param diasParaDisponer cuantos dias pasan hasta tener el neto en la billetera
     * @param disponibleEnElHorizonte si ese momento cae dentro del horizonte pedido
     * @param netoCuandoDisponible lo que llega a la billetera cuando se puede disponer
     * @param diferenciaContraBilletera netoCuandoDisponible menos el importe (puede ser negativa)
     */
    public record Alternativa(
            String tipo,
            String productoCodigo,
            String descripcion,
            int diasParaDisponer,
            boolean disponibleEnElHorizonte,
            BigDecimal interes,
            BigDecimal retencion,
            BigDecimal comision,
            BigDecimal netoCuandoDisponible,
            BigDecimal diferenciaContraBilletera,
            OrigenDatos origen) {}

    public static Alternativa billetera(BigDecimal monto) {
        return new Alternativa(
                BILLETERA,
                null,
                "Dejar el importe en la billetera",
                0,
                true,
                cero(),
                cero(),
                cero(),
                monto,
                cero(),
                OrigenDatos.VERIFICADO);
    }

    public static Alternativa dpf(String codigo, BigDecimal monto, int horizonteDias, Condiciones c) {
        int plazo = c.plazoDias().orElseThrow();
        int base = c.baseDias().orElseThrow();
        BigDecimal tasa = c.tasaNominalAnual().orElseThrow();
        BigDecimal retencion = c.tasaRetencion().orElseThrow();
        if (horizonteDias >= plazo) {
            BigDecimal interes = DevengoDeDpf.interes(monto, tasa, plazo, base);
            return dpfPagado(
                    DPF_AL_VENCIMIENTO,
                    codigo,
                    "DPF mantenido hasta el vencimiento",
                    monto,
                    plazo,
                    true,
                    interes,
                    retencion,
                    c);
        }
        if (c.permiteRescateAnticipado()) {
            BigDecimal bruto = DevengoDeDpf.interes(monto, tasa, horizonteDias, base);
            BigDecimal interes = bruto.multiply(
                            BigDecimal.ONE.subtract(c.penalizacionAnticipo().orElse(BigDecimal.ZERO)))
                    .setScale(2, RoundingMode.HALF_EVEN);
            return dpfPagado(
                    DPF_ANTICIPADO,
                    codigo,
                    "DPF cancelado antes del vencimiento, con penalizacion",
                    monto,
                    horizonteDias,
                    true,
                    interes,
                    retencion,
                    c);
        }
        BigDecimal interes = DevengoDeDpf.interes(monto, tasa, plazo, base);
        return dpfPagado(
                DPF_NO_DISPONIBLE,
                codigo,
                "DPF sin cancelacion anticipada: el importe no esta disponible en el horizonte",
                monto,
                plazo,
                false,
                interes,
                retencion,
                c);
    }

    private static Alternativa dpfPagado(
            String tipo,
            String codigo,
            String descripcion,
            BigDecimal monto,
            int dias,
            boolean enHorizonte,
            BigDecimal interes,
            BigDecimal tasaRetencion,
            Condiciones c) {
        BigDecimal ret = DevengoDeDpf.retencion(interes, tasaRetencion);
        BigDecimal neto = monto.add(interes).subtract(ret);
        return new Alternativa(
                tipo,
                codigo,
                descripcion,
                dias,
                enHorizonte,
                interes,
                ret,
                cero(),
                neto,
                neto.subtract(monto),
                c.origen());
    }

    /** Un escenario hipotetico: el valor de cuota varia {@code variacion} (0,10 = +10 %) desde la entrada. */
    public static Alternativa fondo(
            String codigo, BigDecimal monto, int horizonteDias, Condiciones c, BigDecimal variacion) {
        BigDecimal valor = monto.multiply(BigDecimal.ONE.add(variacion)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ganancia = valor.subtract(monto).max(BigDecimal.ZERO);
        BigDecimal comision = c.tasaComisionExito()
                .map(t -> ganancia.multiply(t).setScale(2, RoundingMode.HALF_EVEN))
                .orElse(cero());
        BigDecimal neto = valor.subtract(comision);
        int dias = c.diasRescate().orElse(0);
        String porcentaje =
                variacion.movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).toPlainString();
        return new Alternativa(
                FONDO_ESCENARIO,
                codigo,
                "Fondo con el valor de cuota " + (variacion.signum() > 0 ? "+" : "") + porcentaje
                        + " % (escenario hipotetico, no una prediccion)",
                dias,
                horizonteDias >= dias,
                ganancia,
                cero(),
                comision,
                neto,
                neto.subtract(monto),
                c.origen());
    }

    /** Las alternativas de un producto, en el orden en que se muestran. */
    public static List<Alternativa> deProducto(
            String codigo,
            TipoProducto tipo,
            BigDecimal monto,
            int horizonteDias,
            Condiciones c,
            List<BigDecimal> variaciones) {
        List<Alternativa> salida = new ArrayList<>();
        if (tipo == TipoProducto.DPF) {
            salida.add(dpf(codigo, monto, horizonteDias, c));
        } else {
            variaciones.forEach(v -> salida.add(fondo(codigo, monto, horizonteDias, c, v)));
        }
        return salida;
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
