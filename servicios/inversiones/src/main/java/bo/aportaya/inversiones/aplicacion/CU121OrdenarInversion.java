package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.LibroNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.SaldoDelTitular;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio.Producto;
import bo.aportaya.inversiones.infraestructura.GuardiaDeProduccion;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio.Orden;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * CU-121 · Aceptar las condiciones y ordenar una inversion.
 *
 * <p>Tres cosas que definen el caso:
 *
 * <ul>
 *   <li><b>El consentimiento es de una version exacta.</b> La persona manda el hash del
 *       texto que vio; si el producto cambio de condiciones entre que lo vio y que ordeno,
 *       se rechaza y se le muestra el texto nuevo. Quedan guardados el consentimiento, su
 *       hash y la version, de forma inmutable.
 *   <li><b>Un saldo afectado a un pozo no se invierte.</b> Se compara contra lo invertible
 *       (disponible menos lo comprometido en pozos). Si el libro no puede informarlo, se
 *       rechaza: no se invierte «por las dudas». La retencion posterior, atomica en el
 *       libro, es el control final contra dos ordenes simultaneas.
 *   <li><b>El saldo se retiene antes de enviar</b> y cada paso queda como intencion
 *       persistida; ver {@link CU122ConfirmarPosicion}.
 * </ul>
 *
 * <p>Idempotencia: el titular y la clave son unicos en la base. La misma clave con el mismo
 * contenido devuelve la orden original y sigue donde quedo; con otro contenido se rechaza.
 */
@Service
public class CU121OrdenarInversion {

