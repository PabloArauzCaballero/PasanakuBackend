package bo.aportaya.grupos.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lo que `grupos` afirma de un periodo para que `aportes` genere sus obligaciones: el turno, las fechas
 * del reglamento y, por cada cupo ocupado, su titular y lo que aporta. `aportes` no lee este esquema
 * (invariante 11) ni cree nada que afirme el cliente: lo pregunta.
 */
public record CalendarioDeAportes(
        UUID periodoId,
        UUID grupoId,
        int numero,
        String estado,
        LocalDate fechaLimitePago,
        LocalDate fechaFinGracia,
        LocalDate fechaEntregaPrevista,
        List<Cupo> cupos) {

    /** Un cupo ocupado: quien lo tiene y el importe que le toca aportar en cada periodo. */
    public record Cupo(UUID cupoId, UUID participanteId, Dinero montoEsperado) {}
}
