package bo.aportaya.inversiones.web;

import bo.aportaya.inversiones.aplicacion.CU120Catalogo;
import bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion;
import bo.aportaya.inversiones.aplicacion.CU122ConfirmarPosicion;
import bo.aportaya.inversiones.aplicacion.CU123Devengo;
import bo.aportaya.inversiones.aplicacion.CU123Posiciones;
import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate;
import bo.aportaya.inversiones.aplicacion.CU125LiquidarRescate;
import bo.aportaya.inversiones.web.generado.InversionesApi;
import bo.aportaya.inversiones.web.generado.modelo.EntradaDevengo;
import bo.aportaya.inversiones.web.generado.modelo.EntradaOrden;
import bo.aportaya.inversiones.web.generado.modelo.EntradaRescate;
import bo.aportaya.inversiones.web.generado.modelo.ListaDeComprobantes;
import bo.aportaya.inversiones.web.generado.modelo.ListaDePosiciones;
import bo.aportaya.inversiones.web.generado.modelo.ListaDeProductos;
import bo.aportaya.inversiones.web.generado.modelo.SalidaDevengo;
import bo.aportaya.inversiones.web.generado.modelo.SalidaOrden;
import bo.aportaya.inversiones.web.generado.modelo.SalidaPosicion;
import bo.aportaya.inversiones.web.generado.modelo.SalidaRescate;
import bo.aportaya.inversiones.web.generado.modelo.SalidaSincronizacion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /inversiones}: traducen y delegan, sin logica.
 *
 * <p>Permisos: operar y ver usan los del catalogo de la billetera ({@code BILLETERA_OPERAR},
 * {@code BILLETERA_VER}) porque invertir es mover saldo del titular; el catalogo, los valores
 * de cuota y el devengo son tareas de plataforma. Un codigo propio ({@code INVERSION_OPERAR})
 * requiere sembrarlo en el catalogo de roles: queda como {@code DR-INV-07}.
 *
 * <p>La identidad sale de la sesion verificada; ningun id de titular viaja en el cuerpo.
 */
@RestController
public class InversionesController implements InversionesApi {

    private final CU120Catalogo cu120;
    private final CU121OrdenarInversion cu121;
    private final CU122ConfirmarPosicion cu122;
    private final CU123Posiciones cu123;
    private final CU123Devengo cu123Devengo;
    private final CU124SolicitarRescate cu124;
    private final CU125LiquidarRescate cu125;
    private final SesionDeLaPeticion sesion;

    public InversionesController(
            CU120Catalogo cu120,
            CU121OrdenarInversion cu121,
            CU122ConfirmarPosicion cu122,
            CU123Posiciones cu123,
            CU123Devengo cu123Devengo,
            CU124SolicitarRescate cu124,
            CU125LiquidarRescate cu125,
            SesionDeLaPeticion sesion) {
        this.cu120 = cu120;
        this.cu121 = cu121;
        this.cu122 = cu122;
        this.cu123 = cu123;
        this.cu123Devengo = cu123Devengo;
        this.cu124 = cu124;
        this.cu125 = cu125;
        this.sesion = sesion;
    }

