package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.SaldoDeLaObligacion;
import bo.aportaya.aportes.infraestructura.AuditoriaRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-21 · compensar el aporte que el libro no pudo aplicar (saga de aporte, regla 98.3).
 *
 * <p>{@code aportes} acredita el pago ({@code PAGADO}) y le PIDE el movimiento de dinero a {@code nucleo-financiero} por
 * {@code aportes.aporte_cobrado}. Si la billetera de la persona no tiene saldo (AP-CU12-01) nucleo no mueve nada y publica
 * {@code nucleo_financiero.aporte_no_aplicado}: esta clase lo CONSUME y deshace lo que {@code aportes} habia dado por hecho.
 * Sin esto la obligacion quedaba {@code PAGADA} sin que el dinero se hubiera movido, y el grupo cobraba un aporte inexistente.
 *
 * <p>La compensacion es la del reembolso (CU-19), sin mover plata: el pago pasa a {@code REVERSADO} y la obligacion vuelve
 * a estar pendiente por ese importe (a {@code EN_MORA} si ya vencio), para que la persona pueda volver a pagar. Garantias:
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} va en la MISMA transaccion que el cambio.
 *   <li><b>Una sola vez por pago</b>: la precondicion va en la escritura ({@code UPDATE ... WHERE estado = 'ACREDITADO'}),
 *       asi que otro evento del mismo pago (el productor republica) encuentra el pago ya {@code REVERSADO} y no hace nada.
 *   <li><b>El importe sale del pago guardado</b>, no del evento; y el pago tiene que ser de la obligacion que el evento dice.
 *   <li><b>No se compensa lo que ya se toco por otro camino</b>: un pago con reembolsos o en disputa queda para una persona
 *       ({@link Resultado#NO_COMPENSABLE}), porque revertirlo dos veces dejaria la obligacion con menos pagado que cero.
 * </ul>
 *
 * <p><b>Alcance declarado:</b> solo compensa el rechazo por saldo insuficiente, que es el unico que {@code nucleo} publica
 * como definitivo. Un aporte por un canal que no sale de una billetera nunca llega aca.
 */
@Service
public class CU21CompensarAporte {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "aportes.aportes-no-aplicados";

    /** El identificador de proceso del actor Sistema; no es una persona ni una fila de usuario. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-00000000a5e3");

    public enum Resultado {
        COMPENSADO,
        DUPLICADO,
        YA_COMPENSADO,
        PAGO_INEXISTENTE,
        NO_COMPENSABLE
    }

    /** Lo que {@code nucleo_financiero.aporte_no_aplicado} trae, ya con tipos: el borde con Kafka valida y convierte. */
    public record AporteNoAplicado(UUID eventoId, UUID correlationId, UUID pagoId, UUID obligacionId, String motivo) {}

    public record Salida(Resultado resultado, Optional<String> estadoDeLaObligacion) {}

    private final Datos datos;
    private final PagoRepositorio pagos;
    private final ObligacionRepositorio obligaciones;
    private final Consumidos consumidos;
    private final Outbox outbox;
    private final AuditoriaRepositorio auditoria;
    private final Reloj reloj;

    public CU21CompensarAporte(
            Datos datos,
            PagoRepositorio pagos,
            ObligacionRepositorio obligaciones,
            Consumidos consumidos,
            Outbox outbox,
            AuditoriaRepositorio auditoria,
            Reloj reloj) {
        this.datos = datos;
        this.pagos = pagos;
        this.obligaciones = obligaciones;
        this.consumidos = consumidos;
        this.outbox = outbox;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Salida procesar(AporteNoAplicado e) {
        ContextoSesion ctx = ContextoSesion.deSistema(PROCESO, new Traza(e.correlationId().toString()));
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, e.eventoId(), CONSUMIDOR)) {
                return sinEfecto(Resultado.DUPLICADO);
            }
            var pago = pagos.ver(dsl, e.pagoId());
            if (pago.isEmpty()) {
                return sinEfecto(Resultado.PAGO_INEXISTENTE);
            }
            if (!pago.get().obligacionId().equals(e.obligacionId())) {
                // El sobre afirma algo que el pago guardado desmiente: no se compensa a ciegas (y no se marca consumido).
                throw new IllegalArgumentException("El pago " + e.pagoId() + " no es de la obligacion del evento.");
            }
            // El candado de la obligacion serializa contra un cobro, un reembolso o otra compensacion del mismo pago.
            var obligacion = obligaciones
                    .bloquear(dsl, pago.get().obligacionId())
                    .orElseThrow(() -> new IllegalStateException("La obligacion del pago no existe."));
            // Con el candado en la mano se vuelve a mirar el pago: otro evento del mismo pago pudo compensarlo mientras esperaba.
            if ("REVERSADO".equals(pagos.ver(dsl, e.pagoId()).orElseThrow().estado())) {
                return sinEfecto(Resultado.YA_COMPENSADO);
            }
            boolean conReembolsos = !pagos.reembolsadoDe(dsl, pago.get().id(), pago.get().monto().moneda())
                    .esCero();
            if (conReembolsos || !pagos.reversar(dsl, pago.get().id())) {
                return sinEfecto(Resultado.NO_COMPENSABLE);
            }

            var monto = pago.get().monto();
            var saldoDespues = new SaldoDeLaObligacion.Estado(
                    obligacion.saldo().esperado(),
                    obligacion.saldo().pagado().menos(monto),
                    obligacion.saldo().condonado(),
                    obligacion.saldo().cubiertoPorGarantia());
            boolean vencida = obligacion.finDeGracia().isBefore(reloj.hoy());
            String estadoNuevo = SaldoDeLaObligacion.estadoSegunSaldo(saldoDespues, vencida);
            obligaciones.revertirPago(dsl, obligacion.id(), monto, estadoNuevo);

            // Rastro append-only de una correccion automatica sobre dinero (regla 90.2.7): sin actor persona.
            auditoria.registrar(
                    dsl,
                    "pago",
                    pago.get().id(),
                    "CAMBIO_ESTADO",
                    null,
                    "SISTEMA",
                    e.correlationId(),
                    "{\"estado\":\"ACREDITADO\"}",
                    "{\"estado\":\"REVERSADO\",\"obligacionId\":\"%s\"}".formatted(obligacion.id()),
                    e.motivo(),
                    ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "aportes.aporte_compensado",
                            "obligacion_aporte",
                            obligacion.id(),
                            Map.of(
                                    "pagoId", pago.get().id().toString(),
                                    "obligacionId", obligacion.id().toString(),
                                    "monto", monto.toString(),
                                    "moneda", monto.moneda().name(),
                                    "motivo", e.motivo(),
                                    "estadoDeLaObligacion", estadoNuevo),
                            e.correlationId()));
            return new Salida(Resultado.COMPENSADO, Optional.of(estadoNuevo));
        });
    }

    private static Salida sinEfecto(Resultado r) {
        return new Salida(r, Optional.empty());
    }
}
