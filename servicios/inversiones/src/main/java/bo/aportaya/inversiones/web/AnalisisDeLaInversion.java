package bo.aportaya.inversiones.web;

import static bo.aportaya.inversiones.web.MapeoDeInversiones.bob;
import static bo.aportaya.inversiones.web.MapeoDeInversiones.dinero;
import static bo.aportaya.inversiones.web.MapeoDeInversiones.seis;

import bo.aportaya.inversiones.aplicacion.CU126DistribuirIntereses;
import bo.aportaya.inversiones.aplicacion.CU127ComisionPorResultado;
import bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal;
import bo.aportaya.inversiones.aplicacion.CU129SimularCaja;
import bo.aportaya.inversiones.dominio.SimulacionDeCaja.Alternativa;
import bo.aportaya.inversiones.web.generado.modelo.ConciliacionConElLibro;
import bo.aportaya.inversiones.web.generado.modelo.Distribucion;
import bo.aportaya.inversiones.web.generado.modelo.EntradaSimulacion;
import bo.aportaya.inversiones.web.generado.modelo.LineaFiscal;
import bo.aportaya.inversiones.web.generado.modelo.ListaDeDistribuciones;
import bo.aportaya.inversiones.web.generado.modelo.ReporteFiscal;
import bo.aportaya.inversiones.web.generado.modelo.SalidaComision;
import bo.aportaya.inversiones.web.generado.modelo.SalidaSimulacion;
import bo.aportaya.inversiones.web.generado.modelo.TotalesFiscales;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Lo que {@code /inversiones} ofrece para entender una inversion sin moverla: los intereses
 * repartidos, la comision estimada, el reporte de comprobantes y la simulacion de caja. Del
 * contrato al caso de uso y de vuelta; no decide nada.
 */
@Component
class AnalisisDeLaInversion {

    private final CU126DistribuirIntereses cu126;
    private final CU127ComisionPorResultado cu127;
    private final CU128ReporteFiscal cu128;
    private final CU129SimularCaja cu129;

    AnalisisDeLaInversion(
            CU126DistribuirIntereses cu126,
            CU127ComisionPorResultado cu127,
            CU128ReporteFiscal cu128,
            CU129SimularCaja cu129) {
        this.cu126 = cu126;
        this.cu127 = cu127;
        this.cu128 = cu128;
        this.cu129 = cu129;
    }

    ListaDeDistribuciones distribuciones(UUID posicionId, ContextoSesion ctx) {
        var lista = new ListaDeDistribuciones();
        cu126.listar(posicionId, ctx).forEach(d -> {
            var s = new Distribucion();
            s.setDistribucionId(d.id());
            s.setRescateId(d.rescateId());
            s.setPeriodoDesde(d.periodoDesde());
            s.setPeriodoHasta(d.periodoHasta());
            s.setInteresDevengado(dinero(d.interesDevengado(), d.moneda()));
            s.setPenalizacion(dinero(d.penalizacion(), d.moneda()));
            s.setInteresEsperado(dinero(d.interesEsperado(), d.moneda()));
            s.setInteresPagado(dinero(d.interesPagado(), d.moneda()));
            s.setRetencion(dinero(d.retencion(), d.moneda()));
            s.setInteresNeto(dinero(d.interesNeto(), d.moneda()));
            s.setEstado(Distribucion.EstadoEnum.fromValue(d.estado()));
            s.setMotivoDiscrepancia(d.motivoDiscrepancia());
            s.setDistribuidaEn(d.distribuidaEn());
            lista.addDistribucionesItem(s);
        });
        return lista;
    }

    SalidaComision comision(UUID posicionId, ContextoSesion ctx) {
        var v = cu127.estimar(posicionId, ctx);
        var s = new SalidaComision();
        s.setPosicionId(v.posicionId());
        s.setCuotas(seis(v.cuotas()));
        s.setValorCuota(seis(v.valorCuota()));
        s.setFechaValor(v.fechaValor());
        s.setMarcaMaximaPrevia(seis(v.marcaMaximaPrevia()));
        s.setTasa(seis(v.tasa()));
        s.setBaseElegible(dinero(v.baseElegible(), v.moneda()));
        s.setComisionEstimada(dinero(v.comisionEstimada(), v.moneda()));
        s.setCuotaNetaEstimada(seis(v.cuotaNetaEstimada()));
        s.setMarcaMaximaNueva(seis(v.marcaMaximaNueva()));
        s.setComisionCobradaAntes(dinero(v.comisionCobradaAntes(), v.moneda()));
        s.setOrigenDatos(SalidaComision.OrigenDatosEnum.fromValue(v.origen().name()));
        s.setAdvertencia(v.advertencia());
        return s;
    }

