package bo.aportaya.tarifas.web;

import bo.aportaya.tarifas.aplicacion.ConsultarTarifarios;
import bo.aportaya.tarifas.web.generado.modelo.CambioDeTarifario;
import bo.aportaya.tarifas.web.generado.modelo.SimulacionDeTarifario;
import bo.aportaya.tarifas.web.generado.modelo.TarifarioEnLista;

/** Del historial de tarifarios de la aplicacion al contrato de {@code GET /tarifas/tarifarios}. */
final class MapeoDeTarifarios {

    private MapeoDeTarifarios() {}

    static TarifarioEnLista aLista(ConsultarTarifarios.Tarifario t) {
        var r = new TarifarioEnLista();
        r.setTarifarioId(t.tarifarioId());
        r.setCodigo(t.codigo());
        r.setVersion(t.version());
        r.setNombre(t.nombre());
        r.setEstado(TarifarioEnLista.EstadoEnum.fromValue(t.estado()));
        r.setMonedaBase(TarifarioEnLista.MonedaBaseEnum.fromValue(t.monedaBase()));
        r.setVigenteDesde(t.vigenteDesde());
        r.setVigenteHasta(t.vigenteHasta().orElse(null));
        r.setDiasPreaviso(t.diasPreaviso());
        r.setPublicadoEn(t.publicadoEn().orElse(null));
        r.setTarifarioAnteriorId(t.tarifarioAnteriorId().orElse(null));
        r.setAprobadoPor(t.aprobadoPor().orElse(null));
        r.setActaAprobacion(t.actaAprobacion().orElse(null));
        r.setCambio(t.cambio().map(MapeoDeTarifarios::cambio).orElse(null));
        r.setSimulacion(t.simulacion().map(MapeoDeTarifarios::simulacion).orElse(null));
        return r;
    }

    private static CambioDeTarifario cambio(ConsultarTarifarios.Cambio c) {
        var r = new CambioDeTarifario();
        r.setCambioId(c.cambioId());
        r.setTipoCambio(CambioDeTarifario.TipoCambioEnum.fromValue(c.tipoCambio()));
        r.setRequierePreaviso(c.requierePreaviso());
        r.setDiasPreaviso(c.diasPreaviso());
        r.setFechaAviso(c.fechaAviso().orElse(null));
        r.setCanalAviso(c.canalAviso().orElse(null));
        r.setUsuariosNotificados(c.usuariosNotificados());
        r.setPermiteRescisionSinCosto(c.permiteRescisionSinCosto());
        return r;
    }

    private static SimulacionDeTarifario simulacion(ConsultarTarifarios.Simulacion s) {
        var r = new SimulacionDeTarifario();
        // Cadena y sin moneda: simulacion_tarifa no la modela (declarado en el contrato).
        r.setDeltaIngresoEstimado(MapeoDeTarifas.importe(s.deltaIngresoEstimado()));
        r.setUsuariosImpactados(s.usuariosImpactados());
        r.setEjecutadaEn(s.ejecutadaEn());
        return r;
    }
}
