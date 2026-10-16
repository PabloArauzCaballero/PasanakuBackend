package bo.aportaya.inversiones.web;

import bo.aportaya.inversiones.aplicacion.CU120Catalogo.VistaProducto;
import bo.aportaya.inversiones.aplicacion.CU123Posiciones.VistaPosicion;
import bo.aportaya.inversiones.aplicacion.VistaComprobante;
import bo.aportaya.inversiones.aplicacion.VistaOrden;
import bo.aportaya.inversiones.aplicacion.VistaRescate;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.web.generado.modelo.Corte;
import bo.aportaya.inversiones.web.generado.modelo.DetalleDpf;
import bo.aportaya.inversiones.web.generado.modelo.Dinero;
import bo.aportaya.inversiones.web.generado.modelo.ProductoInversion;
import bo.aportaya.inversiones.web.generado.modelo.SalidaOrden;
import bo.aportaya.inversiones.web.generado.modelo.SalidaPosicion;
import bo.aportaya.inversiones.web.generado.modelo.SalidaRescate;
import bo.aportaya.inversiones.web.generado.modelo.Valoracion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;

/**
 * Traduce los modelos del contrato a los de la aplicacion y al reves. Sin logica: lo unico
 * que decide aca es la forma. Los importes salen siempre como cadena al centavo; las
 * cuotas y los valores de cuota, a seis decimales.
 */
final class MapeoDeInversiones {

    private MapeoDeInversiones() {}

