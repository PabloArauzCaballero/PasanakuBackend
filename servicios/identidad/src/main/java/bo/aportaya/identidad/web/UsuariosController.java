package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02ConsultarEstadoDeVerificacion;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.ConfirmarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ConsultarNivelKyc;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.web.generado.modelo.ConfirmacionVerificacionCorreo;
import bo.aportaya.identidad.web.generado.modelo.EntradaBusquedaPorTelefono;
import bo.aportaya.identidad.web.generado.modelo.EntradaRegistro;
import bo.aportaya.identidad.web.generado.modelo.EntradaTitularidad;
import bo.aportaya.identidad.web.generado.modelo.EntradaTokenDeInvitacion;
import bo.aportaya.identidad.web.generado.modelo.EntradaValidacionInvitacion;
import bo.aportaya.identidad.web.generado.modelo.NivelKyc;
import bo.aportaya.identidad.web.generado.modelo.SalidaRegistro;
import bo.aportaya.identidad.web.generado.modelo.SalidaTitularidad;
import bo.aportaya.identidad.web.generado.modelo.SalidaTokenDeInvitacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaValidacionInvitacion;
import bo.aportaya.identidad.web.generado.modelo.SolicitudVerificacionCorreo;
import bo.aportaya.identidad.web.generado.modelo.UsuarioEncontrado;
import bo.aportaya.identidad.web.generado.modelo.VerificacionCorreoConfirmada;
import bo.aportaya.identidad.web.generado.modelo.VerificacionCorreoSolicitada;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de CU-01: traduce y delega, sin logica.
 *
 * <p>Devuelve {@code 202} y no {@code 201}. **No promete la billetera**: esa la abre
 * `nucleo-financiero` al consumir el evento, y decir aca que ya esta abierta seria
 * decirle al cliente que tiene algo que todavia nadie abrio.
 */
@RestController
public class UsuariosController extends ExpedienteController {

    /** El «usuario» del contexto mientras todavia no hay usuario. */
    private static final UUID PROCESO_DE_ALTA = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final CU01RegistrarUsuario cu01;
    private final VerificarTitularidad titularidad;
    private final EmitirTokenDeInvitacion tokens;
    private final ValidarTokenDeInvitacion validacionDeInvitacion;
    private final BuscarPorTelefono busqueda;
    private final ConsultarNivelKyc nivelesKyc;
    private final SolicitarVerificacionCorreo solicitarCorreo;
    private final ConfirmarVerificacionCorreo confirmarCorreo;
    private final SesionDeLaPeticion sesion;
    private final HttpServletRequest peticion;
    private final String pimienta;
    private final LimiteDeConsultas limiteDeTitularidad;

    public UsuariosController(
            CU01RegistrarUsuario cu01,
            VerificarTitularidad titularidad,
            CU02GuardarFotoDelExpediente fotos,
            CU02ConsultarEstadoDeVerificacion estadoDeVerificacion,
            EmitirTokenDeInvitacion tokens,
            ValidarTokenDeInvitacion validacionDeInvitacion,
            bo.aportaya.identidad.aplicacion.ConsumirInvitacion consumo,
            BuscarPorTelefono busqueda,
            ConsultarNivelKyc nivelesKyc,
            SolicitarVerificacionCorreo solicitarCorreo,
            ConfirmarVerificacionCorreo confirmarCorreo,
            SesionDeLaPeticion sesion,
            HttpServletRequest peticion,
            @Value("${aportaya.seguridad.pimienta}") String pimienta,
            Reloj reloj,
            @Value("${identidad.alta.vigencia-token:PT30M}") Duration vigenciaTokenDeAlta,
            @Value("${aportaya.titularidad.consultas-por-minuto:120}") int titularidadPorMinuto) {
        super(consumo, sesion, peticion, fotos, estadoDeVerificacion, pimienta, reloj, vigenciaTokenDeAlta);
        this.cu01 = cu01;
        this.titularidad = titularidad;
        this.tokens = tokens;
        this.validacionDeInvitacion = validacionDeInvitacion;
        this.busqueda = busqueda;
        this.nivelesKyc = nivelesKyc;
        this.solicitarCorreo = solicitarCorreo;
        this.confirmarCorreo = confirmarCorreo;
        this.sesion = sesion;
        this.peticion = peticion;
        this.pimienta = pimienta;
        this.limiteDeTitularidad = new LimiteDeConsultas(reloj, titularidadPorMinuto, Duration.ofMinutes(1));
    }

