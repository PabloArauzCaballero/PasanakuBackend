package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.infraestructura.TableroDeCumplimientoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las colas de cumplimiento, riesgo y gobierno que lee el backoffice (carril A · H9).
 *
 * <p>El permiso lo exige la pagina; aca se validan los estados y el cursor con el codigo de cada CU
 * (AP-CU43-05, AP-CU45-05, AP-CU54-04, AP-CU55-04, AP-CU56-04, AP-CU94-07) y se arma la pagina con
 * registros de esta capa: la web no ve el repositorio.
 */
@Service
public class ConsultarTableroDeCumplimiento {

    static final List<String> ESTADOS_REPORTE =
            List.of("PENDIENTE", "GENERADO", "REVISADO", "APROBADO", "ENVIADO", "OBSERVADO", "RECHAZADO");
    static final List<String> ESTADOS_REQUERIMIENTO =
            List.of("RECIBIDO", "EN_PROCESO", "RESPONDIDO", "VENCIDO", "ARCHIVADO");
    static final List<String> ESTADOS_EVENTO = List.of("REGISTRADO", "EN_ANALISIS", "EN_REMEDIACION", "CERRADO");
    static final List<String> ESTADOS_INCIDENTE =
            List.of("DETECTADO", "EN_CONTENCION", "CONTENIDO", "ERRADICADO", "CERRADO");

    private final Datos datos;
    private final TableroDeCumplimientoRepositorio tablero;

    public ConsultarTableroDeCumplimiento(Datos datos, TableroDeCumplimientoRepositorio tablero) {
        this.datos = datos;
        this.tablero = tablero;
    }

    public record Pagina<T, C>(List<T> items, Optional<C> proximoAntesDe, Optional<UUID> proximoAntesDeId) {}

    public record Reporte(
            UUID reporteId,
            UUID catalogoReporteId,
            String periodo,
            LocalDate fechaCorte,
            LocalDate fechaLimite,
            String estado,
            int cantidadRegistros,
            boolean reporteEnCero,
            BigDecimal montoTotal,
            Optional<OffsetDateTime> generadoEn,
            Optional<UUID> aprobadoPor) {}

    public record Requerimiento(
            UUID requerimientoId,
            String autoridad,
            String numeroOficio,
            OffsetDateTime fechaRecepcion,
            OffsetDateTime plazoRespuesta,
            String estado,
            Optional<UUID> usuarioAfectadoId,
            boolean conBloqueoDeSaldo,
            Optional<UUID> respondidoPor,
            Optional<OffsetDateTime> respondidoEn) {}

    public record EventoDeRiesgo(
            UUID eventoId,
            String codigo,
            String categoria,
            String factor,
            String lineaNegocio,
            OffsetDateTime fechaOcurrencia,
            OffsetDateTime fechaDeteccion,
            BigDecimal perdidaBruta,
            BigDecimal recuperacion,
            BigDecimal perdidaNeta,
            String moneda,
            String estado,
            boolean reportadoCentral) {}

    public record Incidente(
            UUID incidenteId,
            String codigo,
            String tipo,
            String severidad,
            boolean datosPersonalesAfectados,
            int usuariosAfectados,
            OffsetDateTime detectadoEn,
            Optional<OffsetDateTime> contenidoEn,
            OffsetDateTime plazoReporte,
            Optional<OffsetDateTime> reportadoAlOrganismoEn,
            Optional<OffsetDateTime> notificadoATitularesEn,
            String estado) {}

    public record Prueba(
            UUID pruebaId,
            UUID planContinuidadId,
            Optional<UUID> actaComiteId,
            UUID ejecutadaPor,
            String tipo,
            LocalDate fecha,
            int rtoMinutos,
            int rpoMinutos,
            String resultado) {}

    public record Acta(
            UUID actaId, UUID comiteGobiernoId, String numero, LocalDate fecha, boolean cumpleQuorum, String hash) {}

