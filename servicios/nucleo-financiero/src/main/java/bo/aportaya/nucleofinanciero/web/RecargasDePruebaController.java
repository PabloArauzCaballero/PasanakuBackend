package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.PagoDeRecargaDePrueba;
import bo.aportaya.nucleofinanciero.web.generado.RecargasDePruebaApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaAcreditacion;
import bo.aportaya.plataforma.web.errores.ErrorApi;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * «Simular pago» del QR de prueba de una recarga: solo existe en ambientes de prueba.
 *
 * <p>Sin el proveedor de prueba la operacion no existe, y se ve igual que cualquier ruta inexistente: el mismo
 * 404 y el mismo cuerpo que arma {@code ManejadorGlobalDeErrores#rutaInexistente}. Se responde aca y no lanzando
 * {@code NoResourceFoundException}: es chequeada, la interfaz generada no la declara, y el proxy de
 * {@code @Permiso} la convertia en un 500 ({@code UndeclaredThrowableException}).
 */
@RestController
public class RecargasDePruebaController implements RecargasDePruebaApi {

    private final PagoDeRecargaDePrueba pagoDePrueba;
    private final SesionDeLaPeticion sesion;

    public RecargasDePruebaController(PagoDeRecargaDePrueba pagoDePrueba, SesionDeLaPeticion sesion) {
        this.pagoDePrueba = pagoDePrueba;
        this.sesion = sesion;
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResponseEntity<SalidaAcreditacion> pagarRecargaDePrueba(UUID ordenId, UUID idempotencyKey) {
        if (!pagoDePrueba.disponible()) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.de("AP-VAL-05", "Esa ruta no existe.", Traza.actual()));
        }
        Traza.marcarCasoDeUso("CU-10", ordenId.toString());
        var salida = pagoDePrueba.pagar(ordenId, sesion.actual());
        var respuesta = new SalidaAcreditacion();
        respuesta.setOrdenRecargaId(salida.ordenRecargaId());
        respuesta.setTransaccionId(salida.transaccionId());
        respuesta.setSaldoDespues(MapeoDeBilletera.dinero(salida.saldoDespues()));
        return ResponseEntity.ok(respuesta);
    }
}
