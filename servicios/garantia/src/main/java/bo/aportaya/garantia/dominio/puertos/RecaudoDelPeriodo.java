package bo.aportaya.garantia.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cuanto del pozo de un periodo es caja confirmada, segun el dueño del dato.
 *
 * <p>Los montos del faltante NO los afirma quien pide la cobertura: los afirma {@code aportes},
 * que es quien acredita los pagos. Este servicio no puede leer ese esquema (invariante 11) y no
 * le cree al llamador: una cobertura por un importe que el cliente dice es un cheque en blanco
 * contra el bolsillo de la empresa.
 *
 * <p>Se pregunta FUERA de la transaccion (invariante 6). Si {@code aportes} no responde, el
 * resultado es vacio y la cobertura se rechaza: denegar por omision.
 */
public interface RecaudoDelPeriodo {

    record Pendiente(UUID obligacionId, Dinero monto) {}

    record Recaudo(
            UUID periodoId,
            UUID grupoId,
            OffsetDateTime corteEn,
            Dinero pozo,
            Dinero confirmado,
            Dinero cubiertoMutual,
            List<Pendiente> pendientes) {}

    Optional<Recaudo> consultar(UUID periodoId);
}
