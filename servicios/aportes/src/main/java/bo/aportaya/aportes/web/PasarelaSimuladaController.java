package bo.aportaya.aportes.web;

import bo.aportaya.aportes.aplicacion.CU100EmitirAvisoDePagoSimulado;
import bo.aportaya.aportes.web.generado.QrApi;
import bo.aportaya.aportes.web.generado.modelo.EntradaAvisoSimulado;
import bo.aportaya.aportes.web.generado.modelo.SalidaAvisoSimulado;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dispara el aviso de pago de la pasarela SIMULADA. Solo existe en {@code local}/{@code test} con la pasarela simulada:
 * en cualquier otro entorno el bean no se crea y la ruta responde 404 (fail-closed). Exige sesion y
 * {@code ADMIN_PLATAFORMA}; la logica y la auditoria viven en {@link CU100EmitirAvisoDePagoSimulado}.
 */
@RestController
@Profile({"local", "test"})
@ConditionalOnProperty(name = "aportaya.aportes.pasarela.modo", havingValue = "simulada", matchIfMissing = true)
public class PasarelaSimuladaController implements QrApi {

    private final CU100EmitirAvisoDePagoSimulado avisos;
    private final SesionDeLaPeticion sesion;

    public PasarelaSimuladaController(CU100EmitirAvisoDePagoSimulado avisos, SesionDeLaPeticion sesion) {
        this.avisos = avisos;
        this.sesion = sesion;
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaAvisoSimulado> emitirAvisoDePagoSimulado(EntradaAvisoSimulado cuerpo) {
        Traza.marcarCasoDeUso("CU-100", cuerpo.getReferenciaProveedor());
        var resultado = avisos.emitir(cuerpo.getReferenciaProveedor(), sesion.actual());
        var respuesta = new SalidaAvisoSimulado(
                SalidaAvisoSimulado.EstadoEnum.fromValue(resultado.estado().name()), resultado.webhookId());
        respuesta.setPagoId(resultado.pagoId());
        respuesta.setMotivo(resultado.motivo());
        return ResponseEntity.ok(respuesta);
    }
}
