package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo;
import bo.aportaya.cumplimiento.web.generado.ReclamosApi;
import bo.aportaya.cumplimiento.web.generado.modelo.CasoDeReclamo;
import bo.aportaya.cumplimiento.web.generado.modelo.EfectoDeReclamos;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de {@code /reclamos}: si un reclamo frena una sancion firme o una deuda ejecutable
 * (H13.S1.M6, debido proceso).
 *
 * <p>Traduce y delega: la regla vive en {@code EfectoDelReclamo} y la lectura en
 * {@code ConsultarEfectoDelReclamo}. Lo unico que decide esta clase es quien puede preguntar:
 * {@code RECLAMO_VER}, que no tiene un participante ni el token de un servicio que no sea de
 * reclamos. La negativa es el mismo 403 de siempre, exista o no la persona por la que se pregunta.
 */
@RestController
public class ReclamosController implements ReclamosApi {

    private final ConsultarEfectoDelReclamo efecto;
    private final SesionDeLaPeticion sesion;

    public ReclamosController(ConsultarEfectoDelReclamo efecto, SesionDeLaPeticion sesion) {
        this.efecto = efecto;
        this.sesion = sesion;
    }

    @Override
    @Permiso("RECLAMO_VER")
    public ResponseEntity<EfectoDeReclamos> consultarEfectoDeReclamos(UUID usuarioId, String producto) {
        Traza.marcarCasoDeUso("CU-52", "efecto");

        var resultado = efecto.delUsuario(usuarioId, Optional.ofNullable(producto), sesion.actual());

        var salida = new EfectoDeReclamos();
        salida.setUsuarioId(resultado.usuarioId());
        salida.setBloqueaSancionFirme(resultado.bloqueaSancionFirme());
        salida.setBloqueaDeudaEjecutable(resultado.bloqueaDeudaEjecutable());
        salida.setMotivoGlobal(
                resultado.motivoGlobal() == null
                        ? null
                        : resultado.motivoGlobal().name());
        for (var caso : resultado.casos()) {
            var c = new CasoDeReclamo();
            c.setReclamoId(caso.reclamoId());
            c.setCodigo(caso.codigo());
            c.setCategoria(caso.categoria());
            c.setProducto(caso.producto());
            c.setEstado(caso.estado());
            c.setResultado(caso.resultado());
            c.setFechaIngreso(caso.fechaIngreso());
            c.setFechaRespuesta(caso.fechaRespuesta());
            c.setBloqueaSancionFirme(caso.efecto().bloqueaSancionFirme());
            c.setBloqueaDeudaEjecutable(caso.efecto().bloqueaDeudaEjecutable());
            c.setMotivo(
                    CasoDeReclamo.MotivoEnum.fromValue(caso.efecto().motivo().name()));
            salida.addCasosItem(c);
        }
        return ResponseEntity.ok(salida);
    }
}
