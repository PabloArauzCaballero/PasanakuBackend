package bo.aportaya.entregas.web;

import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso;
import bo.aportaya.entregas.aplicacion.ConsultarDesembolsos;
import bo.aportaya.entregas.web.generado.DesembolsosApi;
import bo.aportaya.entregas.web.generado.modelo.DesembolsoEnLista;
import bo.aportaya.entregas.web.generado.modelo.EntradaOrdenDesembolso;
import bo.aportaya.entregas.web.generado.modelo.EntradaRespuestaDesembolso;
import bo.aportaya.entregas.web.generado.modelo.PaginaDeDesembolsos;
import bo.aportaya.entregas.web.generado.modelo.SalidaIntentoDesembolso;
import bo.aportaya.entregas.web.generado.modelo.SalidaOrdenDesembolso;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /desembolsos}: la orden y la respuesta del proveedor.
 *
 * <p>La respuesta se anota como un intento mas y nunca sobreescribe al anterior: lo que
 * un proveedor contesto —y cuando— es lo unico que permite reclamarle despues.
 */
@RestController
public class DesembolsosController implements DesembolsosApi {

    private final CU28EmitirDesembolso cu28;
    private final ConsultarDesembolsos consultas;
    private final SesionDeLaPeticion sesion;

    public DesembolsosController(CU28EmitirDesembolso cu28, ConsultarDesembolsos consultas, SesionDeLaPeticion sesion) {
        this.cu28 = cu28;
        this.consultas = consultas;
        this.sesion = sesion;
    }

    // Tesoreria la opera (ENTREGA_EJECUTAR) y Auditoria la lee: ninguno de los dos mueve plata desde aca.
    @Override
    @Permiso("ENTREGA_EJECUTAR")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeDesembolsos> listarDesembolsos(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-28", "bandeja");
        var pagina = consultas.listar(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeDesembolsos();
        salida.setItems(pagina.items().stream()
                .map(d -> {
                    var r = new DesembolsoEnLista();
                    r.setOrdenId(d.ordenId());
                    r.setEntregaId(d.entregaId());
                    r.setProveedorId(d.proveedorId());
                    r.setMonto(MapeoDeEntregas.dinero(d.monto()));
                    r.setEstado(DesembolsoEnLista.EstadoEnum.fromValue(d.estado()));
                    r.setIntentos(d.intentos());
                    r.setUltimoResultado(d.ultimoResultado()
                            .map(DesembolsoEnLista.UltimoResultadoEnum::fromValue)
                            .orElse(null));
                    r.setUltimoCodigoError(d.ultimoCodigoError().orElse(null));
                    r.setReintentableEn(d.reintentableEn().orElse(null));
                    r.setCreadaEn(d.creadaEn());
                    r.setAcreditadaEn(d.acreditadaEn().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaOrdenDesembolso> emitirOrdenDesembolso(
            UUID idempotencyKey, EntradaOrdenDesembolso cuerpo) {
        Traza.marcarCasoDeUso("CU-28", cuerpo.getEntregaId().toString());

        var salida = cu28.emitir(
                new CU28EmitirDesembolso.EntradaOrden(
                        cuerpo.getEntregaId(),
                        cuerpo.getProveedorId(),
                        cuerpo.getCuentaDestinoId(),
                        cuerpo.getGlosa(),
                        idempotencyKey.toString(),
                        Boolean.TRUE.equals(cuerpo.getSaldoRetenido())),
                sesion.actual());

        var respuesta = new SalidaOrdenDesembolso();
        respuesta.setOrdenId(salida.ordenId());
        respuesta.setEstado(SalidaOrdenDesembolso.EstadoEnum.fromValue(salida.estado()));
        respuesta.setClaveIdempotencia(salida.claveIdempotencia());
        respuesta.setIntentos(salida.intentos());
        respuesta.setEsNueva(salida.esNueva());

        var estado = salida.esNueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(respuesta);
    }

    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaIntentoDesembolso> anotarRespuestaDesembolso(
            UUID ordenId, EntradaRespuestaDesembolso cuerpo) {
        Traza.marcarCasoDeUso("CU-28", ordenId.toString());

        var salida = cu28.anotarRespuesta(
                new CU28EmitirDesembolso.EntradaRespuesta(
                        ordenId,
                        cuerpo.getIniciado(),
                        Boolean.TRUE.equals(cuerpo.getExitoso()),
                        cuerpo.getReferenciaProveedor(),
                        cuerpo.getCodigoError(),
                        cuerpo.getMensajeProveedor()),
                sesion.actual());

        var respuesta = new SalidaIntentoDesembolso();
        respuesta.setOrdenId(salida.ordenId());
        respuesta.setEstado(salida.estado());
        respuesta.setNumeroIntento(salida.numeroIntento());
        respuesta.setReintentableEn(salida.reintentableEn());
        respuesta.setEsNuevo(salida.esNuevo());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
