package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.AccionesDeCumplimiento;
import bo.aportaya.cumplimiento.aplicacion.CU43RemitirReportes.SalidaReporte;
import bo.aportaya.cumplimiento.aplicacion.CU45AtenderRequerimiento.EntradaRequerimiento;
import bo.aportaya.cumplimiento.aplicacion.CU56EjecutarPruebaDeContinuidad.EntradaPrueba;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaEnvioReporte;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaGeneracionReporte;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaPruebaDeContinuidad;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaRequerimientoDeAutoridad;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaRespuestaRequerimiento;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaPruebaDeContinuidad;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaReporteRegulatorio;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaRequerimientoDeAutoridad;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Traduce las acciones de cumplimiento del backoffice (carril A · H9) al contrato: 201 si es nuevo, 200 si
 * era un reintento. Ningun actor viaja en el cuerpo: lo pone la sesion en {@link AccionesDeCumplimiento}.
 */
@Component
class AccionesDeCumplimientoWeb {

    private final AccionesDeCumplimiento acciones;
    private final SesionDeLaPeticion sesion;

    AccionesDeCumplimientoWeb(AccionesDeCumplimiento acciones, SesionDeLaPeticion sesion) {
        this.acciones = acciones;
        this.sesion = sesion;
    }

    ResponseEntity<SalidaReporteRegulatorio> generar(EntradaGeneracionReporte cuerpo) {
        Traza.marcarCasoDeUso("CU-43", cuerpo.getPeriodo());
        var r = acciones.generarReporte(
                cuerpo.getPeriodo(),
                cuerpo.getCatalogoCodigo(),
                cuerpo.getFormulario().getValue(),
                sesion.actual());
        return estado(r.esNuevo(), reporte(r.salida()));
    }

    ResponseEntity<SalidaReporteRegulatorio> enviar(EntradaEnvioReporte cuerpo) {
        Traza.marcarCasoDeUso("CU-43", cuerpo.getPeriodo());
        var r = acciones.aprobarYEnviar(
                cuerpo.getPeriodo(),
                cuerpo.getCatalogoCodigo(),
                cuerpo.getCanal().getValue(),
                cuerpo.getNumeroConstancia(),
                cuerpo.getAceptadoPorElOrganismo(),
                cuerpo.getReintentos(),
                sesion.actual());
        return estado(r.esNuevo(), reporte(r.salida()));
    }

    ResponseEntity<SalidaRequerimientoDeAutoridad> registrar(EntradaRequerimientoDeAutoridad cuerpo) {
        Traza.marcarCasoDeUso("CU-45", "registro");
        var r = acciones.registrarRequerimiento(
                new EntradaRequerimiento(
                        cuerpo.getAutoridad().getValue(),
                        cuerpo.getNumeroOficio(),
                        cuerpo.getPlazoRespuesta(),
                        cuerpo.getUsuarioAfectadoId(),
                        cuerpo.getAlcance(),
                        cuerpo.getDocumentoUrl(),
                        cuerpo.getHashDocumento(),
                        cuerpo.getExigeBloqueoDeSaldo()),
                sesion.actual());
        return estado(r.esNuevo(), requerimiento(r.salida()));
    }

    ResponseEntity<SalidaRequerimientoDeAutoridad> responder(
            UUID requerimientoId, EntradaRespuestaRequerimiento cuerpo) {
        Traza.marcarCasoDeUso("CU-45", requerimientoId.toString());
        var r = acciones.responderRequerimiento(requerimientoId, cuerpo.getRespuestaUrl(), sesion.actual());
        return estado(r.esNuevo(), requerimiento(r.salida()));
    }

    ResponseEntity<SalidaPruebaDeContinuidad> registrarPrueba(EntradaPruebaDeContinuidad cuerpo) {
        Traza.marcarCasoDeUso("CU-56", cuerpo.getPlanContinuidadId().toString());
        var r = acciones.registrarPrueba(
                new EntradaPrueba(
                        cuerpo.getPlanContinuidadId(),
                        cuerpo.getTipo().getValue(),
                        cuerpo.getFecha(),
                        cuerpo.getEjecutadaPor(),
                        cuerpo.getActaComiteId(),
                        cuerpo.getRtoObtenido(),
                        cuerpo.getRpoObtenido(),
                        cuerpo.getEvidenciaUrl(),
                        cuerpo.getImpactoEnClientes(),
                        cuerpo.getPerdidaCuantificada()),
                sesion.actual());
        var p = r.salida();
        var salida = new SalidaPruebaDeContinuidad();
        salida.setPruebaId(p.pruebaId());
        salida.setRtoObtenido(p.rtoObtenido());
        salida.setRpoObtenido(p.rpoObtenido());
        salida.setResultado(SalidaPruebaDeContinuidad.ResultadoEnum.fromValue(p.resultado()));
        salida.setPlanAccionId(p.planAccionId());
        return estado(r.esNuevo(), salida);
    }

    private static SalidaReporteRegulatorio reporte(SalidaReporte r) {
        var salida = new SalidaReporteRegulatorio();
        salida.setReporteId(r.reporteId());
        salida.setCantidadRegistros(r.cantidadRegistros());
        salida.setReporteEnCero(r.reporteEnCero());
        salida.setHashArchivo(r.hashArchivo());
        salida.setFueraDePlazo(r.fueraDePlazo());
        return salida;
    }

    private static SalidaRequerimientoDeAutoridad requerimiento(UUID id) {
        var salida = new SalidaRequerimientoDeAutoridad();
        salida.setRequerimientoId(id);
        return salida;
    }

    private static <T> ResponseEntity<T> estado(boolean esNuevo, T cuerpo) {
        return ResponseEntity.status(esNuevo ? HttpStatus.CREATED : HttpStatus.OK)
                .body(cuerpo);
    }
}
