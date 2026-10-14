package bo.aportaya.plataforma.pruebas.seguridad;

import java.util.Map;

/**
 * Las decisiones de seguridad que SOLO una persona puede tomar, escritas una vez y a la
 * vista. La matriz las compara con lo que el codigo hace: si el codigo cambia sin que
 * cambie esta tabla, la prueba cae y obliga a decidirlo de nuevo.
 *
 * <p>Que algo figure aca no lo vuelve correcto: lo vuelve <b>declarado</b>. Lo que esta
 * en {@link #AMBITO} con estado {@code ABIERTO} es un hallazgo con duenio, no una
 * aprobacion.
 */
final class PoliticaDeAcceso {

    private PoliticaDeAcceso() {}

    /**
     * El control que compensa que una ruta no pida sesion.
     *
     * @param archivo donde vive el control, relativo a la raiz del repositorio
     * @param patron lo que ese archivo tiene que contener para que el control exista
     * @param porQue la razon por la que la ruta es publica
     */
    record ControlCompensatorio(String archivo, String patron, String porQue) {}

    /** Una ruta publica de lectura no necesita control compensatorio: no cambia nada. */
    static final Map<String, String> PUBLICAS_DE_LECTURA = Map.of(
            "listarContratosVigentes", "contenido ya publicado en el sitio; ningun dato de nadie (CU-05)",
            "consultarPaqueteDelSorteo", "el paquete se publica para poder rehacer el sorteo desde afuera (CU-61)",
            "verificarSorteo", "verificacion publica del sorteo (CU-61)",
            "verificarCadena", "verificacion publica de la cadena de transparencia (CU-73)",
            "listarBloques", "los eslabones se publican para auditarlos (CU-72)",
            "verificarCertificado", "un certificado se verifica por su codigo, sin cuenta (CU-75)",
            "consultarEstadoDeVerificacion",
                    "lee sin sesion obligatoria porque quien se acaba de registrar aun no puede abrirla (CU-02); "
                            + "el usuarioId de la URL no autoriza nada: exige el token de alta de ESE usuario o su sesion, "
                            + "y todo otro caso es el mismo 403 (TitularDelExpediente)");

    /** Una ruta publica que CAMBIA estado: sin sesion, algo tiene que autenticar o limitar. */
    static final Map<String, ControlCompensatorio> PUBLICAS_QUE_ESCRIBEN = Map.of(
            "recibirWebhookPasarela",
            new ControlCompensatorio(
                    "servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/VerificadorDeFirmaWebhook.java",
                    "HmacSHA256",
                    "la pasarela no tiene JWT nuestro; la firma HMAC es la autenticacion"),
            "procesarRespuestaEntrante",
            new ControlCompensatorio(
                    "servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/FirmaDeWebhook.java",
                    "Hmac",
                    "el proveedor de mensajeria no tiene sesion; lo autentica su firma"),
            "autenticar",
            new ControlCompensatorio(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/CU04Autenticar.java",
                    "(?i)intentos",
                    "el ingreso es el momento en que se obtiene la sesion; limita por intentos"),
            "registrarUsuario",
            new ControlCompensatorio(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/CU01RegistrarUsuario.java",
                    "vincularAlAlta",
                    "el alta ocurre antes de que exista la sesion; exige el correo verificado"),
            "solicitarVerificacionCorreo",
            new ControlCompensatorio(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/VerificacionCorreoRepositorio.java",
                    "max_reenvios_por_hora",
                    "se pide el codigo antes de existir como usuario; tope de reenvios por hora"),
            "confirmarVerificacionCorreo",
            new ControlCompensatorio(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/VerificacionCorreoRepositorio.java",
                    "max_intentos",
                    "se confirma antes de existir como usuario; el codigo se invalida al agotar intentos"),
            "subirDocumento",
            new ControlCompensatorio(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/web/TitularDelExpediente.java",
                    "AccessDeniedException",
                    "las fotos se suben durante el alta, sin sesion; exige el token de alta de ESE usuario o su sesion"),
            "renovarSesion",
            new ControlCompensatorio(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/web/CookieDeRefresco.java",
                    "sameSite\\(\"Strict\"\\)",
                    "ADR-010: la credencial es la cookie de refresh (HttpOnly, Secure, SameSite=Strict contra CSRF), "
                            + "rotada y de un solo uso; no hay token de acceso que presentar porque es lo que se renueva"));

