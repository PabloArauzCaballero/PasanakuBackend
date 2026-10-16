package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.TesoreriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
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
 * CU-50/51/14 · lo que Tesoreria, Contabilidad y Auditoria leen en el backoffice (carril A · H8).
 *
 * <p>El permiso lo exige la pagina; aca se valida el cursor (con el codigo de cada CU) y se arma la
 * pagina con registros de esta capa: la web no ve el repositorio.
 */
@Service
public class ConsultarTesoreria {

    private static final List<String> ESTADOS_REVERSO = List.of("SOLICITADO", "AUTORIZADO", "EJECUTADO", "RECHAZADO");

    private final Datos datos;
    private final TesoreriaRepositorio tesoreria;

    public ConsultarTesoreria(Datos datos, TesoreriaRepositorio tesoreria) {
        this.datos = datos;
        this.tesoreria = tesoreria;
    }

    @Transactional(readOnly = true)
    public Pagina<Conciliacion, LocalDate> conciliaciones(
            Optional<LocalDate> antesDe, Optional<UUID> antesDeId, int limite, ContextoSesion ctx) {
        exigirCursor(50, antesDe, antesDeId);
        var filas = datos.conContexto(
                ctx,
                dsl -> tesoreria.conciliaciones(dsl, Optional.empty(), Optional.empty(), antesDe, antesDeId, limite));
        var items = filas.stream().map(ConsultarTesoreria::conciliacion).toList();
        return pagina(items, limite, Conciliacion::fecha, Conciliacion::conciliacionId);
    }

    @Transactional(readOnly = true)
    public Pagina<Cierre, LocalDate> cierres(
            Optional<LocalDate> antesDe, Optional<UUID> antesDeId, int limite, ContextoSesion ctx) {
        exigirCursor(51, antesDe, antesDeId);
        var items = datos.conContexto(ctx, dsl -> tesoreria.cierres(dsl, antesDe, antesDeId, limite)).stream()
                .map(c -> new Cierre(
                        c.id(),
                        c.fecha(),
                        c.totalRecaudado(),
                        c.totalConciliado(),
                        c.totalExcepciones(),
                        c.cantidadPagos(),
                        c.cuadrado(),
                        c.cerradoPor(),
                        c.cerradoEn(),
                        Optional.ofNullable(c.reabiertoEn())))
                .toList();
        return pagina(items, limite, Cierre::fecha, Cierre::cierreId);
    }

    @Transactional(readOnly = true)
    public Pagina<Reverso, OffsetDateTime> reversos(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = estados == null || estados.isEmpty() ? ESTADOS_REVERSO : estados;
        if (!ESTADOS_REVERSO.containsAll(filtro)) {
            throw filtroInvalido(14);
        }
        exigirCursor(14, antesDe, antesDeId);
        var items = datos.conContexto(ctx, dsl -> tesoreria.reversos(dsl, filtro, antesDe, antesDeId, limite)).stream()
                .map(r -> new Reverso(
                        r.id(),
                        r.transaccionOriginalId(),
                        Optional.ofNullable(r.transaccionReversoId()),
                        r.autorizadaPor(),
                        r.tipo(),
                        r.motivo(),
                        r.montoReversado(),
                        r.estado(),
                        r.solicitadaEn(),
                        Optional.ofNullable(r.ejecutadaEn())))
                .toList();
        return pagina(items, limite, Reverso::solicitadaEn, Reverso::reversoId);
    }

    static Conciliacion conciliacion(TesoreriaRepositorio.Conciliacion c) {
        Moneda moneda = Moneda.valueOf(c.moneda());
        return new Conciliacion(
                c.id(),
                c.cuentaCustodiaId(),
                c.fecha(),
                Dinero.de(c.saldoEmitido(), moneda),
                Dinero.de(c.saldoCustodia(), moneda),
                Dinero.de(c.saldoEnTransito(), moneda),
                Dinero.de(c.diferencia(), moneda),
                c.ratio(),
                c.cumpleEncaje(),
                c.estado(),
                Optional.ofNullable(c.ejecutadaPor()),
                c.ejecutadaEn(),
                c.descuadresAbiertos());
    }

    private static void exigirCursor(int cu, Optional<?> antesDe, Optional<UUID> antesDeId) {
        if (antesDe.isPresent() != antesDeId.isPresent()) {
            throw filtroInvalido(cu);
        }
    }

    private static ErrorDeNegocio filtroInvalido(int cu) {
        int numero =
                switch (cu) {
                    case 50 -> 4;
                    case 51 -> 5;
                    default -> 4;
                };
        return new ErrorDeNegocio(
                CodigoError.de(cu, numero), "Filtro de listado invalido: estado desconocido o cursor a medias.");
    }

    private static <T, C> Pagina<T, C> pagina(List<T> items, int limite, Function<T, C> cursor, Function<T, UUID> id) {
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<T> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(cursor), ultima.map(id));
    }

    public record Pagina<T, C>(List<T> items, Optional<C> proximoAntesDe, Optional<UUID> proximoAntesDeId) {}

    public record Conciliacion(
            UUID conciliacionId,
            UUID cuentaCustodiaId,
            LocalDate fecha,
            Dinero saldoEmitido,
            Dinero saldoCustodia,
            Dinero saldoEnTransito,
            Dinero diferencia,
            BigDecimal ratioCobertura,
            boolean cumpleEncaje,
            String estado,
            Optional<UUID> ejecutadaPor,
            OffsetDateTime ejecutadaEn,
            int descuadresAbiertos) {}

    public record Cierre(
            UUID cierreId,
            LocalDate fecha,
            BigDecimal totalRecaudado,
            BigDecimal totalConciliado,
            BigDecimal totalExcepciones,
            int cantidadPagos,
            boolean cuadrado,
            UUID cerradoPor,
            OffsetDateTime cerradoEn,
            Optional<OffsetDateTime> reabiertoEn) {}

    public record Reverso(
            UUID reversoId,
            UUID transaccionOriginalId,
            Optional<UUID> transaccionReversoId,
            UUID autorizadaPor,
            String tipo,
            String motivo,
            BigDecimal montoReversado,
            String estado,
            OffsetDateTime solicitadaEn,
            Optional<OffsetDateTime> ejecutadaEn) {}
}