    /**
     * Si el nombre y el documento declarados son los del titular.
     *
     * <p>Contesta si coinciden y nada mas. Devolver los guardados haria de esta ruta un
     * directorio de clientes para cualquier servicio comprometido.
     */
    /** @deprecated el telefono viaja en la query; usar {@link #buscarUsuarioPorTelefono} hasta que grupos migre. */
    @Deprecated
    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<UsuarioEncontrado> buscarPorTelefono(String telefono) {
        Traza.marcarCasoDeUso("CU-69", "busqueda");

        var encontrado = busqueda.ejecutar(telefono, sesion.actual());

        var respuesta = new UsuarioEncontrado();
        respuesta.setExiste(encontrado.isPresent());
        encontrado.ifPresent(respuesta::setUsuarioId);
        return ResponseEntity.ok(respuesta);
    }

    /** La misma pregunta, con el telefono en el cuerpo: una URL queda en logs y proxies (90.2.2). */
    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<UsuarioEncontrado> buscarUsuarioPorTelefono(EntradaBusquedaPorTelefono cuerpo) {
        return buscarPorTelefono(cuerpo.getTelefono());
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaTokenDeInvitacion> emitirTokenDeInvitacion(
            UUID idempotencyKey, EntradaTokenDeInvitacion cuerpo) {
        Traza.marcarCasoDeUso("CU-69", cuerpo.getCanal().getValue());
        String agente = agente();

        var emitido = tokens.ejecutar(
                new EmitirTokenDeInvitacion.Entrada(
                        idempotencyKey,
                        cuerpo.getGrupoId(),
                        cuerpo.getTelefonoDestino(),
                        cuerpo.getCanal().getValue(),
                        ip(),
                        agente.substring(0, Math.min(255, agente.length()))),
                sesion.actual());

        var respuesta = new SalidaTokenDeInvitacion();
        respuesta.setTokenId(emitido.tokenId());
        respuesta.setToken(emitido.token());
        respuesta.setExpiraEn(emitido.expiraEn());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaValidacionInvitacion> validarTokenDeInvitacion(EntradaValidacionInvitacion cuerpo) {
        Traza.marcarCasoDeUso("CU-69", "validar-enlace");
        boolean valido = validacionDeInvitacion.ejecutar(
                cuerpo.getTokenId(),
                cuerpo.getToken(),
                cuerpo.getTelefonoE164(),
                cuerpo.getKycMinimo().getValue(),
                sesion.actual());
        return ResponseEntity.ok(new SalidaValidacionInvitacion().valido(valido));
    }

    /**
     * Si el nombre y el documento declarados son los del titular: SOLO contesta si o no, jamas el dato.
     *
     * <p>Dos puertas. {@code DATOS_SENSIBLES_LEER} es de quien lee documentos de clientes (backoffice) y NO de un
     * participante. La otra es la credencial de SERVICIO de {@code entregas} ({@code SERVICIO_ENTREGAS}): es el mecanismo
     * con que entregas comprueba que una cuenta bancaria es de quien la registra (CU-18, R-SEG-02). Antes entregas
     * propagaba el token del PARTICIPANTE, que no tiene el permiso, y el 403 se leia como «no coincide»: ningun
     * participante podia registrar su cuenta. El rol es la puerta mas estrecha posible: solo esta ruta lo nombra, y la
     * respuesta es un booleano (carril I). QUE rol de operacion deberia poder verificar es una decision del modelo de
     * roles que corresponde a Seguridad; aca solo se cierra el mecanismo.
     */
    @Override
    @Permiso("DATOS_SENSIBLES_LEER")
    @PermisoAlternativo("SERVICIO_ENTREGAS")
    public ResponseEntity<SalidaTitularidad> verificarTitularidad(UUID usuarioId, EntradaTitularidad cuerpo) {
        Traza.marcarCasoDeUso("CU-01", usuarioId.toString());
        // Oraculo de si/no: tope por actor (429 con Retry-After) ANTES de tocar el expediente.
        limiteDeTitularidad.registrar(sesion.actual().usuarioId());

        var respuesta = new SalidaTitularidad();
        respuesta.setCoincide(
                titularidad.coincide(usuarioId, cuerpo.getNombreCompleto(), cuerpo.getDocumento(), sesion.actual()));
        return ResponseEntity.ok(respuesta);
    }

    /** Cada quien lee su nivel; el de otra persona lo leen solo backoffice o el sistema. */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<NivelKyc> consultarNivelKyc(UUID usuarioId) {
        Traza.marcarCasoDeUso("CU-02", "nivel-kyc");
        var nivel = nivelesKyc.ejecutar(usuarioId, sesion.actual());
        return ResponseEntity.ok(new NivelKyc(usuarioId, NivelKyc.NivelEnum.fromValue(nivel)));
    }

    @Override
    @Publico("CU-01: el alta ocurre antes de que exista la sesion")
    public ResponseEntity<SalidaRegistro> registrarUsuario(UUID idempotencyKey, EntradaRegistro cuerpo) {
        Traza.marcarCasoDeUso("CU-01", String.valueOf(cuerpo.getTelefonoE164()));

        var salida = cu01.ejecutar(MapeoDelAlta.mapear(cuerpo, pimienta, ip(), agente()), contextoDelAlta());

        var paraLasFotos = tokenDeAlta.emitir(salida.usuarioId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new SalidaRegistro(
                                salida.usuarioId(),
                                SalidaRegistro.EstadoEnum.fromValue(
                                        salida.estado().name()))
                        .tokenDeAlta(paraLasFotos.token())
                        .tokenDeAltaVenceEn(paraLasFotos.venceEn().atOffset(ZoneOffset.UTC)));
    }

    @Override
    @Publico("CU-01: el codigo se solicita antes de crear la cuenta")
    public ResponseEntity<VerificacionCorreoSolicitada> solicitarVerificacionCorreo(
            UUID idempotencyKey, SolicitudVerificacionCorreo cuerpo) {
        Traza.marcarCasoDeUso("CU-01", "correo");
        var salida = solicitarCorreo.ejecutar(cuerpo.getCorreo(), idempotencyKey, ip(), agente(), contextoDelAlta());
        return ResponseEntity.accepted()
                .body(new VerificacionCorreoSolicitada(
                        salida.verificacionId(), salida.destinoEnmascarado(), salida.expiraEn()));
    }

    @Override
    @Publico("CU-01: el codigo se confirma antes de crear la cuenta")
    public ResponseEntity<VerificacionCorreoConfirmada> confirmarVerificacionCorreo(
            UUID verificacionId, ConfirmacionVerificacionCorreo cuerpo) {
        Traza.marcarCasoDeUso("CU-01", "confirmar-correo");
        confirmarCorreo.ejecutar(
                verificacionId, cuerpo.getCorreo(), cuerpo.getCodigo(), ip(), agente(), contextoDelAlta());
        return ResponseEntity.ok(new VerificacionCorreoConfirmada(true));
    }

    /**
     * El alta es la unica operacion que corre sin sesion previa: el contexto es el
     * del sistema, y las politicas de fila del rol {@code sistema} son las que
     * permiten escribir la fila de alguien que todavia no existe.
     */
    private ContextoSesion contextoDelAlta() {
        return ContextoSesion.deSistema(PROCESO_DE_ALTA, new bo.aportaya.plataforma.dominio.Traza(Traza.actual()));
    }

    private String ip() {
        return Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0");
    }

    private String agente() {
        return Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("desconocido");
    }
}
