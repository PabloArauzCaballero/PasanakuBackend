package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.BuscarPorTelefono;
import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.CU02GuardarFotoDelExpediente;
import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.ValidarTokenDeInvitacion;
import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.dominio.CanalDeVerificacion;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.web.generado.UsuariosApi;
import bo.aportaya.identidad.web.generado.modelo.ArchivoDelExpediente;
import bo.aportaya.identidad.web.generado.modelo.EntradaRegistro;
import bo.aportaya.identidad.web.generado.modelo.EntradaTitularidad;
import bo.aportaya.identidad.web.generado.modelo.EntradaTokenDeInvitacion;
import bo.aportaya.identidad.web.generado.modelo.EntradaValidacionInvitacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaRegistro;
import bo.aportaya.identidad.web.generado.modelo.SalidaTitularidad;
import bo.aportaya.identidad.web.generado.modelo.SalidaTokenDeInvitacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaValidacionInvitacion;
import bo.aportaya.identidad.web.generado.modelo.UsuarioEncontrado;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * La pagina de CU-01: traduce y delega, sin logica.
 *
 * <p>Devuelve {@code 202} y no {@code 201}. **No promete la billetera**: esa la abre
 * `nucleo-financiero` al consumir el evento, y decir aca que ya esta abierta seria
 * decirle al cliente que tiene algo que todavia nadie abrio.
 */
@RestController
public class UsuariosController implements UsuariosApi {

    /** El «usuario» del contexto mientras todavia no hay usuario. */
    private static final UUID PROCESO_DE_ALTA = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final CU01RegistrarUsuario cu01;
    private final VerificarTitularidad titularidad;
    private final CU02GuardarFotoDelExpediente fotos;
    private final EmitirTokenDeInvitacion tokens;
    private final ValidarTokenDeInvitacion validacionDeInvitacion;
    private final BuscarPorTelefono busqueda;
    private final SesionDeLaPeticion sesion;
    private final HttpServletRequest peticion;
    private final String pimienta;

    public UsuariosController(
            CU01RegistrarUsuario cu01,
            VerificarTitularidad titularidad,
            CU02GuardarFotoDelExpediente fotos,
            EmitirTokenDeInvitacion tokens,
            ValidarTokenDeInvitacion validacionDeInvitacion,
            BuscarPorTelefono busqueda,
            SesionDeLaPeticion sesion,
            HttpServletRequest peticion,
            @Value("${aportaya.seguridad.pimienta}") String pimienta) {
        this.cu01 = cu01;
        this.titularidad = titularidad;
        this.fotos = fotos;
        this.tokens = tokens;
        this.validacionDeInvitacion = validacionDeInvitacion;
        this.busqueda = busqueda;
        this.sesion = sesion;
        this.peticion = peticion;
        this.pimienta = pimienta;
    }

