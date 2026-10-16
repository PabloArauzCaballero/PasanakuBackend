package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento;
import bo.aportaya.cumplimiento.web.generado.modelo.ActaEnLista;
import bo.aportaya.cumplimiento.web.generado.modelo.Dinero;
import bo.aportaya.cumplimiento.web.generado.modelo.EventoDeRiesgoEnLista;
import bo.aportaya.cumplimiento.web.generado.modelo.IncidenteEnLista;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeActas;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeEventosDeRiesgo;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeIncidentes;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDePruebasDeContinuidad;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeReportesRegulatorios;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeRequerimientos;
import bo.aportaya.cumplimiento.web.generado.modelo.PruebaDeContinuidadEnLista;
import bo.aportaya.cumplimiento.web.generado.modelo.ReporteRegulatorioEnLista;
import bo.aportaya.cumplimiento.web.generado.modelo.RequerimientoEnLista;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Traduce las colas de cumplimiento, riesgo y gobierno (backoffice carril A · H9) al contrato. El permiso
 * lo declara el controlador que delega aca ({@code UifController}, {@code CumplimientoController}).
 */
@Component
class TableroDeCumplimientoWeb {

    private final ConsultarTableroDeCumplimiento tablero;
    private final SesionDeLaPeticion sesion;

    TableroDeCumplimientoWeb(ConsultarTableroDeCumplimiento tablero, SesionDeLaPeticion sesion) {
        this.tablero = tablero;
        this.sesion = sesion;
    }

