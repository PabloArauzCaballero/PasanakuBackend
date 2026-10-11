package bo.aportaya.garantia.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El asiento que el mayor tiene que registrar por un movimiento del respaldo.
 *
 * <p>Este servicio <b>no escribe el libro</b> (invariante 12): solo nucleo-financiero lo
 * hace. Lo que sale de aca es una propuesta con partidas, y viaja en el evento del
 * outbox para que el consumidor la registre con CU-24. Que la propuesta cuadre se
 * exige al construirla: un asiento que no suma cero no sale de este servicio.
 *
 * <p>Las cuentas son <b>logicas</b>. El plan de cuentas real lo gobierna contabilidad
 * (docs/trabajo/.../carril-C/PLAN-CARRIL-C.md, ambiguedad A5): inventar un codigo
 * contable aca seria presentar como hecho lo que es una pregunta abierta.
 */
public record AsientoPropuesto(String origenTipo, UUID origenId, List<Partida> partidas) {

    /** Los tres bolsillos que toca el respaldo. La exposicion es memoria, no cuenta. */
    public enum Cuenta {
        CAJA_EMPRESA,
        RESERVA_RESPALDO,
        POZO_GRUPO
    }

    public record Partida(Cuenta cuenta, Dinero debe, Dinero haber) {}

    public AsientoPropuesto {
        partidas = List.copyOf(partidas);
        Dinero debe = partidas.stream().map(Partida::debe).reduce(Dinero::mas).orElseThrow();
        Dinero haber = partidas.stream().map(Partida::haber).reduce(Dinero::mas).orElseThrow();
        if (!debe.equals(haber) || debe.esCero()) {
            throw new ErrorDeDominio("El asiento propuesto no cuadra: debe %s, haber %s".formatted(debe, haber));
        }
    }

    /** La empresa aparta caja para respaldar un ciclo. */
    public static AsientoPropuesto reserva(UUID reservaId, Dinero monto) {
        return par("AJUSTE", reservaId, Cuenta.RESERVA_RESPALDO, Cuenta.CAJA_EMPRESA, monto);
    }

    /** La reserva llena el pozo del grupo con lo que no llego. */
    public static AsientoPropuesto aplicacion(UUID coberturaId, Dinero monto) {
        return par("COBERTURA", coberturaId, Cuenta.POZO_GRUPO, Cuenta.RESERVA_RESPALDO, monto);
    }

    /** Contra-asiento de {@link #aplicacion}: nunca se edita, se invierte. */
    public static AsientoPropuesto reversaDeAplicacion(UUID coberturaId, Dinero monto) {
        return par("COBERTURA", coberturaId, Cuenta.RESERVA_RESPALDO, Cuenta.POZO_GRUPO, monto);
    }

    /** El aporte tardio devuelve a la empresa lo que adelanto; no repone la reserva. */
    public static AsientoPropuesto recuperacion(UUID recuperacionId, Dinero monto) {
        return par("COBERTURA", recuperacionId, Cuenta.CAJA_EMPRESA, Cuenta.POZO_GRUPO, monto);
    }

    /** Al cerrar el ciclo, lo reservado y no usado vuelve a la caja. */
    public static AsientoPropuesto liberacion(UUID reservaId, Dinero monto) {
        return par("AJUSTE", reservaId, Cuenta.CAJA_EMPRESA, Cuenta.RESERVA_RESPALDO, monto);
    }

    private static AsientoPropuesto par(String origen, UUID id, Cuenta aDebitar, Cuenta aAcreditar, Dinero monto) {
        Dinero cero = Dinero.cero(monto.moneda());
        return new AsientoPropuesto(
                origen, id, List.of(new Partida(aDebitar, monto, cero), new Partida(aAcreditar, cero, monto)));
    }

    /** La forma que viaja en el evento: cadenas decimales, nunca numeros. */
    public Map<String, Object> comoCarga() {
        return Map.of(
                "origenTipo",
                origenTipo,
                "origenId",
                origenId.toString(),
                "partidas",
                partidas.stream()
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
