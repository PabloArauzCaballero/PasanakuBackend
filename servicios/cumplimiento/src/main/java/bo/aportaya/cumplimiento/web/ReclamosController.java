package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo;
import bo.aportaya.cumplimiento.web.generado.ReclamosApi;
import bo.aportaya.cumplimiento.web.generado.modelo.CasoDeReclamo;
import bo.aportaya.cumplimiento.web.generado.modelo.EfectoDeReclamos;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaRespuestaReclamo;
import bo.aportaya.cumplimiento.web.generado.modelo.FichaDeReclamo;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeReclamos;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaRespuestaReclamo;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de {@code /reclamos}: si un reclamo frena una sancion firme o una deuda ejecutable
 * (H13.S1.M6, debido proceso), y la bandeja, la ficha y la respuesta del Punto de Reclamo (CU-52,
 * backoffice carril A · H3).
 *
 * <p>Traduce y delega: la regla vive en {@code EfectoDelReclamo} y la lectura en
 * {@code ConsultarEfectoDelReclamo}. Lo unico que decide esta clase es quien puede preguntar:
 * {@code RECLAMO_VER}, que no tiene un participante ni el token de un servicio que no sea de
 * reclamos. La negativa es el mismo 403 de siempre, exista o no la persona por la que se pregunta.
 */
@RestController
public class ReclamosController implements ReclamosApi {

    private final ConsultarEfectoDelReclamo efecto;
    private final ConsultarBandejaDeReclamos bandeja;
    private final CU52AtenderReclamo cu52;
    private final SesionDeLaPeticion sesion;

    public ReclamosController(
            ConsultarEfectoDelReclamo efecto,
            ConsultarBandejaDeReclamos bandeja,
            CU52AtenderReclamo cu52,
            SesionDeLaPeticion sesion) {
        this.efecto = efecto;
        this.bandeja = bandeja;
        this.cu52 = cu52;
        this.sesion = sesion;
    }

    // La cola del Punto de Reclamo: sin texto ni persona, por eso alcanza RECLAMO_VER.
    @Override
    @Permiso("RECLAMO_VER")
    public ResponseEntity<PaginaDeReclamos> listarBandejaDeReclamos(
            List<String> estados, Integer limite, OffsetDateTime despuesDeLimite, UUID despuesDeId) {
        Traza.marcarCasoDeUso("CU-52", "bandeja");
        return ResponseEntity.ok(MapeoDeReclamos.pagina(bandeja.bandeja(
                estados,
                Optional.ofNullable(despuesDeLimite),
                Optional.ofNullable(despuesDeId),
                limite,
                sesion.actual())));
    }

    // El texto del reclamo lo escribio una persona: lo lee solo quien lo atiende.
    @Override
    @Permiso("RECLAMO_ATENDER")
    public ResponseEntity<FichaDeReclamo> consultarReclamo(UUID reclamoId) {
        Traza.marcarCasoDeUso("CU-52", reclamoId.toString());
        return ResponseEntity.ok(MapeoDeReclamos.ficha(bandeja.ficha(reclamoId, sesion.actual())));
    }

    @Override
    @Permiso("RECLAMO_ATENDER")
    public ResponseEntity<SalidaRespuestaReclamo> responderReclamo(
            UUID reclamoId, UUID idempotencyKey, EntradaRespuestaReclamo cuerpo) {
        Traza.marcarCasoDeUso("CU-52", reclamoId.toString());
        String resultado = cuerpo.getResultado().getValue();
        var salida = cu52.responder(
                new CU52AtenderReclamo.EntradaRespuesta(
                        reclamoId, resultado, cuerpo.getRespuesta(), cuerpo.getDevolucionId()),
                sesion.actual());

        var respuesta = new SalidaRespuestaReclamo();
        respuesta.setReclamoId(salida.reclamoId());
        respuesta.setEstado(SalidaRespuestaReclamo.EstadoEnum.fromValue(salida.estado()));
        respuesta.setResultado(SalidaRespuestaReclamo.ResultadoEnum.fromValue(resultado));
        respuesta.setLimiteVigente(salida.plazoRespuesta());
        return ResponseEntity.ok(respuesta);
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