    ResponseEntity<PaginaDeReportesRegulatorios> reportes(
            List<String> estados, Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-43", "listado");
        var p = tablero.reportes(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeReportesRegulatorios();
        salida.setItems(p.items().stream()
                .map(r -> {
                    var f = new ReporteRegulatorioEnLista();
                    f.setReporteId(r.reporteId());
                    f.setCatalogoReporteId(r.catalogoReporteId());
                    f.setPeriodo(r.periodo());
                    f.setFechaCorte(r.fechaCorte());
                    f.setFechaLimite(r.fechaLimite());
                    f.setEstado(ReporteRegulatorioEnLista.EstadoEnum.fromValue(r.estado()));
                    f.setCantidadRegistros(r.cantidadRegistros());
                    f.setReporteEnCero(r.reporteEnCero());
                    f.setMontoTotal(cadena(r.montoTotal()));
                    f.setGeneradoEn(r.generadoEn().orElse(null));
                    f.setAprobadoPor(r.aprobadoPor().orElse(null));
                    return f;
                })
                .toList());
        salida.setProximoAntesDe(p.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(p.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    ResponseEntity<PaginaDeRequerimientos> requerimientos(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-45", "listado");
        var p = tablero.requerimientos(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeRequerimientos();
        salida.setItems(p.items().stream()
                .map(r -> {
                    var f = new RequerimientoEnLista();
                    f.setRequerimientoId(r.requerimientoId());
                    f.setAutoridad(RequerimientoEnLista.AutoridadEnum.fromValue(r.autoridad()));
                    f.setNumeroOficio(r.numeroOficio());
                    f.setFechaRecepcion(r.fechaRecepcion());
                    f.setPlazoRespuesta(r.plazoRespuesta());
                    f.setEstado(RequerimientoEnLista.EstadoEnum.fromValue(r.estado()));
                    f.setUsuarioAfectadoId(r.usuarioAfectadoId().orElse(null));
                    f.setConBloqueoDeSaldo(r.conBloqueoDeSaldo());
                    f.setRespondidoPor(r.respondidoPor().orElse(null));
                    f.setRespondidoEn(r.respondidoEn().orElse(null));
                    return f;
                })
                .toList());
        salida.setProximoAntesDe(p.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(p.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    ResponseEntity<PaginaDeEventosDeRiesgo> eventos(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-54", "listado");
        var p = tablero.eventos(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeEventosDeRiesgo();
        salida.setItems(p.items().stream()
                .map(e -> {
                    var f = new EventoDeRiesgoEnLista();
                    f.setEventoId(e.eventoId());
                    f.setCodigo(e.codigo());
                    f.setCategoria(e.categoria());
                    f.setFactor(e.factor());
                    f.setLineaNegocio(e.lineaNegocio());
                    f.setFechaOcurrencia(e.fechaOcurrencia());
                    f.setFechaDeteccion(e.fechaDeteccion());
                    f.setPerdidaBruta(dinero(e.perdidaBruta(), e.moneda()));
                    f.setRecuperacion(dinero(e.recuperacion(), e.moneda()));
                    f.setPerdidaNeta(dinero(e.perdidaNeta(), e.moneda()));
                    f.setEstado(EventoDeRiesgoEnLista.EstadoEnum.fromValue(e.estado()));
                    f.setReportadoCentral(e.reportadoCentral());
                    return f;
                })
                .toList());
        salida.setProximoAntesDe(p.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(p.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    ResponseEntity<PaginaDeIncidentes> incidentes(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-55", "listado");
        var p = tablero.incidentes(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeIncidentes();
        salida.setItems(p.items().stream()
                .map(i -> {
                    var f = new IncidenteEnLista();
                    f.setIncidenteId(i.incidenteId());
                    f.setCodigo(i.codigo());
                    f.setTipo(i.tipo());
                    f.setSeveridad(IncidenteEnLista.SeveridadEnum.fromValue(i.severidad()));
                    f.setDatosPersonalesAfectados(i.datosPersonalesAfectados());
                    f.setUsuariosAfectados(i.usuariosAfectados());
                    f.setDetectadoEn(i.detectadoEn());
                    f.setContenidoEn(i.contenidoEn().orElse(null));
                    f.setPlazoReporte(i.plazoReporte());
                    f.setReportadoAlOrganismoEn(i.reportadoAlOrganismoEn().orElse(null));
                    f.setNotificadoATitularesEn(i.notificadoATitularesEn().orElse(null));
                    f.setEstado(IncidenteEnLista.EstadoEnum.fromValue(i.estado()));
                    return f;
                })
                .toList());
        salida.setProximoAntesDe(p.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(p.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    ResponseEntity<PaginaDePruebasDeContinuidad> pruebas(Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-56", "listado");
        var p = tablero.pruebas(Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDePruebasDeContinuidad();
        salida.setItems(p.items().stream()
                .map(x -> {
                    var f = new PruebaDeContinuidadEnLista();
                    f.setPruebaId(x.pruebaId());
                    f.setPlanContinuidadId(x.planContinuidadId());
                    f.setActaComiteId(x.actaComiteId().orElse(null));
                    f.setEjecutadaPor(x.ejecutadaPor());
                    f.setTipo(PruebaDeContinuidadEnLista.TipoEnum.fromValue(x.tipo()));
                    f.setFecha(x.fecha());
                    f.setRtoMinutos(x.rtoMinutos());
                    f.setRpoMinutos(x.rpoMinutos());
                    f.setResultado(PruebaDeContinuidadEnLista.ResultadoEnum.fromValue(x.resultado()));
                    return f;
                })
                .toList());
        salida.setProximoAntesDe(p.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(p.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    ResponseEntity<PaginaDeActas> actas(Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-94", "listado");
        var p = tablero.actas(Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeActas();
        salida.setItems(p.items().stream()
                .map(a -> {
                    var f = new ActaEnLista();
                    f.setActaId(a.actaId());
                    f.setComiteGobiernoId(a.comiteGobiernoId());
                    f.setNumero(a.numero());
                    f.setFecha(a.fecha());
                    f.setCumpleQuorum(a.cumpleQuorum());
                    f.setHashDocumento(a.hash());
                    return f;
                })
                .toList());
        salida.setProximoAntesDe(p.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(p.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    private static String cadena(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static Dinero dinero(BigDecimal valor, String moneda) {
        var d = new Dinero();
        d.setMonto(cadena(valor));
        d.setMoneda(Dinero.MonedaEnum.fromValue(moneda));
        return d;
    }
}
