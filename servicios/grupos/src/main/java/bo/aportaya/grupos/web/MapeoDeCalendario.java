package bo.aportaya.grupos.web;

import bo.aportaya.grupos.dominio.CalendarioDeAportes;
import bo.aportaya.grupos.web.generado.modelo.CalendarioDeAportes.EstadoEnum;
import bo.aportaya.grupos.web.generado.modelo.CupoDelCalendario;
import bo.aportaya.grupos.web.generado.modelo.Dinero;

/** Del calendario de aportes del dominio al contrato OpenAPI, sin agregar ni quitar nada. */
final class MapeoDeCalendario {

    private MapeoDeCalendario() {}

    static bo.aportaya.grupos.web.generado.modelo.CalendarioDeAportes aContrato(CalendarioDeAportes c) {
        var salida = new bo.aportaya.grupos.web.generado.modelo.CalendarioDeAportes();
        salida.setPeriodoId(c.periodoId());
        salida.setGrupoId(c.grupoId());
        salida.setNumero(c.numero());
        salida.setEstado(EstadoEnum.fromValue(c.estado()));
        salida.setFechaLimitePago(c.fechaLimitePago());
        salida.setFechaFinGracia(c.fechaFinGracia());
        salida.setFechaEntregaPrevista(c.fechaEntregaPrevista());
        salida.setCupos(c.cupos().stream()
                .map(cupo -> {
                    var o = new CupoDelCalendario();
                    o.setCupoId(cupo.cupoId());
                    o.setParticipanteId(cupo.participanteId());
                    var monto = new Dinero();
                    monto.setMonto(cupo.montoEsperado().monto().toPlainString());
                    monto.setMoneda(Dinero.MonedaEnum.fromValue(
                            cupo.montoEsperado().moneda().name()));
                    o.setMontoEsperado(monto);
                    return o;
                })
                .toList());
        return salida;
    }
}
