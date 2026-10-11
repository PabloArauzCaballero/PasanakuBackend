package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio.Comprobante;
import bo.aportaya.inversiones.infraestructura.DistribucionRepositorio;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio.Instruccion;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * CU-128 · Reporte de comprobantes del titular en un periodo, con principal, interes,
 * impuesto y comision SEPARADOS, reconciliado contra lo que paso de verdad en el libro.
 *
 * <p>Cada comprobante es una linea. Se cruza con la instruccion que movio la plata en el libro
 * (el debito de la suscripcion o el credito de la liquidacion) y la linea queda
 * {@code CONCILIADA} solo si la instruccion esta aplicada y su monto es el principal (suscripcion)
 * o el neto (liquidacion). Una linea cuyo credito todavia no llego al libro se muestra
 * {@code PENDIENTE_EN_LIBRO}, no se esconde, y una que no cuadra es {@code DIFERENCIA}.
 *
 * <p>El reporte NO es una declaracion tributaria: el tratamiento fiscal de los intereses y de
 * la comision lo define fiscal ({@code DR-INV-03}); mientras tanto la retencion es una tasa
 * SINTETICA de las condiciones de cada producto y el reporte lo dice en su propio encabezado.
 */
@Service
public class CU128ReporteFiscal {

    public static final String CONCILIADA = "CONCILIADA";
    public static final String PENDIENTE_EN_LIBRO = "PENDIENTE_EN_LIBRO";
    public static final String DIFERENCIA = "DIFERENCIA";
    public static final String SIN_INSTRUCCION = "SIN_INSTRUCCION";
    static final int PERIODO_MAXIMO_DIAS = 366;

    public static final String NOTA_FISCAL = "Reporte de movimientos, no una declaracion tributaria. La retencion es"
            + " una tasa SINTETICA de las condiciones de cada producto: el tratamiento fiscal real (DR-INV-03) esta"
            + " pendiente de la definicion de fiscal.";

    public record Linea(
            UUID comprobanteId,
            UUID posicionId,
            String productoCodigo,
            String tipo,
            LocalDate fecha,
            BigDecimal principal,
            BigDecimal interes,
            BigDecimal impuesto,
            BigDecimal comision,
            BigDecimal perdidaRealizada,
            BigDecimal neto,
            String moneda,
            OrigenDatos origen,
            String estadoEnLibro) {}

    public record Totales(
            BigDecimal principalSuscrito,
            BigDecimal principalDevuelto,
            BigDecimal interes,
            BigDecimal impuesto,
            BigDecimal comision,
            BigDecimal perdidaRealizada,
            BigDecimal netoAcreditado) {}

    public record Conciliacion(
            BigDecimal debitadoSegunComprobantes,
            BigDecimal debitadoEnLibro,
            BigDecimal acreditadoSegunComprobantes,
            BigDecimal acreditadoEnLibro,
            int lineasPendientesEnLibro,
            int lineasConDiferencia,
            int distribucionesConDiscrepancia,
            boolean cuadra) {}

    public record Reporte(
            LocalDate desde,
            LocalDate hasta,
            List<Linea> lineas,
            Totales totales,
            Conciliacion conciliacion,
            OrigenDatos origen,
            boolean aptoProduccion,
            String notaFiscal) {}

    private final Datos datos;
    private final Transaccionar tx;
    private final ComprobanteRepositorio comprobantes;
    private final InstruccionRepositorio instrucciones;
    private final DistribucionRepositorio distribuciones;
    private final PosicionRepositorio posiciones;
    private final CatalogoRepositorio catalogo;

    public CU128ReporteFiscal(
            Datos datos,
            Transaccionar tx,
            ComprobanteRepositorio comprobantes,
            InstruccionRepositorio instrucciones,
            DistribucionRepositorio distribuciones,
            PosicionRepositorio posiciones,
            CatalogoRepositorio catalogo) {
        this.datos = datos;
        this.tx = tx;
        this.comprobantes = comprobantes;
        this.instrucciones = instrucciones;
        this.distribuciones = distribuciones;
        this.posiciones = posiciones;
        this.catalogo = catalogo;
    }

    public Reporte reporte(LocalDate desde, LocalDate hasta, ContextoSesion ctx) {
        validar(desde, hasta);
        OffsetDateTime inicio = desde.atStartOfDay(Reloj.ZONA).toOffsetDateTime();
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay(Reloj.ZONA).toOffsetDateTime();
        return tx.en(() -> datos.conContexto(ctx, dsl -> {
            List<Comprobante> lista = comprobantes.delTitular(dsl, ctx.usuarioId(), inicio, fin);
            Map<UUID, Instruccion> delLibro =
                    instrucciones
                            .deOrigenes(
                                    dsl,
                                    lista.stream().map(Comprobante::origenId).toList())
                            .stream()
                            .filter(i -> "DEBITAR".equals(i.tipo()) || "ACREDITAR".equals(i.tipo()))
                            .collect(Collectors.toMap(Instruccion::origenId, i -> i, (a, b) -> a));
            Map<UUID, String> codigos = new HashMap<>();
            List<Linea> lineas = lista.stream()
                    .map(c -> linea(c, delLibro.get(c.origenId()), codigoDe(dsl, c.posicionId(), codigos)))
                    .toList();
            int discrepancias = (int) distribuciones.delTitular(dsl, ctx.usuarioId(), inicio, fin).stream()
                    .filter(d -> "DISCREPANCIA".equals(d.estado()))
                    .count();
            return armar(desde, hasta, lineas, lista, delLibro, discrepancias);
        }));
    }

