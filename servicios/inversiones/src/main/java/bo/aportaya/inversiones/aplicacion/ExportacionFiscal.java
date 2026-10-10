package bo.aportaya.inversiones.aplicacion;

import static bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal.CONCILIADA;
import static bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal.DIFERENCIA;
import static bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal.NOTA_FISCAL;

import bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal.Linea;
import bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal.Reporte;
import bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal.Totales;

/**
 * El reporte fiscal de CU-128 como CSV: nota fiscal y periodo como comentarios, el encabezado, una fila por
 * linea y una fila {@code TOTAL}. Son las mismas cifras del reporte, no un calculo aparte.
 */
final class ExportacionFiscal {

    private ExportacionFiscal() {}

    /** El mismo reporte como CSV (separador coma, decimales con punto), con el encabezado fiscal y una fila de totales. */
    static String csv(Reporte r) {
        StringBuilder csv = new StringBuilder();
        csv.append("# ").append(NOTA_FISCAL).append('\n');
        csv.append("# periodo,").append(r.desde()).append(',').append(r.hasta());
        csv.append(",origen_datos,")
                .append(r.origen())
                .append(",apto_produccion,")
                .append(r.aptoProduccion());
        csv.append('\n');
        csv.append("fecha,comprobante,posicion,producto,tipo,principal,interes,impuesto,comision,")
                .append("perdida_realizada,neto,moneda,origen_datos,estado_en_libro\n");
        for (Linea l : r.lineas()) {
            csv.append(l.fecha())
                    .append(',')
                    .append(l.comprobanteId())
                    .append(',')
                    .append(l.posicionId())
                    .append(',');
            csv.append(texto(l.productoCodigo())).append(',').append(l.tipo()).append(',');
            csv.append(l.principal().toPlainString())
                    .append(',')
                    .append(l.interes().toPlainString())
                    .append(',');
            csv.append(l.impuesto().toPlainString())
                    .append(',')
                    .append(l.comision().toPlainString())
                    .append(',');
            csv.append(l.perdidaRealizada().toPlainString())
                    .append(',')
                    .append(l.neto().toPlainString())
                    .append(',');
            csv.append(l.moneda())
                    .append(',')
                    .append(l.origen())
                    .append(',')
                    .append(l.estadoEnLibro())
                    .append('\n');
        }
        Totales t = r.totales();
        csv.append("TOTAL,,,,,")
                .append(t.principalSuscrito().toPlainString())
                .append(',')
                .append(t.interes().toPlainString())
                .append(',')
                .append(t.impuesto().toPlainString())
                .append(',')
                .append(t.comision().toPlainString())
                .append(',')
                .append(t.perdidaRealizada().toPlainString())
                .append(',')
                .append(t.netoAcreditado().toPlainString())
                .append(",BOB,")
                .append(r.origen())
                .append(',')
                .append(r.conciliacion().cuadra() ? CONCILIADA : DIFERENCIA)
                .append('\n');
        return csv.toString();
    }

    /** Un texto de catalogo nunca arranca con un caracter que una planilla tomaria por formula. */
    private static String texto(String valor) {
        String limpio = valor == null ? "" : valor.replace("\"", "'").replace(",", " ");
        return !limpio.isEmpty() && "=+-@".indexOf(limpio.charAt(0)) >= 0 ? "'" + limpio : limpio;
    }
}
