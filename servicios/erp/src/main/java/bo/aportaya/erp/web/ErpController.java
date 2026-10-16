package bo.aportaya.erp.web;

import static bo.aportaya.erp.web.PermisosDeContabilidad.ACTIVOS_FIJOS;
import static bo.aportaya.erp.web.PermisosDeContabilidad.CERRAR;
import static bo.aportaya.erp.web.PermisosDeContabilidad.COBRAR;
import static bo.aportaya.erp.web.PermisosDeContabilidad.COMPRAS;
import static bo.aportaya.erp.web.PermisosDeContabilidad.CUENTAS_POR_PAGAR;
import static bo.aportaya.erp.web.PermisosDeContabilidad.PAGAR;
import static bo.aportaya.erp.web.PermisosDeContabilidad.PRESUPUESTO;

import bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo;
import bo.aportaya.erp.aplicacion.CU101Presupuestar;
import bo.aportaya.erp.aplicacion.CU102AltaDeTercero;
import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor;
import bo.aportaya.erp.aplicacion.CU104CobrarCuenta;
import bo.aportaya.erp.aplicacion.CU105DepreciarActivo;
import bo.aportaya.erp.aplicacion.CU106DescargarEstadoFinanciero;
import bo.aportaya.erp.aplicacion.CU106GenerarEstadoFinanciero;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp;
import bo.aportaya.erp.web.generado.modelo.EntradaCierre;
import bo.aportaya.erp.web.generado.modelo.EntradaCobro;
import bo.aportaya.erp.web.generado.modelo.EntradaCuentaPorCobrar;
import bo.aportaya.erp.web.generado.modelo.EntradaDepreciacion;
import bo.aportaya.erp.web.generado.modelo.EntradaEjercicio;
import bo.aportaya.erp.web.generado.modelo.EntradaEstadoFinanciero;
import bo.aportaya.erp.web.generado.modelo.EntradaFactura;
import bo.aportaya.erp.web.generado.modelo.EntradaOrden;
import bo.aportaya.erp.web.generado.modelo.EntradaPago;
import bo.aportaya.erp.web.generado.modelo.EntradaPresupuesto;
import bo.aportaya.erp.web.generado.modelo.EntradaTercero;
import bo.aportaya.erp.web.generado.modelo.PaginaDeEjercicios;
import bo.aportaya.erp.web.generado.modelo.PeriodoContable;
import bo.aportaya.erp.web.generado.modelo.PeriodosDelEjercicio;
import bo.aportaya.erp.web.generado.modelo.SalidaCierre;
import bo.aportaya.erp.web.generado.modelo.SalidaCobro;
import bo.aportaya.erp.web.generado.modelo.SalidaCorrida;
import bo.aportaya.erp.web.generado.modelo.SalidaCuentaPorCobrar;
import bo.aportaya.erp.web.generado.modelo.SalidaDepreciacion;
import bo.aportaya.erp.web.generado.modelo.SalidaEjercicio;
import bo.aportaya.erp.web.generado.modelo.SalidaEstadoFinanciero;
import bo.aportaya.erp.web.generado.modelo.SalidaFactura;
import bo.aportaya.erp.web.generado.modelo.SalidaOrden;
import bo.aportaya.erp.web.generado.modelo.SalidaPlantilla;
import bo.aportaya.erp.web.generado.modelo.SalidaPresupuesto;
import bo.aportaya.erp.web.generado.modelo.SalidaTercero;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /erp}: traducen y delegan, sin logica.
 *
 * <p><b>Un solo controlador.</b> El generador agrupa todo {@code /erp} en {@code ErpApi}: dos
 * {@code @RestController} registrarian dos veces cada mapeo. Los listados del backoffice viven
 * en {@link ListadosDeErpController}, que esta clase extiende.
 *
 * <p>Los importes entran y salen como CADENA decimal (ADR-019): un {@code number} de
 * JSON es un doble, y un doble no cuenta plata.
 */
@RestController
public class ErpController extends ListadosDeErpController {

    private final CU100AbrirCerrarPeriodo cu100;
    private final CU101Presupuestar cu101;
    private final CU102AltaDeTercero cu102;
    private final CU103FacturaDeProveedor cu103;
    private final CU104CobrarCuenta cu104;
    private final CU105DepreciarActivo cu105;
    private final CU106GenerarEstadoFinanciero cu106;
    private final CU106DescargarEstadoFinanciero descarga;
    private final SesionDeLaPeticion sesion;

