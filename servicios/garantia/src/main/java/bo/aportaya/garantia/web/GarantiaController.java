package bo.aportaya.garantia.web;

import bo.aportaya.garantia.aplicacion.CU29DevolverFondo;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.CU67DisolverGrupo;
import bo.aportaya.garantia.dominio.CuadreDeDisolucion;
import bo.aportaya.garantia.web.generado.GarantiaApi;
import bo.aportaya.garantia.web.generado.modelo.AprobarReemplazo200Response;
import bo.aportaya.garantia.web.generado.modelo.EntradaAmpliacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.EntradaCoberturaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.EntradaDevolucionFondo;
import bo.aportaya.garantia.web.generado.modelo.EntradaDisolucion;
import bo.aportaya.garantia.web.generado.modelo.EntradaRecuperacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.EntradaReservaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaAmpliacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaCierreDisolucion;
import bo.aportaya.garantia.web.generado.modelo.SalidaCoberturaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaDevolucionFondo;
import bo.aportaya.garantia.web.generado.modelo.SalidaDisolucion;
import bo.aportaya.garantia.web.generado.modelo.SalidaLiberacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaRecuperacionRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaReemplazo;
import bo.aportaya.garantia.web.generado.modelo.SalidaReservaRespaldo;
import bo.aportaya.garantia.web.generado.modelo.SalidaReversaRespaldo;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /garantia}: el fondo, el reemplazo y la disolucion.
 *
 * <p>La disolucion se inicia y se cierra en dos actos: entre uno y otro hay que
 * devolverle a cada quien lo suyo, y **ni un centavo se pierde ni se inventa** — el
 * cuadre de la masa lo comprueba el dominio antes de escribir nada.
 */
@RestController
public class GarantiaController implements GarantiaApi {

    private final CU29DevolverFondo cu29;
    private final CU66ReemplazarParticipante cu66;
    private final CU67DisolverGrupo cu67;
    private final RespaldoWeb respaldo;
    private final SesionDeLaPeticion sesion;

