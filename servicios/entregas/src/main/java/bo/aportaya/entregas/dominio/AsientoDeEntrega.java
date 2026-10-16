package bo.aportaya.entregas.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El asiento que el mayor tiene que registrar cuando el proveedor CONFIRMA que el pozo salio hacia el beneficiario.
 *
 * <p>Este servicio no escribe el libro (invariante 12): solo nucleo-financiero. Lo que sale de aca es una propuesta
 * con partidas que viaja en el evento {@code entregas.desembolso_acreditado}, para que el consumidor la registre
 * con CU-24. Cuadra por construccion (debe = haber, importe positivo) y se identifica por su origen
 * ({@code ENTREGA}, id de la entrega): un evento entregado dos veces se registra una sola vez.
 *
 * <p>Las cuentas son <b>logicas</b>: el plan de cuentas lo gobierna contabilidad y aca no se inventa un codigo
 * (ambiguedad A5 del carril C). Sale el principal COMPLETO, sin deducciones (A6).
 */
public record AsientoDeEntrega(UUID entregaId, Dinero monto) {

    public enum Cuenta {
        POZO_GRUPO,
        DESEMBOLSO_CONFIRMADO
    }

    public static final String ORIGEN_TIPO = "ENTREGA";

    public AsientoDeEntrega {
        if (monto.esCero() || monto.esNegativo()) {
            throw new ErrorDeDominio("El asiento de una entrega es por un importe positivo: " + monto);
        }
    }

    public record Partida(Cuenta cuenta, Dinero debe, Dinero haber) {}

    public List<Partida> partidas() {
        Dinero cero = Dinero.cero(monto.moneda());
        return List.of(
                new Partida(Cuenta.POZO_GRUPO, monto, cero), new Partida(Cuenta.DESEMBOLSO_CONFIRMADO, cero, monto));
    }

    /** La forma que viaja en el evento: cadenas decimales, nunca numeros. */
    public Map<String, Object> comoCarga() {
        return Map.of(
                "origenTipo",
                ORIGEN_TIPO,
                "origenId",
                entregaId.toString(),
                "partidas",
                partidas().stream()
                        .map(p -> Map.<String, Object>of(
                                "cuenta",
                                p.cuenta().name(),
                                "debe",
                                p.debe().toString(),
                                "haber",
                                p.haber().toString(),
                                "moneda",
                                p.debe().moneda().name()))
                        .toList());
    }
}
