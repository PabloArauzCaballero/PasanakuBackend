package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.FiltroDeUsuariosInternos;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado paginado de personal interno (GU-17): como mucho {@code limite} filas, cursor {@code (antesDe, antesDeId)}.
 *
 * <p>Un cursor a medias se rechaza en vez de ignorarse: ignorarlo devolveria la primera pagina y el cliente la
 * tomaria por la siguiente, repitiendo filas sin saberlo.
 */
@Service
public class ListarUsuariosInternos {

    /** AP-CU08-09: el cursor llego con una sola de sus dos mitades. */
    static final CodigoError FILTRO_DE_LISTADO_INVALIDO = CodigoError.de(8, 9);

    private final Datos datos;
    private final UsuariosInternosRepositorio repositorio;
    private final Reloj reloj;

    public ListarUsuariosInternos(Datos datos, UsuariosInternosRepositorio repositorio, Reloj reloj) {
        this.datos = datos;
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<UsuarioInternoResumen> listar(
            String estado,
            String rol,
            String busqueda,
            Integer limite,
            OffsetDateTime antesDe,
            UUID antesDeId,
            ContextoSesion ctx) {
        if ((antesDe == null) != (antesDeId == null)) {
            throw new ErrorDeNegocio(
                    FILTRO_DE_LISTADO_INVALIDO,
                    "El cursor del listado va con antesDe y antesDeId juntos, o sin ninguno.");
        }
        FiltroDeUsuariosInternos filtro = new FiltroDeUsuariosInternos(
                estado,
                rol,
                busqueda,
                limite == null ? FiltroDeUsuariosInternos.FILAS_POR_PAGINA : limite,
                antesDe,
                antesDeId);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> repositorio.listar(dsl, filtro, ahora));
    }
}
