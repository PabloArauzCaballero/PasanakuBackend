package bo.aportaya.entregas.web;

import bo.aportaya.entregas.aplicacion.ComprarOfertaDeTurno;
import bo.aportaya.entregas.aplicacion.GestionDeOfertas;
import bo.aportaya.entregas.web.generado.modelo.EntradaOferta;
import bo.aportaya.entregas.web.generado.modelo.PaginaDeOfertas;
import bo.aportaya.entregas.web.generado.modelo.SalidaCancelacionDeOferta;
import bo.aportaya.entregas.web.generado.modelo.SalidaCompra;
import bo.aportaya.entregas.web.generado.modelo.SalidaOferta;
import bo.aportaya.entregas.web.generado.modelo.VistaDeOferta;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * La traduccion web de {@code /entregas/mercado}: el derecho a cobrar un turno.
 *
 * <p>El generador agrupa las rutas por su primer segmento, asi que estas operaciones son de
 * {@code EntregasApi} y las expone {@link EntregasController}, que es quien declara el permiso
 * de cada una. Aca solo se traduce y se delega: ninguna regla vive en esta clase.
 */
@Component
public class MercadoWeb {

    private final GestionDeOfertas ofertas;
    private final ComprarOfertaDeTurno compra;
    private final SesionDeLaPeticion sesion;

    public MercadoWeb(GestionDeOfertas ofertas, ComprarOfertaDeTurno compra, SesionDeLaPeticion sesion) {
        this.ofertas = ofertas;
        this.compra = compra;
        this.sesion = sesion;
    }

    public ResponseEntity<SalidaOferta> publicarOferta(UUID idempotencyKey, EntradaOferta cuerpo) {
        Traza.marcarCasoDeUso("MERCADO", cuerpo.getTurnoId().toString());

        var salida = ofertas.publicar(
                new GestionDeOfertas.Entrada(
                        cuerpo.getTurnoId(),
                        MapeoDeEntregas.dinero(cuerpo.getPrecio()),
                        MapeoDeEntregas.dinero(cuerpo.getCargos()),
                        cuerpo.getVigenteHasta(),
                        ClaveIdempotencia.deHecho("oferta", idempotencyKey).valor()),
                sesion.actual());

        var respuesta = new SalidaOferta();
        respuesta.setOfertaId(salida.ofertaId());
        respuesta.setEsNueva(salida.esNueva());
        return ResponseEntity.status(salida.esNueva() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(respuesta);
    }

    public ResponseEntity<PaginaDeOfertas> listarOfertas(Integer limite, Integer pagina) {
        int tamano = limite == null ? 20 : limite;
        int numero = pagina == null ? 0 : pagina;
        var vistas = ofertas.listar(sesion.actual(), tamano, numero);

        var respuesta = new PaginaDeOfertas();
        respuesta.setPagina(numero);
        respuesta.setOfertas(vistas.stream()
                .map(v -> {
                    var o = new VistaDeOferta();
                    o.setOfertaId(v.ofertaId());
                    o.setGrupoId(v.grupoId());
                    o.setTurnoId(v.turnoId());
                    o.setDerecho(MapeoDeEntregas.dinero(v.derecho()));
                    o.setPrecio(MapeoDeEntregas.dinero(v.precio()));
                    o.setCargos(MapeoDeEntregas.dinero(v.cargos()));
                    o.setVigenteHasta(v.vigenteHasta());
                    return o;
                })
                .toList());
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<SalidaCancelacionDeOferta> cancelarOferta(UUID ofertaId) {
        Traza.marcarCasoDeUso("MERCADO", ofertaId.toString());

        var respuesta = new SalidaCancelacionDeOferta();
        respuesta.setOfertaId(ofertaId);
        respuesta.setCancelada(ofertas.cancelar(ofertaId, sesion.actual()));
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<SalidaCompra> comprarOferta(UUID ofertaId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("MERCADO", ofertaId.toString());

        var salida = compra.comprar(
                ofertaId, ClaveIdempotencia.deHecho("compra", idempotencyKey).valor(), sesion.actual());

        var respuesta = new SalidaCompra();
        respuesta.setCesionId(salida.cesionId());
        respuesta.setEstado(SalidaCompra.EstadoEnum.fromValue(salida.estado()));
        respuesta.setLiquidada(salida.liquidada());
        // 202: la compra sigue en curso. Se reanuda repitiendo el pedido con la misma clave.
        return ResponseEntity.status(salida.liquidada() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(respuesta);
    }
}
