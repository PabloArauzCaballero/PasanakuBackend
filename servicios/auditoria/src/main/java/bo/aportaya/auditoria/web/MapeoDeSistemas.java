package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos;
import bo.aportaya.auditoria.aplicacion.EndpointsDelDia;
import bo.aportaya.auditoria.aplicacion.OutboxPorServicio;
import bo.aportaya.auditoria.aplicacion.SaludDeServicios;
import bo.aportaya.auditoria.web.generado.modelo.EndpointDelDia;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeIncidenteOperativo;
import bo.aportaya.auditoria.web.generado.modelo.IncidenteOperativo;
import bo.aportaya.auditoria.web.generado.modelo.OutboxDeServicio;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeIncidentesOperativos;
import bo.aportaya.auditoria.web.generado.modelo.SalidaEndpointsDelDia;
import bo.aportaya.auditoria.web.generado.modelo.SalidaOutboxPorServicio;
import bo.aportaya.auditoria.web.generado.modelo.SalidaSaludDeServicios;
import bo.aportaya.auditoria.web.generado.modelo.SaludDeServicio;
import bo.aportaya.auditoria.web.generado.modelo.SeveridadDeIncidenteOperativo;
import java.math.BigDecimal;
import java.time.ZoneOffset;

/**
 * Sistemas · del resultado de cada consulta de operacion al contrato, sin logica. Hermano de
 * {@link MapeoDeCatalogo} y {@link MapeoDeTablero}: el controlador implementa una sola interfaz generada y no se
 * puede partir, asi que lo que traduce vive aca.
 */
final class MapeoDeSistemas {

    private MapeoDeSistemas() {}

    static SalidaEndpointsDelDia aContrato(EndpointsDelDia.Salida salida) {
        return new SalidaEndpointsDelDia(
                salida.desde().atOffset(ZoneOffset.UTC),
                salida.hasta().atOffset(ZoneOffset.UTC),
                salida.fuenteDisponible(),
                salida.endpoints().stream()
                        .map(e -> {
                            var fila = new EndpointDelDia(
                                    e.servicio(),
                                    e.metodo(),
                                    e.ruta(),
                                    e.solicitudes(),
                                    e.errores4xx(),
                                    e.errores5xx());
                            e.p95Ms().map(BigDecimal::toPlainString).ifPresent(fila::setP95Ms);
                            return fila;
                        })
                        .toList());
    }

    static SalidaSaludDeServicios aContrato(SaludDeServicios.Salida salida) {
        var cuerpo = new SalidaSaludDeServicios(
                salida.consultadoEn().atOffset(ZoneOffset.UTC),
                salida.servicios().stream()
                        .map(r -> {
                            var fila = new SaludDeServicio(
                                    r.servicio(),
                                    SaludDeServicio.EstadoEnum.fromValue(
                                            r.estado().name()));
                            r.latenciaMs().ifPresent(fila::setLatenciaMs);
                            var medido = salida.disponibilidad().get(r.servicio());
                            if (medido != null) {
                                fila.setMinutosMedidos30d(medido.minutosMedidos());
                                medido.disponibilidad()
                                        .map(BigDecimal::toPlainString)
                                        .ifPresent(fila::setDisponibilidad30d);
                                medido.presupuestoRestante(salida.objetivoSlo())
                                        .map(BigDecimal::toPlainString)
                                        .ifPresent(fila::setPresupuestoErrorRestante);
                                medido.ultimaInterrupcion()
                                        .map(i -> i.atOffset(ZoneOffset.UTC))
                                        .ifPresent(fila::setUltimaInterrupcion);
                            }
                            return fila;
                        })
                        .toList());
        cuerpo.setObjetivoSlo(salida.objetivoSlo().toPlainString());
        return cuerpo;
    }

    static SalidaOutboxPorServicio aContrato(OutboxPorServicio.Salida salida) {
        var servicios = salida.servicios().stream()
                .map(o -> {
                    var x = new OutboxDeServicio(o.servicio());
                    o.pendientes().ifPresent(x::setPendientes);
                    o.fallidos().ifPresent(x::setFallidos);
                    o.edadMasViejoSegundos().ifPresent(x::setEdadMasViejoSegundos);
                    o.publicados24h().ifPresent(x::setPublicados24h);
                    o.errores24h().ifPresent(x::setErroresDePublicacion24h);
                    return x;
                })
                .toList();
        return new SalidaOutboxPorServicio(
                salida.consultadoEn().atOffset(ZoneOffset.UTC), salida.fuenteDisponible(), servicios);
    }

    static PaginaDeIncidentesOperativos aContrato(ConsultarIncidentesOperativos.Pagina resultado) {
        var items = resultado.items().stream()
                .map(f -> {
                    var i = new IncidenteOperativo(
                            f.id(),
                            f.codigo(),
                            f.titulo(),
                            SeveridadDeIncidenteOperativo.fromValue(f.severidad()),
                            f.sistemaAfectado(),
                            EstadoDeIncidenteOperativo.fromValue(f.estado()),
                            f.detectadoEn(),
                            f.impactoUsuarios());
                    f.resueltoEn().ifPresent(i::setResueltoEn);
                    return i;
                })
                .toList();
        return new PaginaDeIncidentesOperativos(
                items, resultado.pagina(), resultado.limite(), resultado.total(), resultado.totalPaginas());
    }
}