    private final Datos datos;
    private final Transaccionar tx;
    private final CatalogoRepositorio catalogo;
    private final OrdenRepositorio ordenes;
    private final InstruccionRepositorio instrucciones;
    private final LibroDelTitular libro;
    private final CU122ConfirmarPosicion confirmacion;
    private final GuardiaDeProduccion guardia;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU121OrdenarInversion(
            Datos datos,
            Transaccionar tx,
            CatalogoRepositorio catalogo,
            OrdenRepositorio ordenes,
            InstruccionRepositorio instrucciones,
            LibroDelTitular libro,
            CU122ConfirmarPosicion confirmacion,
            GuardiaDeProduccion guardia,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.catalogo = catalogo;
        this.ordenes = ordenes;
        this.instrucciones = instrucciones;
        this.libro = libro;
        this.confirmacion = confirmacion;
        this.guardia = guardia;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record EntradaOrden(
            String productoCodigo,
            UUID versionCondicionesId,
            String textoHash,
            UUID cuentaBilleteraId,
            Dinero monto,
            String clave) {}

    private record Evaluacion(Optional<Orden> existente, Producto producto) {}

    public VistaOrden ordenar(EntradaOrden e, ContextoSesion ctx) {
        if (e.monto().moneda() != Moneda.BOB) {
            throw Errores.de(121, 8, "Por ahora solo se invierte en bolivianos.");
        }
        String hash = huella(e);
        Evaluacion previa = tx.en(() -> datos.conContexto(ctx, dsl -> evaluar(dsl, e, hash, ctx)));
        if (previa.existente().isPresent()) {
            return confirmacion.avanzar(previa.existente().get().id(), ctx);
        }
        verificarSaldo(e, ctx);
        UUID ordenId = tx.en(() -> datos.conContexto(ctx, dsl -> crear(dsl, e, hash, previa.producto(), ctx)));
        return confirmacion.avanzar(ordenId, ctx);
    }

    // ------------------------------------------------------------------ validacion
    private Evaluacion evaluar(DSLContext dsl, EntradaOrden e, String hash, ContextoSesion ctx) {
        Optional<Orden> existente = ordenes.porClave(dsl, ctx.usuarioId(), e.clave());
        if (existente.isPresent()) {
            if (!existente.get().hashSolicitud().equals(hash)) {
                throw Errores.de(121, 6, "Esa clave de idempotencia ya se uso con otra orden distinta.");
            }
            return new Evaluacion(existente, null);
        }
        Producto p = catalogo.porCodigo(dsl, e.productoCodigo())
                .filter(x -> "ACTIVO".equals(x.estado()))
                .orElseThrow(() -> Errores.de(121, 1, "Ese producto no esta disponible."));
        Condiciones vigente =
                catalogo.vigente(dsl, p.id()).orElseThrow(() -> Errores.de(121, 1, "Ese producto no esta disponible."));
        if (guardia.productivo() && !vigente.aptoProduccion()) {
            // Un dato sintetico jamas se ofrece en produccion.
            throw Errores.de(121, 1, "Ese producto no esta disponible.");
        }
        if (!vigente.versionId().equals(e.versionCondicionesId())
                || !vigente.textoHash().equals(e.textoHash())) {
            throw Errores.de(121, 2, "Las condiciones cambiaron desde que las viste: revisalas y volve a aceptar.");
        }
        if (e.monto().monto().compareTo(vigente.montoMinimo()) < 0) {
            throw Errores.de(
                    121,
                    3,
                    "El monto minimo de este producto es "
                            + vigente.montoMinimo().toPlainString() + " BOB.");
        }
        return new Evaluacion(Optional.empty(), p);
    }

    private void verificarSaldo(EntradaOrden e, ContextoSesion ctx) {
        SaldoDelTitular saldo;
        try {
            saldo = libro.saldo(e.cuentaBilleteraId());
        } catch (LibroNoDisponible noSeSabe) {
            throw Errores.de(121, 7, "No pudimos verificar tu saldo en este momento. No se hizo ninguna reserva.");
        }
        if (!saldo.titularId().equals(ctx.usuarioId())
                || saldo.disponible().moneda() != e.monto().moneda()) {
            throw Errores.de(121, 4, "Esa cuenta no puede usarse para esta inversion.");
        }
        if (e.monto().esMayorQue(saldo.invertible())) {
            boolean soloPorElPozo =
                    !saldo.comprometidoEnPozos().esCero() && !e.monto().esMayorQue(saldo.disponible());
            if (soloPorElPozo) {
                throw Errores.de(
                        121,
                        5,
                        "Parte de tu saldo esta afectada a un pozo y no se puede invertir.",
                        Map.of("invertible", saldo.invertible().toString()));
            }
            throw Errores.de(121, 4, "No tenes saldo disponible suficiente.");
        }
    }

    // ------------------------------------------------------------------ creacion
    private UUID crear(DSLContext dsl, EntradaOrden e, String hash, Producto p, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        Condiciones vigente = catalogo.vigente(dsl, p.id()).orElseThrow();
        UUID consentimientoId = UUID.randomUUID();
        boolean nuevo = ordenes.insertarConsentimiento(
                dsl,
                consentimientoId,
                ctx.usuarioId(),
                vigente.versionId(),
                vigente.textoHash(),
                e.clave(),
                hash,
                ahora);
        if (!nuevo) {
            // Dos pedidos con la misma clave a la vez: gano el otro, y su orden ya esta visible.
            Orden ganadora = ordenes.porClave(dsl, ctx.usuarioId(), e.clave()).orElseThrow();
            if (!ganadora.hashSolicitud().equals(hash)) {
                throw Errores.de(121, 6, "Esa clave de idempotencia ya se uso con otra orden distinta.");
            }
            return ganadora.id();
        }
        UUID ordenId = UUID.randomUUID();
        ordenes.insertarOrden(
                dsl,
                ordenId,
                ctx.usuarioId(),
                p.id(),
                vigente.versionId(),
                consentimientoId,
                e.cuentaBilleteraId(),
                e.monto().monto(),
                e.monto().moneda().name(),
                e.clave(),
                hash,
                ahora);
        instrucciones.crear(
                dsl,
                "ORDEN",
                ordenId,
                "RETENER",
                ctx.usuarioId(),
                e.cuentaBilleteraId(),
                e.monto().monto(),
                e.monto().moneda().name(),
                ahora);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "inversiones.orden_creada",
                        "orden_inversion",
                        ordenId,
                        Map.of("productoCodigo", p.codigo()),
                        UUID.fromString(ctx.traza().id())));
        return ordenId;
    }

    /** Lo que identifica el CONTENIDO del pedido: misma clave con otro contenido es otro pedido. */
    private static String huella(EntradaOrden e) {
        String canon = String.join(
                "|",
                e.productoCodigo(),
                e.versionCondicionesId().toString(),
                e.textoHash(),
                e.cuentaBilleteraId().toString(),
                e.monto().monto().toPlainString(),
                e.monto().moneda().name());
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(canon.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
