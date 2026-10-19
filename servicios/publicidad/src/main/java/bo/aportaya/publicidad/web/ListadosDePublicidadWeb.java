package bo.aportaya.publicidad.web;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.traza.Traza;
import bo.aportaya.publicidad.aplicacion.ConsultarListadosDePublicidad;
import bo.aportaya.publicidad.web.generado.modelo.AnuncianteEnLista;
import bo.aportaya.publicidad.web.generado.modelo.CampanaEnLista;
import bo.aportaya.publicidad.web.generado.modelo.Dinero;
import bo.aportaya.publicidad.web.generado.modelo.PaginaDeAnunciantes;
import bo.aportaya.publicidad.web.generado.modelo.PaginaDeCampanas;
import bo.aportaya.publicidad.web.generado.modelo.PaginaDePiezas;
import bo.aportaya.publicidad.web.generado.modelo.PiezaEnLista;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Los tres listados del backoffice de publicidad (carril A · H7): traducen y delegan.
 *
 * <p>No es un {@code @RestController} propio: el generador agrupa todo {@code /publicidad} en una
 * interfaz y {@link PublicidadController} la implementa; esto es a donde delega (mismo patron que
 * {@code AdhesionController} de cumplimiento).
 *
 * <p>La cola de campanas es de Operaciones ({@code PUBLICIDAD_APROBAR_CAMPANA}): el anunciante
 * ({@code PUBLICIDAD_CAMPANA_GESTIONAR}) no ve las campanas de otros por esta ruta.
 */
final class ListadosDePublicidadWeb {

    private final ConsultarListadosDePublicidad listados;

    ListadosDePublicidadWeb(ConsultarListadosDePublicidad listados) {
        this.listados = listados;
    }

    PaginaDeAnunciantes anunciantes(
            List<String> estados, Integer limite, OffsetDateTime fecha, UUID id, ContextoSesion ctx) {
        Traza.marcarCasoDeUso("CU-110", "listado");
        var pagina = listados.anunciantes(estados, Optional.ofNullable(fecha), Optional.ofNullable(id), limite, ctx);
        var salida = new PaginaDeAnunciantes();
        salida.setItems(pagina.items().stream()
                .map(a -> {
                    var r = new AnuncianteEnLista();
                    r.setAnuncianteId(a.anuncianteId());
                    r.setTipo(AnuncianteEnLista.TipoEnum.fromValue(a.tipo()));
                    r.setRazonSocialFacturacion(a.razonSocialFacturacion());
                    r.setEstado(AnuncianteEnLista.EstadoEnum.fromValue(a.estado()));
                    r.setCreadoEn(a.creadoEn());
                    r.setCuentaPublicitariaId(a.cuentaPublicitariaId().orElse(null));
                    r.setEstadoCuenta(a.estadoCuenta()
                            .map(AnuncianteEnLista.EstadoCuentaEnum::fromValue)
                            .orElse(null));
                    r.setLimiteGastoMensual(a.limiteGastoMensual()
                            .flatMap(l -> a.moneda().map(m -> dinero(l, m)))
                            .orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoFecha(pagina.proximoFecha().orElse(null));
        salida.setProximoId(pagina.proximoId().orElse(null));
        return salida;
    }

    PaginaDeCampanas campanas(List<String> estados, Integer limite, OffsetDateTime fecha, UUID id, ContextoSesion ctx) {
        Traza.marcarCasoDeUso("CU-111", "listado");
        var pagina = listados.campanas(estados, Optional.ofNullable(fecha), Optional.ofNullable(id), limite, ctx);
        var salida = new PaginaDeCampanas();
        salida.setItems(pagina.items().stream()
                .map(c -> {
                    var r = new CampanaEnLista();
                    r.setCampanaId(c.campanaId());
                    r.setCuentaPublicitariaId(c.cuentaPublicitariaId());
                    r.setAnuncianteId(c.anuncianteId());
                    r.setNombre(c.nombre());
                    r.setObjetivo(CampanaEnLista.ObjetivoEnum.fromValue(c.objetivo()));
                    r.setPresupuestoTotal(dinero(c.presupuestoTotal(), c.moneda()));
                    r.setPresupuestoConsumido(dinero(c.presupuestoConsumido(), c.moneda()));
                    r.setFechaInicio(c.fechaInicio());
                    r.setFechaFin(c.fechaFin().orElse(null));
                    r.setEstado(CampanaEnLista.EstadoEnum.fromValue(c.estado()));
                    r.setAprobadaPor(c.aprobadaPor().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoFecha(pagina.proximoFecha().orElse(null));
        salida.setProximoId(pagina.proximoId().orElse(null));
        return salida;
    }

    PaginaDePiezas piezas(String estado, Integer limite, OffsetDateTime fecha, UUID id, ContextoSesion ctx) {
        Traza.marcarCasoDeUso("CU-112", "listado");
        var pagina = listados.piezas(estado, Optional.ofNullable(fecha), Optional.ofNullable(id), limite, ctx);
        var salida = new PaginaDePiezas();
        salida.setItems(pagina.items().stream()
                .map(p -> {
                    var r = new PiezaEnLista();
                    r.setPiezaCreativaId(p.piezaCreativaId());
                    r.setAnuncianteId(p.anuncianteId());
                    r.setTitulo(p.titulo());
                    r.setTexto(p.texto().orElse(null));
                    r.setUrlRecurso(p.urlRecurso());
                    r.setTipoRecurso(PiezaEnLista.TipoRecursoEnum.fromValue(p.tipoRecurso()));
                    r.setEstadoModeracion(PiezaEnLista.EstadoModeracionEnum.fromValue(p.estadoModeracion()));
                    r.setCreadaEn(p.creadaEn());
                    return r;
                })
                .toList());
        salida.setProximoFecha(pagina.proximoFecha().orElse(null));
        salida.setProximoId(pagina.proximoId().orElse(null));
        return salida;
    }

    private static Dinero dinero(BigDecimal monto, String moneda) {
        var d = new Dinero();
        d.setMonto(MapeoDePublicidad.monto(monto));
        d.setMoneda(Dinero.MonedaEnum.fromValue(moneda));
        return d;
    }
}
