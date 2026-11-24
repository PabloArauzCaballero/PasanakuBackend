package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import java.util.Set;

/**
 * Que roles se le pueden otorgar a una persona del personal interno (GU-13).
 *
 * <p>El catalogo mezcla tres poblaciones: el personal de la plataforma, los participantes de la app
 * ({@code PARTICIPANTE}, {@code ORGANIZADOR}, y todo rol de ambito {@code GRUPO}, que solo tiene sentido
 * dentro de un grupo) y los que no son personas internas ({@code ANUNCIANTE} y las identidades de servicio
 * {@code SERVICIO_*}). Ofrecerle a un operador un rol de participante, o asignarselo como GLOBAL, le da
 * permisos sobre billeteras y grupos que su funcion no pide.
 *
 * <p>Las identidades de servicio se reconocen por su prefijo a proposito: una nueva
 * ({@code SERVICIO_X}) queda afuera sin que nadie se acuerde de agregarla, que es el lado seguro del error.
 */
public final class RolesParaPersonalInterno {

    /** AP-CU08-08: el rol existe pero no se le otorga a personal interno. */
    public static final CodigoError ROL_NO_ASIGNABLE = CodigoError.de(8, 8);

    private static final Set<String> AJENOS_AL_PERSONAL = Set.of("PARTICIPANTE", "ORGANIZADOR", "ANUNCIANTE");
    private static final String PREFIJO_DE_SERVICIO = "SERVICIO_";

    private RolesParaPersonalInterno() {}

    public static boolean admite(String codigo, String ambito) {
        return !AmbitoDeRol.GRUPO.name().equals(ambito)
                && !AJENOS_AL_PERSONAL.contains(codigo)
                && !codigo.startsWith(PREFIJO_DE_SERVICIO);
    }
}
