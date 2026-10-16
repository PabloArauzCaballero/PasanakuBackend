package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.infraestructura.RecaudoDiarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo recaudado, conciliado y en excepcion de un dia, por moneda (CU-51, backoffice carril A · H8).
 *
 * <p>No es un caso de uso de la boveda: es la respuesta que {@code nucleo-financiero} necesita para
 * cerrar el dia sin leer este esquema (invariante 11) y sin que el operador afirme las cifras. El dia se
 * corta en {@link Reloj#ZONA}, la misma zona en la que el negocio cuenta sus plazos.
 */
@Service
public class ConsultarRecaudoDiario {

    private final Datos datos;
    private final RecaudoDiarioRepositorio recaudos;

    public ConsultarRecaudoDiario(Datos datos, RecaudoDiarioRepositorio recaudos) {
        this.datos = datos;
        this.recaudos = recaudos;
    }

    public record Salida(LocalDate fecha, String zonaHoraria, List<PorMoneda> porMoneda, int excepcionesAbiertas) {}

    /** Los totales de una moneda. Los importes son exactos; la moneda acompaña a cada uno. */
    public record PorMoneda(
            String moneda, BigDecimal recaudado, BigDecimal conciliado, BigDecimal excepciones, int pagos) {}

    @Transactional(readOnly = true)
    public Salida ejecutar(LocalDate fecha, ContextoSesion ctx) {
        OffsetDateTime desde = fecha.atStartOfDay(Reloj.ZONA).toOffsetDateTime();
        OffsetDateTime hasta = fecha.plusDays(1).atStartOfDay(Reloj.ZONA).toOffsetDateTime();
        return datos.conContexto(
                ctx,
                dsl -> new Salida(
                        fecha,
                        Reloj.ZONA.getId(),
                        recaudos.porMoneda(dsl, desde, hasta).stream()
                                .map(m -> new PorMoneda(
                                        m.moneda(), m.recaudado(), m.conciliado(), m.excepciones(), m.pagos()))
                                .toList(),
                        recaudos.excepcionesAbiertas(dsl, desde, hasta)));
    }
}
