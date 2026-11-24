package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lo que un rol personalizado tiene que cumplir para existir (GU-16, CU-135). Puro: no lee la base ni el reloj.
 *
 * <p>Un rol personalizado es siempre {@code GLOBAL} (A4: los de grupo son de la app y no se arman desde el backoffice)
 * y nunca se llama como una identidad de servicio ni como un rol de participante. Su nivel de riesgo no se inventa: sale
 * de {@code permiso.nivel_riesgo} del catalogo, y un rol con algun permiso {@code ALTO} o {@code CRITICO} se cambia a
 * cuatro ojos (A1: no hay una clasificacion «dinero/identidad» con fuente, asi que rige el criterio mas estricto que si
 * tiene fuente).
 */
public final class ReglasDeRolPersonalizado {

    public static final int CU = 135;
    public static final CodigoError CODIGO_INVALIDO = CodigoError.de(CU, 1);
    public static final CodigoError CODIGO_EN_USO = CodigoError.de(CU, 2);
    public static final CodigoError SIN_PERMISOS = CodigoError.de(CU, 3);
    public static final CodigoError PERMISO_INEXISTENTE = CodigoError.de(CU, 4);
    public static final CodigoError PAR_INCOMPATIBLE = CodigoError.de(CU, 5);
    public static final CodigoError ROL_DE_SISTEMA = CodigoError.de(CU, 6);
    public static final CodigoError ROL_INEXISTENTE = CodigoError.de(CU, 7);
    public static final CodigoError MOTIVO_REQUERIDO = CodigoError.de(CU, 8);
    public static final CodigoError STEP_UP_REQUERIDO = CodigoError.de(CU, 9);
    public static final CodigoError CON_ASIGNACIONES_VIGENTES = CodigoError.de(CU, 10);
    public static final CodigoError SOLICITUD_INEXISTENTE = CodigoError.de(CU, 11);
    public static final CodigoError APROBADOR_ES_EL_PROPONENTE = CodigoError.de(CU, 12);
    public static final CodigoError ASIGNADO_QUEDARIA_EN_CONFLICTO = CodigoError.de(CU, 13);
    public static final CodigoError SOLICITUD_YA_RESUELTA = CodigoError.de(CU, 14);

    /** El ambito unico de un rol personalizado. */
    public static final String AMBITO = AmbitoDeRol.GLOBAL.name();

    public static final int LARGO_MAXIMO_CODIGO = 30;
    public static final int LARGO_MAXIMO_NOMBRE = 60;
    public static final int LARGO_MAXIMO_DESCRIPCION = 255;
    public static final int LARGO_MAXIMO_MOTIVO = 160;

    private static final Pattern FORMA_DEL_CODIGO = Pattern.compile("^[A-Z][A-Z0-9_]{2,29}$");
    private static final Set<String> RIESGO_DE_CUATRO_OJOS = Set.of("ALTO", "CRITICO");

    private ReglasDeRolPersonalizado() {}

    /** El codigo que se puede escribir: forma, y ni identidad de servicio ni rol de participante. */
    public static boolean codigoValido(String codigo) {
        return codigo != null
                && FORMA_DEL_CODIGO.matcher(codigo).matches()
                && RolesParaPersonalInterno.admite(codigo, AMBITO);
    }

    /** El par incompatible que el conjunto reuniria, si lo hay (R-SEG-04, lote A). */
    public static Optional<String> conflicto(Set<String> permisos) {
        return SegregacionDeFunciones.conflicto(permisos);
    }

    /** Cuatro ojos si algun permiso del conjunto es {@code ALTO} o {@code CRITICO} segun el catalogo. */
    public static boolean exigeCuatroOjos(Set<String> permisos, Map<String, String> riesgoPorPermiso) {
        return permisos.stream().map(riesgoPorPermiso::get).anyMatch(RIESGO_DE_CUATRO_OJOS::contains);
    }

    public static boolean motivoValido(String motivo) {
        return motivo != null && !motivo.isBlank() && motivo.length() <= LARGO_MAXIMO_MOTIVO;
    }
}