    public ErpController(
            CU100AbrirCerrarPeriodo cu100,
            CU101Presupuestar cu101,
            CU102AltaDeTercero cu102,
            CU103FacturaDeProveedor cu103,
            CU104CobrarCuenta cu104,
            CU105DepreciarActivo cu105,
            CU106GenerarEstadoFinanciero cu106,
            CU106DescargarEstadoFinanciero descarga,
            ConsultarListadosDeErp listados,
            SesionDeLaPeticion sesion) {
        super(listados, sesion);
        this.cu100 = cu100;
        this.cu101 = cu101;
        this.cu102 = cu102;
        this.cu103 = cu103;
        this.cu104 = cu104;
        this.cu105 = cu105;
        this.cu106 = cu106;
        this.descarga = descarga;
        this.sesion = sesion;
    }

    // ------------------------------------------------------------------ CU-100 --

    @Override
    @Permiso("CONTABILIDAD_ERP_CERRAR")
    public ResponseEntity<SalidaEjercicio> abrirEjercicioFiscal(UUID idempotencyKey, EntradaEjercicio cuerpo) {
        Traza.marcarCasoDeUso("CU-100", String.valueOf(cuerpo.getAnio()));

        var salida = cu100.abrirEjercicio(cuerpo.getAnio(), sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.ejercicioAbierto(salida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_CERRAR")
    public ResponseEntity<SalidaCierre> cerrarPeriodoContable(
            UUID periodoId, UUID idempotencyKey, EntradaCierre cuerpo) {
        Traza.marcarCasoDeUso("CU-100", periodoId.toString());

        var salida = cu100.cerrarPeriodo(
                new CU100AbrirCerrarPeriodo.EntradaCierre(periodoId, cuerpo.getGlosa()), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.cierre(salida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_REPORTES")
    @PermisoAlternativo({CERRAR, PRESUPUESTO, COMPRAS, CUENTAS_POR_PAGAR, PAGAR, COBRAR, ACTIVOS_FIJOS})
    public ResponseEntity<PeriodosDelEjercicio> listarPeriodosDelEjercicio(UUID ejercicioId) {
        Traza.marcarCasoDeUso("CU-100", ejercicioId.toString());
        return ResponseEntity.ok(
                MapeoDeErp.periodosDelEjercicio(cu100.periodosDelEjercicio(ejercicioId, sesion.actual())));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_REPORTES")
    @PermisoAlternativo({CERRAR, PRESUPUESTO, COMPRAS, CUENTAS_POR_PAGAR, PAGAR, COBRAR, ACTIVOS_FIJOS})
    public ResponseEntity<PaginaDeEjercicios> listarEjerciciosFiscales(Integer limite, Integer antesDeAnio) {
        Traza.marcarCasoDeUso("CU-100", "ejercicios");
        return ResponseEntity.ok(
                MapeoDeErp.ejercicios(cu100.ejercicios(Optional.ofNullable(antesDeAnio), limite, sesion.actual())));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_REPORTES")
    @PermisoAlternativo({CERRAR, PRESUPUESTO, COMPRAS, CUENTAS_POR_PAGAR, PAGAR, COBRAR, ACTIVOS_FIJOS})
    public ResponseEntity<PeriodoContable> consultarPeriodoVigente() {
        Traza.marcarCasoDeUso("CU-100", "vigente");
        return ResponseEntity.ok(MapeoDeErp.periodo(cu100.vigente(sesion.actual())));
    }

    // ------------------------------------------------------------------ CU-101 --

    @Override
    @Permiso("CONTABILIDAD_ERP_PRESUPUESTO")
    public ResponseEntity<SalidaPresupuesto> crearPresupuesto(UUID idempotencyKey, EntradaPresupuesto cuerpo) {
        Traza.marcarCasoDeUso("CU-101", cuerpo.getCentroCostoId().toString());

        var salida = cu101.crear(MapeoDeErp.presupuesto(cuerpo), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.presupuesto(salida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_PRESUPUESTO")
    public ResponseEntity<SalidaPresupuesto> aprobarPresupuesto(UUID presupuestoId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-101", presupuestoId.toString());
        return ResponseEntity.ok(MapeoDeErp.presupuesto(cu101.aprobar(presupuestoId, sesion.actual())));
    }

    // ------------------------------------------------------------------ CU-102 --

    @Override
    @Permiso("CONTABILIDAD_ERP_COMPRAS")
    public ResponseEntity<SalidaTercero> darDeAltaTercero(UUID idempotencyKey, EntradaTercero cuerpo) {
        Traza.marcarCasoDeUso("CU-102", "alta-tercero"); // nunca el documento: regla 90.2.1

        UUID id = cu102.darDeAlta(MapeoDeErp.tercero(cuerpo), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(new SalidaTercero().terceroComercialId(id));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_COMPRAS")
    public ResponseEntity<SalidaOrden> crearOrdenDeCompra(UUID idempotencyKey, EntradaOrden cuerpo) {
        Traza.marcarCasoDeUso("CU-102", cuerpo.getNumero());

        var salida = cu102.crearOrden(MapeoDeErp.orden(cuerpo), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.orden(salida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_COMPRAS")
    public ResponseEntity<SalidaOrden> aprobarOrdenDeCompra(UUID ordenId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-102", ordenId.toString());
        return ResponseEntity.ok(MapeoDeErp.orden(cu102.aprobarOrden(ordenId, sesion.actual())));
    }

    // ------------------------------------------------------------------ CU-103 --

    @Override
    @Permiso("CONTABILIDAD_ERP_CUENTAS_POR_PAGAR")
    public ResponseEntity<SalidaFactura> registrarFacturaDeProveedor(UUID idempotencyKey, EntradaFactura cuerpo) {
        Traza.marcarCasoDeUso("CU-103", cuerpo.getNumeroFactura());

        var salida = cu103.registrar(MapeoDeErp.factura(cuerpo), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.factura(salida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_PAGAR")
    public ResponseEntity<SalidaFactura> pagarFacturaDeProveedor(
            UUID facturaId, UUID idempotencyKey, EntradaPago cuerpo) {
        Traza.marcarCasoDeUso("CU-103", facturaId.toString());

        var salida = cu103.pagar(MapeoDeErp.pago(facturaId, idempotencyKey, cuerpo), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.factura(salida));
    }

    // ------------------------------------------------------------------ CU-104 --

    @Override
    @Permiso("CONTABILIDAD_ERP_COBRAR")
    public ResponseEntity<SalidaCuentaPorCobrar> abrirCuentaPorCobrar(
            UUID idempotencyKey, EntradaCuentaPorCobrar cuerpo) {
        Traza.marcarCasoDeUso("CU-104", cuerpo.getOrigenTipo());

        UUID id = cu104.abrir(MapeoDeErp.cuentaPorCobrar(cuerpo), sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(new SalidaCuentaPorCobrar().cuentaPorCobrarId(id));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_COBRAR")
    public ResponseEntity<SalidaCobro> registrarCobro(UUID cuentaId, UUID idempotencyKey, EntradaCobro cuerpo) {
        Traza.marcarCasoDeUso("CU-104", cuentaId.toString());

        var salida = cu104.cobrar(
                new CU104CobrarCuenta.EntradaCobro(
                        cuentaId,
                        new BigDecimal(cuerpo.getMonto()),
                        cuerpo.getFormaCobro().getValue()),
                sesion.actual());

        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.cobro(salida));
    }

    // ------------------------------------------------------------------ CU-105 --

    @Override
    @Permiso("CONTABILIDAD_ERP_ACTIVOS_FIJOS")
    public ResponseEntity<SalidaDepreciacion> depreciarActivo(
            UUID activoId, UUID idempotencyKey, EntradaDepreciacion cuerpo) {
        Traza.marcarCasoDeUso("CU-105", activoId.toString());

        var corrida = cu105.depreciar(activoId, cuerpo.getPeriodoContableId(), sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.depreciacion(corrida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_ACTIVOS_FIJOS")
    public ResponseEntity<SalidaCorrida> correrDepreciacionDelPeriodo(UUID periodoId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-105", periodoId.toString());

        return ResponseEntity.ok(MapeoDeErp.corrida(cu105.correr(periodoId, sesion.actual())));
    }

    // ------------------------------------------------------------------ CU-106 --

    @Override
    @Permiso("CONTABILIDAD_ERP_REPORTES")
    public ResponseEntity<SalidaEstadoFinanciero> generarEstadoFinanciero(
            UUID periodoId, UUID idempotencyKey, EntradaEstadoFinanciero cuerpo) {
        Traza.marcarCasoDeUso("CU-106", cuerpo.getTipo().getValue());

        var salida = cu106.generar(periodoId, cuerpo.getTipo().getValue(), sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeErp.estadoFinanciero(salida));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_REPORTES")
    public ResponseEntity<Resource> descargarEstadoFinanciero(UUID estadoFinancieroId) {
        Traza.marcarCasoDeUso("CU-106", estadoFinancieroId.toString());
        return MapeoDeErp.documento(descarga.descargar(estadoFinancieroId, sesion.actual()));
    }

    @Override
    @Permiso("CONTABILIDAD_ERP_REPORTES")
    public ResponseEntity<SalidaPlantilla> validarPlantillaDeAsiento(UUID plantillaId) {
        Traza.marcarCasoDeUso("CU-106", plantillaId.toString());

        return ResponseEntity.ok(MapeoDeErp.plantilla(cu106.validarPlantilla(plantillaId, sesion.actual())));
    }
}
