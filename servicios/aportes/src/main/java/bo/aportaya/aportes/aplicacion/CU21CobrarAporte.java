package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.RecargoDeMora;
import bo.aportaya.aportes.dominio.SaldoDeLaObligacion;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-21 · Cobrar el aporte del periodo.
 *
 * <p>Este servicio **no mueve el saldo**: pide a nucleo-financiero que lo mueva y
 * registra el pago cuando le confirman. El invariante 12 dice que solo el nucleo
 * escribe el libro, y por eso aca no hay ni una linea que toque una billetera.
 *
 * <p>La obligacion se bloquea antes de acreditar. Dos pagos simultaneos sobre la misma
 * cuota sin bloqueo leen el mismo {@code monto_pagado} y los dos escriben el suyo: el
 * segundo pisa al primero, y la persona pago dos veces por una cuota registrada una.
 */
@Service
public class CU21CobrarAporte {

    private final Datos datos;
    private final ObligacionRepositorio obligaciones;
    private final PagoRepositorio pagos;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU21CobrarAporte(
            Datos datos, ObligacionRepositorio obligaciones, PagoRepositorio pagos, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public SalidaCobro acreditar(EntradaCobro entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            // La clave se valida ANTES de escribir (invariante 7): el webhook del
            // proveedor llega dos veces mas seguido de lo que uno cree.
            var repetido = repeticion(dsl, entrada);
            if (repetido.isPresent()) {
                return repetido.get();
            }

            var obligacion = obligaciones
                    .bloquear(dsl, entrada.obligacionId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(21, 1), "Esa obligacion no existe."));

            // Carril G (hallado con 8 envios simultaneos de la MISMA clave): el reintento que llega mientras el
            // original todavia no
            // confirmo pasa la comprobacion de arriba, espera el candado de la obligacion y, al obtenerlo, la ve PAGADA
            // y recibia
            // AP-CU21-01 en vez de la respuesta del original. Con el candado en la mano se vuelve a mirar: no hay doble
            // cobro
            // (ya no lo habia), pero ahora el reintento recibe lo mismo que el original.
            var repetidoTrasElCandado = repeticion(dsl, entrada);
            if (repetidoTrasElCandado.isPresent()) {
                return repetidoTrasElCandado.get();
            }

            // AP-CU21-01.
            if ("PAGADO".equals(obligacion.estado()) || "ANULADO".equals(obligacion.estado())) {
                throw new ErrorDeNegocio(CodigoError.de(21, 1), "Esa obligacion esta " + obligacion.estado() + ".");
            }
            // AP-CU21-04: el periodo lo resuelve grupos y llega ya decidido — este
            // servicio no lee el esquema de grupos (invariante 11).
            if (!entrada.periodoAbierto()) {
                throw new ErrorDeNegocio(CodigoError.de(21, 4), "El periodo ya se cerro.");
            }
            // AP-CU21-02: pagar de mas no es un favor, es un descuadre. El excedente
            // se devuelve por su propio camino, no se mete en la cuota.
            if (entrada.monto().esMayorQue(obligacion.saldo().pendiente())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 2),
                        "El monto no coincide: falta " + obligacion.saldo().pendiente() + ".");
            }

            UUID pagoId = pagos.registrar(
                    dsl,
                    obligacion.id(),
                    entrada.proveedorId(),
                    entrada.monto(),
                    entrada.comision(),
                    entrada.monto().menos(entrada.comision()),
                    entrada.canal(),
                    entrada.referenciaProveedor(),
                    entrada.claveIdempotencia(),
                    entrada.esManual(),
                    Optional.of(ctx.usuarioId()),
                    ahora);

            var saldoDespues = new SaldoDeLaObligacion.Estado(
                    obligacion.saldo().esperado(),
                    obligacion.saldo().pagado().mas(entrada.monto()),
                    obligacion.saldo().condonado(),
                    obligacion.saldo().cubiertoPorGarantia());
            // El dia de la obligacion es el de La Paz, no el de UTC: a las 21:00 de La Paz del ultimo dia de
            // gracia, UTC ya marca el dia siguiente y la persona quedaria en mora por pagar a tiempo.
            boolean vencida = obligacion.finDeGracia().isBefore(reloj.hoy());
            String estadoNuevo = SaldoDeLaObligacion.estadoSegunSaldo(saldoDespues, vencida);

            if (!obligaciones.acreditar(
                    dsl, obligacion.id(), entrada.monto(), estadoNuevo, obligacion.version(), ahora)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 1), "Otro pago modifico esa obligacion primero: reintenta.");
            }

            // El movimiento de dinero lo hace nucleo-financiero, no este servicio.
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "aportes.aporte_cobrado",
                            "obligacion_aporte",
                            obligacion.id(),
                            Map.of(
                                    "pagoId", pagoId.toString(),
                                    "grupoId", obligacion.grupoId().toString(),
                                    // Sin el participante, cumplimiento no puede
                                    // registrar a nombre de quien se supero el umbral
                                    // (R-UIF-02): el control queda apagado sin que se note.
                                    "participanteId",
                                            obligacion.participanteId().toString(),
                                    "monto", entrada.monto().toString(),
                                    "estado", estadoNuevo,
                                    // Carril I: lo que nucleo-financiero necesita para mover el dinero SIN adivinar. Se
                                    // SUMAN
                                    // campos (los consumidores previos los ignoran). `monto` nunca viajo sin moneda en
                                    // la
                                    // base, pero si en el evento (regla 91.1.2). `usuarioId` es quien paga: el
                                    // controlador
                                    // ya comprobo que es el titular de la obligacion (AP-CU21-06), y `participanteId`
                                    // es el
                                    // de `grupos`, no una billetera.
                                    "obligacionId", obligacion.id().toString(),
                                    "moneda", entrada.monto().moneda().name(),
                                    "canal", entrada.canal(),
                                    "usuarioId", ctx.usuarioId().toString(),
                                    // Tableros (H1.S2): sin el tipo, nucleo-financiero asienta TODO cobro al
                                    // pozo del grupo, y un RECARGO_MORA terminaria repartido en vez de en el
                                    // fondo de garantia. Campo sumado: los consumidores previos lo ignoran.
                                    // Es el decimo par: Map.of no admite mas; el proximo campo pide Map.ofEntries.
                                    "tipo", obligacion.tipo()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaCobro(pagoId, obligacion.id(), estadoNuevo, saldoDespues.pendiente(), true);
        });
    }

    /** El resultado del pago que ya uso esta clave sobre esta obligacion, si lo hay. */
    private Optional<SalidaCobro> repeticion(DSLContext dsl, EntradaCobro entrada) {
        return pagos.porClaveIdempotencia(dsl, entrada.obligacionId(), entrada.claveIdempotencia())
                .map(pagoId -> {
                    var pago = pagos.ver(dsl, pagoId).orElseThrow();
                    var obligacion = obligaciones.ver(dsl, pago.obligacionId()).orElseThrow();
                    return new SalidaCobro(
                            pago.id(),
                            obligacion.id(),
                            obligacion.estado(),
                            obligacion.saldo().pendiente(),
                            false);
                });
    }

    /**
     * Trabajo diario: genera el recargo de las vencidas.
     *
     * <p>**Uno por obligacion, no uno por corrida.** Si el trabajo corre dos veces el
     * mismo dia —o si se reintenta tras un fallo— la persona no puede terminar con dos
     * recargos por el mismo atraso.
     */
    @Transactional
    public SalidaRecargos generarRecargos(ContextoSesion ctx) {
        LocalDate hoy = reloj.hoy();

        return datos.conContexto(ctx, dsl -> {
            List<UUID> generados = new ArrayList<>();
            for (UUID obligacionId : obligaciones.vencidasSinRecargo(dsl, hoy)) {
                var obligacion = obligaciones.ver(dsl, obligacionId).orElseThrow();
                var politica = obligaciones.politicaDe(dsl, Optional.of(obligacion.grupoId()));
                if (politica.isEmpty()) {
                    // Sin politica no se inventa un recargo: cobrar sin regla escrita
                    // es exactamente lo que la persona va a reclamar, con razon.
                    continue;
                }
                var p = politica.get();
                int atraso = (int) ChronoUnit.DAYS.between(obligacion.finDeGracia(), hoy);
                var calculo = RecargoDeMora.calcular(
                        obligacion.saldo().pendiente(),
                        atraso,
                        new RecargoDeMora.Politica(
                                0,
                                RecargoDeMora.Tipo.valueOf(p.tipoRecargo()),
                                p.valorRecargo(),
                                Dinero.de(
                                        p.topeRecargo(),
                                        obligacion.saldo().esperado().moneda()),
                                p.diasParaMoraGrave(),
                                p.diasParaIncumplimiento()));

                if (calculo.recargo().esCero()) {
                    continue;
                }
                UUID recargoId = obligaciones.crearRecargo(
                        dsl, obligacion, calculo.recargo(), calculo.diasDeMora(), hoy.plusDays(1));
                generados.add(recargoId);

                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "aportes.recargo_generado",
                                "obligacion_aporte",
                                recargoId,
                                Map.of(
                                        "obligacionOrigenId", obligacion.id().toString(),
                                        "monto", calculo.recargo().toString(),
                                        "severidad", calculo.severidad()),
                                UUID.fromString(ctx.traza().id())));
            }
            return new SalidaRecargos(generados.size(), generados);
        });
    }

    /** De quien es la obligacion y de que periodo: lo que `grupos` necesita para decir si se puede pagar. */
    @Transactional(readOnly = true)
    public Optional<ContextoDeObligacion> contextoDe(UUID obligacionId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> obligaciones
                .ver(dsl, obligacionId)
                .map(o -> new ContextoDeObligacion(o.participanteId(), o.periodoId())));
    }

    public record ContextoDeObligacion(UUID participanteId, UUID periodoId) {}

    public record EntradaCobro(
            String claveIdempotencia,
            UUID obligacionId,
            Dinero monto,
            Dinero comision,
            String canal,
            String referenciaProveedor,
            Optional<UUID> proveedorId,
            boolean esManual,
            boolean periodoAbierto) {}

    public record SalidaCobro(
            UUID pagoId, UUID obligacionId, String estadoObligacion, Dinero pendiente, boolean esNuevo) {}

    public record SalidaRecargos(int generados, List<UUID> obligacionesDeRecargo) {}
}
