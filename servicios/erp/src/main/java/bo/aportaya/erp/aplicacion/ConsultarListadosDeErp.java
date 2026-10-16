package bo.aportaya.erp.aplicacion;

import bo.aportaya.erp.aplicacion.FilasDeErp.Activo;
import bo.aportaya.erp.aplicacion.FilasDeErp.CuentaPorCobrar;
import bo.aportaya.erp.aplicacion.FilasDeErp.Factura;
import bo.aportaya.erp.aplicacion.FilasDeErp.Orden;
import bo.aportaya.erp.aplicacion.FilasDeErp.Partida;
import bo.aportaya.erp.dominio.Depreciacion;
import bo.aportaya.erp.infraestructura.ListadosDeErpRepositorio;
import bo.aportaya.erp.infraestructura.ListadosDeTercerosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
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
 * CU-101..105 · los listados que la seccion Contabilidad del backoffice lee (carril A · A6).
 *
 * <p>El permiso lo exige la pagina; aca se valida el filtro (estado conocido, cursor entero) con el codigo
 * de cada CU y se arma la pagina con registros de esta capa: la web no ve el repositorio. Un estado
 * desconocido es un 422 y no una lista vacia: una pantalla que filtra mal no tiene que parecer al dia.
 */
@Service
public class ConsultarListadosDeErp {

    static final List<String> ESTADOS_PRESUPUESTO = List.of("BORRADOR", "APROBADO", "CERRADO");
    static final List<String> ESTADOS_ORDEN =
            List.of("BORRADOR", "APROBADA", "RECIBIDA_PARCIAL", "RECIBIDA_TOTAL", "CANCELADA");
    static final List<String> ESTADOS_FACTURA =
            List.of("REGISTRADA", "APROBADA", "PAGADA_PARCIAL", "PAGADA", "ANULADA");
    static final List<String> ESTADOS_CUENTA = List.of("PENDIENTE", "COBRADA_PARCIAL", "COBRADA", "INCOBRABLE");
    static final List<String> ESTADOS_ACTIVO = List.of("ACTIVO", "DADO_DE_BAJA", "VENDIDO");

    private final Datos datos;
    private final ListadosDeErpRepositorio listados;
    private final ListadosDeTercerosRepositorio terceros;
    private final Reloj reloj;

    public ConsultarListadosDeErp(
            Datos datos, ListadosDeErpRepositorio listados, ListadosDeTercerosRepositorio terceros, Reloj reloj) {
        this.datos = datos;
        this.listados = listados;
        this.terceros = terceros;
        this.reloj = reloj;
    }