    ReporteFiscal reporte(LocalDate desde, LocalDate hasta, ContextoSesion ctx) {
        var r = cu128.reporte(desde, hasta, ctx);
        var s = new ReporteFiscal();
        s.setDesde(r.desde());
        s.setHasta(r.hasta());
        r.lineas().forEach(l -> s.addLineasItem(linea(l)));
        var t = r.totales();
        var totales = new TotalesFiscales();
        totales.setPrincipalSuscrito(bob(t.principalSuscrito()));
        totales.setPrincipalDevuelto(bob(t.principalDevuelto()));
        totales.setInteres(bob(t.interes()));
        totales.setImpuesto(bob(t.impuesto()));
        totales.setComision(bob(t.comision()));
        totales.setPerdidaRealizada(bob(t.perdidaRealizada()));
        totales.setNetoAcreditado(bob(t.netoAcreditado()));
        s.setTotales(totales);
        var c = r.conciliacion();
        var conciliacion = new ConciliacionConElLibro();
        conciliacion.setDebitadoSegunComprobantes(bob(c.debitadoSegunComprobantes()));
        conciliacion.setDebitadoEnLibro(bob(c.debitadoEnLibro()));
        conciliacion.setAcreditadoSegunComprobantes(bob(c.acreditadoSegunComprobantes()));
        conciliacion.setAcreditadoEnLibro(bob(c.acreditadoEnLibro()));
        conciliacion.setLineasPendientesEnLibro(c.lineasPendientesEnLibro());
        conciliacion.setLineasConDiferencia(c.lineasConDiferencia());
        conciliacion.setDistribucionesConDiscrepancia(c.distribucionesConDiscrepancia());
        conciliacion.setCuadra(c.cuadra());
        s.setConciliacion(conciliacion);
        s.setOrigenDatos(ReporteFiscal.OrigenDatosEnum.fromValue(r.origen().name()));
        s.setAptoProduccion(r.aptoProduccion());
        s.setNotaFiscal(r.notaFiscal());
        return s;
    }

    String csv(LocalDate desde, LocalDate hasta, ContextoSesion ctx) {
        return cu128.exportarCsv(desde, hasta, ctx);
    }

    SalidaSimulacion simular(EntradaSimulacion e, ContextoSesion ctx) {
        var monto = Dinero.de(
                e.getMonto().getMonto(), Moneda.valueOf(e.getMonto().getMoneda().getValue()));
        var productos = e.getProductos() == null ? new HashSet<String>() : new HashSet<>(e.getProductos());
        List<BigDecimal> variaciones = e.getVariacionesDeFondo() == null
                ? List.of()
                : e.getVariacionesDeFondo().stream().map(BigDecimal::new).toList();
        var r = cu129.simular(new CU129SimularCaja.Entrada(monto, e.getHorizonteDias(), productos, variaciones), ctx);
        var s = new SalidaSimulacion();
        s.setMonto(dinero(r.monto().monto(), r.monto().moneda().name()));
        s.setHorizonteDias(r.horizonteDias());
        r.alternativas().forEach(a -> s.addAlternativasItem(alternativa(a)));
        s.setUsaSaldoReal(r.usaSaldoReal());
        s.setAdvertencia(r.advertencia());
        return s;
    }

    private static bo.aportaya.inversiones.web.generado.modelo.Alternativa alternativa(Alternativa a) {
        var s = new bo.aportaya.inversiones.web.generado.modelo.Alternativa();
        s.setTipo(bo.aportaya.inversiones.web.generado.modelo.Alternativa.TipoEnum.fromValue(a.tipo()));
        s.setProductoCodigo(a.productoCodigo());
        s.setDescripcion(a.descripcion());
        s.setDiasParaDisponer(a.diasParaDisponer());
        s.setDisponibleEnElHorizonte(a.disponibleEnElHorizonte());
        s.setInteres(bob(a.interes()));
        s.setRetencion(bob(a.retencion()));
        s.setComision(bob(a.comision()));
        s.setNetoCuandoDisponible(bob(a.netoCuandoDisponible()));
        s.setDiferenciaContraBilletera(bob(a.diferenciaContraBilletera()));
        s.setOrigenDatos(bo.aportaya.inversiones.web.generado.modelo.Alternativa.OrigenDatosEnum.fromValue(
                a.origen().name()));
        return s;
    }

    private static LineaFiscal linea(CU128ReporteFiscal.Linea l) {
        var s = new LineaFiscal();
        s.setComprobanteId(l.comprobanteId());
        s.setPosicionId(l.posicionId());
        s.setProductoCodigo(l.productoCodigo());
        s.setTipo(LineaFiscal.TipoEnum.fromValue(l.tipo()));
        s.setFecha(l.fecha());
        s.setPrincipal(dinero(l.principal(), l.moneda()));
        s.setInteres(dinero(l.interes(), l.moneda()));
        s.setImpuesto(dinero(l.impuesto(), l.moneda()));
        s.setComision(dinero(l.comision(), l.moneda()));
        s.setPerdidaRealizada(dinero(l.perdidaRealizada(), l.moneda()));
        s.setNeto(dinero(l.neto(), l.moneda()));
        s.setOrigenDatos(LineaFiscal.OrigenDatosEnum.fromValue(l.origen().name()));
        s.setEstadoEnLibro(LineaFiscal.EstadoEnLibroEnum.fromValue(l.estadoEnLibro()));
        return s;
    }
}
