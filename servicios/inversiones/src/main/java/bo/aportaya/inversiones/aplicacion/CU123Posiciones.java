package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.ComisionDeExito;
import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.DevengoDeDpf;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.dominio.TextoDeCondiciones;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.ValoracionDeCuotas;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio.ValorGuardado;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio.Comprobante;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * CU-123 · Devengo de DPF y valoracion de cuotas, SIN promesa.
 *
 * <p>Un DPF muestra por separado lo devengado (que todavia no es plata en mano), lo pagado,
 * el impuesto estimado y el pagado, y el vencimiento. Un fondo muestra lo que valen hoy sus
 * cuotas con la fecha del dato, la variacion contra lo invertido —que puede ser negativa—
 * y una marca de «desactualizado» cuando el ultimo valor es viejo. Nada se anuncia como
 * rentabilidad segura.
 *
 * <p>El devengo diario es idempotente por posicion y fecha (UNIQUE): correrlo dos veces el
 * mismo dia no duplica nada, y correrlo despues de dias sin correr cubre la diferencia.
 */
@Service
public class CU123Posiciones {

    static final String ADVERTENCIA = TextoDeCondiciones.ADVERTENCIA;

    public record Valoracion(
            BigDecimal cuotas,
            BigDecimal valorCuota,
            LocalDate fechaValor,
            long antiguedadDias,
            boolean desactualizado,
            BigDecimal valorBruto,
            BigDecimal variacion,
            BigDecimal comisionDevengada,
            BigDecimal valorNetoEstimado,
            OrigenDatos origen) {}

    public record DetalleDpf(
            BigDecimal tasaNominalAnual,
            int baseDias,
            int diasDevengados,
            BigDecimal interesDevengado,
            BigDecimal interesPagado,
            BigDecimal impuestoEstimado,
            BigDecimal impuestoPagado,
            LocalDate vencimiento) {}

    public record VistaPosicion(
            UUID id,
            TipoProducto tipo,
            String productoCodigo,
            String estado,
            BigDecimal principal,
            String moneda,
            LocalDate fechaConstitucion,
            LocalDate fechaVencimiento,
            BigDecimal costoBaseVigente,
            Optional<Valoracion> valoracion,
            Optional<DetalleDpf> dpf,
            OrigenDatos origen,
            String advertencia) {}

    private final Datos datos;
    private final Transaccionar tx;
    private final PosicionRepositorio posiciones;
    private final CatalogoRepositorio catalogo;
    private final ComprobanteRepositorio comprobantes;
    private final AliadoDeInversion aliado;
    private final Reloj reloj;
    private final int toleranciaDias;

    public CU123Posiciones(
            Datos datos,
            Transaccionar tx,
            PosicionRepositorio posiciones,
            CatalogoRepositorio catalogo,
            ComprobanteRepositorio comprobantes,
            AliadoDeInversion aliado,
            Reloj reloj,
            @Value("${aportaya.inversiones.valor-cuota.tolerancia-dias:3}") int toleranciaDias) {
        this.datos = datos;
        this.tx = tx;
        this.posiciones = posiciones;
        this.catalogo = catalogo;
        this.comprobantes = comprobantes;
        this.aliado = aliado;
        this.reloj = reloj;
        this.toleranciaDias = toleranciaDias;
    }