    /** El mismo reporte como CSV (separador coma, decimales con punto), con el encabezado fiscal y una fila de totales. */
    public String exportarCsv(LocalDate desde, LocalDate hasta, ContextoSesion ctx) {
        return ExportacionFiscal.csv(reporte(desde, hasta, ctx));
    }

    private String codigoDe(org.jooq.DSLContext dsl, UUID posicionId, Map<UUID, String> cache) {
        return cache.computeIfAbsent(posicionId, id -> posiciones
                .porId(dsl, id)
                .flatMap(p -> catalogo.porId(dsl, p.productoId()))
                .map(CatalogoRepositorio.Producto::codigo)
                .orElse("?"));
    }

    private static Linea linea(Comprobante c, Instruccion i, String codigo) {
        var d = c.d();
        BigDecimal esperado = "SUSCRIPCION".equals(c.tipo()) ? d.principal() : d.neto();
        String estado;
        if (i == null) {
            estado = SIN_INSTRUCCION;
        } else if ("PENDIENTE".equals(i.estado())) {
            estado = PENDIENTE_EN_LIBRO;
        } else if ("APLICADA".equals(i.estado()) && i.monto().compareTo(esperado) == 0) {
            estado = CONCILIADA;
        } else {
            estado = DIFERENCIA;
        }
        return new Linea(
                c.id(),
                c.posicionId(),
                codigo,
                c.tipo(),
                c.emitidoEn().atZoneSameInstant(Reloj.ZONA).toLocalDate(),
                d.principal(),
                d.interes(),
                d.impuesto(),
                d.comision(),
                d.perdidaRealizada(),
                d.neto(),
                c.moneda(),
                OrigenDatos.valueOf(c.origen()),
                estado);
    }

    private static Reporte armar(
            LocalDate desde,
            LocalDate hasta,
            List<Linea> lineas,
            List<Comprobante> lista,
            Map<UUID, Instruccion> delLibro,
            int discrepancias) {
        BigDecimal cero = BigDecimal.ZERO.setScale(2);
        BigDecimal suscrito = cero;
        BigDecimal devuelto = cero;
        BigDecimal interes = cero;
        BigDecimal impuesto = cero;
        BigDecimal comision = cero;
        BigDecimal perdida = cero;
        BigDecimal acreditado = cero;
        BigDecimal debitadoEnLibro = cero;
        BigDecimal acreditadoEnLibro = cero;
        for (Comprobante c : lista) {
            var d = c.d();
            Instruccion i = delLibro.get(c.origenId());
            boolean aplicada = i != null && "APLICADA".equals(i.estado());
            if ("SUSCRIPCION".equals(c.tipo())) {
                suscrito = suscrito.add(d.principal());
                debitadoEnLibro = aplicada ? debitadoEnLibro.add(i.monto()) : debitadoEnLibro;
            } else {
                devuelto = devuelto.add(d.principal());
                interes = interes.add(d.interes());
                impuesto = impuesto.add(d.impuesto());
                comision = comision.add(d.comision());
                perdida = perdida.add(d.perdidaRealizada());
                acreditado = acreditado.add(d.neto());
                acreditadoEnLibro = aplicada ? acreditadoEnLibro.add(i.monto()) : acreditadoEnLibro;
            }
        }
        int pendientes = (int) lineas.stream()
                .filter(l -> PENDIENTE_EN_LIBRO.equals(l.estadoEnLibro()))
                .count();
        int diferencias = (int) lineas.stream()
                .filter(l -> DIFERENCIA.equals(l.estadoEnLibro()) || SIN_INSTRUCCION.equals(l.estadoEnLibro()))
                .count();
        boolean cuadra = pendientes == 0 && diferencias == 0 && discrepancias == 0;
        boolean sintetico = lineas.stream().anyMatch(l -> l.origen() == OrigenDatos.SINTETICO);
        return new Reporte(
                desde,
                hasta,
                lineas,
                new Totales(suscrito, devuelto, interes, impuesto, comision, perdida, acreditado),
                new Conciliacion(
                        suscrito,
                        debitadoEnLibro,
                        acreditado,
                        acreditadoEnLibro,
                        pendientes,
                        diferencias,
                        discrepancias,
                        cuadra),
                sintetico || lineas.isEmpty() ? OrigenDatos.SINTETICO : OrigenDatos.VERIFICADO,
                false,
                NOTA_FISCAL);
    }

    private static void validar(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw Errores.de(128, 1, "El periodo es invalido: la fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= PERIODO_MAXIMO_DIAS) {
            throw Errores.de(128, 1, "El periodo no puede pasar de " + PERIODO_MAXIMO_DIAS + " dias.");
        }
    }
}
