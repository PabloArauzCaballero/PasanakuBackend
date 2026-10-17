package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU02ActualizarFechaExpiracion;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.CU02IniciarVerificacion;
import bo.aportaya.identidad.aplicacion.ConsultarMiPerfil;
import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.web.generado.modelo.ArchivoDelExpediente;
import bo.aportaya.identidad.web.generado.modelo.CaraDelExpediente;
import bo.aportaya.identidad.web.generado.modelo.Documento;
import bo.aportaya.identidad.web.generado.modelo.EstadoDeVerificacion;
import bo.aportaya.identidad.web.generado.modelo.FechaExpiracionDocumento;
import bo.aportaya.identidad.web.generado.modelo.MiPerfil;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.Permiso;
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
 * operaciones sin sesion obligatoria que sirven a quien todavia no la tiene, y las dos comparten el control de
 * titularidad.
 */
public abstract class ExpedienteController extends InvitacionesController {

    private final CU02GuardarFotoDelExpediente fotos;
    private final CU02ConsultarEstadoDeVerificacion estadoDeVerificacion;
    private final CU02ActualizarFechaExpiracion actualizarFechaExpiracion;
    private final SesionDeLaPeticion sesion;
    private final Reloj reloj;
    private final TitularDelExpediente titular;
    private final ConsultarMiPerfil miPerfil;
    private final CU02IniciarVerificacion iniciarVerificacion;
    private final String pimienta;

    /** El token que devuelve el alta y autoriza las fotos de ESA persona; el alta lo emite desde la subclase. */
    protected final TokenDeAlta tokenDeAlta;

    protected ExpedienteController(
            ConsumirInvitacion consumo,
            SesionDeLaPeticion sesion,
            HttpServletRequest peticion,
            CU02GuardarFotoDelExpediente fotos,
            CU02ConsultarEstadoDeVerificacion estadoDeVerificacion,
            CU02ActualizarFechaExpiracion actualizarFechaExpiracion,
            ConsultarMiPerfil miPerfil,
            CU02IniciarVerificacion iniciarVerificacion,
            String pimienta,
            Reloj reloj,
            Duration vigenciaTokenDeAlta) {
        super(consumo, sesion, peticion);
        this.miPerfil = miPerfil;
        this.iniciarVerificacion = iniciarVerificacion;
        this.pimienta = pimienta;
        this.fotos = fotos;
        this.estadoDeVerificacion = estadoDeVerificacion;
        this.actualizarFechaExpiracion = actualizarFechaExpiracion;
        this.sesion = sesion;
        this.reloj = reloj;
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

    /**
     * Estado mínimo para que quien aún no puede iniciar sesión siga su verificación.
     *
     * <p>Sin sesión obligatoria, pero el {@code usuarioId} de la URL no autoriza nada: lo consulta
     * solo quien presenta el token que le dio SU alta (cabecera {@code Token-De-Alta}) o quien tiene
     * sesión propia. Todo lo demás es el mismo 403, exista o no ese usuario, y se decide ANTES de
     * leer el expediente. {@code motivoRechazo} es texto de quien revisó: solo sale cuando el estado
     * es RECHAZADA (a quien le rechazan la cuenta le corresponde saber por qué) y nunca antes.
     */
    @Override
    @Publico(
            "CU-02: quien acaba de registrarse consulta su expediente; exige el token de alta del propio usuario o su sesion")
    public ResponseEntity<EstadoDeVerificacion> consultarEstadoDeVerificacion(
            UUID usuarioId, String tokenDeAltaPresentado) {
        titular.exigir(usuarioId, tokenDeAltaPresentado);
        Traza.marcarCasoDeUso("CU-02", usuarioId.toString());
        var expediente = estadoDeVerificacion.ejecutar(usuarioId, Traza.actual());
        var estado = EstadoDeVerificacion.EstadoEnum.fromValue(expediente.estado());
        var salida = new EstadoDeVerificacion()
                .verificacionId(expediente.verificacionId())
                .estado(estado)
                .fechaExpiracionDocumento(expediente.fechaExpiracionDocumento())
                .motivoRechazo(estado == EstadoDeVerificacion.EstadoEnum.RECHAZADA ? expediente.motivoRechazo() : null);
        expediente.fotos().forEach(f -> salida.addFotosItem(CaraDelExpediente.fromValue(f)));
        return ResponseEntity.ok(salida);
    }

    /** Fecha del documento aportada por el titular; la aprobación permanece en manos de la revisión humana. */
    @Override
    @Publico("CU-02: valida sesión y titularidad antes de modificar la fecha del documento propio")
    public ResponseEntity<Void> actualizarFechaExpiracionDocumento(UUID usuarioId, FechaExpiracionDocumento cuerpo) {
        var actor = sesion.actual();
        titular.exigir(usuarioId, null);
        actualizarFechaExpiracion.ejecutar(usuarioId, cuerpo.getFechaExpiracionDocumento(), actor, reloj.hoy());
        return ResponseEntity.noContent().build();
    }

    /**
     * CU-02 diferido: quien abrio la cuenta sin documento lo envia cuando quiere operar con
     * dinero. Exige la sesion del titular (el mismo 403 para cualquier otra persona) antes de
     * tocar nada; la aprobacion sigue en manos de la revision humana.
     */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<EstadoDeVerificacion> iniciarVerificacion(UUID usuarioId, Documento cuerpo) {
        // Solo la propia: el token de alta no alcanza y el UUID de la ruta no autoriza nada.
        titular.exigir(usuarioId, null);
        Traza.marcarCasoDeUso("CU-02", usuarioId.toString());
        var documento = MapeoDelAlta.documentoDe(cuerpo, pimienta);
        UUID verificacionId = iniciarVerificacion.ejecutar(
                usuarioId, documento, MapeoDelAlta.numeroCifradoDe(documento), Traza.actual());
        return ResponseEntity.accepted()
                .body(new EstadoDeVerificacion()
                        .verificacionId(verificacionId)
                        .estado(EstadoDeVerificacion.EstadoEnum.EN_REVISION));
    }

    /** CU-07: el correo de la propia cuenta. Sin id en la ruta: sale de la sesion. */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<MiPerfil> consultarMiPerfil() {
        Traza.marcarCasoDeUso("CU-07", "mi-perfil");
        var p = miPerfil.ejecutar(sesion.actual());
        var salida = new MiPerfil(
                p.usuarioId(), p.correoVerificado(), MiPerfil.EstadoDeCuentaEnum.fromValue(p.estadoDeCuenta()));
        salida.setCorreo(p.correo());
        return ResponseEntity.ok(salida);
    }
}
