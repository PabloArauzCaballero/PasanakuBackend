package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU60Sortear;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.web.generado.modelo.CompromisoDeSorteo;
import bo.aportaya.grupos.web.generado.modelo.EntradaCompromiso;
import bo.aportaya.grupos.web.generado.modelo.EntradaRevelacion;
import bo.aportaya.grupos.web.generado.modelo.RevelacionDeSorteo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Las dos fases del sorteo: comprometer y revelar.
 *
 * <p>El compromiso publica **solo el hash**. La semilla queda sellada del lado del servidor junto al snapshot
 * del plantel, el calendario y las reglas: ningun cliente la conoce ni puede probar semillas hasta que le
 * guste el orden, y revelar de nuevo devuelve el resultado original en vez de sortear otra vez.
 *
 * <p>Ambas operaciones exigen ser administrador del grupo de la ruta (o backoffice), y el sorteo tiene que ser
 * de ESE grupo: un sorteoId ajeno por la ruta de otro grupo no se toca.
 */
@Component
class SorteoDelGrupo {

    private final CU60Sortear cu60;
    private final Consultas consultas;

    SorteoDelGrupo(CU60Sortear cu60, Consultas consultas) {
        this.cu60 = cu60;
        this.consultas = consultas;
    }

    ResponseEntity<CompromisoDeSorteo> comprometer(UUID grupoId, EntradaCompromiso cuerpo, ContextoSesion ctx) {
        exigirAdministrador(grupoId, ctx);
        var compromiso = cu60.comprometer(grupoId, entropias(cuerpo), Optional.empty(), ctx);

        var respuesta = new CompromisoDeSorteo();
        respuesta.setSorteoId(compromiso.sorteoId());
        respuesta.setHashSemilla(compromiso.hashSemilla());
        respuesta.setSemilla(compromiso.semilla());
        respuesta.setAlgoritmo(CompromisoDeSorteo.AlgoritmoEnum.FISHER_YATES_SHA256);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    ResponseEntity<RevelacionDeSorteo> revelar(UUID grupoId, EntradaRevelacion cuerpo, ContextoSesion ctx) {
        exigirAdministrador(grupoId, ctx);
        UUID sorteoId = cuerpo.getSorteoId();
        // Ni existencia ni pertenencia se distinguen hacia afuera: es el mismo "ese sorteo no existe".
        if (!cu60.grupoDelSorteo(sorteoId, ctx).filter(grupoId::equals).isPresent()) {
            throw new ErrorDeNegocio(CodigoError.de(60, 2), "Ese sorteo no existe.");
        }
        var original = cu60.resultadoVigente(sorteoId, ctx);
        var revelacion = original.isPresent() ? original.get() : revelarUnaVez(sorteoId, cuerpo, ctx);

        var respuesta = new RevelacionDeSorteo();
        respuesta.setSorteoId(revelacion.sorteoId());
        respuesta.setVerificado(revelacion.verificado());
        respuesta.setSemilla(revelacion.semilla());
        respuesta.setCuposEnOrden(revelacion.cuposEnOrden());
        return ResponseEntity.ok(respuesta);
    }

    /** Un reintento simultaneo pierde la carrera y recibe el resultado del que gano, no un error. */
    private CU60Sortear.Revelacion revelarUnaVez(UUID sorteoId, EntradaRevelacion cuerpo, ContextoSesion ctx) {
        try {
            return cu60.revelar(
                    sorteoId,
                    null,
                    cuerpo.getEntropias() == null || cuerpo.getEntropias().isEmpty() ? null : cuerpo.getEntropias(),
                    List.of(),
                    null,
                    Optional.empty(),
                    ctx);
        } catch (ErrorDeNegocio yaRevelado) {
            if ("AP-CU60-07".equals(yaRevelado.codigo().valor())) {
                return cu60.resultadoVigente(sorteoId, ctx).orElseThrow(() -> yaRevelado);
            }
            throw yaRevelado;
        }
    }

    private void exigirAdministrador(UUID grupoId, ContextoSesion ctx) {
        if (!consultas.puedeAdministrar(grupoId, ctx)) {
            throw new ErrorDeNegocio(CodigoError.de(60, 10), "No administras este grupo.");
        }
    }

    private static List<String> entropias(EntradaCompromiso cuerpo) {
        return cuerpo == null || cuerpo.getEntropias() == null ? List.of() : cuerpo.getEntropias();
    }
}
