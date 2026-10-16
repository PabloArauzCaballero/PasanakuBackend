package bo.aportaya.aportes.aplicacion;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que `aportes` necesita saber de `grupos` para generar las obligaciones de un periodo: quien
 * es titular de cada cupo, que numero de periodo (el turno) es, cuanto se aporta y las fechas
 * contractuales. Se pregunta por contrato; este servicio no lee el esquema de grupos (invariante 11).
 *
 * <p>El productor real es `grupos`: {@code GET /grupos/periodos/{periodoId}/calendario-de-aportes}
 * ({@code consultarCalendarioDeAportes}, permiso {@code ADMIN_PLATAFORMA}, esquema {@code CalendarioDeAportes}
 * de {@code grupos.yaml}). La implementacion de produccion es {@code CalendarioDelPeriodoPorHttp}, con tiempo
 * maximo, circuit breaker y fallo cerrado. Las pruebas de este caso de uso corren contra un doble de tres
 * niveles; el adaptador se prueba contra un servidor HTTP que habla el cuerpo real del productor.
 */
public interface CalendarioDelPeriodo {

    /** {@code empty} si grupos no contesto o no conoce el periodo: quien pregunta decide (para dinero, rechazar). */
    Optional<Periodo> deLosCupos(UUID periodoId);

    /**
     * El periodo tal cual lo afirma `grupos`. {@code numero} es el turno del ciclo (un turno por periodo);
     * {@code estado} es el de {@code grupos.periodo} (ABIERTO, EN_GRACIA, ...).
     */
    record Periodo(
            UUID periodoId,
            UUID grupoId,
            int numero,
            String estado,
            LocalDate fechaLimitePago,
            LocalDate fechaFinGracia,
            List<Cupo> cupos) {}

    /** Un cupo ocupado: su titular y lo que debe aportar en este periodo (grupos decide el importe). */
    record Cupo(UUID cupoId, UUID participanteId, Dinero montoEsperado) {}
}
