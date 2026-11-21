package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.dominio.FondeoDelPozo;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-22 (pozo completo) · Fondear el pozo del turno con la caja confirmada.
 *
 * <p>Recibir mesa es recibir el pozo completo, y el importe no lo afirma el cliente:
 *
 * <ol>
 *   <li>{@code aportes} afirma pozo, confirmado y fondo mutual (un pendiente NO es caja);
 *   <li>se escribe en una transaccion local ({@link RegistroDelFondeo}).
 * </ol>
 *
 * <p>Si falta plata, la entrega queda BLOQUEADA con el faltante como deuda conservada, a la espera
 * de la cobertura con el fondo del grupo (ADR-055 E7, F3.L2.M05). La empresa no cubre nada: el
 * respaldo empresarial se retiro (F2.L2.M01) y este caso de uso ya no le pregunta a {@code garantia}.
 *
 * <p>Esta clase NO abre transaccion: la pregunta a {@code aportes} va fuera de ella (invariante 6).
 * Si no responde, no se escribe nada y el reintento con la misma clave es seguro.
 *
 * <p>El importe a entregar es SIEMPRE el pozo: sin deducciones sobre el principal (ambiguedad A6).
 */
@Service
public class CU22EntregarPozoCompleto {

    private final RecaudoDelPozo recaudos;
    private final RegistroDelFondeo registro;

    public CU22EntregarPozoCompleto(RecaudoDelPozo recaudos, RegistroDelFondeo registro) {
        this.recaudos = recaudos;
        this.registro = registro;
    }

    public record Entrada(
            UUID grupoId,
            UUID periodoId,
            UUID turnoId,
            UUID cupoId,
            UUID beneficiarioId,
            String metodoDesembolso,
            LocalDate fechaProgramada,
            String clave) {}

    public RegistroDelFondeo.Salida fondear(Entrada e, ContextoSesion ctx) {
        var recaudo = recaudos.consultar(e.periodoId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(22, 6), "No se pudo confirmar el recaudo con aportes: no se fondea nada."));
        if (!e.grupoId().equals(recaudo.grupoId()) || !e.periodoId().equals(recaudo.periodoId())) {
            throw new ErrorDeNegocio(CodigoError.de(22, 7), "Ese periodo no es de ese grupo.");
        }

        return registro.registrar(
                new RegistroDelFondeo.Entrada(
                        e.grupoId(),
                        e.periodoId(),
                        e.turnoId(),
                        e.cupoId(),
                        e.beneficiarioId(),
                        e.metodoDesembolso(),
                        e.fechaProgramada(),
                        e.clave()),
                FondeoDelPozo.calcular(recaudo),
                recaudo.corteEn(),
                ctx);
    }
}
