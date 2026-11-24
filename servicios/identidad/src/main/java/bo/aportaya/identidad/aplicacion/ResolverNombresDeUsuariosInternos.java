package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.NombresDeUsuariosInternosRepositorio;
import bo.aportaya.identidad.infraestructura.NombresDeUsuariosInternosRepositorio.Nombre;
import bo.aportaya.identidad.web.generado.modelo.NombreDeUsuarioInterno;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El nombre de cada operador interno de un lote, para que el backoffice muestre quien propuso y quien aprobo
 * (CU-132) en vez de un identificador.
 *
 * <p>Nombres y apellidos ({@link ConsultarMiPerfil#nombreCompleto}) y no «Nombre A.»: el control de cuatro ojos
 * solo sirve si se distingue sin ambiguedad a dos colegas, y en un equipo chico dos «Carlos M.» son probables. Es
 * el nombre de un empleado firmando en ejercicio de su funcion, no el dato de un cliente.
 *
 * <p>Un id que no es usuario interno activo se OMITE, sin decir si existe: la respuesta de un cliente, de un
 * suspendido y de un id inventado es la misma. El limite del lote (50) lo pone el contrato; los repetidos se
 * rechazan aca porque el contrato no puede expresarlo sin volver la lista un conjunto.
 */
@Service
public class ResolverNombresDeUsuariosInternos {

    static final CodigoError LOTE_CON_REPETIDOS = CodigoError.de(8, 7);

    private final Datos datos;
    private final NombresDeUsuariosInternosRepositorio repositorio;

    public ResolverNombresDeUsuariosInternos(Datos datos, NombresDeUsuariosInternosRepositorio repositorio) {
        this.datos = datos;
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<NombreDeUsuarioInterno> ejecutar(List<UUID> usuarioIds, ContextoSesion ctx) {
        var unicos = new LinkedHashSet<>(usuarioIds);
        if (unicos.size() != usuarioIds.size()) {
            throw new ErrorDeNegocio(LOTE_CON_REPETIDOS, "La lista repite un identificador de operador.");
        }
        Map<UUID, Nombre> nombres = datos.conContexto(ctx, dsl -> repositorio.activos(dsl, unicos));
        List<NombreDeUsuarioInterno> salida = new ArrayList<>();
        for (UUID id : usuarioIds) {
            Nombre n = nombres.get(id);
            String nombre = n == null ? null : ConsultarMiPerfil.nombreCompleto(n.nombres(), n.apellidos());
            if (nombre != null) {
                salida.add(new NombreDeUsuarioInterno(id, nombre));
            }
        }
        return salida;
    }
}
