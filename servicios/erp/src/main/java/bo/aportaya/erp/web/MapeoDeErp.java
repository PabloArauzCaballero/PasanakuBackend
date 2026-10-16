package bo.aportaya.erp.web;

import bo.aportaya.erp.aplicacion.CU101Presupuestar;
import bo.aportaya.erp.aplicacion.CU102AltaDeTercero;
import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor;
import bo.aportaya.erp.aplicacion.CU104CobrarCuenta;
import bo.aportaya.erp.aplicacion.CU105DepreciarActivo;
import bo.aportaya.erp.web.generado.modelo.EntradaCuentaPorCobrar;
import bo.aportaya.erp.web.generado.modelo.EntradaFactura;
import bo.aportaya.erp.web.generado.modelo.EntradaOrden;
import bo.aportaya.erp.web.generado.modelo.EntradaPago;
import bo.aportaya.erp.web.generado.modelo.EntradaPresupuesto;
import bo.aportaya.erp.web.generado.modelo.SalidaDepreciacion;
import bo.aportaya.erp.web.generado.modelo.SalidaFactura;
import bo.aportaya.erp.web.generado.modelo.SalidaOrden;
import bo.aportaya.erp.web.generado.modelo.SalidaPresupuesto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * La traduccion entre los modelos del contrato y los del caso de uso.
 *
 * <p>Vive aparte del controlador por una razon concreta: el generador agrupa las quince
 * operaciones de {@code /erp} en una sola interfaz, asi que el controlador no se puede
 * partir en varios {@code @RestController} —Spring registraria dos veces cada mapeo—.
 * Lo que si se puede separar es esto, que ademas es lo unico del paquete {@code web}
 * que tiene sentido leer solo.
 *
 * <p>Sin logica: si aparece un {@code if} sobre una regla del pasanaku, esta mal
 * ubicado. Lo unico que decide aca es la forma.
 */
final class MapeoDeErp {

    private MapeoDeErp() {}