    public GarantiaController(
            CU29DevolverFondo cu29,
            CU66ReemplazarParticipante cu66,
            CU67DisolverGrupo cu67,
            RespaldoWeb respaldo,
            SesionDeLaPeticion sesion) {
        this.cu29 = cu29;
        this.cu66 = cu66;
        this.cu67 = cu67;
        this.respaldo = respaldo;
        this.sesion = sesion;
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaDevolucionFondo> devolverFondo(UUID grupoId, EntradaDevolucionFondo cuerpo) {
        Traza.marcarCasoDeUso("CU-29", grupoId.toString());

        var salida = cu29.devolver(
                new CU29DevolverFondo.EntradaDevolucion(
                        grupoId, Boolean.TRUE.equals(cuerpo.getGrupoCerrado()), cuerpo.getDeudasVivas()),
                sesion.actual());

        var respuesta = new SalidaDevolucionFondo();
        respuesta.setFondoId(salida.fondoId());
        respuesta.setTotalAportado(MapeoDeGarantia.dinero(salida.totalAportado()));
        respuesta.setTotalDevuelto(MapeoDeGarantia.dinero(salida.totalDevuelto()));
        respuesta.setConsumidoPorCoberturas(MapeoDeGarantia.dinero(salida.consumidoPorCoberturas()));
        respuesta.setDevoluciones(
                salida.devoluciones().stream().map(MapeoDeGarantia::devolucion).toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<AprobarReemplazo200Response> aprobarReemplazo(UUID reemplazoId) {
        Traza.marcarCasoDeUso("CU-66", reemplazoId.toString());

        var respuesta = new AprobarReemplazo200Response();
        respuesta.setAprobado(cu66.aprobar(reemplazoId, sesion.actual()));
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaReemplazo> ejecutarReemplazo(UUID reemplazoId) {
        Traza.marcarCasoDeUso("CU-66", reemplazoId.toString());
        return ResponseEntity.ok(MapeoDeReemplazo.salida(cu66.ejecutar(reemplazoId, sesion.actual())));
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaDisolucion> iniciarDisolucion(UUID grupoId, EntradaDisolucion cuerpo) {
        Traza.marcarCasoDeUso("CU-67", grupoId.toString());

        var salida = cu67.iniciar(
                new CU67DisolverGrupo.EntradaDisolucion(
                        grupoId,
                        cuerpo.getCausal().getValue(),
                        cuerpo.getMotivo(),
                        MapeoDeGarantia.dinero(cuerpo.getTotalAportado()),
                        MapeoDeGarantia.dinero(cuerpo.getTotalEntregado()),
                        MapeoDeGarantia.dinero(cuerpo.getMasaDisponible()),
                        cuerpo.getPosiciones().stream()
                                .map(p -> new CuadreDeDisolucion.Posicion(
                                        p.getParticipanteId(),
                                        MapeoDeGarantia.dinero(p.getAportado()),
                                        MapeoDeGarantia.dinero(p.getRecibido())))
                                .toList()),
                sesion.actual());

        var respuesta = new SalidaDisolucion();
        respuesta.setDisolucionId(salida.disolucionId());
        respuesta.setEstado(salida.estado());
        respuesta.setMasaARepartir(MapeoDeGarantia.dinero(salida.masaARepartir()));
        respuesta.setTotalADevolver(MapeoDeGarantia.dinero(salida.totalADevolver()));
        respuesta.setTotalACobrar(MapeoDeGarantia.dinero(salida.totalACobrar()));
        respuesta.setLiquidaciones(salida.liquidaciones().stream()
                .map(MapeoDeGarantia::liquidacion)
                .toList());
        respuesta.setEsNueva(salida.esNueva());

        var estado = salida.esNueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(respuesta);
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaCierreDisolucion> cerrarDisolucion(UUID disolucionId) {
        Traza.marcarCasoDeUso("CU-67", disolucionId.toString());

        var salida = cu67.cerrar(disolucionId, sesion.actual());

        var respuesta = new SalidaCierreDisolucion();
        respuesta.setDisolucionId(salida.disolucionId());
        respuesta.setEstado(SalidaCierreDisolucion.EstadoEnum.fromValue(salida.estado()));
        respuesta.setCerradaEn(salida.cerradaEn());
        return ResponseEntity.ok(respuesta);
    }

    // --- respaldo empresarial (carril C): las seis rutas de /garantia/respaldo ---------------
    // La traduccion vive en RespaldoWeb; el permiso, que es lo que la guardia lee, vive aca.

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaReservaRespaldo> reservarRespaldo(UUID idempotencyKey, EntradaReservaRespaldo cuerpo) {
        return respaldo.reservarRespaldo(idempotencyKey, cuerpo);
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaAmpliacionRespaldo> ampliarRespaldo(
            UUID reservaId, UUID idempotencyKey, EntradaAmpliacionRespaldo cuerpo) {
        return respaldo.ampliarRespaldo(reservaId, idempotencyKey, cuerpo);
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaLiberacionRespaldo> liberarRespaldo(UUID reservaId, UUID idempotencyKey) {
        return respaldo.liberarRespaldo(reservaId, idempotencyKey);
    }

    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaCoberturaRespaldo> cubrirConRespaldo(
            UUID idempotencyKey, EntradaCoberturaRespaldo cuerpo) {
        return respaldo.cubrirConRespaldo(idempotencyKey, cuerpo);
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaReversaRespaldo> reversarCoberturaRespaldo(UUID coberturaId, UUID idempotencyKey) {
        return respaldo.reversarCoberturaRespaldo(coberturaId, idempotencyKey);
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaRecuperacionRespaldo> recuperarRespaldo(
            UUID idempotencyKey, EntradaRecuperacionRespaldo cuerpo) {
        return respaldo.recuperarRespaldo(idempotencyKey, cuerpo);
    }
}