    @Transactional(readOnly = true)
    public Pagina<Reporte, LocalDate> reportes(
            List<String> estados,
            Optional<LocalDate> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        var filtro = filtro(43, 5, estados, ESTADOS_REPORTE, antesDe, antesDeId);
        var items = datos
                .conContexto(
                        ctx, dsl -> tablero.reportes(dsl, filtro, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(r -> new Reporte(
                        r.id(),
                        r.catalogoReporteId(),
                        r.periodo(),
                        r.fechaCorte(),
                        r.fechaLimite(),
                        r.estado(),
                        r.cantidadRegistros(),
                        r.reporteEnCero(),
                        r.montoTotal(),
                        Optional.ofNullable(r.generadoEn()),
                        Optional.ofNullable(r.aprobadoPor())))
                .toList();
        return pagina(items, limite, Reporte::fechaCorte, Reporte::reporteId);
    }

    @Transactional(readOnly = true)
    public Pagina<Requerimiento, OffsetDateTime> requerimientos(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        var filtro = filtro(45, 5, estados, ESTADOS_REQUERIMIENTO, antesDe, antesDeId);
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> tablero.requerimientos(
                                dsl, filtro, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(r -> new Requerimiento(
                        r.id(),
                        r.autoridad(),
                        r.numeroOficio(),
                        r.fechaRecepcion(),
                        r.plazoRespuesta(),
                        r.estado(),
                        Optional.ofNullable(r.usuarioAfectadoId()),
                        r.conBloqueo(),
                        Optional.ofNullable(r.respondidoPor()),
                        Optional.ofNullable(r.respondidoEn())))
                .toList();
        return pagina(items, limite, Requerimiento::fechaRecepcion, Requerimiento::requerimientoId);
    }

    @Transactional(readOnly = true)
    public Pagina<EventoDeRiesgo, OffsetDateTime> eventos(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        var filtro = filtro(54, 4, estados, ESTADOS_EVENTO, antesDe, antesDeId);
        var items = datos
                .conContexto(
                        ctx, dsl -> tablero.eventos(dsl, filtro, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(e -> new EventoDeRiesgo(
                        e.id(),
                        e.codigo(),
                        e.categoria(),
                        e.factor(),
                        e.lineaNegocio(),
                        e.fechaOcurrencia(),
                        e.fechaDeteccion(),
                        e.perdidaBruta(),
                        e.recuperacion(),
                        e.perdidaNeta(),
                        e.moneda(),
                        e.estado(),
                        e.reportadoCentral()))
                .toList();
        return pagina(items, limite, EventoDeRiesgo::fechaDeteccion, EventoDeRiesgo::eventoId);
    }

    @Transactional(readOnly = true)
    public Pagina<Incidente, OffsetDateTime> incidentes(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        var filtro = filtro(55, 4, estados, ESTADOS_INCIDENTE, antesDe, antesDeId);
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> tablero.incidentes(dsl, filtro, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(i -> new Incidente(
                        i.id(),
                        i.codigo(),
                        i.tipo(),
                        i.severidad(),
                        i.datosPersonalesAfectados(),
                        i.usuariosAfectados(),
                        i.detectadoEn(),
                        Optional.ofNullable(i.contenidoEn()),
                        i.plazoReporte(),
                        Optional.ofNullable(i.reportadoAlOrganismoEn()),
                        Optional.ofNullable(i.notificadoATitularesEn()),
                        i.estado()))
                .toList();
        return pagina(items, limite, Incidente::detectadoEn, Incidente::incidenteId);
    }

    @Transactional(readOnly = true)
    public Pagina<Prueba, LocalDate> pruebas(
            Optional<LocalDate> antesDe, Optional<UUID> antesDeId, int limite, ContextoSesion ctx) {
        filtro(56, 4, List.of(), List.of(), antesDe, antesDeId);
        var items = datos
                .conContexto(ctx, dsl -> tablero.pruebas(dsl, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(p -> new Prueba(
                        p.id(),
                        p.planContinuidadId(),
                        Optional.ofNullable(p.actaComiteId()),
                        p.ejecutadaPor(),
                        p.tipo(),
                        p.fecha(),
                        p.rtoMinutos(),
                        p.rpoMinutos(),
                        p.resultado()))
                .toList();
        return pagina(items, limite, Prueba::fecha, Prueba::pruebaId);
    }

    @Transactional(readOnly = true)
    public Pagina<Acta, LocalDate> actas(
            Optional<LocalDate> antesDe, Optional<UUID> antesDeId, int limite, ContextoSesion ctx) {
        filtro(94, 7, List.of(), List.of(), antesDe, antesDeId);
        var items = datos
                .conContexto(ctx, dsl -> tablero.actas(dsl, antesDe.orElse(null), antesDeId.orElse(null), limite))
                .stream()
                .map(a -> new Acta(
                        a.id(), a.comiteGobiernoId(), a.numero(), a.fecha(), a.cumpleQuorum(), a.hashDocumento()))
                .toList();
        return pagina(items, limite, Acta::fecha, Acta::actaId);
    }

    /** Estados conocidos (sin filtro = todos) y cursor completo; si no, el codigo de filtro del CU. */
    private static List<String> filtro(
            int cu, int numero, List<String> pedidos, List<String> conocidos, Optional<?> antesDe, Optional<UUID> id) {
        List<String> filtro = pedidos == null || pedidos.isEmpty() ? conocidos : pedidos;
        if (!conocidos.containsAll(filtro) || antesDe.isPresent() != id.isPresent()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(cu, numero), "Filtro de listado invalido: estado desconocido o cursor a medias.");
        }
        return filtro;
    }

    private static <T, C> Pagina<T, C> pagina(List<T> items, int limite, Function<T, C> cursor, Function<T, UUID> id) {
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<T> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(cursor), ultima.map(id));
    }
}
