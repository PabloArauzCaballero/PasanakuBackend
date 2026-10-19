package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio.RegistroEvento;
import bo.aportaya.auditoria.web.generado.modelo.EventoBitacora;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeBitacora;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: consulta la bitacora inmutable del sistema (comun.bitacora_evento).
 * Registro append-only encadenado por hash SHA-256 para auditoria y cumplimiento regulatorio.
 */
@Service
public class ConsultarBitacora {

    private final Datos datos;
    private final BitacoraRepositorio bitacoraRepositorio;
    private final ObjectMapper mapper;

    public ConsultarBitacora(Datos datos, BitacoraRepositorio bitacoraRepositorio, ObjectMapper mapper) {
        this.datos = datos;
        this.bitacoraRepositorio = bitacoraRepositorio;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginaDeBitacora listar(
            String entidad,
            String accion,
            UUID actorUsuarioId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Integer limiteParam,
            OffsetDateTime antesDe,
            UUID antesDeId,
            ContextoSesion ctx) {
        int limite = limiteParam != null ? Math.min(Math.max(limiteParam, 1), 100) : 25;

        List<RegistroEvento> registros = datos.conContexto(
                ctx,
                dsl -> bitacoraRepositorio.listar(
                        dsl, entidad, accion, actorUsuarioId, desde, hasta, antesDe, antesDeId, limite));

        List<EventoBitacora> items = registros.stream().map(this::aEventoBitacora).toList();

        PaginaDeBitacora pagina = new PaginaDeBitacora(items);
        if (registros.size() == limite) {
            RegistroEvento ultimo = registros.get(registros.size() - 1);
            pagina.setProximoAntesDe(ultimo.fechaHora());
            pagina.setProximoAntesDeId(ultimo.id());
        }
        pagina.setTotalAproximado(items.size());
        return pagina;
    }

    @Transactional(readOnly = true)
    public Optional<EventoBitacora> buscarPorId(UUID id, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> bitacoraRepositorio.buscarPorId(dsl, id))
                .map(this::aEventoBitacora);
    }

    private EventoBitacora aEventoBitacora(RegistroEvento r) {
        EventoBitacora e = new EventoBitacora(
                r.id(),
                r.secuencia(),
                r.entidad(),
                r.entidadId(),
                r.accion(),
                r.origen(),
                r.correlationId(),
                r.hashRegistro(),
                r.fechaHora());
        e.setActorUsuarioId(r.actorUsuarioId());
        e.setActorRol(r.actorRol());
        e.setIpOrigen(r.ipOrigen());
        e.setAgenteUsuario(r.agenteUsuario());
        e.setValorAnterior(parsearJson(r.valorAnteriorJson()));
        e.setValorNuevo(parsearJson(r.valorNuevoJson()));
        e.setCamposModificados(r.camposModificados());
        e.setMotivo(r.motivo());
        e.setHashAnterior(r.hashAnterior());
        return e;
    }

    private Object parsearJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return json;
        }
    }
}
