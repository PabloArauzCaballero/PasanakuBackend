package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.dominio.ClasificadorDeQr;
import bo.aportaya.nucleofinanciero.dominio.ContenidoDeQr;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.QrRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.QrRepositorio.Qr;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * H4.S1.M5 y M6 · Transferir por QR interno, y no confundirlo con un QR bancario.
 *
 * <p>Un QR interno es un cobro que una billetera de Pasanaku publica. Hay dos modalidades y el
 * comportamiento de cada una es distinto a proposito: el <b>dinamico</b> fija importe y
 * vencimiento y se consume al pagarse (un segundo pago, de quien sea, se rechaza); el
 * <b>estatico</b> es el de una persona o un comercio, no fija importe, no vence y puede pagarse
 * muchas veces, cada una con su propia clave de idempotencia.
 *
 * <p>Quien paga ve primero a quien y cuanto ({@link #leer}) y confirma ese importe al pagar
 * ({@link #pagar}): pagar un QR dinamico por un importe distinto del que se le mostro se rechaza.
 * El pago es una transferencia comun (CU-12), en la misma transaccion en que se marca el QR como
 * usado: o ocurren las dos cosas o ninguna.
 *
 * <p>Un QR interoperable (el de otro sistema de pagos) NO se procesa aca: no hay proveedor
 * habilitado, y decir que se acepta seria prometer un cobro que nadie va a hacer.
 */
@Service
public class QrInterno {

    private static final int CONCEPTO_MAXIMO = 140;

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final QrRepositorio qrs;
    private final LibroDeBilletera libro;
    private final CU12TransferirSaldo transferencias;
    private final Outbox outbox;
    private final Reloj reloj;
    private final ContenidoDeQr contenido;
    private final Duration vigenciaDelDinamico;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public QrInterno(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            QrRepositorio qrs,
            LibroDeBilletera libro,
            CU12TransferirSaldo transferencias,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.qr.clave}") String clave,
            @Value("${aportaya.qr.vigencia-del-dinamico:PT15M}") Duration vigenciaDelDinamico) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.qrs = qrs;
        this.libro = libro;
        this.transferencias = transferencias;
        this.outbox = outbox;
        this.reloj = reloj;
        this.contenido = new ContenidoDeQr(clave);
        this.vigenciaDelDinamico = vigenciaDelDinamico;
    }

    @Transactional
    public QrEmitido emitir(EntradaQr entrada, ContextoSesion ctx) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            var cuenta = cuentas.ver(dsl, entrada.cuentaBilleteraId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(12, 2), "Esa billetera no existe."));
            if (!ctx.usuarioId().equals(cuenta.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(12, 2), "Solo el titular puede emitir un QR de su billetera.");
            }
            if (!cuenta.operativa()) {
                throw new ErrorDeNegocio(CodigoError.de(12, 3), "La billetera esta " + cuenta.estado() + ": no cobra.");
            }
            boolean dinamico = "DINAMICO".equals(entrada.modalidad());
            if (!dinamico && !"ESTATICO".equals(entrada.modalidad())) {
                throw new ErrorDeNegocio(CodigoError.de(12, 7), "La modalidad del QR es DINAMICO o ESTATICO.");
            }
            boolean importeValido = dinamico
                    ? entrada.monto()
                            .filter(m -> m.moneda() == cuenta.moneda() && m.esMayorQue(Dinero.cero(m.moneda())))
                            .isPresent()
                    : entrada.monto().isEmpty();
            if (!importeValido) {
                throw new ErrorDeNegocio(
                        CodigoError.de(12, 7),
                        "El QR dinamico lleva un importe positivo en la moneda de la billetera; el estatico no lleva importe.");
            }
            if (entrada.concepto().filter(c -> c.length() > CONCEPTO_MAXIMO).isPresent()) {
                throw new ErrorDeNegocio(CodigoError.de(12, 7), "El concepto del QR admite hasta 140 caracteres.");
            }
            var expira = dinamico ? Optional.of(ahora.plus(vigenciaDelDinamico)) : Optional.<OffsetDateTime>empty();
            UUID id = qrs.crear(
                    dsl,
                    cuenta.id(),
                    entrada.modalidad(),
                    entrada.monto(),
                    cuenta.moneda(),
                    entrada.concepto(),
                    expira,
                    ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.qr_emitido",
                            "qr_transferencia",
                            id,
                            Map.of("cuentaBilleteraId", cuenta.id().toString(), "modalidad", entrada.modalidad()),
                            UUID.fromString(ctx.traza().id())));
            return new QrEmitido(id, contenido.de(id), entrada.modalidad(), entrada.monto(), expira);
        });
    }

    /** Lo que ve quien va a pagar, antes de pagar. No cambia nada. */
    @Transactional(readOnly = true)
    public QrLeido leer(String texto, ContextoSesion ctx) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        UUID id = identificar(texto);
        return datos.conContexto(ctx, dsl -> {
            var qr = qrs.ver(dsl, id).orElseThrow(QrInterno::noValido);
            exigirVigente(qr, ahora);
            return new QrLeido(
                    qr.id(), qr.modalidad(), qr.monto(), destinatario(dsl, qr), qr.concepto(), qr.expiraEn());
        });
    }

    @Transactional
    public ComprobanteQr pagar(EntradaPagoQr entrada, ContextoSesion ctx) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        UUID id = identificar(entrada.contenido());
        return datos.conContexto(ctx, dsl -> {
            var qr = qrs.bloquear(dsl, id).orElseThrow(QrInterno::noValido);
            var repetido = pagoPrevio(dsl, qr, entrada, ctx);
            if (repetido.isPresent()) {
                return repetido.get();
            }
            exigirVigente(qr, ahora);
            Dinero importe = importeConfirmado(qr, entrada);
            if (qr.cuentaId().equals(entrada.cuentaOrigenId())) {
                throw new ErrorDeNegocio(CodigoError.de(12, 2), "No podes pagarte un QR a vos mismo.");
            }
            boolean esReintento = libro.porClaveIdempotencia(
                            dsl, ctx.usuarioId(), "TRANSFERENCIA_P2P", entrada.claveIdempotencia())
                    .isPresent();
            var salida = transferencias.ejecutar(
                    new EntradaTransferencia(
                            entrada.claveIdempotencia(),
                            entrada.cuentaOrigenId(),
                            qr.cuentaId(),
                            importe,
                            qr.concepto().orElse("Pago por QR"),
                            Optional.empty(),
                            Optional.empty()),
                    ctx);
            if (qr.esDinamico() && !qrs.usar(dsl, qr.id(), salida.transaccionId(), ahora)) {
                throw new ErrorDeNegocio(CodigoError.de(12, 9), "Ese QR ya se uso.");
            }
            if (!esReintento) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "nucleo_financiero.qr_pagado",
                                "qr_transferencia",
                                qr.id(),
                                Map.of("transaccionId", salida.transaccionId().toString()),
                                UUID.fromString(ctx.traza().id())));
            }
            return comprobante(dsl, qr, salida.transaccionId(), importe, salida.saldoDespues());
        });
    }

    /** El texto a su cobro: solo un QR interno con firma valida pasa; lo demas se dice por lo que es. */
    private UUID identificar(String texto) {
        return switch (ClasificadorDeQr.de(texto)) {
            case INTERNO -> contenido.verificar(texto).orElseThrow(QrInterno::noValido);
            case INTEROPERABLE_BANCARIO ->
                throw new ErrorDeNegocio(
                        CodigoError.de(12, 10),
                        "Este QR es de otro sistema de pagos y todavia no lo aceptamos. Pedile un QR de Pasanaku.");
            case DESCONOCIDO -> throw noValido();
        };
    }

    private static ErrorDeNegocio noValido() {
        return new ErrorDeNegocio(CodigoError.de(12, 7), "Este QR no es valido.");
    }

    private void exigirVigente(Qr qr, OffsetDateTime ahora) {
        if (!"VIGENTE".equals(qr.estado())) {
            throw new ErrorDeNegocio(CodigoError.de(12, 9), "Ese QR ya no esta disponible (" + qr.estado() + ").");
        }
        if (qr.expiraEn().filter(vence -> !ahora.isBefore(vence)).isPresent()) {
            throw new ErrorDeNegocio(CodigoError.de(12, 8), "Ese QR vencio. Pedi uno nuevo.");
        }
    }

    /** El QR dinamico se paga por SU importe, confirmado; el estatico, por el que la persona indica. */
    private Dinero importeConfirmado(Qr qr, EntradaPagoQr entrada) {
        if (qr.esDinamico()) {
            var fijado = qr.monto().orElseThrow();
            if (entrada.monto().filter(fijado::equals).isEmpty()) {
                throw new ErrorDeNegocio(CodigoError.de(12, 11), "Confirma el importe del QR: es " + fijado + ".");
            }
            return fijado;
        }
        var indicado = entrada.monto()
                .filter(m -> m.esMayorQue(Dinero.cero(m.moneda())) && m.moneda() == qr.moneda())
                .orElseThrow(() ->
                        new ErrorDeNegocio(CodigoError.de(12, 11), "Indica el importe a pagar, en la moneda del QR."));
        return indicado;
    }

    /** Si este mismo pago ya se hizo (misma persona, misma clave) se devuelve su comprobante, no un error. */
    private Optional<ComprobanteQr> pagoPrevio(DSLContext dsl, Qr qr, EntradaPagoQr entrada, ContextoSesion ctx) {
        if (!qr.esDinamico() || qr.transaccionId().isEmpty()) {
            return Optional.empty();
        }
        var mia = libro.porClaveIdempotencia(dsl, ctx.usuarioId(), "TRANSFERENCIA_P2P", entrada.claveIdempotencia());
        if (mia.isPresent() && mia.get().equals(qr.transaccionId().get())) {
            var saldo = cuentas.ver(dsl, entrada.cuentaOrigenId()).orElseThrow();
            return Optional.of(comprobante(dsl, qr, mia.get(), qr.monto().orElseThrow(), saldo.disponible()));
        }
        return Optional.empty();
    }

    private ComprobanteQr comprobante(DSLContext dsl, Qr qr, UUID transaccionId, Dinero importe, Dinero saldoDespues) {
        return new ComprobanteQr(transaccionId, qr.id(), importe, destinatario(dsl, qr), qr.concepto(), saldoDespues);
    }

    /** Del destino solo se muestra el numero de cuenta enmascarado: quien paga confirma, no averigua. */
    private String destinatario(DSLContext dsl, Qr qr) {
        String numero = qrs.numeroDeCuenta(dsl, qr.cuentaId()).orElse("");
        return numero.length() <= 4 ? "****" : "****" + numero.substring(numero.length() - 4);
    }

    public record EntradaQr(
            UUID cuentaBilleteraId, String modalidad, Optional<Dinero> monto, Optional<String> concepto) {}

    public record QrEmitido(
            UUID qrId, String contenido, String modalidad, Optional<Dinero> monto, Optional<OffsetDateTime> expiraEn) {}

    public record QrLeido(
            UUID qrId,
            String modalidad,
            Optional<Dinero> monto,
            String destinatario,
            Optional<String> concepto,
            Optional<OffsetDateTime> expiraEn) {}

    public record EntradaPagoQr(
            String contenido, UUID cuentaOrigenId, Optional<Dinero> monto, String claveIdempotencia) {}

    public record ComprobanteQr(
            UUID transaccionId,
            UUID qrId,
            Dinero monto,
            String destinatario,
            Optional<String> concepto,
            Dinero saldoDespues) {}
}
