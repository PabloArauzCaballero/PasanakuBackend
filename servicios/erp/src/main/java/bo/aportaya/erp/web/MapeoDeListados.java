package bo.aportaya.erp.web;

import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.erp.web.generado.modelo.ActivoEnLista;
import bo.aportaya.erp.web.generado.modelo.CuentaPorCobrarEnLista;
import bo.aportaya.erp.web.generado.modelo.Dinero;
import bo.aportaya.erp.web.generado.modelo.FacturaEnLista;
import bo.aportaya.erp.web.generado.modelo.OrdenEnLista;
import bo.aportaya.erp.web.generado.modelo.PartidaEnLista;

/**
 * Filas de listado (carril A · A6) a los tipos del contrato. Los importes salen como CADENA de dos
 * decimales con su moneda (ADR-019): nunca un {@code number} de JSON.
 */
final class MapeoDeListados {

    private MapeoDeListados() {}

    static PartidaEnLista partida(FilasDeErp.Partida p) {
        return new PartidaEnLista()
                .partidaId(p.partidaId())
                .presupuestoId(p.presupuestoId())
                .presupuestoNombre(p.presupuestoNombre())
                .estado(PartidaEnLista.EstadoEnum.fromValue(p.estado()))
                .centroCostoId(p.centroCostoId())
                .centroCostoNombre(p.centroCostoNombre())
                .cuentaContableId(p.cuentaContableId())
                .periodoContableId(p.periodoContableId())
                .anio(p.anio())
                .mes(p.mes())
                .fechaInicioPeriodo(p.fechaInicioPeriodo())
                .montoPresupuestado(dinero(p.presupuestado()))
                .montoEjecutado(dinero(p.ejecutado()));
    }

    static OrdenEnLista orden(FilasDeErp.Orden o) {
        return new OrdenEnLista()
                .ordenCompraId(o.ordenCompraId())
                .numero(o.numero())
                .terceroComercialId(o.terceroId())
                .terceroRazonSocial(o.terceroRazonSocial())
                .centroCostoId(o.centroCostoId().orElse(null))
                .centroCostoNombre(o.centroCostoNombre().orElse(null))
                .estado(OrdenEnLista.EstadoEnum.fromValue(o.estado()))
                .monto(dinero(o.monto()))
                .creadaEn(o.creadaEn());
    }

    static FacturaEnLista factura(FilasDeErp.Factura f) {
        return new FacturaEnLista()
                .facturaProveedorId(f.facturaProveedorId())
                .numeroFactura(f.numeroFactura())
                .terceroComercialId(f.terceroId())
                .terceroRazonSocial(f.terceroRazonSocial())
                .ordenCompraId(f.ordenCompraId().orElse(null))
                .fechaEmision(f.fechaEmision())
                .fechaVencimiento(f.fechaVencimiento())
                .estado(FacturaEnLista.EstadoEnum.fromValue(f.estado()))
                .monto(dinero(f.monto()))
                .montoPagado(dinero(f.pagado()))
                .saldoPendiente(dinero(f.saldoPendiente()))
                .aprobadaPorLaSesion(f.aprobadaPorLaSesion())
                .asientoContableId(f.asientoContableId().orElse(null));
    }

    static CuentaPorCobrarEnLista cuenta(FilasDeErp.CuentaPorCobrar c) {
        return new CuentaPorCobrarEnLista()
                .cuentaPorCobrarId(c.cuentaPorCobrarId())
                .origenTipo(CuentaPorCobrarEnLista.OrigenTipoEnum.fromValue(c.origenTipo()))
                .origenId(c.origenId())
                .terceroComercialId(c.terceroId().orElse(null))
                .terceroRazonSocial(c.terceroRazonSocial().orElse(null))
                .fechaVencimiento(c.fechaVencimiento())
                .estado(CuentaPorCobrarEnLista.EstadoEnum.fromValue(c.estado()))
                .monto(dinero(c.monto()))
                .cobrado(dinero(c.cobrado()))
                .saldoPendiente(dinero(c.saldoPendiente()));
    }

    static ActivoEnLista activo(FilasDeErp.Activo a) {
        return new ActivoEnLista()
                .activoFijoId(a.activoFijoId())
                .codigo(a.codigo())
                .descripcion(a.descripcion())
                .categoriaNombre(a.categoriaNombre())
                .vidaUtilMeses(a.vidaUtilMeses())
                .fechaAdquisicion(a.fechaAdquisicion())
                .estado(ActivoEnLista.EstadoEnum.fromValue(a.estado()))
                .costoAdquisicion(dinero(a.costo()))
                .valorResidual(dinero(a.residual()))
                .depreciacionAcumulada(dinero(a.acumulada()))
                .valorEnLibros(dinero(a.valorEnLibros()))
                .depreciadoEnElPeriodoVigente(a.depreciadoEnElPeriodoVigente());
    }

    private static Dinero dinero(bo.aportaya.plataforma.dominio.Dinero d) {
        return new Dinero()
                .monto(d.toString())
                .moneda(Dinero.MonedaEnum.fromValue(d.moneda().name()));
    }
}