    // ------------------------------------------------------------------ CU-120 --
    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<ListaDeProductos> listarProductosDeInversion() {
        Traza.marcarCasoDeUso("CU-120", "catalogo");
        var lista = new ListaDeProductos();
        cu120.listar(sesion.actual()).forEach(v -> lista.addProductosItem(MapeoDeInversiones.producto(v)));
        return ResponseEntity.ok(lista);
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaSincronizacion> sincronizarCatalogoDeInversion(UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-120", "sincronizacion");
        return ResponseEntity.ok(sincronizacion(
                cu120.sincronizar(sesion.actual()),
                cu120.listar(sesion.actual()).size()));
    }

    // ------------------------------------------------------------------ CU-121 --
    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaOrden> crearOrdenDeInversion(UUID idempotencyKey, EntradaOrden cuerpo) {
        Traza.marcarCasoDeUso("CU-121", cuerpo.getProductoCodigo());
        var monto = Dinero.de(
                cuerpo.getMonto().getMonto(),
                Moneda.valueOf(cuerpo.getMonto().getMoneda().getValue()));
        var vista = cu121.ordenar(
                new CU121OrdenarInversion.EntradaOrden(
                        cuerpo.getProductoCodigo(),
                        cuerpo.getVersionCondicionesId(),
                        cuerpo.getTextoHash(),
                        cuerpo.getCuentaBilleteraId(),
                        monto,
                        idempotencyKey.toString()),
                sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeInversiones.orden(vista));
    }

    // ------------------------------------------------------------------ CU-122 --
    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<SalidaOrden> verOrdenDeInversion(UUID ordenId) {
        Traza.marcarCasoDeUso("CU-122", ordenId.toString());
        return ResponseEntity.ok(MapeoDeInversiones.orden(cu122.ver(ordenId, sesion.actual())));
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaOrden> sincronizarOrdenDeInversion(UUID ordenId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-122", ordenId.toString());
        return ResponseEntity.ok(MapeoDeInversiones.orden(cu122.sincronizar(ordenId, sesion.actual())));
    }

    // ------------------------------------------------------------------ CU-123 --
    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<ListaDePosiciones> listarPosicionesDeInversion() {
        Traza.marcarCasoDeUso("CU-123", "posiciones");
        var lista = new ListaDePosiciones();
        cu123.listar(sesion.actual()).forEach(v -> lista.addPosicionesItem(MapeoDeInversiones.posicion(v)));
        return ResponseEntity.ok(lista);
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<SalidaPosicion> verPosicionDeInversion(UUID posicionId) {
        Traza.marcarCasoDeUso("CU-123", posicionId.toString());
        return ResponseEntity.ok(MapeoDeInversiones.posicion(cu123.ver(posicionId, sesion.actual())));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaSincronizacion> sincronizarValoresDeCuota(UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-123", "valores-cuota");
        return ResponseEntity.ok(sincronizacion(cu123.sincronizarValores(sesion.actual()), 0));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaDevengo> correrDevengoDeDpf(UUID idempotencyKey, EntradaDevengo cuerpo) {
        Traza.marcarCasoDeUso("CU-123", cuerpo.getFecha().toString());
        var r = cu123Devengo.devengar(cuerpo.getFecha(), sesion.actual());
        var s = new SalidaDevengo();
        s.setFecha(r.fecha());
        s.setDevengadas(r.devengadas());
        s.setYaDevengadas(r.yaDevengadas());
        s.setTotal(MapeoDeInversiones.bob(r.total()));
        return ResponseEntity.ok(s);
    }

    // ------------------------------------------------------------------ CU-124 / 125 --
    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaRescate> solicitarRescateDeInversion(
            UUID posicionId, UUID idempotencyKey, EntradaRescate cuerpo) {
        Traza.marcarCasoDeUso("CU-124", posicionId.toString());
        Optional<BigDecimal> cuotas = Optional.ofNullable(cuerpo.getCuotas()).map(BigDecimal::new);
        var vista = cu124.solicitar(
                new CU124SolicitarRescate.EntradaRescate(posicionId, cuotas, idempotencyKey.toString()),
                sesion.actual());
        return ResponseEntity.status(HttpStatus.CREATED).body(MapeoDeInversiones.rescate(vista));
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaRescate> sincronizarRescateDeInversion(UUID rescateId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-125", rescateId.toString());
        return ResponseEntity.ok(MapeoDeInversiones.rescate(cu125.sincronizar(rescateId, sesion.actual())));
    }

    // ------------------------------------------------------------------ CU-128 --
    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<ListaDeComprobantes> listarComprobantesDeInversion(UUID posicionId) {
        Traza.marcarCasoDeUso("CU-128", posicionId.toString());
        var lista = new ListaDeComprobantes();
        cu123.comprobantes(posicionId, sesion.actual())
                .forEach(c -> lista.addComprobantesItem(MapeoDeInversiones.comprobante(c, "BOB")));
        return ResponseEntity.ok(lista);
    }

    private static SalidaSincronizacion sincronizacion(int nuevos, int productos) {
        var s = new SalidaSincronizacion();
        s.setNuevos(nuevos);
        s.setProductos(productos);
        return s;
    }
}
