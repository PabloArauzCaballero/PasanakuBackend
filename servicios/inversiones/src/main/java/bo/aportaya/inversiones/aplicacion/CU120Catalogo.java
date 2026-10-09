package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.dominio.TextoDeCondiciones;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.ProductoDelAliado;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio.Producto;
import bo.aportaya.inversiones.infraestructura.GuardiaDeProduccion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-120 · Productos y condiciones, con fecha de cotizacion y fuente.
 *
 * <p>Sincronizar trae el catalogo del aliado y, SOLO si cambio algo, agrega una version
 * nueva de condiciones (las anteriores no se tocan: son lo que alguien ya acepto). Las
 * condiciones que se ofrecen salen de la ultima version; en un entorno productivo no se
 * ofrece ninguna cuyo origen sea sintetico.
 *
 * <p>La clasificacion de riesgo y la redaccion de la titularidad son SINTETICAS hasta que
 * el aliado y legal las definan ({@code DR-INV-01}, {@code DR-INV-06}); el texto que se
 * acepta lo dice.
 */
@Service
public class CU120Catalogo {

    static final String TITULARIDAD = "la posicion queda registrada a nombre de la persona titular ante el aliado"
            + " (modalidad SINTETICA, a definir con el aliado y legal).";
    static final String COSTOS =
            "Costos y comisiones SINTETICOS de demostracion; antes de operar de verdad deben salir del reglamento del aliado.";

    public record VistaProducto(
            UUID id,
            String codigo,
            TipoProducto tipo,
            String nombre,
            String emisor,
            String moneda,
            String nivelRiesgo,
            Condiciones condiciones,
            String titularidad) {}

    private final Datos datos;
    private final Transaccionar tx;
    private final CatalogoRepositorio catalogo;
    private final AliadoDeInversion aliado;
    private final GuardiaDeProduccion guardia;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU120Catalogo(
            Datos datos,
            Transaccionar tx,
            CatalogoRepositorio catalogo,
            AliadoDeInversion aliado,
            GuardiaDeProduccion guardia,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.catalogo = catalogo;
        this.aliado = aliado;
        this.guardia = guardia;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** @return cuantas versiones de condiciones eran nuevas. */
    public int sincronizar(ContextoSesion ctx) {
        List<ProductoDelAliado> productos;
        try {
            productos = aliado.catalogo();
        } catch (AliadoNoDisponible e) {
            throw Errores.de(120, 2, "No pudimos consultar el catalogo del aliado en este momento.");
        }
        int nuevas = 0;
        for (ProductoDelAliado p : productos) {
            if (tx.en(() -> datos.conContexto(ctx, dsl -> sincronizar(dsl, p, ctx)))) {
                nuevas++;
            }
        }
        return nuevas;
    }

    private boolean sincronizar(org.jooq.DSLContext dsl, ProductoDelAliado p, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        Producto producto = catalogo.asegurar(
                dsl,
                p.codigo(),
                p.tipo(),
                p.nombre(),
                "Aliado de demostracion (SINTETICO)",
                p.tipo() == TipoProducto.DPF ? "BAJO" : "MEDIO",
                ahora);
        catalogo.bloquear(dsl, producto.id());
        Condiciones nueva = condiciones(p, producto, catalogo.siguienteNumero(dsl, producto.id()));
        Optional<Condiciones> vigente = catalogo.vigente(dsl, producto.id());
        if (vigente.isPresent() && vigente.get().textoHash().equals(nueva.textoHash())) {
            return false; // nada cambio: la version vigente sigue siendo la que se acepto
        }
        catalogo.insertarVersion(dsl, producto.id(), nueva.numero(), nueva, ahora);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "inversiones.condiciones_publicadas",
                        "producto_inversion",
                        producto.id(),
                        Map.of("version", Integer.toString(nueva.numero())),
                        UUID.fromString(ctx.traza().id())));
        return true;
    }

    private static Condiciones condiciones(ProductoDelAliado p, Producto producto, int numero) {
        boolean dpf = p.tipo() == TipoProducto.DPF;
        OrigenDatos origen = p.sintetico() ? OrigenDatos.SINTETICO : OrigenDatos.VERIFICADO;
        var sinTexto = new Condiciones(
                UUID.randomUUID(),
                numero,
                p.tipo(),
                p.plazoDias(),
                p.baseDias(),
                p.tasaNominalAnual(),
                p.permiteRescateAnticipado(),
                p.penalizacionAnticipo(),
                p.diasRescate(),
                p.horaCorte().map(LocalTime::parse),
                p.montoMinimo(),
                dpf ? Optional.of(p.tasaRetencion()) : Optional.empty(),
                p.tasaComisionExito(),
                COSTOS,
                "",
                "",
                p.fuente(),
                p.fechaCotizacion(),
                origen,
                !p.sintetico());
        String texto = TextoDeCondiciones.redactar(producto.nombre(), producto.emisor(), sinTexto, TITULARIDAD);
        return new Condiciones(
                sinTexto.versionId(),
                numero,
                p.tipo(),
                sinTexto.plazoDias(),
                sinTexto.baseDias(),
                sinTexto.tasaNominalAnual(),
                sinTexto.permiteRescateAnticipado(),
                sinTexto.penalizacionAnticipo(),
                sinTexto.diasRescate(),
                sinTexto.horaCorte(),
                sinTexto.montoMinimo(),
                sinTexto.tasaRetencion(),
                sinTexto.tasaComisionExito(),
                COSTOS,
                texto,
                TextoDeCondiciones.hash(texto),
                p.fuente(),
                p.fechaCotizacion(),
                origen,
                !p.sintetico());
    }

    public List<VistaProducto> listar(ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> catalogo.activos(dsl).stream()
                .flatMap(p -> catalogo.vigente(dsl, p.id()).stream()
                        .filter(c -> !guardia.productivo() || c.aptoProduccion())
                        .map(c -> new VistaProducto(
                                p.id(),
                                p.codigo(),
                                p.tipo(),
                                p.nombre(),
                                p.emisor(),
                                p.moneda(),
                                p.nivelRiesgo(),
                                c,
                                TITULARIDAD)))
                .toList()));
    }
}
