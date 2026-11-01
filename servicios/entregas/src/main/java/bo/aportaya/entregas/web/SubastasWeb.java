package bo.aportaya.entregas.web;

import bo.aportaya.entregas.aplicacion.ConsultasDeSubasta;
import bo.aportaya.entregas.aplicacion.SubastaDelCupo;
import bo.aportaya.entregas.web.generado.modelo.EntradaPuja;
import bo.aportaya.entregas.web.generado.modelo.EntradaSubasta;
import bo.aportaya.entregas.web.generado.modelo.MiPuja;
import bo.aportaya.entregas.web.generado.modelo.OfertaParaPago;
import bo.aportaya.entregas.web.generado.modelo.PaginaDeSubastas;
import bo.aportaya.entregas.web.generado.modelo.SalidaPuja;
import bo.aportaya.entregas.web.generado.modelo.VistaDeSubasta;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * La traduccion web de {@code /entregas/subastas} (CU-64 por subasta). Como {@link MercadoWeb}: las rutas son de
 * {@code EntregasApi} y las expone {@link EntregasController}, que declara el permiso. Aca solo se traduce.
 */
@Component
public class SubastasWeb {

    private final SubastaDelCupo subasta;
    private final ConsultasDeSubasta consultas;
    private final SesionDeLaPeticion sesion;

    public SubastasWeb(SubastaDelCupo subasta, ConsultasDeSubasta consultas, SesionDeLaPeticion sesion) {
        this.subasta = subasta;
        this.consultas = consultas;
        this.sesion = sesion;
    }

    public ResponseEntity<VistaDeSubasta> abrir(UUID idempotencyKey, EntradaSubasta cuerpo) {
        Traza.marcarCasoDeUso("CU-64", cuerpo.getCupoId().toString());
        var ctx = sesion.actual();
        var abierta = subasta.abrir(
                new SubastaDelCupo.Apertura(
                        cuerpo.getGrupoId(),
                        cuerpo.getCupoId(),
                        MapeoDeEntregas.dinero(cuerpo.getMontoBase()),
                        cuerpo.getHoras().getValue(),
                        ClaveIdempotencia.deHecho("subasta", idempotencyKey).valor()),
                ctx);
        var vista = consultas.ver(abierta.subastaId(), ctx).orElseThrow();
        return ResponseEntity.status(abierta.esNueva() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(vista(vista));
    }

    public ResponseEntity<PaginaDeSubastas> listar(UUID cupoId, Integer limite, Integer pagina) {
        int tamano = limite == null ? 20 : limite;
        int numero = pagina == null ? 0 : pagina;
        var ctx = sesion.actual();
        List<ConsultasDeSubasta.Vista> vistas = cupoId != null
                ? consultas.delCupo(cupoId, ctx).map(List::of).orElse(List.of())
                : consultas.abiertas(ctx, tamano, numero);
        var respuesta = new PaginaDeSubastas();
        respuesta.setPagina(numero);
        respuesta.setSubastas(vistas.stream().map(SubastasWeb::vista).toList());
        return ResponseEntity.ok(respuesta);
    }

    public ResponseEntity<VistaDeSubasta> ver(UUID subastaId) {
        Traza.marcarCasoDeUso("CU-64", subastaId.toString());
        return ResponseEntity.ok(vista(consultas
                .ver(subastaId, sesion.actual())
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(136, 5), "Esa subasta no esta disponible."))));
    }

    public ResponseEntity<SalidaPuja> pujar(UUID subastaId, UUID idempotencyKey, EntradaPuja cuerpo) {
        Traza.marcarCasoDeUso("CU-64", subastaId.toString());
        var r = subasta.pujar(
                subastaId,
                MapeoDeEntregas.dinero(cuerpo.getMonto()),
                Boolean.TRUE.equals(cuerpo.getAceptoReglamento()),
                ClaveIdempotencia.deHecho("puja", idempotencyKey).valor(),
                sesion.actual());
        var respuesta = new SalidaPuja();
        respuesta.setPujaId(r.pujaId());
        respuesta.setEstado(SalidaPuja.EstadoEnum.fromValue(r.estado()));
        // 202: la billetera no respondio y la puja quedo PENDIENTE; se reanuda con la misma clave.
        return ResponseEntity.status("PENDIENTE".equals(r.estado()) ? HttpStatus.ACCEPTED : HttpStatus.CREATED)
                .body(respuesta);
    }

    public ResponseEntity<VistaDeSubasta> cancelar(UUID subastaId) {
        Traza.marcarCasoDeUso("CU-64", subastaId.toString());
        var ctx = sesion.actual();
        var cancelada = subasta.cancelar(subastaId, ctx);
        return ResponseEntity.ok(vista(consultas.ver(cancelada, ctx).orElseThrow()));
    }

    /** Para la billetera: si el id es de una subasta adjudicada, su precio es la puja ganadora. */
    public Optional<ResponseEntity<OfertaParaPago>> paraElPago(UUID id) {
        return consultas.paraElPago(id, sesion.actual()).map(o -> {
            var respuesta = new OfertaParaPago();
            respuesta.setOfertaId(o.ofertaId());
            respuesta.setVendedorUsuarioId(o.vendedorUsuarioId());
            respuesta.setPrecio(MapeoDeEntregas.dinero(o.precio()));
            respuesta.setCargos(MapeoDeEntregas.dinero(o.cargos()));
            respuesta.setEstado(OfertaParaPago.EstadoEnum.fromValue(o.estado()));
            return ResponseEntity.ok(respuesta);
        });
    }

    private static VistaDeSubasta vista(ConsultasDeSubasta.Vista v) {
        var r = new VistaDeSubasta();
        r.setSubastaId(v.subastaId());
        r.setGrupoId(v.grupoId());
        r.setCupoId(v.cupoId());
        r.setNombreDelGrupo(v.nombreDelGrupo());
        r.setPeriodicidad(v.periodicidad());
        r.setMontoAporte(MapeoDeEntregas.dinero(v.aporte()));
        r.setMontoBase(MapeoDeEntregas.dinero(v.base()));
        r.setMontoMasAlto(v.masAlto().map(MapeoDeEntregas::dinero).orElse(null));
        r.setMontoMinimo(MapeoDeEntregas.dinero(v.minimo()));
        r.setEstado(VistaDeSubasta.EstadoEnum.fromValue(v.estado()));
        r.setCierraEn(v.cierraEn());
        r.setCantidadDePujas(v.cantidadDePujas());
        r.setSoyElSaliente(v.soyElSaliente());
        r.setMiPuja(v.miPuja()
                .map(p -> {
                    var mia = new MiPuja();
                    mia.setMonto(MapeoDeEntregas.dinero(p.monto()));
                    mia.setEstado(MiPuja.EstadoEnum.fromValue(p.estado()));
                    return mia;
                })
                .orElse(null));
        return r;
    }
}
