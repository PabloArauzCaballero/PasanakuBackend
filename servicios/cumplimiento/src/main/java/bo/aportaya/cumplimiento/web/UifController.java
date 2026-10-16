package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.web.generado.UifApi;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeReportesRegulatorios;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeRequerimientos;
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
 * oficial de cumplimiento (backoffice carril A · H9). Auditoria los lee.
 */
@RestController
public class UifController implements UifApi {

    private final TableroDeCumplimientoWeb tablero;

    public UifController(TableroDeCumplimientoWeb tablero) {
        this.tablero = tablero;
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