    static Dinero dinero(BigDecimal monto, String moneda) {
        var d = new Dinero();
        d.setMonto(monto.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        d.setMoneda(Dinero.MonedaEnum.fromValue(moneda));
        return d;
    }

    static Dinero bob(BigDecimal monto) {
        return dinero(monto, "BOB");
    }

    static String seis(BigDecimal valor) {
        return valor == null
                ? null
                : valor.setScale(6, RoundingMode.UNNECESSARY).toPlainString();
    }

    static String tasa(java.util.Optional<BigDecimal> t) {
        return t.map(MapeoDeInversiones::seis).orElse(null);
    }

    static ProductoInversion producto(VistaProducto v) {
        var c = v.condiciones();
        var cond = new bo.aportaya.inversiones.web.generado.modelo.Condiciones();
        cond.setVersionId(c.versionId());
        cond.setNumero(c.numero());
        cond.setPlazoDias(c.plazoDias().orElse(null));
        cond.setBaseDias(c.baseDias().orElse(null));
        cond.setTasaNominalAnual(tasa(c.tasaNominalAnual()));
        cond.setPermiteRescateAnticipado(c.permiteRescateAnticipado());
        cond.setPenalizacionAnticipo(tasa(c.penalizacionAnticipo()));
        cond.setDiasRescate(c.diasRescate().orElse(null));
        cond.setHoraCorte(c.horaCorte().map(Object::toString).orElse(null));
        cond.setMontoMinimo(bob(c.montoMinimo()));
        cond.setTasaRetencion(tasa(c.tasaRetencion()));
        cond.setTasaComisionExito(tasa(c.tasaComisionExito()));
        cond.setCostos(c.costos());
        cond.setTexto(c.texto());
        cond.setTextoHash(c.textoHash());
        cond.setFuente(c.fuente());
        cond.setFechaCotizacion(c.fechaCotizacion().atOffset(ZoneOffset.UTC));
        cond.setOrigenDatos(bo.aportaya.inversiones.web.generado.modelo.Condiciones.OrigenDatosEnum.fromValue(
                c.origen().name()));
        cond.setAptoProduccion(c.aptoProduccion());
        cond.setAdvertencia(bo.aportaya.inversiones.dominio.TextoDeCondiciones.ADVERTENCIA);

        var p = new ProductoInversion();
        p.setCodigo(v.codigo());
        p.setTipo(ProductoInversion.TipoEnum.fromValue(v.tipo().name()));
        p.setNombre(v.nombre());
        p.setEmisor(v.emisor());
        p.setMoneda(ProductoInversion.MonedaEnum.fromValue(v.moneda()));
        p.setNivelRiesgo(ProductoInversion.NivelRiesgoEnum.fromValue(v.nivelRiesgo()));
        p.setTitularidad(v.titularidad());
        p.setCondiciones(cond);
        return p;
    }

    static SalidaOrden orden(VistaOrden v) {
        var s = new SalidaOrden();
        s.setOrdenId(v.ordenId());
        s.setEstado(SalidaOrden.EstadoEnum.fromValue(v.estado()));
        s.setProductoCodigo(v.productoCodigo());
        s.setMonto(dinero(v.monto(), v.moneda()));
        s.setVersionCondicionesId(v.versionCondicionesId());
        s.setConsentimientoId(v.consentimientoId());
        s.setPosicionId(v.posicionId());
        s.setFechaValor(v.fechaValor());
        s.setMotivoRechazo(v.motivoRechazo());
        s.setMensaje(v.mensaje());
        return s;
    }

    static SalidaPosicion posicion(VistaPosicion v) {
        var s = new SalidaPosicion();
        s.setPosicionId(v.id());
        s.setTipo(SalidaPosicion.TipoEnum.fromValue(v.tipo().name()));
        s.setProductoCodigo(v.productoCodigo());
        s.setEstado(SalidaPosicion.EstadoEnum.fromValue(v.estado()));
        s.setPrincipal(dinero(v.principal(), v.moneda()));
        s.setCostoBaseVigente(dinero(v.costoBaseVigente(), v.moneda()));
        s.setFechaConstitucion(v.fechaConstitucion());
        s.setFechaVencimiento(v.fechaVencimiento());
        s.setOrigenDatos(SalidaPosicion.OrigenDatosEnum.fromValue(v.origen().name()));
        s.setAdvertencia(v.advertencia());
        v.valoracion().ifPresent(x -> s.setValoracion(valoracion(x, v.moneda())));
        v.dpf().ifPresent(x -> s.setDpf(dpf(x, v.moneda())));
        return s;
    }

    private static Valoracion valoracion(
            bo.aportaya.inversiones.aplicacion.CU123Posiciones.Valoracion x, String moneda) {
        var v = new Valoracion();
        v.setCuotas(seis(x.cuotas()));
        v.setValorCuota(seis(x.valorCuota()));
        v.setFechaValor(x.fechaValor());
        v.setAntiguedadDias((int) x.antiguedadDias());
        v.setDesactualizado(x.desactualizado());
        v.setValorBruto(dinero(x.valorBruto(), moneda));
        v.setVariacion(dinero(x.variacion(), moneda));
        v.setComisionDeExitoDevengada(dinero(x.comisionDevengada(), moneda));
        v.setValorNetoEstimado(dinero(x.valorNetoEstimado(), moneda));
        v.setOrigenDatos(Valoracion.OrigenDatosEnum.fromValue(x.origen().name()));
        v.setAdvertencia(
                x.origen() == OrigenDatos.SINTETICO
                        ? "Valor SINTETICO de demostracion. Puede subir o bajar; no hay rendimiento garantizado."
                        : "Puede subir o bajar; no hay rendimiento garantizado.");
        return v;
    }

    private static DetalleDpf dpf(bo.aportaya.inversiones.aplicacion.CU123Posiciones.DetalleDpf x, String moneda) {
        var d = new DetalleDpf();
        d.setTasaNominalAnual(seis(x.tasaNominalAnual()));
        d.setBaseDias(x.baseDias());
        d.setDiasDevengados(x.diasDevengados());
        d.setInteresDevengado(dinero(x.interesDevengado(), moneda));
        d.setInteresPagado(dinero(x.interesPagado(), moneda));
        d.setImpuestoEstimado(dinero(x.impuestoEstimado(), moneda));
        d.setImpuestoPagado(dinero(x.impuestoPagado(), moneda));
        d.setVencimiento(x.vencimiento());
        return d;
    }

    static SalidaRescate rescate(VistaRescate v) {
        var s = new SalidaRescate();
        s.setRescateId(v.rescateId());
        s.setPosicionId(v.posicionId());
        s.setTipo(SalidaRescate.TipoEnum.fromValue(v.tipo()));
        s.setEstado(SalidaRescate.EstadoEnum.fromValue(v.estado()));
        s.setCuotas(seis(v.cuotas()));
        if (v.fechaValor() != null || v.liquidaEn() != null) {
            var corte = new Corte();
            corte.setFechaValor(v.fechaValor());
            corte.setLiquidaEn(v.liquidaEn());
            s.setCorte(corte);
        }
        if (v.netoAcreditar() != null) {
            s.setNetoAcreditar(bob(v.netoAcreditar()));
        }
        s.setMotivoRechazo(v.motivoRechazo());
        s.setMensaje(v.mensaje());
        return s;
    }

    static bo.aportaya.inversiones.web.generado.modelo.Comprobante comprobante(VistaComprobante c, String moneda) {
        var s = new bo.aportaya.inversiones.web.generado.modelo.Comprobante();
        s.setComprobanteId(c.id());
        s.setTipo(bo.aportaya.inversiones.web.generado.modelo.Comprobante.TipoEnum.fromValue(c.tipo()));
        s.setSentidoTitular(bo.aportaya.inversiones.web.generado.modelo.Comprobante.SentidoTitularEnum.fromValue(
                c.sentidoTitular()));
        s.setPrincipal(dinero(c.d().principal(), moneda));
        s.setInteres(dinero(c.d().interes(), moneda));
        s.setImpuesto(dinero(c.d().impuesto(), moneda));
        s.setComision(dinero(c.d().comision(), moneda));
        s.setPerdidaRealizada(dinero(c.d().perdidaRealizada(), moneda));
        s.setNeto(dinero(c.d().neto(), moneda));
        s.setOrigenDatos(bo.aportaya.inversiones.web.generado.modelo.Comprobante.OrigenDatosEnum.fromValue(c.origen()));
        s.setEmitidoEn(c.emitidoEn());
        return s;
    }
}
