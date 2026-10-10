package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.infraestructura.DistribucionRepositorio.Distribucion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** El interes repartido al titular, tal como lo ve quien consulta: cifras, periodo y estado de la conciliacion. */
public record VistaDistribucion(
        UUID id,
        UUID rescateId,
        LocalDate periodoDesde,
        LocalDate periodoHasta,
        BigDecimal interesDevengado,
        BigDecimal penalizacion,
        BigDecimal interesEsperado,
        BigDecimal interesPagado,
        BigDecimal retencion,
        BigDecimal interesNeto,
        String moneda,
        String estado,
        String motivoDiscrepancia,
        OffsetDateTime distribuidaEn) {

    static VistaDistribucion de(Distribucion d) {
        return new VistaDistribucion(
                d.id(),
                d.rescateId(),
                d.periodoDesde(),
                d.periodoHasta(),
                d.interesDevengado(),
                d.penalizacion(),
                d.interesEsperado(),
                d.interesPagado(),
                d.retencion(),
                d.interesNeto(),
                d.moneda(),
                d.estado(),
                d.motivoDiscrepancia(),
                d.distribuidaEn());
    }
}
