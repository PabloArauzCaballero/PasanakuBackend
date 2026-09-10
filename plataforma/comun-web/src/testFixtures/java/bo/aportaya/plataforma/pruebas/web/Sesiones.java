package bo.aportaya.plataforma.pruebas.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Fabrica la sesion de una peticion de prueba, con los mismos reclamos que emite
 * {@code identidad} (ADR-024) y sin firmar nada.
 *
 * <p>No se firma un JWT de verdad a proposito. Hacerlo ataria la prueba de la capa web
 * de trece servicios al emisor de un catorceavo: cambiar el algoritmo de firma en
 * {@code identidad} rompería suites que no tienen nada que ver con la firma. Que la
 * firma se valide bien es lo que prueba {@code ArranqueTest}, contra el decodificador
 * real.
 *
 * <p>Lo que si es real es lo que la sesion <b>significa</b>: los mismos nombres de
 * reclamo que lee {@code SesionDeLaPeticion}. Un nombre distinto aca daria una suite
 * verde con un token que en produccion no autoriza.
 */
public final class Sesiones {

    /** El usuario de las pruebas. Fijo: un UUID aleatorio hace fallar assertions por turnos. */
    public static final UUID USUARIO = UUID.fromString("11111111-1111-4111-8111-111111111111");

    public static final UUID OTRO_USUARIO = UUID.fromString("22222222-2222-4222-8222-222222222222");

    /**
     * Un rol que el catalogo no tiene y unos permisos vacios.
     *
     * <p>Es la sesion con la que se comprueba que <b>todo</b> endpoint con
     * {@code @Permiso} responde {@code 403}: cualquier rol real satisfaria por
     * casualidad a alguno de los treinta y dos controladores, y esa casualidad
     * convertiria un agujero en una prueba verde.
     */
    public static final String ROL_SIN_NADA = "ROL_QUE_NO_EXISTE_EN_EL_CATALOGO";

    private Sesiones() {}

    /** Sesion de {@link #USUARIO} con ese rol y esos permisos efectivos. */
    public static RequestPostProcessor como(String rol, String... permisos) {
        return de(USUARIO, rol, permisos);
    }

    /** Sesion de otro usuario: para probar que el dueño del recurso importa (IDOR). */
    public static RequestPostProcessor comoOtro(String rol, String... permisos) {
        return de(OTRO_USUARIO, rol, permisos);
    }

    /** Autenticado y sin poder hacer nada. La sesion del {@code 403}. */
    public static RequestPostProcessor sinPermisos() {
        return de(USUARIO, ROL_SIN_NADA);
    }

    public static RequestPostProcessor de(UUID usuario, String rol, String... permisos) {
        return jwt().jwt(token -> reclamos(token, usuario.toString(), rol, List.of(permisos)));
    }

    /**
     * Un token verificado al que le falta un reclamo obligatorio.
     *
     * <p>Existe porque la firma valida y el contenido util son dos cosas distintas:
     * {@code SesionDeLaPeticion} tiene que rechazar un token sin sujeto o sin rol, y sin
     * esto ese camino no lo ejerce nadie.
     */
    public static RequestPostProcessor sinRol() {
        return jwt().jwt(token -> reclamos(token, USUARIO.toString(), null, List.of()));
    }

    /** Un sujeto que no es un identificador de usuario. */
    public static RequestPostProcessor conSujetoInvalido() {
        return jwt().jwt(token -> reclamos(token, "no-soy-un-uuid", "PARTICIPANTE", List.of()));
    }

    private static void reclamos(Jwt.Builder token, String sujeto, String rol, List<String> permisos) {
        token.subject(sujeto).claim("permisos", permisos).claim("dispositivo", "dispositivo-de-prueba");
        if (rol != null) {
            token.claim("rol", rol);
        }
    }
}
