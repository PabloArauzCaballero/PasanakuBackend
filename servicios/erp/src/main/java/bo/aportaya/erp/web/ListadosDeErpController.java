package bo.aportaya.erp.web;

import static bo.aportaya.erp.web.PermisosDeContabilidad.CUENTAS_POR_PAGAR;
import static bo.aportaya.erp.web.PermisosDeContabilidad.PAGAR;
import static bo.aportaya.erp.web.PermisosDeContabilidad.REPORTES;

import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.web.generado.ErpApi;
import bo.aportaya.erp.web.generado.modelo.PaginaDeActivos;
import bo.aportaya.erp.web.generado.modelo.PaginaDeCuentasPorCobrar;
import bo.aportaya.erp.web.generado.modelo.PaginaDeFacturas;
import bo.aportaya.erp.web.generado.modelo.PaginaDeOrdenes;
import bo.aportaya.erp.web.generado.modelo.PaginaDePartidas;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * Los listados de Contabilidad del backoffice (carril A · A6): traducen y delegan.
 *
 * <p>Clase abstracta y no otro {@code @RestController}: el generador pone todas las rutas de {@code /erp}
 * en {@code ErpApi}, y dos controladores implementandola registrarian dos veces cada mapeo. Lo que si se
 * separa es el codigo: {@link ErpController} la extiende (mismo patron que {@code RutasDeHabilitacion} y que el
 * {@code ExpedienteController} abstracto de identidad). El nombre termina en {@code Controller} porque
 * {@code scripts/generar_postman.py} lee los {@code @Permiso} de esos archivos.
 *
 * <p>Cada listado lo lee quien opera el modulo, quien tiene {@code CONTABILIDAD_ERP_REPORTES} (lectura) y,
 * donde hace falta para operar, el permiso vecino: Tesoreria ({@code PAGAR}) ve las facturas que paga, y
 * quien registra facturas ({@code CUENTAS_POR_PAGAR}) ve las ordenes contra las que factura.
 */
abstract class ListadosDeErpController implements ErpApi {

    private final ConsultarListadosDeErp listados;
    private final SesionDeLaPeticion sesion;

    protected ListadosDeErpController(ConsultarListadosDeErp listados, SesionDeLaPeticion sesion) {
        this.listados = listados;
        this.sesion = sesion;
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_PRESUPUESTO")
    @PermisoAlternativo({REPORTES})
    public ResponseEntity<PaginaDePartidas> listarPartidasPresupuestarias(
            UUID ejercicioId, List<String> estados, Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-101", "partidas");
        var pagina = listados.partidas(
                Optional.ofNullable(ejercicioId), filtro(estados, limite, antesDe, antesDeId), sesion.actual());
        return ResponseEntity.ok(new PaginaDePartidas()
                .items(pagina.items().stream().map(MapeoDeListados::partida).toList())
                .proximoAntesDe(pagina.proximoAntesDe().orElse(null))
                .proximoAntesDeId(pagina.proximoAntesDeId().orElse(null)));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_COMPRAS")
    @PermisoAlternativo({CUENTAS_POR_PAGAR, REPORTES})
    public ResponseEntity<PaginaDeOrdenes> listarOrdenesDeCompra(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-102", "ordenes");
        var pagina = listados.ordenes(filtro(estados, limite, antesDe, antesDeId), sesion.actual());
        return ResponseEntity.ok(new PaginaDeOrdenes()
                .items(pagina.items().stream().map(MapeoDeListados::orden).toList())
                .proximoAntesDe(pagina.proximoAntesDe().orElse(null))
                .proximoAntesDeId(pagina.proximoAntesDeId().orElse(null)));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_CUENTAS_POR_PAGAR")
    @PermisoAlternativo({PAGAR, REPORTES})
    public ResponseEntity<PaginaDeFacturas> listarFacturasDeProveedor(
            List<String> estados, Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-103", "facturas");
        var pagina = listados.facturas(filtro(estados, limite, antesDe, antesDeId), sesion.actual());
        return ResponseEntity.ok(new PaginaDeFacturas()
                .items(pagina.items().stream().map(MapeoDeListados::factura).toList())
                .proximoAntesDe(pagina.proximoAntesDe().orElse(null))
                .proximoAntesDeId(pagina.proximoAntesDeId().orElse(null)));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_COBRAR")
    @PermisoAlternativo({REPORTES})
    public ResponseEntity<PaginaDeCuentasPorCobrar> listarCuentasPorCobrar(
            List<String> estados, Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-104", "cuentas");
        var pagina = listados.cuentasPorCobrar(filtro(estados, limite, antesDe, antesDeId), sesion.actual());
        return ResponseEntity.ok(new PaginaDeCuentasPorCobrar()
                .items(pagina.items().stream().map(MapeoDeListados::cuenta).toList())
                .proximoAntesDe(pagina.proximoAntesDe().orElse(null))
                .proximoAntesDeId(pagina.proximoAntesDeId().orElse(null)));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_ACTIVOS_FIJOS")
    @PermisoAlternativo({REPORTES})
    public ResponseEntity<PaginaDeActivos> listarActivosFijos(
            List<String> estados, Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-105", "activos");
        var pagina = listados.activos(filtro(estados, limite, antesDe, antesDeId), sesion.actual());
        return ResponseEntity.ok(new PaginaDeActivos()
                .items(pagina.items().stream().map(MapeoDeListados::activo).toList())
                .proximoAntesDe(pagina.proximoAntesDe().orElse(null))
                .proximoAntesDeId(pagina.proximoAntesDeId().orElse(null)));
    }

    /** El contrato fija el tamano por omision (25) y el rango (1..100); aca solo se pasa lo que llego. */
    private static <C> Filtro<C> filtro(List<String> estados, Integer limite, C antesDe, UUID antesDeId) {
        return new Filtro<>(
                estados == null ? List.of() : estados,
                Optional.ofNullable(antesDe),
                Optional.ofNullable(antesDeId),
                limite);
    }
}