    /**
     * Un importe siempre como cadena al centavo.
     *
     * <p>{@code UNNECESSARY} a proposito: si un importe llega con mas de dos decimales,
     * es que el caso de uso lo dejo sin cerrar y redondearlo aca lo taparia. Mejor que
     * falle donde se ve.
     */
    static String monto(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    static List<CU101Presupuestar.Partida> partidas(EntradaPresupuesto cuerpo) {
        return cuerpo.getPartidas().stream()
                .map(p -> new CU101Presupuestar.Partida(
                        p.getCuentaContableId(), p.getPeriodoContableId(), new BigDecimal(p.getMontoPresupuestado())))
                .toList();
    }

    static CU101Presupuestar.EntradaPresupuesto presupuesto(EntradaPresupuesto cuerpo) {
        return new CU101Presupuestar.EntradaPresupuesto(
                cuerpo.getCentroCostoId(),
                cuerpo.getEjercicioFiscalId(),
                cuerpo.getNombre(),
                cuerpo.getMoneda().getValue(),
                partidas(cuerpo));
    }

    static CU102AltaDeTercero.EntradaOrden orden(EntradaOrden cuerpo) {
        return new CU102AltaDeTercero.EntradaOrden(
                cuerpo.getTerceroComercialId(),
                cuerpo.getCentroCostoId(),
                cuerpo.getNumero(),
                cuerpo.getDescripcion(),
                new BigDecimal(cuerpo.getMonto()),
                cuerpo.getMoneda().getValue());
    }

    static CU103FacturaDeProveedor.EntradaFactura factura(EntradaFactura cuerpo) {
        return new CU103FacturaDeProveedor.EntradaFactura(
                cuerpo.getTerceroComercialId(),
                cuerpo.getOrdenCompraId(),
                cuerpo.getCentroCostoId(),
                cuerpo.getNumeroFactura(),
                cuerpo.getFechaEmision(),
                cuerpo.getFechaVencimiento(),
                new BigDecimal(cuerpo.getMonto()),
                cuerpo.getMoneda().getValue(),
                cuerpo.getCuentaGastoId(),
                Boolean.TRUE.equals(cuerpo.getAprobar()));
    }

    static CU103FacturaDeProveedor.EntradaPago pago(
            java.util.UUID facturaId, java.util.UUID claveIdempotencia, EntradaPago cuerpo) {
        return new CU103FacturaDeProveedor.EntradaPago(
                facturaId,
                new BigDecimal(cuerpo.getMonto()),
                cuerpo.getMoneda().getValue(),
                cuerpo.getFormaPago().getValue(),
                claveIdempotencia);
    }

    static CU104CobrarCuenta.EntradaCuenta cuentaPorCobrar(EntradaCuentaPorCobrar cuerpo) {
        return new CU104CobrarCuenta.EntradaCuenta(
                cuerpo.getOrigenTipo(),
                cuerpo.getOrigenId(),
                cuerpo.getTerceroComercialId(),
                new BigDecimal(cuerpo.getMonto()),
                cuerpo.getMoneda().getValue(),
                cuerpo.getFechaVencimiento());
    }

    static SalidaPresupuesto presupuesto(CU101Presupuestar.SalidaPresupuesto salida) {
        var respuesta = new SalidaPresupuesto();
        respuesta.setPresupuestoId(salida.presupuestoId());
        respuesta.setEstado(SalidaPresupuesto.EstadoEnum.fromValue(salida.estado()));
        respuesta.setTotal(monto(salida.total()));
        return respuesta;
    }

    static SalidaOrden orden(CU102AltaDeTercero.SalidaOrden salida) {
        var respuesta = new SalidaOrden();
        respuesta.setOrdenCompraId(salida.ordenId());
        respuesta.setEstado(SalidaOrden.EstadoEnum.fromValue(salida.estado()));
        respuesta.setMonto(monto(salida.monto()));
        return respuesta;
    }

    /**
     * La factura, con {@code asientoContableId} nulo.
     *
     * <p>No es un olvido: el asiento lo escribe {@code nucleo-financiero} al consumir el
     * evento (invariante 12), y {@code factura_proveedor} es append-only, asi que la
     * fila no lo va a tener nunca. El contrato lo declara nulable por eso.
     */
    static SalidaFactura factura(CU103FacturaDeProveedor.SalidaFactura salida) {
        var respuesta = new SalidaFactura();
        respuesta.setFacturaProveedorId(salida.facturaId());
        respuesta.setEstado(SalidaFactura.EstadoEnum.fromValue(salida.estado()));
        respuesta.setMonto(monto(salida.monto()));
        respuesta.setMontoPagado(monto(salida.montoPagado()));
        respuesta.setSaldoPendiente(monto(salida.saldoPendiente()));
        return respuesta;
    }

    static SalidaDepreciacion depreciacion(CU105DepreciarActivo.Corrida corrida) {
        var respuesta = new SalidaDepreciacion();
        respuesta.setActivoFijoId(corrida.activoId());
        respuesta.setDepreciacionId(corrida.depreciacionId());
        respuesta.setMonto(monto(corrida.monto()));
        respuesta.setValorEnLibros(monto(corrida.valorEnLibros()));
        respuesta.setTotalmenteDepreciado(corrida.totalmenteDepreciado());
        return respuesta;
    }

    static bo.aportaya.erp.web.generado.modelo.SalidaEjercicio ejercicioAbierto(
            bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.SalidaEjercicio salida) {
        var respuesta = new bo.aportaya.erp.web.generado.modelo.SalidaEjercicio();
        respuesta.setEjercicioId(salida.ejercicioId());
        respuesta.setAnio(salida.anio());
        respuesta.setPeriodos(salida.periodosAbiertos());
        return respuesta;
    }

    static bo.aportaya.erp.web.generado.modelo.SalidaCierre cierre(
            bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.SalidaCierre salida) {
        var respuesta = new bo.aportaya.erp.web.generado.modelo.SalidaCierre();
        respuesta.setCierreId(salida.cierreId());
        respuesta.setPeriodoId(salida.periodoId());
        respuesta.setTotalDebe(monto(salida.totalDebe()));
        respuesta.setTotalHaber(monto(salida.totalHaber()));
        respuesta.setCerradoEn(salida.cerradoEn());
        return respuesta;
    }

    static bo.aportaya.erp.web.generado.modelo.PaginaDeEjercicios ejercicios(
            bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.PaginaDeEjercicios pagina) {
        var salida = new bo.aportaya.erp.web.generado.modelo.PaginaDeEjercicios();
        salida.setItems(pagina.items().stream()
                .map(e -> {
                    var ejercicio = new bo.aportaya.erp.web.generado.modelo.EjercicioFiscal();
                    ejercicio.setEjercicioId(e.ejercicioId());
                    ejercicio.setAnio(e.anio());
                    ejercicio.setEstado(
                            bo.aportaya.erp.web.generado.modelo.EjercicioFiscal.EstadoEnum.fromValue(e.estado()));
                    ejercicio.setFechaInicio(e.desde());
                    ejercicio.setFechaFin(e.hasta());
                    ejercicio.setPeriodos(
                            e.periodos().stream().map(MapeoDeErp::periodo).toList());
                    return ejercicio;
                })
                .toList());
        salida.setProximoAntesDeAnio(pagina.proximoAntesDeAnio().orElse(null));
        return salida;
    }

    static bo.aportaya.erp.web.generado.modelo.PeriodoContable periodo(
            bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.PeriodoVigente p) {
        var periodo = new bo.aportaya.erp.web.generado.modelo.PeriodoContable();
        periodo.setPeriodoId(p.periodoId());
        periodo.setEjercicioId(p.ejercicioId());
        periodo.setAnio(p.anio());
        periodo.setMes(p.mes());
        periodo.setEstado(bo.aportaya.erp.web.generado.modelo.PeriodoContable.EstadoEnum.fromValue(p.estado()));
        periodo.setFechaInicio(p.desde());
        periodo.setFechaFin(p.hasta());
        return periodo;
    }

    static bo.aportaya.erp.web.generado.modelo.SalidaCobro cobro(
            bo.aportaya.erp.aplicacion.CU104CobrarCuenta.SalidaCobro salida) {
        var respuesta = new bo.aportaya.erp.web.generado.modelo.SalidaCobro();
        respuesta.setCobroId(salida.cobroId());
        respuesta.setCuentaPorCobrarId(salida.cuentaPorCobrarId());
        respuesta.setEstado(bo.aportaya.erp.web.generado.modelo.SalidaCobro.EstadoEnum.fromValue(salida.estado()));
        respuesta.setCobrado(monto(salida.cobrado()));
        respuesta.setSaldoPendiente(monto(salida.saldoPendiente()));
        return respuesta;
    }

    static bo.aportaya.erp.web.generado.modelo.SalidaCorrida corrida(
            bo.aportaya.erp.aplicacion.CU105DepreciarActivo.SalidaCorrida salida) {
        var respuesta = new bo.aportaya.erp.web.generado.modelo.SalidaCorrida();
        respuesta.setPeriodoId(salida.periodoId());
        respuesta.setDepreciaciones(
                salida.depreciaciones().stream().map(MapeoDeErp::depreciacion).toList());
        respuesta.setTotalDepreciado(monto(salida.totalDepreciado()));
        respuesta.setYaCorridos(salida.yaCorridos());
        respuesta.setTotalmenteDepreciados(salida.totalmenteDepreciados());
        return respuesta;
    }

    static bo.aportaya.erp.web.generado.modelo.SalidaEstadoFinanciero estadoFinanciero(
            bo.aportaya.erp.aplicacion.CU106GenerarEstadoFinanciero.SalidaEstado salida) {
        var respuesta = new bo.aportaya.erp.web.generado.modelo.SalidaEstadoFinanciero();
        respuesta.setEstadoFinancieroId(salida.estadoId());
        respuesta.setTipo(bo.aportaya.erp.web.generado.modelo.SalidaEstadoFinanciero.TipoEnum.fromValue(salida.tipo()));
        respuesta.setCuadra(salida.estado().ecuacionCierra());
        respuesta.setHashContenido(salida.hashContenido());
        respuesta.setGeneradoEn(salida.generadoEn());
        return respuesta;
    }

    static bo.aportaya.erp.web.generado.modelo.SalidaPlantilla plantilla(
            bo.aportaya.erp.aplicacion.CU106GenerarEstadoFinanciero.Plantilla plantilla) {
        var respuesta = new bo.aportaya.erp.web.generado.modelo.SalidaPlantilla();
        respuesta.setPlantillaId(plantilla.plantillaId());
        respuesta.setLineas(plantilla.lineas());
        respuesta.setDebes(plantilla.debes());
        respuesta.setHaberes(plantilla.haberes());
        return respuesta;
    }

    static bo.aportaya.erp.aplicacion.CU102AltaDeTercero.EntradaTercero tercero(
            bo.aportaya.erp.web.generado.modelo.EntradaTercero cuerpo) {
        return new bo.aportaya.erp.aplicacion.CU102AltaDeTercero.EntradaTercero(
                cuerpo.getTipo().getValue(),
                cuerpo.getRazonSocial(),
                cuerpo.getNumeroDocumento(),
                cuerpo.getEmail(),
                cuerpo.getCuentaContableId());
    }

    static bo.aportaya.erp.web.generado.modelo.PeriodosDelEjercicio periodosDelEjercicio(
            bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.PeriodosDelEjercicio ejercicio) {
        var salida = new bo.aportaya.erp.web.generado.modelo.PeriodosDelEjercicio();
        salida.setEjercicioId(ejercicio.ejercicioId());
        salida.setAnio(ejercicio.anio());
        salida.setEstado(
                bo.aportaya.erp.web.generado.modelo.PeriodosDelEjercicio.EstadoEnum.fromValue(ejercicio.estado()));
        salida.setPeriodos(ejercicio.periodos().stream()
                .map(p -> {
                    var r = new bo.aportaya.erp.web.generado.modelo.PeriodoConCierre();
                    r.setPeriodoId(p.periodoId());
                    r.setMes(p.mes());
                    r.setEstado(bo.aportaya.erp.web.generado.modelo.PeriodoConCierre.EstadoEnum.fromValue(p.estado()));
                    r.setFechaInicio(p.desde());
                    r.setFechaFin(p.hasta());
                    r.setCerradoEn(p.cerradoEn().orElse(null));
                    r.setTotalDebe(p.totalDebe().map(MapeoDeErp::monto).orElse(null));
                    r.setTotalHaber(p.totalHaber().map(MapeoDeErp::monto).orElse(null));
                    return r;
                })
                .toList());
        return salida;
    }

    /** La cabecera en la que se repite el hash del documento entregado (el cliente compara, no recalcula). */
    static final String CABECERA_DE_HASH = "X-Hash-Contenido";

    static org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> documento(
            bo.aportaya.erp.aplicacion.CU106DescargarEstadoFinanciero.Documento doc) {
        String nombre = doc.tipo().toLowerCase(java.util.Locale.ROOT).replace('_', '-') + "-"
                + doc.hashContenido().substring(0, 8) + ".json";
        return org.springframework.http.ResponseEntity.ok()
                .header(CABECERA_DE_HASH, doc.hashContenido())
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombre + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new org.springframework.core.io.ByteArrayResource(doc.contenido()));
    }
}