    /** CU-101 · AP-CU101-04 si el filtro no es valido. */
    @Transactional(readOnly = true)
    public Pagina<Partida, LocalDate> partidas(
            Optional<UUID> ejercicioId, Filtro<LocalDate> filtro, ContextoSesion ctx) {
        var estados = exigir(filtro, ESTADOS_PRESUPUESTO, CodigoError.de(101, 4));
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> listados.partidas(
                                dsl, ejercicioId.orElse(null), estados, desde(filtro), hasta(filtro), filtro.limite()))
                .stream()
                .map(p -> new Partida(
                        p.id(),
                        p.presupuestoId(),
                        p.presupuestoNombre(),
                        p.estado(),
                        p.centroCostoId(),
                        p.centroCostoNombre(),
                        p.cuentaContableId(),
                        p.periodoContableId(),
                        p.anio(),
                        p.mes(),
                        p.fechaInicio(),
                        dinero(p.presupuestado(), p.moneda()),
                        dinero(p.ejecutado(), p.moneda())))
                .toList();
        return pagina(items, filtro.limite(), Partida::fechaInicioPeriodo, Partida::partidaId);
    }

    /** CU-102 · AP-CU102-04 si el filtro no es valido. */
    @Transactional(readOnly = true)
    public Pagina<Orden, OffsetDateTime> ordenes(Filtro<OffsetDateTime> filtro, ContextoSesion ctx) {
        var estados = exigir(filtro, ESTADOS_ORDEN, CodigoError.de(102, 4));
        var items = datos
                .conContexto(ctx, dsl -> terceros.ordenes(dsl, estados, desde(filtro), hasta(filtro), filtro.limite()))
                .stream()
                .map(o -> new Orden(
                        o.id(),
                        o.numero(),
                        o.terceroId(),
                        o.razonSocial(),
                        Optional.ofNullable(o.centroCostoId()),
                        Optional.ofNullable(o.centroCostoNombre()),
                        o.estado(),
                        dinero(o.monto(), o.moneda()),
                        o.creadaEn()))
                .toList();
        return pagina(items, filtro.limite(), Orden::creadaEn, Orden::ordenCompraId);
    }

    /** CU-103 · AP-CU103-06 si el filtro no es valido. Quien pregunta es el sujeto del token. */
    @Transactional(readOnly = true)
    public Pagina<Factura, LocalDate> facturas(Filtro<LocalDate> filtro, ContextoSesion ctx) {
        var estados = exigir(filtro, ESTADOS_FACTURA, CodigoError.de(103, 6));
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> terceros.facturas(
                                dsl, ctx.usuarioId(), estados, desde(filtro), hasta(filtro), filtro.limite()))
                .stream()
                .map(f -> new Factura(
                        f.id(),
                        f.numero(),
                        f.terceroId(),
                        f.razonSocial(),
                        Optional.ofNullable(f.ordenCompraId()),
                        f.emision(),
                        f.vencimiento(),
                        f.estado(),
                        dinero(f.monto(), f.moneda()),
                        dinero(f.pagado(), f.moneda()),
                        dinero(f.saldo(), f.moneda()),
                        f.aprobadaPorLaSesion(),
                        Optional.ofNullable(f.asientoContableId())))
                .toList();
        return pagina(items, filtro.limite(), Factura::fechaEmision, Factura::facturaProveedorId);
    }

    /** CU-104 · AP-CU104-04 si el filtro no es valido. */
    @Transactional(readOnly = true)
    public Pagina<CuentaPorCobrar, LocalDate> cuentasPorCobrar(Filtro<LocalDate> filtro, ContextoSesion ctx) {
        var estados = exigir(filtro, ESTADOS_CUENTA, CodigoError.de(104, 4));
        var items = datos
                .conContexto(
                        ctx,
                        dsl -> terceros.cuentasPorCobrar(dsl, estados, desde(filtro), hasta(filtro), filtro.limite()))
                .stream()
                .map(c -> new CuentaPorCobrar(
                        c.id(),
                        c.origenTipo(),
                        c.origenId(),
                        Optional.ofNullable(c.terceroId()),
                        Optional.ofNullable(c.razonSocial()),
                        c.vencimiento(),
                        c.estado(),
                        dinero(c.monto(), c.moneda()),
                        dinero(c.cobrado(), c.moneda()),
                        dinero(c.saldo(), c.moneda())))
                .toList();
        return pagina(items, filtro.limite(), CuentaPorCobrar::fechaVencimiento, CuentaPorCobrar::cuentaPorCobrarId);
    }

    /** CU-105 · AP-CU105-04 si el filtro no es valido. "Vigente" es el periodo que contiene hoy en La Paz. */
    @Transactional(readOnly = true)
    public Pagina<Activo, LocalDate> activos(Filtro<LocalDate> filtro, ContextoSesion ctx) {
        var estados = exigir(filtro, ESTADOS_ACTIVO, CodigoError.de(105, 4));
        LocalDate hoy = reloj.hoy();
        var items = datos
                .conContexto(
                        ctx, dsl -> listados.activos(dsl, hoy, estados, desde(filtro), hasta(filtro), filtro.limite()))
                .stream()
                .map(a -> new Activo(
                        a.id(),
                        a.codigo(),
                        a.descripcion(),
                        a.categoriaNombre(),
                        a.vidaUtilMeses(),
                        a.adquisicion(),
                        a.estado(),
                        dinero(a.costo(), a.moneda()),
                        dinero(a.residual(), a.moneda()),
                        dinero(a.acumulada(), a.moneda()),
                        dinero(Depreciacion.valorEnLibros(a.costo(), a.acumulada()), a.moneda()),
                        a.depreciadoEnElPeriodoVigente()))
                .toList();
        return pagina(items, filtro.limite(), Activo::fechaAdquisicion, Activo::activoFijoId);
    }

    /** Sin estados se listan todos; un estado fuera del catalogo o el cursor a medias es el 422 del CU. */
    private static List<String> exigir(Filtro<?> filtro, List<String> catalogo, CodigoError codigo) {
        List<String> pedidos = filtro.estados() == null || filtro.estados().isEmpty() ? catalogo : filtro.estados();
        if (!catalogo.containsAll(pedidos)
                || filtro.antesDe().isPresent() != filtro.antesDeId().isPresent()) {
            throw new ErrorDeNegocio(codigo, "Filtro de listado invalido: estado desconocido o cursor a medias.");
        }
        return pedidos;
    }

    private static <C> C desde(Filtro<C> filtro) {
        return filtro.antesDe().orElse(null);
    }

    private static UUID hasta(Filtro<?> filtro) {
        return filtro.antesDeId().orElse(null);
    }

    private static Dinero dinero(BigDecimal monto, String moneda) {
        return Dinero.de(monto, Moneda.valueOf(moneda));
    }

    private static <T, C> Pagina<T, C> pagina(List<T> items, int limite, Function<T, C> cursor, Function<T, UUID> id) {
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<T> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(cursor), ultima.map(id));
    }

    /** Lo que pide la pagina: estados (vacio = todos), cursor keyset y tamano. */
    public record Filtro<C>(List<String> estados, Optional<C> antesDe, Optional<UUID> antesDeId, int limite) {}

    public record Pagina<T, C>(List<T> items, Optional<C> proximoAntesDe, Optional<UUID> proximoAntesDeId) {}
}
