package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU24RegistrarAsiento.EntradaAsiento;
import bo.aportaya.nucleofinanciero.dominio.OrigenAsiento;
import bo.aportaya.nucleofinanciero.dominio.Partida;
import bo.aportaya.nucleofinanciero.dominio.puertos.CuentasLogicas;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio.Cuenta;
import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DestinoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Vinculos;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carril I · {@code aportes.aporte_cobrado} mueve el dinero: debita la billetera del participante, acredita la del grupo y
 * lo asienta en el mayor, todo en UNA transaccion.
 *
 * <p>Antes nadie consumia el evento (brecha hallada por el carril G contra el stack real): el aporte quedaba {@code PAGADO}
 * en {@code aportes} y no se movia ni la billetera ni el mayor. {@code aportes} no escribe el libro (invariante 12); lo
 * PIDE por evento, y esta clase lo escribe con estas garantias (regla 91 y 98):
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} y el movimiento van en la MISMA transaccion. Un
 *       reintento del broker es un no-op ({@link Resultado#DUPLICADO}).
 *   <li><b>Una sola vez por pago</b>: la clave de idempotencia del libro es {@code aporte-cobrado:<pagoId>}, amparada por
 *       el titular (R-BIL-06, {@code uq_tx_idem}). Si el MISMO pago llega con otro id de evento, el libro ya lo tiene
 *       ({@link Resultado#YA_APLICADO}) y no se escribe nada.
 *   <li><b>Cuadrado en las dos contabilidades</b>: la transaccion de billetera ({@code APORTE_A_GRUPO}) tiene dos patas
 *       iguales (R-BIL-01) y el asiento del mayor debita {@code SALDO_USUARIOS} y acredita {@code SALDO_GRUPOS} por el
 *       mismo importe. El asiento se escribe PRIMERO y la transaccion nace apuntandolo; el asiento apunta a la
 *       transaccion ({@code origen_id}). Nada se actualiza despues (append-only).
 *   <li><b>Moneda</b>: el importe viaja con su moneda; las cuentas de billetera y las del plan se buscan EN ESA moneda. No
 *       existe conversion: sin cuenta en esa moneda, el evento se rechaza.
 *   <li><b>Sin dinero inventado</b>: el saldo del participante se lee del libro. Si una de las cuentas no existe o no esta
 *       operativa ({@code AP-CU12-02}/{@code 03}), NO se escribe nada y el evento NO se marca como consumido: el reproceso
 *       lo aplica cuando la causa se resuelva (se abre la cuenta, se descongela). Si el saldo no alcanza ({@code AP-CU12-01})
 *       el rechazo es DEFINITIVO: no se escribe en el libro ni en el mayor, el evento SE marca como consumido y se publica
 *       {@code nucleo_financiero.aporte_no_aplicado} para que {@code aportes} compense el pago.
 * </ul>
 *
 * <p><b>Alcance declarado:</b> solo el canal {@code BILLETERA_MOVIL} sale de una billetera. Los demas canales (QR,
 * tarjeta, transferencia bancaria, efectivo al organizador, compensacion interna, fondo de garantia) entran por la
 * conciliacion y por su propio asiento; aca no se inventa su contrapartida ({@link Resultado#CANAL_SIN_BILLETERA}, se
 * marca consumido y se avisa en el registro). Tampoco se aplican los limites por concepto de CU-40 (DECISION PENDIENTE de
 * producto/riesgos; ver el informe del carril Q).
 */
@Service
public class ConsumoDeAportes {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "nucleo-financiero.aportes-cobrados";

    /** Las cuentas logicas del plan que usa este consumidor (ver {@code application.yml}). */
    public static final String SALDO_USUARIOS = "SALDO_USUARIOS";

    public static final String SALDO_GRUPOS = "SALDO_GRUPOS";

    /** La cuenta logica del fondo de garantia: la misma que usa {@code garantia} para sus coberturas. */
    public static final String RESERVA_RESPALDO = "RESERVA_RESPALDO";

    /** El tipo de obligacion que NO es del grupo: el recargo por mora es del fondo de garantia (tableros, D1). */
    static final String RECARGO_MORA = "RECARGO_MORA";

    static final String CANAL_BILLETERA = "BILLETERA_MOVIL";
    private static final String ORIGEN_TRANSACCION = "OBLIGACION_APORTE";
    private static final String TIPO_TRANSACCION = "APORTE_A_GRUPO";

    /** El identificador de proceso del actor Sistema; no es una persona ni una fila de usuario. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-00000000a5e2");

    public enum Resultado {
        APLICADO,
        DUPLICADO,
        YA_APLICADO,
        CANAL_SIN_BILLETERA,
        /** Rechazo DEFINITIVO por saldo: no se movio nada y se publico {@code aporte_no_aplicado} para compensar. */
        RECHAZADO_POR_SALDO
    }

    /** Lo que {@code aportes.aporte_cobrado} trae, ya con tipos: el borde con Kafka valida y convierte. */
    public record AporteCobrado(
            UUID eventoId,
            UUID correlationId,
            UUID pagoId,
            UUID obligacionId,
            UUID grupoId,
            UUID usuarioId,
            Dinero monto,
            String canal,
            Optional<String> tipo) {

        /**
         * Un evento anterior al campo {@code tipo} (2026-10-16). Sin tipo, el cobro sigue el camino de siempre: billetera
         * del participante -> billetera del grupo. Asi conviven el productor viejo y el nuevo durante el despliegue.
         */
        public AporteCobrado(
                UUID eventoId,
                UUID correlationId,
                UUID pagoId,
                UUID obligacionId,
                UUID grupoId,
                UUID usuarioId,
                Dinero monto,
                String canal) {
            this(eventoId, correlationId, pagoId, obligacionId, grupoId, usuarioId, monto, canal, Optional.empty());
        }
    }

    public record Salida(Resultado resultado, Optional<UUID> transaccionId, Optional<UUID> asientoId) {
        static Salida sinEfecto(Resultado r) {
            return new Salida(r, Optional.empty(), Optional.empty());
        }
    }

    private final Datos datos;
    private final DestinoRepositorio destinos;
    private final CuentaBilleteraRepositorio cuentas;
    private final LibroDeBilletera libro;
    private final CU24RegistrarAsiento cu24;
    private final CuentasLogicas logicas;
    private final CuentasDeSistemaRepositorio sistema;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("nucleo_financiero");

    public ConsumoDeAportes(
            Datos datos,
            DestinoRepositorio destinos,
            CuentaBilleteraRepositorio cuentas,
            LibroDeBilletera libro,
            CU24RegistrarAsiento cu24,
            CuentasLogicas logicas,
            CuentasDeSistemaRepositorio sistema,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.destinos = destinos;
        this.cuentas = cuentas;
        this.libro = libro;
        this.cu24 = cu24;
        this.logicas = logicas;
        this.sistema = sistema;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public Salida procesar(AporteCobrado aporte) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(aporte.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, aporte.eventoId(), CONSUMIDOR)) {
                return Salida.sinEfecto(Resultado.DUPLICADO);
            }
            if (!CANAL_BILLETERA.equals(aporte.canal())) {
                return Salida.sinEfecto(Resultado.CANAL_SIN_BILLETERA);
            }
            if (aporte.monto().esCero() || aporte.monto().esNegativo()) {
                throw new ErrorDeNegocio(CodigoError.de(24, 7), "El aporte cobrado no es por un importe positivo.");
            }
            String clave = "aporte-cobrado:" + aporte.pagoId();
            Optional<UUID> usuario = Optional.of(aporte.usuarioId());
            libro.bloquearIdempotencia(dsl, usuario, ORIGEN_TRANSACCION, clave);
            var previa = libro.porClaveIdempotencia(dsl, usuario, ORIGEN_TRANSACCION, clave);
            if (previa.isPresent()) {
                return new Salida(Resultado.YA_APLICADO, previa, Optional.empty());
            }
            return aplicar(dsl, aporte, clave, ctx);
        });
    }

    private Salida aplicar(DSLContext dsl, AporteCobrado aporte, String clave, ContextoSesion ctx) {
        String moneda = aporte.monto().moneda().name();
        Dinero monto = aporte.monto();

        // El recargo por mora no es del grupo: va al fondo de garantia (tableros, D1). Repartirlo en el pozo seria
        // premiar a todos con la mora de uno. Un evento sin tipo (anterior al campo) sigue el camino de siempre.
        boolean alFondo = aporte.tipo().filter(RECARGO_MORA::equals).isPresent();

        // 1. El plan de cuentas EN ESA moneda (sin mapeo no se asienta ni se mueve nada: AP-CU24-02).
        String deUsuarios = codigoDelPlan(SALDO_USUARIOS, aporte);
        String deGrupos = codigoDelPlan(alFondo ? RESERVA_RESPALDO : SALDO_GRUPOS, aporte);

        // 2. Las dos billeteras EN ESA moneda, bloqueadas en orden de identificador (como CU-12: sin abrazo mortal).
        // Sin la del fondo se rechaza SIN marcar, igual que sin la del grupo: el reproceso lo aplica cuando exista, y
        // mientras tanto el recargo no cae en ninguna otra cuenta.
        UUID cuentaUsuario = destinos.cuentaDeUsuarioEn(dsl, aporte.usuarioId(), moneda)
                .orElseThrow(() -> sinCuenta("El participante no tiene billetera en " + moneda + ".", aporte));
        UUID cuentaGrupo = (alFondo
                        ? sistema.fondoDeGarantia(dsl, monto.moneda())
                        : destinos.cuentaDeGrupoEn(dsl, aporte.grupoId(), moneda))
                .orElseThrow(() -> sinCuenta(
                        (alFondo ? "El fondo de garantia" : "El grupo") + " no tiene billetera en " + moneda + ".",
                        aporte));
        List<UUID> enOrden =
                List.of(cuentaUsuario, cuentaGrupo).stream().sorted().toList();
        enOrden.forEach(id -> cuentas.bloquear(dsl, id));
        Cuenta origen = cuentas.ver(dsl, cuentaUsuario).orElseThrow();
        Cuenta destino = cuentas.ver(dsl, cuentaGrupo).orElseThrow();

        if (!origen.operativa()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(12, 3),
                    "La billetera del participante esta " + origen.estado() + ".",
                    detalle(aporte));
        }
        if (!destino.operativa()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(12, 3),
                    (alFondo ? "La billetera del fondo de garantia" : "La billetera del grupo") + " esta "
                            + destino.estado() + ".",
                    detalle(aporte));
        }
        // El saldo sale del LIBRO, no de la cache: lo que autoriza un debito es lo que el libro dice ahora.
        if (monto.esMayorQue(origen.disponible()) && !origen.permiteNegativo()) {
            // Rechazo DEFINITIVO (no se arregla reprocesando): se compensa. Nada se escribio todavia, asi que se
            // publica el
            // hecho en la misma transaccion que la marca de consumo y `aportes` revierte el pago (saga, regla 98.3).
            return rechazarPorSaldo(dsl, aporte);
        }

        // 3. El asiento PRIMERO (apunta a la transaccion que va a existir) y la transaccion con el asiento ya enlazado.
        UUID transaccionId = UUID.randomUUID();
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        String glosa = (alFondo ? "Recargo por mora cobrado, pago " : "Aporte cobrado, pago ") + aporte.pagoId();
        var asiento = cu24.ejecutar(
                dsl,
                new EntradaAsiento(
                        OrigenAsiento.PAGO,
                        transaccionId,
                        List.of(
                                new Partida(deUsuarios, monto.monto(), java.math.BigDecimal.ZERO, monto.moneda()),
                                new Partida(deGrupos, java.math.BigDecimal.ZERO, monto.monto(), monto.moneda())),
                        glosa,
                        Optional.of(aporte.grupoId())),
                ctx);
        libro.registrarVinculada(
                dsl,
                transaccionId,
                alFondo ? RECARGO_MORA : TIPO_TRANSACCION,
                ORIGEN_TRANSACCION,
                aporte.obligacionId(),
                "API",
                monto,
                clave,
                Optional.of(aporte.usuarioId()),
                List.of(Pata.debito(cuentaUsuario, monto, glosa), Pata.credito(cuentaGrupo, monto, glosa)),
                new Vinculos(Optional.of(aporte.grupoId()), Optional.of(asiento.asientoId())),
                ahora);

        outbox.emitir(
                dsl,
                new EventoDominio(
                        "nucleo_financiero.aporte_aplicado",
                        "obligacion_aporte",
                        aporte.obligacionId(),
                        Map.of(
                                "pagoId", aporte.pagoId().toString(),
                                "obligacionId", aporte.obligacionId().toString(),
                                "grupoId", aporte.grupoId().toString(),
                                "transaccionId", transaccionId.toString(),
                                "asientoId", asiento.asientoId().toString(),
                                "monto", monto.toString(),
                                "moneda", moneda,
                                // garantia lo lee para anotar el ingreso en su fondo (H1.S2.M1c). Campo sumado.
                                "destino", alFondo ? "FONDO_GARANTIA" : "GRUPO"),
                        aporte.correlationId()));
        return new Salida(Resultado.APLICADO, Optional.of(transaccionId), Optional.of(asiento.asientoId()));
    }

    /**
     * AP-CU12-01 como hecho, no como excepcion: una excepcion dejaba el aporte {@code PAGADO} en {@code aportes} sin que el
     * dinero se hubiera movido, y el contenedor de Kafka terminaba descartando el evento tras agotar los reintentos. El
     * evento queda marcado como consumido (el reproceso no repite el aviso) y {@code aportes} lo compensa de forma
     * idempotente por {@code pagoId}.
     */
    private Salida rechazarPorSaldo(DSLContext dsl, AporteCobrado aporte) {
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "nucleo_financiero.aporte_no_aplicado",
                        "obligacion_aporte",
                        aporte.obligacionId(),
                        Map.of(
                                "pagoId", aporte.pagoId().toString(),
                                "obligacionId", aporte.obligacionId().toString(),
                                "grupoId", aporte.grupoId().toString(),
                                "monto", aporte.monto().toString(),
                                "moneda", aporte.monto().moneda().name(),
                                "motivo", "SALDO_INSUFICIENTE",
                                "codigo", CodigoError.de(12, 1).valor()),
                        aporte.correlationId()));
        return Salida.sinEfecto(Resultado.RECHAZADO_POR_SALDO);
    }

    private String codigoDelPlan(String cuentaLogica, AporteCobrado aporte) {
        return logicas.codigoDe(cuentaLogica, aporte.monto().moneda())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(24, 2),
                        "La cuenta logica «%s» no tiene codigo en el plan de cuentas para %s."
                                .formatted(cuentaLogica, aporte.monto().moneda()),
                        Map.of(
                                "cuentaLogica",
                                cuentaLogica,
                                "moneda",
                                aporte.monto().moneda().name())));
    }

    private static ErrorDeNegocio sinCuenta(String mensaje, AporteCobrado aporte) {
        return new ErrorDeNegocio(CodigoError.de(12, 2), mensaje, detalle(aporte));
    }

    /** Identificadores, nunca importes ni datos de personas (regla 90.2). */
    private static Map<String, Object> detalle(AporteCobrado aporte) {
        return Map.of(
                "pagoId",
                aporte.pagoId().toString(),
                "obligacionId",
                aporte.obligacionId().toString());
    }
}
