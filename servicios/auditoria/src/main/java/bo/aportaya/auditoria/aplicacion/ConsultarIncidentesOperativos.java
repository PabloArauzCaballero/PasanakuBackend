package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.IncidenteOperativo;
import bo.aportaya.auditoria.infraestructura.IncidentesOperativosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sistemas › Incidentes: los incidentes operativos registrados, paginados. Antes la pantalla leia la fuente «no
 * disponible» aunque la tabla existia. Una pagina pasada del final vuelve vacia, no es un error.
 */
@Service
public class ConsultarIncidentesOperativos {

    public record Pagina(List<IncidenteOperativo> items, int pagina, int limite, int total) {
        public int totalPaginas() {
            return total == 0 ? 0 : (total + limite - 1) / limite;
        }
    }

    private final Datos datos;
    private final IncidentesOperativosRepositorio repositorio;

    public ConsultarIncidentesOperativos(Datos datos, IncidentesOperativosRepositorio repositorio) {
        this.datos = datos;
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public Pagina listar(
            Optional<String> estado, Optional<String> severidad, int pagina, int limite, ContextoSesion ctx) {
        int desde = (pagina - 1) * limite;
        return datos.conContexto(
                ctx,
                dsl -> new Pagina(
                        repositorio.pagina(dsl, estado, severidad, desde, limite),
                        pagina,
                        limite,
                        repositorio.contar(dsl, estado, severidad)));
    }
}
