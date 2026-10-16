package bo.aportaya.tarifas.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.tarifas.infraestructura.HistorialDeTarifariosRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-34 · los tarifarios y sus versiones que mira el backoffice (carril A · A6).
 *
 * <p>Sin filtro trae todos los estados: que version esta en preaviso es tan importante como cual
 * rige. El permiso lo exige la pagina; aca se validan el filtro y el cursor (AP-CU34-05).
 */
@Service
public class ConsultarTarifarios {

    /** Los de {@code ck_tarifario_estado}. */
    private static final List<String> ESTADOS =
            List.of("ARCHIVADO", "BORRADOR", "EN_PREAVISO", "SUSTITUIDO", "VIGENTE");

    private final Datos datos;
    private final HistorialDeTarifariosRepositorio historial;

    public ConsultarTarifarios(Datos datos, HistorialDeTarifariosRepositorio historial) {
        this.datos = datos;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public Pagina listar(
            Optional<String> codigo,
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = estados == null || estados.isEmpty() ? ESTADOS : estados;
        if (!ESTADOS.containsAll(filtro) || antesDe.isPresent() != antesDeId.isPresent()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(34, 5), "Filtro de listado invalido: estado desconocido o cursor a medias.");
        }
        var items =
                datos
                        .conContexto(ctx, dsl -> historial.listar(dsl, codigo, filtro, antesDe, antesDeId, limite))
                        .stream()
                        .map(f -> new Tarifario(
                                f.id(),
                                f.codigo(),
                                f.version(),
                                f.nombre(),
                                f.estado(),
                                f.monedaBase(),
                                f.vigenteDesde(),
                                Optional.ofNullable(f.vigenteHasta()),
                                f.diasPreaviso(),
                                Optional.ofNullable(f.publicadoEn()),
                                Optional.ofNullable(f.tarifarioAnteriorId()),
                                Optional.ofNullable(f.aprobadoPor()),
                                Optional.ofNullable(f.actaAprobacion()),
                                Optional.ofNullable(f.cambioId())
                                        .map(id -> new Cambio(
                                                id,
                                                f.tipoCambio(),
                                                Boolean.TRUE.equals(f.requierePreaviso()),
                                                f.cambioDiasPreaviso(),
                                                Optional.ofNullable(f.fechaAviso()),
                                                Optional.ofNullable(f.canalAviso()),
                                                f.usuariosNotificados(),
                                                Boolean.TRUE.equals(f.permiteRescisionSinCosto()))),
                                Optional.ofNullable(f.simuladaEn())
                                        .map(en ->
                                                new Simulacion(f.deltaIngresoEstimado(), f.usuariosImpactados(), en))))
                        .toList();
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<Tarifario> ultima =
                items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina(items, ultima.map(Tarifario::vigenteDesde), ultima.map(Tarifario::tarifarioId));
    }

    public record Pagina(
            List<Tarifario> items, Optional<OffsetDateTime> proximoAntesDe, Optional<UUID> proximoAntesDeId) {}

    public record Tarifario(
            UUID tarifarioId,
            String codigo,
            int version,
            String nombre,
            String estado,
            String monedaBase,
            OffsetDateTime vigenteDesde,
            Optional<OffsetDateTime> vigenteHasta,
            int diasPreaviso,
            Optional<OffsetDateTime> publicadoEn,
            Optional<UUID> tarifarioAnteriorId,
            Optional<UUID> aprobadoPor,
            Optional<String> actaAprobacion,
            Optional<Cambio> cambio,
            Optional<Simulacion> simulacion) {}

    public record Cambio(
            UUID cambioId,
            String tipoCambio,
            boolean requierePreaviso,
            int diasPreaviso,
            Optional<OffsetDateTime> fechaAviso,
            Optional<String> canalAviso,
            int usuariosNotificados,
            boolean permiteRescisionSinCosto) {}

    /** El delta va sin moneda: {@code simulacion_tarifa} no la modela y no se infiere. */
    public record Simulacion(BigDecimal deltaIngresoEstimado, int usuariosImpactados, OffsetDateTime ejecutadaEn) {}
}
