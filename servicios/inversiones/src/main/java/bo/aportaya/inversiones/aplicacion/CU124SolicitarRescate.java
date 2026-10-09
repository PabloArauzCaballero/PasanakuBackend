package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.CorteDeOrden;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoRechazo;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio.DobleDisponibilidad;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio.Rescate;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * CU-124 · Solicitar un rescate.
 *
 * <p>Informa el corte aplicable y deja el rescate PENDIENTE hasta que el aliado confirme.
 * Tres rechazos que no se negocian:
 *
 * <ul>
 *   <li>un DPF que no admite cancelacion anticipada no se rescata antes de vencer;
 *   <li>unas cuotas ya comprometidas en otro rescate no se vuelven a ofrecer: lo decide la
 *       BASE ({@code R-INV-05}, bloqueando la posicion), no un {@code if} previo;
 *   <li>un depósito se rescata una sola vez.
 * </ul>
 *
 * <p>Idempotencia: titular + clave unicos; mismo contenido devuelve el rescate original,
 * otro contenido se rechaza.
 */
@Service
public class CU124SolicitarRescate {

    private final Datos datos;
    private final Transaccionar tx;
    private final PosicionRepositorio posiciones;
    private final CatalogoRepositorio catalogo;
    private final RescateRepositorio rescates;
    private final CU125LiquidarRescate liquidacion;
    private final AliadoDeInversion aliado;
    private final CalendarioHabil calendario;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU124SolicitarRescate(
            Datos datos,
            Transaccionar tx,
            PosicionRepositorio posiciones,
            CatalogoRepositorio catalogo,
            RescateRepositorio rescates,
            CU125LiquidarRescate liquidacion,
            AliadoDeInversion aliado,
            CalendarioHabil calendario,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.posiciones = posiciones;
        this.catalogo = catalogo;
        this.rescates = rescates;
        this.liquidacion = liquidacion;
        this.aliado = aliado;
        this.calendario = calendario;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record EntradaRescate(UUID posicionId, Optional<BigDecimal> cuotas, String clave) {}

    private record Creacion(Rescate rescate, Posicion posicion, boolean nuevo) {}

    public VistaRescate solicitar(EntradaRescate e, ContextoSesion ctx) {
        Creacion c;
        try {
            c = tx.en(() -> datos.conContexto(ctx, dsl -> crear(dsl, e, ctx)));
        } catch (DobleDisponibilidad d) {
            throw Errores.de(124, 2, "Esas cuotas (o ese deposito) ya estan comprometidas en otro rescate.");
        }
        if (c.nuevo()) {
            pedirAlAliado(c.rescate(), c.posicion(), ctx);
        } else {
            liquidacion.sincronizar(c.rescate().id(), ctx);
        }
        return liquidacion.ver(c.rescate().id(), ctx);
    }

    private Creacion crear(DSLContext dsl, EntradaRescate e, ContextoSesion ctx) {
        String hash = huella(e);
        Optional<Rescate> previo = rescates.porClave(dsl, ctx.usuarioId(), e.clave());
        if (previo.isPresent()) {
            if (!previo.get().hashSolicitud().equals(hash)) {
                throw Errores.de(124, 5, "Esa clave de idempotencia ya se uso con otro pedido de rescate.");
            }
            Posicion p = posiciones.porId(dsl, previo.get().posicionId()).orElseThrow();
            return new Creacion(previo.get(), p, false);
        }
        Posicion p = posiciones
                .porIdBloqueando(dsl, e.posicionId())
                .filter(x -> x.usuarioId().equals(ctx.usuarioId()))
                .orElseThrow(() -> Errores.de(124, 6, "Esa posicion no existe."));
        if (!"ABIERTA".equals(p.estado())) {
            throw Errores.de(124, 3, "Esa posicion ya no esta abierta.");
        }
        Condiciones c = catalogo.version(dsl, p.versionId()).orElseThrow();
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        String tipo;
        BigDecimal cuotas = null;
        LocalDate fechaValor = null;
        if (p.tipo() == TipoProducto.DPF) {
            if (e.cuotas().isPresent()) {
                throw Errores.de(124, 4, "Un deposito a plazo se rescata entero, sin cuotas.");
            }
            if (!reloj.hoy().isBefore(p.fechaVencimiento())) {
                tipo = "VENCIMIENTO";
            } else if (c.permiteRescateAnticipado()) {
                tipo = "ANTICIPADO";
            } else {
                throw Errores.de(
                        124,
                        1,
                        "Este deposito no admite cancelacion anticipada: se cobra el " + p.fechaVencimiento() + ".");
            }
        } else {
            BigDecimal libres = p.cuotas().subtract(rescates.cuotasEnCurso(dsl, p.id()));
            cuotas = e.cuotas().orElse(libres);
            if (cuotas.signum() <= 0 || cuotas.scale() > 6) {
                throw Errores.de(124, 4, "La cantidad de cuotas no es valida.");
            }
            if (cuotas.compareTo(libres) > 0) {
                throw Errores.de(124, 2, "Pediste mas cuotas que las libres: parte esta comprometida en otro rescate.");
            }
            tipo = cuotas.compareTo(p.cuotas()) == 0 ? "TOTAL" : "PARCIAL";
            fechaValor = CorteDeOrden.fechaValor(reloj.ahora(), c.horaCorte().orElseThrow(), calendario);
        }
        UUID id = rescates.crear(dsl, p.id(), ctx.usuarioId(), tipo, cuotas, e.clave(), hash, fechaValor, ahora);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "inversiones.rescate_solicitado",
                        "rescate_inversion",
                        id,
                        Map.of("posicionId", p.id().toString(), "tipo", tipo),
                        UUID.fromString(ctx.traza().id())));
        return new Creacion(rescates.porId(dsl, id).orElseThrow(), p, true);
    }

    private void pedirAlAliado(Rescate r, Posicion p, ContextoSesion ctx) {
        try {
            liquidacion.resolver(r, aliado.rescatar(r.id(), p.posicionExterna(), Optional.ofNullable(r.cuotas())), ctx);
        } catch (AliadoRechazo rechazo) {
            liquidacion.rechazar(r.id(), rechazo.codigo(), ctx);
        } catch (AliadoNoDisponible noSeSabe) {
            tx.en(() -> datos.conContexto(
                    ctx,
                    dsl -> rescates.pasar(
                            dsl,
                            r.id(),
                            List.of("SOLICITADO"),
                            "INCIERTO",
                            null,
                            null,
                            null,
                            null,
                            reloj.ahora().atOffset(ZoneOffset.UTC))));
        }
        liquidacion.acreditar(r.id(), ctx);
    }

    private static String huella(EntradaRescate e) {
        String canon =
                e.posicionId() + "|" + e.cuotas().map(BigDecimal::toPlainString).orElse("TODO");
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(canon.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
