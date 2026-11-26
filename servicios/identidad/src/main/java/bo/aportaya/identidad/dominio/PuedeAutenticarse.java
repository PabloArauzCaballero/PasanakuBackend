package bo.aportaya.identidad.dominio;

import java.util.Optional;
import java.util.Set;

/**
 * La regla unica de si una cuenta puede abrir o renovar una sesion, segun
 * {@code usuario.estado} (F1.S3.M1 / IDCU-SEG-075).
 *
 * <p>Antes el ingreso y la renovacion solo miraban {@code bloqueo_cuenta}: una cuenta
 * SUSPENDIDA, BLOQUEADA o dada de BAJA volvia a entrar con su clave y seguia renovando
 * su refresh. La lista es <b>blanca</b>: un estado nuevo que nadie clasifico no entra
 * (falla cerrada). Los estados del alta y del KYC si entran, porque completar el alta y
 * la verificacion de identidad se hace con sesion; abrir la sesion no aprueba nada.
 *
 * <p>Fuente de los valores: {@code ck_usuario_estado} en
 * {@code sql/10_tablas/01_identidad_usuarios/usuario.sql}.
 */
public final class PuedeAutenticarse {

    /** Pueden abrir y renovar sesion. Todo lo demas, no. */
    static final Set<String> HABILITADOS = Set.of("ACTIVO", "PRE_REGISTRO", "INCOMPLETO_KYC", "PENDIENTE_VERIFICACION");

    private PuedeAutenticarse() {}

    /** {@code true} solo si la cuenta existe y su estado esta en la lista blanca. */
    public static boolean segun(Optional<String> estado) {
        return estado.map(HABILITADOS::contains).orElse(false);
    }

    /**
     * CU-134 (D-6): un participante con la cuenta {@code SUSPENDIDO} entra, pero con alcance restringido, para ver
     * su medida, defenderse, pagar y reclamar. No vale para el personal interno (CU-08 suspende operadores con el
     * mismo estado) ni para {@code BLOQUEADO_SEGURIDAD}: una cuenta comprometida se atiende por canal verificado.
     */
    public static boolean conAlcanceRestringido(Optional<String> estado, boolean esOperador) {
        return !esOperador && estado.filter("SUSPENDIDO"::equals).isPresent();
    }
}
