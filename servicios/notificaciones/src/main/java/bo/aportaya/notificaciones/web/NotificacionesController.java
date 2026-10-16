package bo.aportaya.notificaciones.web;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta;
import bo.aportaya.notificaciones.aplicacion.ConsultarSupresion;
import bo.aportaya.notificaciones.aplicacion.SolicitarAviso;
import bo.aportaya.notificaciones.web.generado.NotificacionesApi;
import bo.aportaya.notificaciones.web.generado.modelo.EntradaConsultaSupresion;
import bo.aportaya.notificaciones.web.generado.modelo.EntradaNotificacion;
import bo.aportaya.notificaciones.web.generado.modelo.EntradaRespuesta;
import bo.aportaya.notificaciones.web.generado.modelo.SalidaNotificacion;
import bo.aportaya.notificaciones.web.generado.modelo.SalidaRespuesta;
import bo.aportaya.notificaciones.web.generado.modelo.Supresion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /notificaciones}.
 *
 * <p>El webhook es **la unica puerta que abre alguien de afuera** de todo el sistema
 * que no es una ruta publica de consulta. Va sin sesion porque el proveedor no la
 * tiene, y lo que lo protege es la firma, que CU-82 verifica antes de tocar la base.
 */
@RestController
public class NotificacionesController implements NotificacionesApi {

    /**
     * El identificador del proceso que atiende los webhooks.
     *
     * <p>Fijo y no aleatorio por peticion: es lo que permite mirar la bitacora y ver
     * todo lo que entro por esta puerta, que es la unica abierta desde afuera.
     */
    private static final UUID PROCESO_DEL_WEBHOOK = UUID.fromString("00000000-0000-0000-0000-0000000082cb");

    private final SolicitarAviso avisos;
    private final ConsultarSupresion supresiones;
    private final CU82ProcesarRespuesta cu82;
    private final SesionDeLaPeticion sesion;
    private final LimiteDeConsultas limiteDeConsultas;

    public NotificacionesController(
            SolicitarAviso avisos,
            ConsultarSupresion supresiones,
            CU82ProcesarRespuesta cu82,
            SesionDeLaPeticion sesion,
            Reloj reloj,
            @Value("${aportaya.supresion.consultas-por-minuto:30}") int consultasPorMinuto) {
        this.avisos = avisos;
        this.supresiones = supresiones;
        this.cu82 = cu82;
        this.sesion = sesion;
        this.limiteDeConsultas = new LimiteDeConsultas(reloj, consultasPorMinuto, Duration.ofMinutes(1));
    }

    /**
     * Si un destino pidio no recibir mas.
     *
     * <p>La pregunta {@code grupos} antes de invitar: a quien se dio de baja no se le
     * escribe, aunque quien invita no lo sepa.
     */
    /** @deprecated el contacto viaja en la query; usar {@link #verificarSupresion} hasta que grupos migre. */
    @Deprecated
    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<Supresion> consultarSupresion(String identificador, String categoria) {
        return preguntar(identificador, categoria);
    }

    /**
     * La misma pregunta con el contacto en el cuerpo (90.2.2) y con tope por actor: es un oraculo
     * de «este contacto pidio no recibir» y sin tope recorre una agenda.
     */
    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<Supresion> verificarSupresion(EntradaConsultaSupresion cuerpo) {
        return preguntar(cuerpo.getIdentificador(), cuerpo.getCategoria());
    }

    private ResponseEntity<Supresion> preguntar(String identificador, String categoria) {
        Traza.marcarCasoDeUso("CU-80", categoria);

        var contexto = sesion.actual();
        limiteDeConsultas.registrar(contexto.usuarioId());
        var respuesta = new Supresion();
        respuesta.setSuprimido(supresiones.estaSuprimido(identificador, categoria, contexto));
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("SOPORTE")
    public ResponseEntity<SalidaNotificacion> emitirNotificacion(UUID idempotencyKey, EntradaNotificacion cuerpo) {
        Traza.marcarCasoDeUso("CU-80", cuerpo.getEvento());

        // El contrato dice `datos: object`, pero el generador lo tipa como Object y una cadena o una lista
        // pasan la validacion: se cierra aca (96.2.1) en vez de publicar en el outbox algo que ninguna plantilla sabe
        // leer.
        if (!(cuerpo.getDatos() instanceof java.util.Map<?, ?>)) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeDominio("Los datos del aviso tienen que ser un objeto.");
        }

        var salida = avisos.ejecutar(
                new SolicitarAviso.Entrada(cuerpo.getDestinatarioId(), cuerpo.getEvento(), cuerpo.getDatos()),
                sesion.actual());

        var respuesta = new SalidaNotificacion();
        respuesta.setNotificacionId(salida.notificacionId());
        respuesta.setCanales(salida.canales().stream()
                .map(SalidaNotificacion.CanalesEnum::fromValue)
                .toList());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }

    @Override
    @Publico("CU-82: el proveedor que entrega el mensaje no tiene sesion; lo autentica su firma")
    public ResponseEntity<SalidaRespuesta> procesarRespuestaEntrante(String proveedor, EntradaRespuesta cuerpo) {
        Traza.marcarCasoDeUso("CU-82", proveedor);

        var salida = cu82.ejecutar(
                new CU82ProcesarRespuesta.EntradaRespuesta(
                        cuerpo.getCanal() == null
                                ? proveedor
                                : cuerpo.getCanal().getValue(),
                        cuerpo.getRemitente(),
                        cuerpo.getFirma(),
                        cuerpo.getCargaUtil(),
                        cuerpo.getContenido(),
                        cuerpo.getClaveIdempotencia(),
                        Optional.ofNullable(cuerpo.getNotificacionRelacionadaId())),
                // Sin sesion de usuario: el proveedor no la tiene. `deSistema` no es
                // una excepcion a las politicas de fila, es un rol con las suyas.
                ContextoSesion.deSistema(
                        PROCESO_DEL_WEBHOOK, new bo.aportaya.plataforma.dominio.Traza(Traza.actual())));

        var respuesta = new SalidaRespuesta();
        respuesta.setRespuestaId(salida.respuestaId());
        respuesta.setIntencion(SalidaRespuesta.IntencionEnum.fromValue(salida.intencion()));
        respuesta.setAccion(SalidaRespuesta.AccionEnum.fromValue(salida.accion()));
        respuesta.setReferenciaId(salida.referenciaId());
        return ResponseEntity.ok(respuesta);
    }
}