    // No hay lista de «publicas que escriben sin control»: el ultimo hallazgo (subirDocumento) se
    // cerro con el token de alta. Una ruta publica que escribe y no figura arriba rompe la matriz.

    /** Quien responde por el que una operacion sobre «una persona» no cruce a otra. */
    enum Estado {
        /** El codigo lo verifica: el archivo contiene el patron. */
        CONTROL,
        /** Lo sostiene la politica de fila de la base (R-SEG, restricciones.sql). */
        POLITICA_DE_FILA,
        /** Falta el control, o no se pudo demostrar: tiene duenio y esta en la evidencia. */
        ABIERTO
    }

    record Ambito(Estado estado, String archivo, String patron, String nota) {

        static Ambito control(String archivo, String patron, String nota) {
            return new Ambito(Estado.CONTROL, archivo, patron, nota);
        }

        static Ambito filaDeLaBase(String nota) {
            return new Ambito(Estado.POLITICA_DE_FILA, "sql/40_reglas/restricciones.sql", "fn_seg_aplicar_rls", nota);
        }

        static Ambito abierto(String nota) {
            return new Ambito(Estado.ABIERTO, null, null, nota);
        }
    }

    /**
     * Operaciones con identificador de persona bajo un permiso que tiene CUALQUIER
     * participante (PARTICIPANTE, BILLETERA_*): ahi el rol correcto sobre el recurso
     * ajeno sigue siendo acceso indebido (regla 90.1.2). Las de funcion —soporte,
     * analista, administrador— se autorizan por el permiso mismo y no figuran.
     */
    static final Map<String, Ambito> AMBITO = Map.of(
            "declararPep",
            Ambito.control(
                    "servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/AdhesionController.java",
                    "soloElPropio",
                    "H13.S1.M2: cada quien declara lo suyo; el analista no declara por el usuario"),
            "aceptarContrato",
            Ambito.control(
                    "servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/AdhesionController.java",
                    "soloElPropio",
                    "H13.S1.M2: la aceptacion es evidencia legal de quien acepta"),
            "consultarRestriccion",
            Ambito.control(
                    "servicios/garantia/src/main/java/bo/aportaya/garantia/web/CobranzaController.java",
                    "usuarioId.equals\\(sesion.actual\\(\\).usuarioId\\(\\)\\)",
                    "consulta lo suyo; consultar a otro exige GRUPO_ADMINISTRAR"),
            "consultarNivelKyc",
            Ambito.control(
                    "servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/ConsultarNivelKyc.java",
                    "usuarioId.equals\\(ctx.usuarioId\\(\\)\\)",
                    "lo suyo, o un rol lector declarado"),
            "emitirCertificado",
            Ambito.control(
                    "servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU75EmitirCertificado.java",
                    "usuarioId\\(\\).equals\\(ctx.usuarioId\\(\\)\\)",
                    "el certificado es del titular"),
            "consultarHabilitacionPersonal",
            Ambito.control(
                    "servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/ConsultarHabilitacion.java",
                    "usuarioId.equals\\(ctx.usuarioId\\(\\)\\)",
                    "solo la propia habilitacion; el backoffice consulta la de otros"),
            "consultarPuntaje",
            Ambito.filaDeLaBase(
                    "transparencia.puntaje_reputacion lleva usuario_id: la politica de fila lo acota al titular"),
            "solicitarRetiro",
            Ambito.filaDeLaBase(
                    "grupos.participante lleva usuario_id: el participante ajeno no existe para quien pregunta"),
            "consultarActividad",
            Ambito.filaDeLaBase(
                    "cuenta solo las filas que la politica de fila le deja ver: otro usuario devuelve cero"),
            "consultarEstadoDelParticipante",
            Ambito.filaDeLaBase(
                    "obligacion_aporte no lleva usuario_id: la base la niega por omision a un participante"));
}
