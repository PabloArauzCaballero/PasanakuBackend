package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.aplicacion.PasosDeEntregaAutomatica.Pendiente;
import bo.aportaya.entregas.dominio.puertos.MetodoDeDesembolso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * F3.L2.M01 · {@code entregas} fondea sola cuando {@code aportes} avisa que el recaudo del periodo esta listo
 * ({@code aportes.periodo_recaudado}). Nadie tiene que pedir la entrega a mano.
 *
 * <p>La saga (regla 98.3, ADR-028) tiene su estado en {@code entregas.estado_saga}, una fila por periodo:
 *
 * <ol>
 *   <li>El aviso se registra con la marca del evento en la MISMA transaccion: un aviso repetido es un no-op.
 *   <li>Se busca el turno del periodo en la copia de {@code grupos}. Si todavia no llego, la saga espera: la toma el
 *       barrido cuando llegue (fuera de orden).
 *   <li>Se fondea con {@link CU22EntregarPozoCompleto}: le pregunta el recaudo a {@code aportes} FUERA de toda
 *       transaccion. Si {@code aportes} no responde, no se escribe nada y el barrido reintenta.
 *   <li>La clave {@code fondeo-auto-<periodoId>} sale del hecho, no del mensaje, y el fondeo es unico por turno: dos
 *       avisos del mismo periodo (gracia vencida y despues completo) completan el MISMO fondeo.
 * </ol>
 *
 * <p>No toca el fondo del grupo (eso espera a L-01): si falta plata, la entrega queda bloqueada con la deuda
 * conservada, como cuando se pide a mano. <b>El metodo de desembolso es DECISION_REQUIRED</b>: lo da el puerto
 * {@link MetodoDeDesembolso}; si todavia no hay metodo, la saga espera y no fondea. Todo esto esta apagado por omision.
 */
@Service
@ConditionalOnProperty(name = "aportaya.entrega-automatica.habilitada", havingValue = "true")
public class EntregaAutomatica {

    private static final Logger BITACORA = LoggerFactory.getLogger(EntregaAutomatica.class);

    /** aportes no respondio (22-06) u otro pedido movio el fondeo primero (22-09): se reintenta. */
    private static final Set<CodigoError> REINTENTABLES = Set.of(CodigoError.de(22, 6), CodigoError.de(22, 9));

    public enum Resultado {
        FONDEADO,
        CON_PENDIENTE,
        ESPERA_TURNO,
        ESPERA_APORTES,
        ESPERA_METODO,
        RECHAZADO,
        SIN_PENDIENTE,
        DUPLICADO
    }

    /** Lo que trae {@code aportes.periodo_recaudado}: identificadores y motivo, sin importes. */
    public record PeriodoRecaudado(UUID eventoId, UUID correlationId, UUID periodoId, UUID grupoId, String motivo) {}

    private final PasosDeEntregaAutomatica pasos;
    private final CU22EntregarPozoCompleto pozo;
    private final Reloj reloj;
    private final MetodoDeDesembolso metodos;

    public EntregaAutomatica(
            PasosDeEntregaAutomatica pasos, CU22EntregarPozoCompleto pozo, Reloj reloj, MetodoDeDesembolso metodos) {
        this.pasos = pasos;
        this.pozo = pozo;
        this.reloj = reloj;
        this.metodos = metodos;
    }

    public Resultado recibir(PeriodoRecaudado aviso) {
        if (!pasos.avisar(
                aviso.eventoId(), aviso.correlationId(), aviso.periodoId(), aviso.grupoId(), aviso.motivo())) {
            return Resultado.DUPLICADO;
        }
        return avanzar(aviso.periodoId());
    }

    /** Un paso de la saga del periodo. Lo llaman el consumidor y el barrido; repetirlo no duplica nada. */
    public Resultado avanzar(UUID periodoId) {
        Pendiente p = pasos.pendiente(periodoId).orElse(null);
        if (p == null) {
            return Resultado.SIN_PENDIENTE;
        }
        String correlacion = p.saga().correlationId();
        if (p.turno().isEmpty()) {
            pasos.postergar(periodoId, correlacion);
            BITACORA.info("entrega automatica del periodo {}: todavia no se conoce el turno", periodoId);
            return Resultado.ESPERA_TURNO;
        }
        var turno = p.turno().get();
        if (!turno.grupoId().equals(p.saga().grupoId())) {
            return rechazar(periodoId, correlacion, "el turno del periodo es de otro grupo");
        }
        var metodo = metodos.para(turno.titularParticipanteId());
        if (metodo.isEmpty()) {
            pasos.postergar(periodoId, correlacion);
            BITACORA.info("entrega automatica del periodo {}: todavia no hay metodo de desembolso", periodoId);
            return Resultado.ESPERA_METODO;
        }
        if (!MetodoDeDesembolso.METODOS.contains(metodo.get())) {
            return rechazar(periodoId, correlacion, "metodo de desembolso fuera del contrato");
        }
        try {
            var salida = pozo.fondear(
                    new CU22EntregarPozoCompleto.Entrada(
                            turno.grupoId(),
                            periodoId,
                            turno.turnoId(),
                            turno.cupoId(),
                            turno.titularParticipanteId(),
                            metodo.get(),
                            reloj.hoy(),
                            "fondeo-auto-" + periodoId),
                    PasosDeEntregaAutomatica.contexto(correlacion));
            boolean completo = "FONDEADO".equals(salida.estadoDelFondeo());
            pasos.cerrar(periodoId, completo ? "COMPLETADA" : "EN_CURSO", completo ? 2 : 1, correlacion);
            BITACORA.info(
                    "entrega automatica del periodo {}: entrega {} {}",
                    periodoId,
                    salida.entregaId(),
                    salida.estadoDelFondeo());
            return completo ? Resultado.FONDEADO : Resultado.CON_PENDIENTE;
        } catch (ErrorDeNegocio e) {
            if (REINTENTABLES.contains(e.codigo())) {
                pasos.postergar(periodoId, correlacion);
                BITACORA.warn("entrega automatica del periodo {}: se reintenta ({})", periodoId, e.codigo());
                return Resultado.ESPERA_APORTES;
            }
            return rechazar(periodoId, correlacion, e.codigo().toString());
        }
    }

    /** Toma las sagas que esperan desde hace mas de {@code espera} y les da un paso a cada una. */
    public int barrer(Duration espera, int limite) {
        var antesDe = reloj.ahora().minus(espera).atOffset(ZoneOffset.UTC);
        int avanzadas = 0;
        for (UUID periodoId : pasos.iniciadas(antesDe, limite)) {
            try {
                var r = avanzar(periodoId);
                if (r == Resultado.FONDEADO || r == Resultado.CON_PENDIENTE) {
                    avanzadas++;
                }
            } catch (RuntimeException e) {
                // Una saga rota no frena a las demas; el error queda con su identificador, sin datos de personas.
                BITACORA.error("entrega automatica del periodo {}: fallo el paso", periodoId, e);
            }
        }
        return avanzadas;
    }

    private Resultado rechazar(UUID periodoId, String correlacion, String motivo) {
        pasos.cerrar(periodoId, "FALLIDA", 9, correlacion);
        // Pide una persona: el aviso no cierra con lo que afirman aportes o grupos.
        BITACORA.error("entrega automatica del periodo {}: RECHAZADA ({})", periodoId, motivo);
        return Resultado.RECHAZADO;
    }
}
