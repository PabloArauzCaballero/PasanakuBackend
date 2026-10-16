package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.web.generado.CumplimientoApi;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeActas;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeEventosDeRiesgo;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeIncidentes;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDePruebasDeContinuidad;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * Las colas de {@code /cumplimiento} que lee el backoffice (carril A · H9): eventos de riesgo (CU-54),
 * incidentes (CU-55), pruebas de continuidad (CU-56) y actas de comite (CU-94). Auditoria lee todas.
 *
 * <p>Es la base de {@link CumplimientoController}: el generador agrupa todo {@code /cumplimiento} en una
 * sola interfaz que un solo controlador implementa, y ese controlador ya esta en el limite de tamano. Los
 * metodos implementan los de {@code CumplimientoApi} (el mapeo HTTP lo pone la interfaz). Implementa la
 * interfaz ella misma: si no, la validacion de metodos ve una jerarquia paralela y rechaza el controlador.
 */
abstract class ColasDeCumplimientoWeb implements CumplimientoApi {

    private final TableroDeCumplimientoWeb tablero;

    protected ColasDeCumplimientoWeb(TableroDeCumplimientoWeb tablero) {
        this.tablero = tablero;
    }

    @Override
    @Permiso("RESPONSABLE_RIESGOS")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeEventosDeRiesgo> listarEventosDeRiesgo(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        return tablero.eventos(estados, limite, antesDe, antesDeId);
    }

    @Override
    @Permiso("RESPONSABLE_SEGURIDAD")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeIncidentes> listarIncidentesDeSeguridad(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        return tablero.incidentes(estados, limite, antesDe, antesDeId);
    }

    @Override
    @Permiso("RESPONSABLE_RIESGOS")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDePruebasDeContinuidad> listarPruebasDeContinuidad(
            Integer limite, LocalDate antesDe, UUID antesDeId) {
        return tablero.pruebas(limite, antesDe, antesDeId);
    }

    @Override
    @Permiso("OFICIAL_CUMPLIMIENTO")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeActas> listarActasDeComite(Integer limite, LocalDate antesDe, UUID antesDeId) {
        return tablero.actas(limite, antesDe, antesDeId);
    }
}
