package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.infraestructura.BandejaDeReclamosRepositorio;
import bo.aportaya.cumplimiento.infraestructura.BandejaDeReclamosRepositorio.EnBandeja;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-52 · la bandeja del Punto de Reclamo y la ficha para responder (backoffice carril A · H3).
 *
 * <p>Los permisos ({@code RECLAMO_VER} para la cola, {@code RECLAMO_ATENDER} para el texto) los
 * exige la pagina. Aca se valida el filtro y se marca {@code vencido}: comparar el limite GUARDADO
 * con la hora no recalcula ningun plazo (R-CON-01), solo dice si ya paso.
 */
@Service
public class ConsultarBandejaDeReclamos {

    private static final Set<String> ESTADOS = Set.of("INGRESADO", "EN_ANALISIS", "RESPONDIDO", "CERRADO", "ELEVADO");
    private static final List<String> ABIERTOS = List.of("INGRESADO", "EN_ANALISIS");

    private final Datos datos;
    private final BandejaDeReclamosRepositorio bandeja;
    private final Reloj reloj;

    public ConsultarBandejaDeReclamos(Datos datos, BandejaDeReclamosRepositorio bandeja, Reloj reloj) {
        this.datos = datos;
        this.bandeja = bandeja;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina bandeja(
            List<String> estados,
            Optional<OffsetDateTime> despuesDeLimite,
            Optional<UUID> despuesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = estados == null || estados.isEmpty() ? ABIERTOS : estados;
        if (!ESTADOS.containsAll(filtro) || despuesDeLimite.isPresent() != despuesDeId.isPresent()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(52, 7), "Filtro de bandeja invalido: estado desconocido o cursor a medias.");
        }
        OffsetDateTime ahora = OffsetDateTime.ofInstant(reloj.ahora(), Reloj.ZONA);
        var filas = datos.conContexto(ctx, dsl -> bandeja.pagina(dsl, filtro, despuesDeLimite, despuesDeId, limite));
        var items = filas.stream().map(f -> fila(f, ahora)).toList();
        Optional<Fila> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina(items, ultima.map(Fila::limiteVigente), ultima.map(Fila::reclamoId));
    }

    @Transactional(readOnly = true)
    public Ficha ficha(UUID reclamoId, ContextoSesion ctx) {
        OffsetDateTime ahora = OffsetDateTime.ofInstant(reloj.ahora(), Reloj.ZONA);
        var ficha = datos.conContexto(ctx, dsl -> bandeja.ficha(dsl, reclamoId))
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(52, 5), "Ese reclamo no existe."));
        return new Ficha(
                fila(ficha.reclamo(), ahora),
                ficha.descripcion(),
                Optional.ofNullable(ficha.resultado()),
                Optional.ofNullable(ficha.respuesta()),
                Optional.ofNullable(ficha.fechaRespuesta()),
                Optional.ofNullable(ficha.devolucionId()));
    }

    private static Fila fila(EnBandeja f, OffsetDateTime ahora) {
        boolean vencido = ABIERTOS.contains(f.estado()) && f.limiteVigente().isBefore(ahora);
        return new Fila(
                f.id(),
                f.codigo(),
                f.categoria(),
                f.producto(),
                f.canalIngreso(),
                Optional.ofNullable(f.montoReclamado())
                        .map(m -> m.setScale(2, RoundingMode.UNNECESSARY).toPlainString()),
                f.estado(),
                f.fechaIngreso(),
                f.plazoRespuesta(),
                Optional.ofNullable(f.plazoProrrogadoHasta()),
                f.diasHabilesPlazo(),
                f.limiteVigente(),
                vencido,
                Optional.ofNullable(f.responsableId()));
    }

    /** Una fila de la bandeja. El monto va como cadena decimal: la moneda no esta modelada (A-7). */
    public record Fila(
            UUID reclamoId,
            String codigo,
            String categoria,
            String producto,
            String canalIngreso,
            Optional<String> montoReclamado,
            String estado,
            OffsetDateTime fechaIngreso,
            OffsetDateTime plazoRespuesta,
            Optional<OffsetDateTime> plazoProrrogadoHasta,
            int diasHabilesPlazo,
            OffsetDateTime limiteVigente,
            boolean vencido,
            Optional<UUID> responsableId) {}

    public record Pagina(List<Fila> items, Optional<OffsetDateTime> proximoLimite, Optional<UUID> proximoId) {}

    public record Ficha(
            Fila reclamo,
            String descripcion,
            Optional<String> resultado,
            Optional<String> respuesta,
            Optional<OffsetDateTime> fechaRespuesta,
            Optional<UUID> devolucionId) {}
}
