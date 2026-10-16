package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.SimulacionDeCaja;
import bo.aportaya.inversiones.dominio.SimulacionDeCaja.Alternativa;
import bo.aportaya.inversiones.dominio.TextoDeCondiciones;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * CU-129 · Simular la caja de dejar un importe en la billetera, en un DPF o en un fondo.
 *
 * <p>Es una cuenta SEPARADA de la operacion real: recibe un importe hipotetico, lee las
 * condiciones vigentes del catalogo y devuelve cifras. No consulta el saldo de nadie, no
 * retiene, no ordena y no escribe nada; este caso de uso ni siquiera tiene el puerto del
 * libro. Es el unico lugar donde se puede comparar el rendimiento de un importe de grupo
 * contra la billetera, y por eso se hace sin tocarlo: la inversion de dinero de grupos
 * esta desactivada (AP-CU121-05) y simular no la reactiva.
 *
 * <p>Un fondo no tiene tasa fija: las variaciones del valor de cuota las pone quien simula,
 * y por omision son -10 %, 0 % y +10 %. Los parametros de los productos son SINTETICOS
 * mientras no tengan fuente aprobada.
 */
@Service
public class CU129SimularCaja {

    public static final int HORIZONTE_MAXIMO_DIAS = 3650;
    static final int ESCENARIOS_MAXIMOS = 9;

    public record Entrada(
            Dinero monto, int horizonteDias, Set<String> productos, List<BigDecimal> variacionesDeFondo) {}

    public record Salida(
            Dinero monto,
            int horizonteDias,
            List<Alternativa> alternativas,
            boolean usaSaldoReal,
            String advertencia) {}

    private final CU120Catalogo catalogo;
    private final List<BigDecimal> escenariosPorOmision;

    /** @param escenariosPorOmision las variaciones de fondo que se simulan si quien pregunta no pone las suyas */
    public CU129SimularCaja(
            CU120Catalogo catalogo,
            @Value("${aportaya.inversiones.simulacion.escenarios-por-omision:-0.10,0.00,0.10}")
                    List<BigDecimal> escenariosPorOmision) {
        this.catalogo = catalogo;
        this.escenariosPorOmision = List.copyOf(escenariosPorOmision);
    }

    public Salida simular(Entrada e, ContextoSesion ctx) {
        validar(e);
        var variaciones =
                e.variacionesDeFondo() == null || e.variacionesDeFondo().isEmpty()
                        ? escenariosPorOmision
                        : e.variacionesDeFondo();
        var vigentes = catalogo.listar(ctx);
        if (e.productos() != null && !e.productos().isEmpty()) {
            var codigos =
                    vigentes.stream().map(CU120Catalogo.VistaProducto::codigo).toList();
            e.productos().stream().filter(p -> !codigos.contains(p)).findFirst().ifPresent(p -> {
                throw Errores.de(129, 3, "Ese producto no esta disponible para simular.");
            });
        }
        List<Alternativa> salida = new ArrayList<>();
        salida.add(SimulacionDeCaja.billetera(e.monto().monto()));
        for (var p : vigentes) {
            if (e.productos() != null
                    && !e.productos().isEmpty()
                    && !e.productos().contains(p.codigo())) {
                continue;
            }
            salida.addAll(SimulacionDeCaja.deProducto(
                    p.codigo(), p.tipo(), e.monto().monto(), e.horizonteDias(), p.condiciones(), variaciones));
        }
        return new Salida(e.monto(), e.horizonteDias(), salida, false, TextoDeCondiciones.ADVERTENCIA);
    }

    private static void validar(Entrada e) {
        if (e.monto() == null || e.monto().esCero() || e.monto().esNegativo()) {
            throw Errores.de(129, 1, "El importe a simular tiene que ser mayor que cero.");
        }
        if (e.monto().moneda() != Moneda.BOB) {
            throw Errores.de(129, 1, "Por ahora solo se simula en bolivianos.");
        }
        if (e.horizonteDias() < 1 || e.horizonteDias() > HORIZONTE_MAXIMO_DIAS) {
            throw Errores.de(129, 2, "El horizonte va de 1 a " + HORIZONTE_MAXIMO_DIAS + " dias.");
        }
        var v = e.variacionesDeFondo();
        if (v != null
                && (v.size() > ESCENARIOS_MAXIMOS
                        || v.stream()
                                .anyMatch(x -> x.compareTo(BigDecimal.ONE.negate()) <= 0
                                        || x.compareTo(BigDecimal.TEN) > 0))) {
            throw Errores.de(
                    129, 4, "Hasta " + ESCENARIOS_MAXIMOS + " escenarios, cada uno mayor que -100 % y hasta +1000 %.");
        }
    }
}
