package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.RecaudoDelPeriodo;
import bo.aportaya.aportes.infraestructura.RecaudoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuanto del pozo del periodo es caja confirmada, al momento de preguntar.
 *
 * <p>No es un caso de uso de la boveda: es la respuesta que {@code entregas} necesita para
 * calcular el faltante del corte sin leer este esquema (invariante 11) y sin que el cliente
 * le afirme cuanto se recaudo. Hasta ahora el monto «recaudado» de una liquidacion lo
 * mandaba quien la pedia; con esto lo afirma el dueño del dato.
 *
 * <p>El corte es el instante de la respuesta ({@code corteEn}): el historico a una fecha
 * anterior no esta soportado y no se finge.
 */
@Service
public class ConsultarRecaudoDelPeriodo {

    private final Datos datos;
    private final RecaudoRepositorio recaudos;
    private final Reloj reloj;

    public ConsultarRecaudoDelPeriodo(Datos datos, RecaudoRepositorio recaudos, Reloj reloj) {
        this.datos = datos;
        this.recaudos = recaudos;
        this.reloj = reloj;
    }

    public record Salida(UUID periodoId, UUID grupoId, OffsetDateTime corteEn, RecaudoDelPeriodo.Resultado recaudo) {}

    @Transactional(readOnly = true)
    public Salida ejecutar(UUID periodoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var fila = recaudos.obligacionesDelPeriodo(dsl, periodoId);
            if (fila.obligaciones().isEmpty()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 5), "Ese periodo no tiene obligaciones de aporte: no hay pozo que medir.");
            }
            return new Salida(
                    periodoId,
                    fila.grupoId(),
                    reloj.ahora().atOffset(ZoneOffset.UTC),
                    RecaudoDelPeriodo.calcular(fila.moneda(), fila.obligaciones()));
        });
    }
}
