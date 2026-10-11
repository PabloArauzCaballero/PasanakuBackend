package bo.aportaya.entregas.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Cuanto del pozo de un periodo es caja confirmada, segun {@code aportes}.
 *
 * <p>Antes de esto el monto «recaudado» de una liquidacion lo mandaba quien la pedia. Ahora lo
 * afirma el dueño del dato (invariante 11), preguntado FUERA de la transaccion (invariante 6).
 * Vacio significa que {@code aportes} no respondio: quien pregunta no avanza.
 */
public interface RecaudoDelPozo {

    record Recaudo(
            UUID periodoId,
            UUID grupoId,
            OffsetDateTime corteEn,
            Dinero pozo,
            Dinero confirmado,
            Dinero cubiertoMutual) {

        public Dinero faltante() {
            return pozo.menos(confirmado).menos(cubiertoMutual);
        }
    }

    Optional<Recaudo> consultar(UUID periodoId);
}
