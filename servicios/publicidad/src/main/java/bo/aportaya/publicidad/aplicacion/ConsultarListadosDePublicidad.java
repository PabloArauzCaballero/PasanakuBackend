package bo.aportaya.publicidad.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.publicidad.infraestructura.ListadosDePublicidadRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-110/111/112 · los listados del backoffice de publicidad (carril A · H7).
 *
 * <p>Los permisos los exige la pagina; aca se validan filtros y cursor con el codigo de cada CU
 * (AP-CU110-04, AP-CU111-04, AP-CU112-04) y se arma la pagina con registros de esta capa: la web
 * no ve el repositorio.
 */
@Service
public class ConsultarListadosDePublicidad {

    private static final List<String> ESTADOS_ANUNCIANTE = List.of("ACTIVO", "SUSPENDIDO", "DADO_DE_BAJA");
    private static final List<String> ESTADOS_CAMPANA =
            List.of("BORRADOR", "EN_REVISION", "ACTIVA", "PAUSADA", "RECHAZADA", "FINALIZADA");
    private static final List<String> ESTADOS_MODERACION = List.of("PENDIENTE", "APROBADA", "RECHAZADA");

    private final Datos datos;
    private final ListadosDePublicidadRepositorio listados;

    public ConsultarListadosDePublicidad(Datos datos, ListadosDePublicidadRepositorio listados) {
        this.datos = datos;
        this.listados = listados;
    }

    @Transactional(readOnly = true)
    public Pagina<FilaAnunciante> anunciantes(
            List<String> estados, Optional<OffsetDateTime> fecha, Optional<UUID> id, int limite, ContextoSesion ctx) {
        List<String> filtro = validar(110, estados, ESTADOS_ANUNCIANTE, ESTADOS_ANUNCIANTE, fecha, id);
        var filas = datos.conContexto(ctx, dsl -> listados.anunciantes(dsl, filtro, fecha, id, limite));
        var items = filas.stream()
                .map(a -> new FilaAnunciante(
                        a.id(),
                        a.tipo(),
                        a.razonSocialFacturacion(),
                        a.estado(),
                        a.creadoEn(),
                        Optional.ofNullable(a.cuentaId()),
                        Optional.ofNullable(a.estadoCuenta()),
                        Optional.ofNullable(a.limiteGastoMensual()),
                        Optional.ofNullable(a.moneda())))
                .toList();
        return pagina(items, limite, FilaAnunciante::creadoEn, FilaAnunciante::anuncianteId);
    }

    @Transactional(readOnly = true)
    public Pagina<FilaCampana> campanas(
            List<String> estados, Optional<OffsetDateTime> fecha, Optional<UUID> id, int limite, ContextoSesion ctx) {
        List<String> filtro = validar(111, estados, ESTADOS_CAMPANA, List.of("EN_REVISION"), fecha, id);
        var filas = datos.conContexto(ctx, dsl -> listados.campanas(dsl, filtro, fecha, id, limite));
        var items = filas.stream()
                .map(c -> new FilaCampana(
                        c.id(),
                        c.cuentaId(),
                        c.anuncianteId(),
                        c.nombre(),
                        c.objetivo(),
                        c.presupuestoTotal(),
                        c.presupuestoConsumido(),
                        c.moneda(),
                        c.fechaInicio(),
                        Optional.ofNullable(c.fechaFin()),
                        c.estado(),
                        Optional.ofNullable(c.aprobadaPor())))
                .toList();
        return pagina(items, limite, FilaCampana::fechaInicio, FilaCampana::campanaId);
    }

    @Transactional(readOnly = true)
    public Pagina<FilaPieza> piezas(
            String estado, Optional<OffsetDateTime> fecha, Optional<UUID> id, int limite, ContextoSesion ctx) {
        List<String> filtro = validar(
                112, estado == null ? null : List.of(estado), ESTADOS_MODERACION, List.of("PENDIENTE"), fecha, id);
        var filas = datos.conContexto(ctx, dsl -> listados.piezas(dsl, filtro.get(0), fecha, id, limite));
        var items = filas.stream()
                .map(p -> new FilaPieza(
                        p.id(),
                        p.anuncianteId(),
                        p.titulo(),
                        Optional.ofNullable(p.texto()),
                        p.urlRecurso(),
                        p.tipoRecurso(),
                        p.estadoModeracion(),
                        p.creadaEn()))
                .toList();
        return pagina(items, limite, FilaPieza::creadaEn, FilaPieza::piezaCreativaId);
    }

    private static List<String> validar(
            int cu,
            List<String> pedidos,
            List<String> validos,
            List<String> porOmision,
            Optional<OffsetDateTime> fecha,
            Optional<UUID> id) {
        List<String> filtro = pedidos == null || pedidos.isEmpty() ? porOmision : pedidos;
        if (!validos.containsAll(filtro) || fecha.isPresent() != id.isPresent()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(cu, 4), "Filtro de listado invalido: estado desconocido o cursor a medias.");
        }
        return filtro;
    }

    private static <T> Pagina<T> pagina(
            List<T> items, int limite, Function<T, OffsetDateTime> fecha, Function<T, UUID> id) {
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<T> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(fecha), ultima.map(id));
    }

    public record Pagina<T>(List<T> items, Optional<OffsetDateTime> proximoFecha, Optional<UUID> proximoId) {}

    public record FilaAnunciante(
            UUID anuncianteId,
            String tipo,
            String razonSocialFacturacion,
            String estado,
            OffsetDateTime creadoEn,
            Optional<UUID> cuentaPublicitariaId,
            Optional<String> estadoCuenta,
            Optional<BigDecimal> limiteGastoMensual,
            Optional<String> moneda) {}

    public record FilaCampana(
            UUID campanaId,
            UUID cuentaPublicitariaId,
            UUID anuncianteId,
            String nombre,
            String objetivo,
            BigDecimal presupuestoTotal,
            BigDecimal presupuestoConsumido,
            String moneda,
            OffsetDateTime fechaInicio,
            Optional<OffsetDateTime> fechaFin,
            String estado,
            Optional<UUID> aprobadaPor) {}

    public record FilaPieza(
            UUID piezaCreativaId,
            UUID anuncianteId,
            String titulo,
            Optional<String> texto,
            String urlRecurso,
            String tipoRecurso,
            String estadoModeracion,
            OffsetDateTime creadaEn) {}
}