    /**
     * Si el nombre y el documento declarados son los del titular.
     *
     * <p>Contesta si coinciden y nada mas. Devolver los guardados haria de esta ruta un
     * directorio de clientes para cualquier servicio comprometido.
     */
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

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<SalidaTokenDeInvitacion> emitirTokenDeInvitacion(
            UUID idempotencyKey, EntradaTokenDeInvitacion cuerpo) {
        Traza.marcarCasoDeUso("CU-69", cuerpo.getCanal().getValue());
        String agente = Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("grupos");

        var emitido = tokens.ejecutar(
                cuerpo.getCanal().getValue(),
                cuerpo.getDestinoEnmascarado(),
                idempotencyKey,
                Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0"),
                agente.substring(0, Math.min(255, agente.length())),
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

    @Override
    @Permiso("DATOS_SENSIBLES_LEER")
    public ResponseEntity<SalidaTitularidad> verificarTitularidad(UUID usuarioId, EntradaTitularidad cuerpo) {
        Traza.marcarCasoDeUso("CU-01", usuarioId.toString());

        var respuesta = new SalidaTitularidad();
        respuesta.setCoincide(
                titularidad.coincide(usuarioId, cuerpo.getNombreCompleto(), cuerpo.getDocumento(), sesion.actual()));
        return ResponseEntity.ok(respuesta);
    }

    /**
     * La foto del documento o la prueba de vida. Va al servidor de archivos y en la
     * fila queda la clave del objeto: el binario nunca se sirve directo ni se guarda
     * en el disco del contenedor, que se reemplaza — y la evidencia legal no.
     *
     * <p>Publica por el mismo motivo que el alta: estas fotos se sacan durante el
     * registro, cuando todavia no hay sesion que presentar.
     */
    @Override
    @Publico("CU-02: las fotos del expediente se sacan durante el alta, sin sesion")
    public ResponseEntity<ArchivoDelExpediente> subirDocumento(
            UUID usuarioId, UUID idempotencyKey, String cara, MultipartFile archivo) {
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

    @Override
    @Publico("CU-01: el alta ocurre antes de que exista la sesion")
    public ResponseEntity<SalidaRegistro> registrarUsuario(UUID idempotencyKey, EntradaRegistro cuerpo) {
        Traza.marcarCasoDeUso("CU-01", String.valueOf(cuerpo.getTelefonoE164()));

        var salida = cu01.ejecutar(mapear(cuerpo), contextoDelAlta());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new SalidaRegistro(
                        salida.usuarioId(),
                        SalidaRegistro.EstadoEnum.fromValue(salida.estado().name())));
    }

    /**
     * El alta es la unica operacion que corre sin sesion previa: el contexto es el
     * del sistema, y las politicas de fila del rol {@code sistema} son las que
     * permiten escribir la fila de alguien que todavia no existe.
     */
    private ContextoSesion contextoDelAlta() {
        return ContextoSesion.deSistema(PROCESO_DE_ALTA, new bo.aportaya.plataforma.dominio.Traza(Traza.actual()));
    }

    private CU01RegistrarUsuario.EntradaRegistro mapear(EntradaRegistro cuerpo) {
        DocumentoDeIdentidad documento = DocumentoDeIdentidad.de(
                DocumentoDeIdentidad.Tipo.valueOf(
                        tipoDelModelo(cuerpo.getDocumento().getTipo().getValue())),
                cuerpo.getDocumento().getNumero(),
                pimienta,
                "BO",
                // La extension solo la lleva el CI; el contrato la deja opcional
                // porque depende del tipo, y el dominio es el que la exige.
                cuerpo.getDocumento().getLugarExpedicion() == null
                        ? null
                        : cuerpo.getDocumento().getLugarExpedicion().getValue());
        return new CU01RegistrarUsuario.EntradaRegistro(
                cuerpo.getTelefonoE164(),
                cuerpo.getNombres(),
                cuerpo.getApellidos(),
                cuerpo.getFechaNacimiento(),
                cuerpo.getCorreo(),
                // El contrato le pone SMS por omision: quien no elige, recibe el
                // codigo en el telefono que acaba de declarar.
                cuerpo.getCanalVerificacion() == null
                        ? CanalDeVerificacion.SMS
                        : CanalDeVerificacion.valueOf(
                                cuerpo.getCanalVerificacion().getValue()),
                documento,
                // El cifrado real lo hace el adaptador de archivos; aca la frontera.
                "cifrado:" + documento.hashNumero(),
                // `char[]` y no `String`: una `String` de clave queda en el pool hasta
                // que el recolector pase, y no hay forma de borrarla antes. El caso de
                // uso limpia este arreglo apenas la hashea.
                cuerpo.getContrasena().toCharArray(),
                cuerpo.getAceptaContratos(),
                true,
                Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0"),
                Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("desconocido"));
    }

    /** El contrato dice {@code CEX}; el {@code .puml} dice {@code CARNET_EXTRANJERIA}. */
    private String tipoDelModelo(String tipoDelContrato) {
        return "CEX".equals(tipoDelContrato) ? "CARNET_EXTRANJERIA" : tipoDelContrato;
    }
}
