package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.InicioDelGrupo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Una vez iniciado, el grupo no recibe a nadie mas (decision del dueño 2026-10-30): se entra SOLO con el grupo en
 * {@code ABIERTO_A_INSCRIPCION}, por cualquier camino (canje, enlace, enlace del grupo, postulacion, decision del
 * organizador, admision humana). Antes, varios caminos aceptaban tambien {@code CONFORMADO}.
 *
 * <p>Se lee con rol de sistema: quien pide entrar todavia no ve la fila del grupo por su politica, y una fila que no
 * se ve no puede leerse como «abierto». Los caminos que reservan u ocupan un cupo lo llaman con la fila del grupo YA
 * tomada ({@code FOR UPDATE}), la misma que toma el inicio: un ingreso y un inicio simultaneos no pueden pasar los dos.
 * Un grupo que no existe no se rechaza aca: el caso de uso responde lo suyo.
 */
final class GrupoAbierto {

    private GrupoAbierto() {}

    static void exigir(DSLContext dsl, UUID grupoId) {
        String estado = Datos.comoSistema(
                dsl,
                () -> dsl.fetchValue("SELECT estado FROM grupos.grupo WHERE id = ?", grupoId) instanceof String s
                        ? s
                        : null);
        if (estado != null && !InicioDelGrupo.ABIERTO.equals(estado)) {
            throw yaIniciado();
        }
    }

    /** AP-CU69-17 GRUPO_YA_INICIADO. */
    static ErrorDeNegocio yaIniciado() {
        return new ErrorDeNegocio(CodigoError.de(69, 17), "Este grupo ya inicio y no recibe a nadie mas.");
    }
}