    public List<VistaPosicion> listar(ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> posiciones.delTitular(dsl, ctx.usuarioId()).stream()
                .map(p -> vista(dsl, p))
                .toList()));
    }

    public VistaPosicion ver(UUID posicionId, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> vista(dsl, propia(dsl, posicionId, ctx))));
    }

    public List<VistaComprobante> comprobantes(UUID posicionId, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> {
            propia(dsl, posicionId, ctx);
            return comprobantes.deLaPosicion(dsl, posicionId).stream()
                    .map(c -> new VistaComprobante(
                            c.id(), c.tipo(), c.sentidoTitular(), c.d(), c.origen(), c.emitidoEn()))
                    .toList();
        }));
    }

    Posicion propia(DSLContext dsl, UUID posicionId, ContextoSesion ctx) {
        return posiciones
                .porId(dsl, posicionId)
                .filter(p -> ctx.esSistema() || p.usuarioId().equals(ctx.usuarioId()))
                .orElseThrow(() -> Errores.de(123, 1, "Esa posicion no existe."));
    }

    private VistaPosicion vista(DSLContext dsl, Posicion p) {
        Condiciones c = catalogo.version(dsl, p.versionId()).orElseThrow();
        String codigo = catalogo.porId(dsl, p.productoId()).orElseThrow().codigo();
        BigDecimal costoBase = comprobantes.costoBaseVigente(dsl, p.id());
        if (p.tipo() == TipoProducto.DPF) {
            return armar(p, codigo, costoBase, Optional.empty(), Optional.of(dpf(dsl, p, c)), c);
        }
        return armar(p, codigo, costoBase, valoracion(dsl, p, c, costoBase), Optional.empty(), c);
    }

    private static VistaPosicion armar(
            Posicion p,
            String codigo,
            BigDecimal costoBase,
            Optional<Valoracion> valoracion,
            Optional<DetalleDpf> dpf,
            Condiciones c) {
        return new VistaPosicion(
                p.id(),
                p.tipo(),
                codigo,
                p.estado(),
                p.principal(),
                p.moneda(),
                p.fechaConstitucion(),
                p.fechaVencimiento(),
                costoBase,
                valoracion,
                dpf,
                c.origen(),
                ADVERTENCIA);
    }

    private Optional<Valoracion> valoracion(DSLContext dsl, Posicion p, Condiciones c, BigDecimal costoBase) {
        if (p.cuotas() == null || p.cuotas().signum() == 0) {
            return Optional.empty();
        }
        return catalogo.ultimoValor(dsl, p.productoId()).map(v -> {
            var val = ValoracionDeCuotas.de(p.cuotas(), v.valor(), costoBase, v.fecha(), reloj.hoy(), toleranciaDias);
            BigDecimal comision = c.tasaComisionExito()
                    .map(tasa -> ComisionDeExito.calcular(p.cuotas(), v.valor(), p.marcaMaxima(), tasa)
                            .comision())
                    .orElse(BigDecimal.ZERO.setScale(2));
            return new Valoracion(
                    p.cuotas(),
                    v.valor(),
                    v.fecha(),
                    val.antiguedadDias(),
                    val.desactualizado(),
                    val.valorBruto(),
                    val.variacion(),
                    comision,
                    val.valorBruto().subtract(comision),
                    OrigenDatos.valueOf(v.origen()));
        });
    }

    private DetalleDpf dpf(DSLContext dsl, Posicion p, Condiciones c) {
        BigDecimal devengado = posiciones
                .ultimoDevengo(dsl, p.id(), reloj.hoy())
                .map(PosicionRepositorio.Devengo::interesAcumulado)
                .orElse(BigDecimal.ZERO.setScale(2));
        int dias = (int) Math.max(
                0,
                Math.min(
                        ChronoUnit.DAYS.between(p.fechaConstitucion(), reloj.hoy()),
                        c.plazoDias().orElseThrow()));
        List<Comprobante> pagos = comprobantes.deLaPosicion(dsl, p.id()).stream()
                .filter(x -> "LIQUIDACION".equals(x.tipo()))
                .toList();
        BigDecimal pagado = pagos.stream().map(x -> x.d().interes()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impuestoPagado = pagos.stream().map(x -> x.d().impuesto()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimado = c.tasaRetencion()
                .map(t -> DevengoDeDpf.retencion(devengado.subtract(pagado), t))
                .orElse(BigDecimal.ZERO);
        return new DetalleDpf(
                c.tasaNominalAnual().orElseThrow(),
                c.baseDias().orElseThrow(),
                dias,
                devengado,
                pagado,
                "ABIERTA".equals(p.estado()) ? estimado : BigDecimal.ZERO.setScale(2),
                impuestoPagado,
                p.fechaVencimiento());
    }
    // ------------------------------------------------------------------ valor de cuota
    /** Guarda el ultimo valor que publico el aliado de cada fondo. Un valor publicado no se reescribe. */
    public int sincronizarValores(ContextoSesion ctx) {
        int nuevos = 0;
        List<CatalogoRepositorio.Producto> fondos =
                tx.en(() -> datos.conContexto(ctx, dsl -> catalogo.activos(dsl).stream()
                        .filter(p -> p.tipo() == TipoProducto.FONDO)
                        .toList()));
        for (var fondo : fondos) {
            Optional<AliadoDeInversion.ValorDeCuotaDelAliado> v;
            try {
                v = aliado.valorDeCuota(fondo.codigo());
            } catch (AliadoNoDisponible e) {
                throw Errores.de(123, 2, "No pudimos consultar el valor de cuota del aliado en este momento.");
            }
            if (v.isEmpty()) {
                continue;
            }
            var valor = v.get();
            boolean nuevo = tx.en(() -> datos.conContexto(ctx, dsl -> guardar(dsl, fondo, valor)));
            if (nuevo) {
                nuevos++;
            }
        }
        return nuevos;
    }

    private boolean guardar(
            DSLContext dsl, CatalogoRepositorio.Producto fondo, AliadoDeInversion.ValorDeCuotaDelAliado v) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        Condiciones c = catalogo.vigente(dsl, fondo.id()).orElseThrow();
        boolean nuevo = catalogo.insertarValor(
                dsl,
                fondo.id(),
                v.fecha(),
                v.valor(),
                c.fuente(),
                c.origen(),
                v.publicadoEn().atOffset(ZoneOffset.UTC),
                ahora);
        if (!nuevo) {
            ValorGuardado previo =
                    catalogo.valorDelDia(dsl, fondo.id(), v.fecha()).orElseThrow();
            if (previo.valor().compareTo(v.valor()) != 0) {
                throw Errores.de(
                        123,
                        2,
                        "El aliado publico dos valores distintos para la misma fecha: no se reescribe el primero.");
            }
        }
        return nuevo;
    }
}
