package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.web.generado.modelo.ArchivoDelExpediente;
import bo.aportaya.identidad.web.generado.modelo.CaraDelExpediente;
import bo.aportaya.identidad.web.generado.modelo.EstadoDeVerificacion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * El expediente de identidad (CU-02): la subida de las fotos durante el alta y el estado de la verificacion.
 *
 * <p>Vive aparte de {@link UsuariosController} por tamano (el barrido corta en 300 lineas): son las dos
 * operaciones publicas que sirven a quien todavia no tiene sesion, y las dos comparten el control de titularidad.
 */
public abstract class ExpedienteController extends InvitacionesController {

    private final CU02GuardarFotoDelExpediente fotos;
    private final CU02ConsultarEstadoDeVerificacion estadoDeVerificacion;
    private final TitularDelExpediente titular;

    /** El token que devuelve el alta y autoriza las fotos de ESA persona; el alta lo emite desde la subclase. */
    protected final TokenDeAlta tokenDeAlta;

    protected ExpedienteController(
            ConsumirInvitacion consumo,
            SesionDeLaPeticion sesion,
            HttpServletRequest peticion,
            CU02GuardarFotoDelExpediente fotos,
            CU02ConsultarEstadoDeVerificacion estadoDeVerificacion,
            String pimienta,
            Reloj reloj,
            Duration vigenciaTokenDeAlta) {
        super(consumo, sesion, peticion);
        this.fotos = fotos;
        this.estadoDeVerificacion = estadoDeVerificacion;
        this.tokenDeAlta = new TokenDeAlta(pimienta, reloj, vigenciaTokenDeAlta);
        this.titular = new TitularDelExpediente(tokenDeAlta, sesion);
    }

    /**
     * La foto del documento o la prueba de vida. Va al servidor de archivos y en
     * la fila queda la clave del objeto: el binario nunca se sirve directo ni se guarda
     * en el disco del contenedor, que se reemplaza — y la evidencia legal no.
     *
     * <p>Sin sesion exigida porque estas fotos se sacan durante el registro, cuando todavia no
     * hay sesion que presentar; pero NO es una ruta abierta: el {@code usuarioId} de la URL no
     * autoriza nada. Escribe en el expediente solo quien presenta el token que le dio SU alta
     * (cabecera {@code Token-De-Alta}) o quien tiene sesion propia. Todo lo demas es el mismo
     * 403, exista o no ese usuario.
     */
    @Override
    @Publico("CU-02: se sube durante el alta, sin sesion; exige el token de alta del propio usuario o su sesion")
    public ResponseEntity<ArchivoDelExpediente> subirDocumento(
            UUID usuarioId, UUID idempotencyKey, String cara, MultipartFile archivo, String tokenDeAltaPresentado) {
        titular.exigir(usuarioId, tokenDeAltaPresentado);
        try {
            var guardado = fotos.ejecutar(
                    usuarioId,
                    CU02GuardarFotoDelExpediente.Cara.valueOf(cara),
                    archivo.getInputStream(),
                    archivo.getSize(),
                    archivo.getOriginalFilename(),
                    Traza.actual());
            var salida = new ArchivoDelExpediente()
                    .cara(ArchivoDelExpediente.CaraEnum.fromValue(cara))
                    .claveObjeto(guardado.clave().toString())
                    .hashArchivo(guardado.hashSha256())
                    .tipoMime(guardado.tipoMime())
                    .bytes(guardado.bytes());
            return ResponseEntity.status(201).body(salida);
        } catch (java.io.IOException e) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeDominio(
                    "Se corto la subida de la foto. Probá de nuevo.", e);
        }
    }

    /** Estado mínimo para que quien aún no puede iniciar sesión siga su verificación. */
    @Override
    @Publico("CU-02: quien acaba de registrarse consulta el estado de su expediente")
    public ResponseEntity<EstadoDeVerificacion> consultarEstadoDeVerificacion(UUID usuarioId) {
        Traza.marcarCasoDeUso("CU-02", usuarioId.toString());
        var expediente = estadoDeVerificacion.ejecutar(usuarioId, Traza.actual());
        var salida = new EstadoDeVerificacion()
                .verificacionId(expediente.verificacionId())
                .estado(EstadoDeVerificacion.EstadoEnum.fromValue(expediente.estado()))
                .motivoRechazo(expediente.motivoRechazo());
        expediente.fotos().forEach(f -> salida.addFotosItem(CaraDelExpediente.fromValue(f)));
        return ResponseEntity.ok(salida);
    }
}
