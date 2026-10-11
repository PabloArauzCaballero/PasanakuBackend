package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.infraestructura.RescateRepositorio.Rescate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lo que la persona ve de su rescate. «Pendiente» significa pendiente: el dinero NO esta
 * disponible hasta que el aliado confirme los fondos y el libro los acredite.
 */
public record VistaRescate(
        UUID rescateId,
        UUID posicionId,
        String tipo,
        String estado,
        BigDecimal cuotas,
        LocalDate fechaValor,
        OffsetDateTime liquidaEn,
        BigDecimal netoAcreditar,
        String motivoRechazo,
        String mensaje) {

    static VistaRescate de(Rescate r, BigDecimal neto) {
        return new VistaRescate(
                r.id(),
                r.posicionId(),
                r.tipo(),
                r.estado(),
                r.cuotas(),
                r.fechaValor(),
                r.liquidaEn(),
                neto,
                r.motivoRechazo(),
                mensaje(r));
    }

    private static String mensaje(Rescate r) {
        String corte = r.fechaValor() == null ? "" : " Corte aplicable: " + r.fechaValor() + ".";
        return switch (r.estado()) {
            case "SOLICITADO" -> "Pedido de rescate registrado; falta enviarlo al aliado." + corte;
            case "PENDIENTE" ->
                "Rescate pendiente: el dinero NO esta disponible hasta que el aliado confirme los fondos." + corte;
            case "INCIERTO" ->
                "No pudimos confirmar el rescate con el aliado; lo vamos a consultar. Tus cuotas siguen comprometidas.";
            case "POR_ACREDITAR" -> "El aliado confirmo el rescate; falta acreditarlo en tu saldo.";
            case "LIQUIDADO" -> "Rescate liquidado y acreditado en tu saldo.";
            default -> "El rescate no se concreto; tus cuotas o tu deposito siguen en tu posicion.";
        };
    }
}
