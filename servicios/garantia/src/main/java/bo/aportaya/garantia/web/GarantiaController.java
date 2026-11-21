package bo.aportaya.garantia.web;

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
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.OperacionRetirada;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /garantia}: el fondo, el reemplazo y la disolucion. El respaldo empresarial se retiro
 * (F2.L2.M02): sus rutas responden 410.
 *
 * <p>La disolucion se inicia y se cierra en dos actos: entre uno y otro hay que
 * devolverle a cada quien lo suyo, y **ni un centavo se pierde ni se inventa** — el
 * cuadre de la masa lo comprueba el dominio antes de escribir nada.
 */
@RestController
public class GarantiaController implements GarantiaApi {

    private final CU66ReemplazarParticipante cu66;
    private final CU67DisolverGrupo cu67;
    private final SesionDeLaPeticion sesion;

    public GarantiaController(CU66ReemplazarParticipante cu66, CU67DisolverGrupo cu67, SesionDeLaPeticion sesion) {
        this.cu66 = cu66;
        this.cu67 = cu67;
        this.sesion = sesion;
    }

    /**
     * F1.L2.M03 (SEG-004): la devolucion decidia con {@code grupoCerrado} y {@code deudasVivas} del cuerpo. Se retira
     * de la API: la dispara el sistema al cerrarse el ciclo, con las precondiciones leidas de sus duenos (F3.L2.M06).
     * 410 sin efecto: el fondo no cambia.
     */
    @Deprecated
    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaDevolucionFondo> devolverFondo(UUID grupoId, EntradaDevolucionFondo cuerpo) {
        throw new OperacionRetirada(
                CodigoError.de(29, 6),
                "El fondo ya no se devuelve a pedido: lo devuelve el sistema al cerrar el ciclo.");
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

    // --- respaldo empresarial: RETIRADO (F2.L2.M02, ADR-055 E7) --------------------------------
    // La empresa no cubre ni reserva nada: el faltante lo cubre el fondo del grupo (F3.L2.M05). Las seis rutas de
    // /garantia/respaldo quedan deprecated y responden 410 AP-CU23-15 sin efecto. El permiso se conserva para que
    // la guardia siga respondiendo 401/403 antes que el 410: a quien no puede invocarla no se le cuenta que existio.

    private static OperacionRetirada respaldoRetirado() {
        return new OperacionRetirada(
                CodigoError.de(23, 15),
                "El respaldo empresarial se retiro: el faltante del pozo lo cubre el fondo del grupo.");
    }

    @Deprecated
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaReservaRespaldo> reservarRespaldo(UUID idempotencyKey, EntradaReservaRespaldo cuerpo) {
        throw respaldoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaAmpliacionRespaldo> ampliarRespaldo(
            UUID reservaId, UUID idempotencyKey, EntradaAmpliacionRespaldo cuerpo) {
        throw respaldoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaLiberacionRespaldo> liberarRespaldo(UUID reservaId, UUID idempotencyKey) {
        throw respaldoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaCoberturaRespaldo> cubrirConRespaldo(
            UUID idempotencyKey, EntradaCoberturaRespaldo cuerpo) {
        throw respaldoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaReversaRespaldo> reversarCoberturaRespaldo(UUID coberturaId, UUID idempotencyKey) {
        throw respaldoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<SalidaRecuperacionRespaldo> recuperarRespaldo(
            UUID idempotencyKey, EntradaRecuperacionRespaldo cuerpo) {
        throw respaldoRetirado();
    }
}
