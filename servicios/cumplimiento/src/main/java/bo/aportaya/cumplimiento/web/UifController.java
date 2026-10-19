package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.web.generado.UifApi;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaEnvioReporte;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaGeneracionReporte;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaRequerimientoDeAutoridad;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaRespuestaRequerimiento;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeReportesRegulatorios;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeRequerimientos;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaReporteRegulatorio;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaRequerimientoDeAutoridad;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de {@code /uif}: reportes regulatorios (CU-43) y requerimientos de autoridad (CU-45) para el
 * oficial de cumplimiento (backoffice carril A · H9): las colas, que Auditoria tambien lee, y las acciones.
 */
@RestController
public class UifController implements UifApi {

    private final TableroDeCumplimientoWeb tablero;
    private final AccionesDeCumplimientoWeb acciones;

    public UifController(TableroDeCumplimientoWeb tablero, AccionesDeCumplimientoWeb acciones) {
        this.tablero = tablero;
        this.acciones = acciones;
    }

    @Override
    @Permiso("CUMPLIMIENTO_REPORTAR")
    public ResponseEntity<SalidaReporteRegulatorio> generarReporteRegulatorio(
            UUID idempotencyKey, EntradaGeneracionReporte cuerpo) {
        return acciones.generar(cuerpo);
    }

    // El aprobador es la sesion: el CU exige que no sea quien genero (R-SEG-04).
    @Override
    @Permiso("CUMPLIMIENTO_REPORTAR")
    public ResponseEntity<SalidaReporteRegulatorio> aprobarYEnviarReporte(
            UUID idempotencyKey, EntradaEnvioReporte cuerpo) {
        return acciones.enviar(cuerpo);
    }

    @Override
    @Permiso("OFICIAL_CUMPLIMIENTO")
    public ResponseEntity<SalidaRequerimientoDeAutoridad> registrarRequerimientoDeAutoridad(
            UUID idempotencyKey, EntradaRequerimientoDeAutoridad cuerpo) {
        return acciones.registrar(cuerpo);
    }

    @Override
    @Permiso("OFICIAL_CUMPLIMIENTO")
    public ResponseEntity<SalidaRequerimientoDeAutoridad> responderRequerimientoDeAutoridad(
            UUID idempotencyKey, UUID requerimientoId, EntradaRespuestaRequerimiento cuerpo) {
        return acciones.responder(requerimientoId, cuerpo);
    }

    @Override
    @Permiso("CUMPLIMIENTO_REPORTAR")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeReportesRegulatorios> listarReportesRegulatorios(
            List<String> estados, Integer limite, LocalDate antesDe, UUID antesDeId) {
        return tablero.reportes(estados, limite, antesDe, antesDeId);
    }

    // El area legal opera por rol (OFICIAL_CUMPLIMIENTO, que el token trae como codigo de rol vigente).
    @Override
    @Permiso("OFICIAL_CUMPLIMIENTO")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeRequerimientos> listarRequerimientosDeAutoridad(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        return tablero.requerimientos(estados, limite, antesDe, antesDeId);
    }
}
