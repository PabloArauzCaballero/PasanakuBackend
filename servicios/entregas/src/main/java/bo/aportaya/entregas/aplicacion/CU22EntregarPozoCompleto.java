package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.dominio.FondeoDelPozo;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.entregas.dominio.puertos.RespaldoEmpresarial;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-22 (pozo completo) · Fondear el pozo del turno: caja confirmada + respaldo empresarial.
 *
 * <p>Recibir mesa es recibir el pozo completo. El camino anterior ({@code CU22LiquidarEntrega})
 * descontaba del principal y rechazaba la bolsa incompleta con un monto que el cliente afirmaba;
 * este camino pregunta, no cree:
 *
 * <ol>
 *   <li>{@code aportes} afirma pozo, confirmado y fondo mutual (un pendiente NO es caja);
 *   <li>si falta, {@code garantia} cubre con la reserva del ciclo (todo o nada, idempotente por turno);
 *   <li>recien ahi se escribe, en una transaccion local ({@link RegistroDelFondeo}).
 * </ol>
 *
 * <p>Esta clase NO abre transaccion: las dos preguntas remotas van fuera de ella (invariante 6).
 * Si cualquiera no responde, no se escribe nada y el reintento es seguro: la cobertura es
 * idempotente por turno, asi que repetir tras una caida no cubre dos veces.
 *
 * <p>El importe a entregar es SIEMPRE el pozo: sin deducciones sobre el principal (ambiguedad A6).
 * Autorizar y ejecutar siguen siendo {@code CU22LiquidarEntrega#autorizar/ejecutar}.
 */
@Service
public class CU22EntregarPozoCompleto {

    private final RecaudoDelPozo recaudos;
    private final RespaldoEmpresarial respaldo;
    private final RegistroDelFondeo registro;

    public CU22EntregarPozoCompleto(RecaudoDelPozo recaudos, RespaldoEmpresarial respaldo, RegistroDelFondeo registro) {
        this.recaudos = recaudos;
        this.respaldo = respaldo;
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

        Dinero faltante = recaudo.faltante();
        Dinero cubiertoEmpresa = Dinero.cero(faltante.moneda());
        UUID coberturaId = null;
        if (!faltante.esCero()) {
            var cobertura = respaldo.cubrir(e.grupoId(), e.periodoId(), e.turnoId(), faltante)
                    .orElseThrow(() -> new ErrorDeNegocio(
                            CodigoError.de(22, 8),
                            "No se pudo confirmar el respaldo con garantia: no se sabe si cubrio, reintenta."));
            if (cobertura.resultado() == RespaldoEmpresarial.Resultado.APLICADA) {
                if (!cobertura.cubierto().equals(faltante)) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(22, 7),
                            "El respaldo cubrio un importe distinto al faltante confirmado.",
                            Map.of(
                                    "faltante",
                                    faltante.toString(),
                                    "cubierto",
                                    cobertura.cubierto().toString()));
                }
                cubiertoEmpresa = cobertura.cubierto();
                coberturaId = cobertura.coberturaId();
            }
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
                FondeoDelPozo.calcular(recaudo, cubiertoEmpresa),
                coberturaId,
                recaudo.corteEn(),
                ctx);
    }
}
