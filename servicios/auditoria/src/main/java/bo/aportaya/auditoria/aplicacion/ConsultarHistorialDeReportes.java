package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.HistorialDeReportesRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-58 · el catalogo de reportes y el historial de ejecuciones que lee el backoffice (carril A · A6).
 * El permiso lo exige la pagina; aca se validan estados y cursor (AP-CU58-07).
 */
@Service
public class ConsultarHistorialDeReportes {

    private static final List<String> ESTADOS = List.of("EN_COLA", "EJECUTANDO", "COMPLETADA", "FALLIDA", "EXPIRADA");

    private final Datos datos;
    private final HistorialDeReportesRepositorio historial;

    public ConsultarHistorialDeReportes(Datos datos, HistorialDeReportesRepositorio historial) {
        this.datos = datos;
        this.historial = historial;
    }

    public record Definicion(
            UUID definicionId,
            String tipo,
            String nombre,
            String descripcion,
            String permisoRequerido,
            boolean sensible,
            List<bo.aportaya.auditoria.dominio.ParametrosDeReporte.Esperado> esperados) {}

    public record Ejecucion(
            UUID ejecucionId,
            UUID definicionId,
            UUID solicitadoPor,
            String estado,
            int filas,
            int duracionMs,
            Optional<String> hash,
            OffsetDateTime iniciadaEn,
            Optional<OffsetDateTime> finalizadaEn) {}

    public record Pagina<T, C>(List<T> items, Optional<C> proximo, Optional<UUID> proximoId) {}

    @Transactional(readOnly = true)
    public Pagina<Definicion, String> definiciones(
            Optional<String> despuesDe, Optional<UUID> despuesDeId, int limite, ContextoSesion ctx) {
        exigirCursor(despuesDe, despuesDeId);
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> historial.definiciones(dsl, despuesDe.orElse(null), despuesDeId.orElse(null), limite))
                .stream()
                .map(d -> new Definicion(
                        d.id(),
                        d.tipo(),
                        d.nombre(),
                        d.descripcion(),
                        d.permisoRequerido(),
                        d.sensible(),
                        d.esperados()))
                .toList();
        Optional<Definicion> ultima = items.size() == limite ? Optional.of(items.get(limite - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(Definicion::nombre), ultima.map(Definicion::definicionId));
    }

    @Transactional(readOnly = true)
    public Pagina<Ejecucion, OffsetDateTime> ejecuciones(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = estados == null || estados.isEmpty() ? ESTADOS : estados;
        if (!ESTADOS.containsAll(filtro)) {
            throw invalido();
        }
        exigirCursor(antesDe, antesDeId);
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> historial.ejecuciones(dsl, filtro, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(e -> new Ejecucion(
                        e.id(),
                        e.definicionId(),
                        e.solicitadoPor(),
                        e.estado(),
                        e.filas(),
                        e.duracionMs(),
                        Optional.ofNullable(e.hash()),
                        e.iniciadaEn(),
                        Optional.ofNullable(e.finalizadaEn())))
                .toList();
        Optional<Ejecucion> ultima = items.size() == limite ? Optional.of(items.get(limite - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(Ejecucion::iniciadaEn), ultima.map(Ejecucion::ejecucionId));
    }

    private static void exigirCursor(Optional<?> valor, Optional<UUID> id) {
        if (valor.isPresent() != id.isPresent()) {
            throw invalido();
        }
    }

    private static ErrorDeNegocio invalido() {
        return new ErrorDeNegocio(
                CodigoError.de(58, 7), "Filtro de listado invalido: estado desconocido o cursor a medias.");
    }
}
