package bo.aportaya.nucleofinanciero.aplicacion;

import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.SERVICIO_ENTREGAS;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.SERVICIO_INVERSIONES;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.bloquearEnOrden;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.comoOperacion;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.exigirEjecutable;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.exigirServicio;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.iniciador;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.sinCuentaDeSistema;

import bo.aportaya.nucleofinanciero.dominio.VigenciaDeRetencion.Motivo;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio.Cuenta;
import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio.Retencion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-13 · Ejecutar una retencion CON DESTINO: cierra la retencion y escribe el movimiento que la motivo en
 * <b>la misma transaccion</b> (el saldo se deriva del libro: cerrarla sin movimiento devolveria la plata).
 * {@link #debitarInversion} (llama {@code inversiones}) y {@link #pagarAlTercero} (llama {@code entregas}
 * via {@link PagoDeCompraEnMercado}) son operaciones de SERVICIO, no de usuario final (ver
 * {@link ReglasDeEjecucion}); idempotentes por clave (R-BIL-06) y por hecho (R-BIL-24).
 */
@Service
public class CU13EjecutarConDestino {

    private static final String ORIGEN_INVERSION = "ORDEN_INVERSION";
    private static final String ORIGEN_MERCADO = "OFERTA_MERCADO";

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final RetencionRepositorio retenciones;
    private final CuentasDeSistemaRepositorio sistema;
    private final CU13RetenerSaldo cu13;
    private final LibroDeBilletera libro;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU13EjecutarConDestino(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            RetencionRepositorio retenciones,
            CuentasDeSistemaRepositorio sistema,
            CU13RetenerSaldo cu13,
            LibroDeBilletera libro,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.retenciones = retenciones;
        this.sistema = sistema;
        this.cu13 = cu13;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ inversion

    @Transactional
    public SalidaEjecucion debitarInversion(EntradaDebito e, ContextoSesion llamador) {
        exigirServicio(llamador, SERVICIO_INVERSIONES, CodigoError.de(13, 9));
        ContextoSesion ctx = comoOperacion(llamador);
        return datos.conContexto(ctx, dsl -> {
            libro.bloquearIdempotencia(dsl, iniciador(ctx), ORIGEN_INVERSION, e.claveIdempotencia());
            Retencion retencion = retencionExistente(dsl, e.retencionId());
            Cuenta duena = cuentas.ver(dsl, retencion.cuentaId()).orElseThrow();
            // El servicio llamador identifica a la persona que ya valido: el libro lo verifica, no lo supone.
            e.titularId().ifPresent(titular -> {
                if (!titular.equals(duena.usuarioId())) {
                    throw new ErrorDeNegocio(CodigoError.de(13, 1), "Esa retencion no es de esa persona.");
                }
            });
            Optional<UUID> previa =
                    libro.porClaveIdempotencia(dsl, iniciador(ctx), ORIGEN_INVERSION, e.claveIdempotencia());
            if (previa.isPresent()) {
                return repetida(dsl, previa.get(), retencion, e.monto(), duena);
            }
            exigirEjecutable(retencion, Motivo.INVERSION_VOLUNTARIA, e.monto());
            UUID custodia = sistema.custodiaDeInversion(dsl, duena.moneda())
                    .orElseThrow(() -> sinCuentaDeSistema("la custodia de inversion", duena.moneda()));

            bloquearEnOrden(dsl, cuentas, List.of(duena.id(), custodia));
            cu13.ejecutarDentroDe(dsl, retencion.id(), ctx);
            UUID origenId = retencion.referenciaId().orElse(retencion.id());
            UUID transaccionId = libro.registrar(
                    dsl,
                    "INVERSION",
                    ORIGEN_INVERSION,
                    origenId,
                    "API",
                    retencion.monto(),
                    e.claveIdempotencia(),
                    iniciador(ctx),
                    List.of(
                            Pata.debito(duena.id(), retencion.monto(), "Inversion voluntaria"),
                            Pata.credito(custodia, retencion.monto(), "Custodia de inversion")),
                    ahora());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.inversion_debitada",
                            "transaccion_billetera",
                            transaccionId,
                            Map.of(
                                    "cuentaBilleteraId", duena.id().toString(),
                                    "retencionId", retencion.id().toString(),
                                    "ordenId", origenId.toString(),
                                    "monto", retencion.monto().toString(),
                                    "moneda", retencion.monto().moneda().name(),
                                    "servicio", llamador.rol()),
                            UUID.fromString(ctx.traza().id())));
            return salida(dsl, transaccionId, duena.id(), false);
        });
    }

    // ------------------------------------------------------------------ mercado

    /**
     * Lo que hay que saber ANTES de preguntarle a {@code entregas} por la oferta: a cual oferta apunta la
     * retencion, y si este pedido es el reintento de uno que ya se ejecuto (entonces no hace falta la oferta).
     */
    @Transactional(readOnly = true)
    public Preparacion preparar(UUID retencionId, String clave, ContextoSesion llamador) {
        exigirServicio(llamador, SERVICIO_ENTREGAS, CodigoError.de(13, 9));
        ContextoSesion ctx = comoOperacion(llamador);
        return datos.conContexto(ctx, dsl -> {
            Retencion retencion = retencionExistente(dsl, retencionId);
            UUID ofertaId = ofertaDe(retencion);
            Optional<UUID> previa = libro.porClaveIdempotencia(dsl, iniciador(ctx), ORIGEN_MERCADO, clave);
            Cuenta comprador = cuentas.ver(dsl, retencion.cuentaId()).orElseThrow();
            return new Preparacion(
                    ofertaId, previa.map(tx -> repetida(dsl, tx, retencion, retencion.monto(), comprador)));
        });
    }

    @Transactional
    public SalidaEjecucion pagarAlTercero(EntradaPagoATercero e, ContextoSesion llamador) {
        exigirServicio(llamador, SERVICIO_ENTREGAS, CodigoError.de(13, 9));
        ContextoSesion ctx = comoOperacion(llamador);
        return datos.conContexto(ctx, dsl -> {
            libro.bloquearIdempotencia(dsl, iniciador(ctx), ORIGEN_MERCADO, e.claveIdempotencia());
            Retencion retencion = retencionExistente(dsl, e.retencionId());
            Cuenta comprador = cuentas.ver(dsl, retencion.cuentaId()).orElseThrow();
            Optional<UUID> previa =
                    libro.porClaveIdempotencia(dsl, iniciador(ctx), ORIGEN_MERCADO, e.claveIdempotencia());
            if (previa.isPresent()) {
                return repetida(dsl, previa.get(), retencion, retencion.monto(), comprador);
            }
            exigirEjecutable(retencion, Motivo.COMPRA_EN_MERCADO, retencion.monto());
            // La oferta que se paga es la que la retencion nombra; no la que diga quien llama.
            if (!ofertaDe(retencion).equals(e.ofertaId())) {
                throw new ErrorDeNegocio(CodigoError.de(13, 8), "La oferta no es la que motivo esa retencion.");
            }
            // El reparto lo afirma entregas (dueno de la oferta); aca solo se verifica que cuadre con lo retenido.
            Dinero precio = e.precio();
            Dinero cargos = e.cargos();
            Dinero paraElVendedor = RepartoDeCompraEnMercado.paraElVendedor(precio, cargos, retencion.monto());
            if (e.vendedorUsuarioId().equals(comprador.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(13, 8), "El vendedor no puede ser el comprador.");
            }
            UUID vendedor = sistema.cuentaOperativaDe(dsl, e.vendedorUsuarioId(), comprador.moneda())
                    .orElseThrow(() -> new ErrorDeNegocio(
                            CodigoError.de(13, 8), "El vendedor no tiene una billetera operativa en esa moneda."));
            Optional<UUID> ingresos = cargos.esCero()
                    ? Optional.empty()
                    : Optional.of(sistema.ingresosDePlataforma(dsl, comprador.moneda())
                            .orElseThrow(
                                    () -> sinCuentaDeSistema("los ingresos de la plataforma", comprador.moneda())));

            List<UUID> aBloquear = new ArrayList<>(List.of(comprador.id(), vendedor));
            ingresos.ifPresent(aBloquear::add);
            bloquearEnOrden(dsl, cuentas, aBloquear);
            cu13.ejecutarDentroDe(dsl, retencion.id(), ctx);

            List<Pata> patas = new ArrayList<>();
            patas.add(Pata.debito(comprador.id(), precio, "Compra en el mercado"));
            if (!paraElVendedor.esCero()) {
                patas.add(Pata.credito(vendedor, paraElVendedor, "Venta en el mercado (precio menos cargos)"));
            }
            ingresos.ifPresent(cuenta -> patas.add(Pata.credito(cuenta, cargos, "Cargos del mercado")));
            UUID transaccionId = libro.registrar(
                    dsl,
                    "COMPRA_EN_MERCADO",
                    ORIGEN_MERCADO,
                    e.ofertaId(),
                    "API",
                    retencion.monto(),
                    e.claveIdempotencia(),
                    iniciador(ctx),
                    patas,
                    ahora());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.compra_en_mercado_pagada",
                            "transaccion_billetera",
                            transaccionId,
                            Map.of(
                                    "cuentaCompradorId", comprador.id().toString(),
                                    "cuentaVendedorId", vendedor.toString(),
                                    "ofertaId", e.ofertaId().toString(),
                                    "paraElVendedor", paraElVendedor.toString(),
                                    "cargos", cargos.toString(),
                                    "moneda", precio.moneda().name(),
                                    "servicio", llamador.rol()),
                            UUID.fromString(ctx.traza().id())));
            return salida(dsl, transaccionId, comprador.id(), false);
        });
    }

    // ------------------------------------------------------------------ compartido

    private Retencion retencionExistente(DSLContext dsl, UUID retencionId) {
        return retenciones
                .ver(dsl, retencionId)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 3), "Esa retencion no existe."));
    }

    /** La oferta que la retencion nombra; una retencion de mercado sin referencia de oferta no se paga. */
    private static UUID ofertaDe(Retencion retencion) {
        if (!"OFERTA_MERCADO".equals(retencion.referenciaTipo().orElse(""))
                || retencion.referenciaId().isEmpty()) {
            throw new ErrorDeNegocio(CodigoError.de(13, 8), "Esa retencion no nombra una oferta del mercado.");
        }
        return retencion.referenciaId().get();
    }

    /**
     * Repetir la clave es repetir el MISMO hecho: misma retencion (ya ejecutada), mismo importe, y la
     * transaccion nacio de esa orden u oferta. Si algo cambia, la clave ya identifica otra cosa.
     */
    private SalidaEjecucion repetida(
            DSLContext dsl, UUID transaccionId, Retencion retencion, Dinero monto, Cuenta duena) {
        var previa = libro.resumen(dsl, transaccionId).orElseThrow();
        boolean mismo = "EJECUTADA".equals(retencion.estado())
                && previa.origenId().equals(retencion.referenciaId().orElse(retencion.id()))
                && previa.monto().equals(monto)
                && retencion.monto().equals(monto);
        if (!mismo) {
            throw new ErrorDeNegocio(CodigoError.de(13, 4), "La clave de idempotencia ya identifica otra operacion.");
        }
        return salida(dsl, transaccionId, duena.id(), true);
    }

    private SalidaEjecucion salida(DSLContext dsl, UUID transaccionId, UUID cuentaId, boolean repetida) {
        Cuenta despues = cuentas.ver(dsl, cuentaId).orElseThrow();
        return new SalidaEjecucion(transaccionId, despues.disponible(), despues.retenido(), repetida);
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    public record EntradaDebito(String claveIdempotencia, UUID retencionId, Dinero monto, Optional<UUID> titularId) {}

    /** Los hechos de la oferta los trajo el servidor de {@code entregas}; el cliente no dicta ninguno. */
    public record EntradaPagoATercero(
            String claveIdempotencia,
            UUID retencionId,
            UUID ofertaId,
            UUID vendedorUsuarioId,
            Dinero precio,
            Dinero cargos) {}

    public record Preparacion(UUID ofertaId, Optional<SalidaEjecucion> yaEjecutada) {}

    /** {@code repetida} distingue el reintento del primer intento sin cambiar el codigo HTTP del contrato. */
    public record SalidaEjecucion(UUID transaccionId, Dinero saldoDisponible, Dinero saldoRetenido, boolean repetida) {}
}
