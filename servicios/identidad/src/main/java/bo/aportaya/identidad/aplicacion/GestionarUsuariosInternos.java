package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.RolesYPermisosRepositorio;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.PermisoCatalogo;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarUsuariosInternos {

    private final Datos datos;
    private final UsuariosInternosRepositorio repositorio;
    private final RolesYPermisosRepositorio rolesYPermisos;
    private final Reloj reloj;
    private final Outbox outbox;

    public GestionarUsuariosInternos(
            Datos datos,
            UsuariosInternosRepositorio repositorio,
            RolesYPermisosRepositorio rolesYPermisos,
            Reloj reloj,
            Outbox outbox) {
        this.datos = datos;
        this.repositorio = repositorio;
        this.rolesYPermisos = rolesYPermisos;
        this.reloj = reloj;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public List<UsuarioInternoResumen> listar(String estado, String rol, String busqueda, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> repositorio.listar(dsl, estado, rol, busqueda, ahora));
    }

    @Transactional
    public UsuarioInternoDetalle crear(EntradaAltaUsuarioInterno entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UsuarioInternoDetalle detalle = repositorio.crear(dsl, entrada, ctx.usuarioId(), ahora);
            UUID correlacion = ctx.traza() != null ? UUID.fromString(ctx.traza().id()) : UUID.randomUUID();
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.usuario_interno_creado",
                            "usuario_interno",
                            detalle.getUsuarioId(),
                            Map.of(
                                    "usuarioId", detalle.getUsuarioId().toString(),
                                    "departamento", detalle.getDepartamento(),
                                    "cargo", detalle.getCargo()),
                            correlacion));
            return detalle;
        });
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioInternoDetalle> consultar(UUID id, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> repositorio.consultar(dsl, id, ahora));
    }

    @Transactional
    public UsuarioInternoDetalle actualizar(UUID id, EntradaActualizarUsuarioInterno entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UsuarioInternoDetalle detalle = repositorio.actualizar(dsl, id, entrada, ctx.usuarioId(), ahora);
            UUID correlacion = ctx.traza() != null ? UUID.fromString(ctx.traza().id()) : UUID.randomUUID();
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.usuario_interno_actualizado",
                            "usuario_interno",
                            detalle.getUsuarioId(),
                            Map.of(
                                    "usuarioId", detalle.getUsuarioId().toString(),
                                    "motivo", entrada.getMotivo() != null ? entrada.getMotivo() : "",
                                    "estado", detalle.getEstado() != null ? detalle.getEstado() : ""),
                            correlacion));
            return detalle;
        });
    }

    @Transactional
    public UsuarioInternoDetalle asignarRoles(UUID id, EntradaAsignarRolesUsuarioInterno entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UsuarioInternoDetalle detalle = repositorio.asignarRoles(dsl, id, entrada, ctx.usuarioId(), ahora);
            UUID correlacion = ctx.traza() != null ? UUID.fromString(ctx.traza().id()) : UUID.randomUUID();
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.usuario_interno_roles_asignados",
                            "usuario_interno",
                            detalle.getUsuarioId(),
                            Map.of(
                                    "usuarioId", detalle.getUsuarioId().toString(),
                                    "roles", String.join(",", entrada.getRoles()),
                                    "motivo", entrada.getMotivo() != null ? entrada.getMotivo() : ""),
                            correlacion));
            return detalle;
        });
    }

    @Transactional
    public UsuarioInternoDetalle desbloquear(UUID id, String motivo, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UsuarioInternoDetalle detalle = repositorio.desbloquear(dsl, id, motivo, ctx.usuarioId(), ahora);
            UUID correlacion = ctx.traza() != null ? UUID.fromString(ctx.traza().id()) : UUID.randomUUID();
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.usuario_interno_desbloqueado",
                            "usuario_interno",
                            detalle.getUsuarioId(),
                            Map.of(
                                    "usuarioId", detalle.getUsuarioId().toString(),
                                    "motivo", motivo != null ? motivo : "",
                                    "operadorId", ctx.usuarioId().toString()),
                            correlacion));
            return detalle;
        });
    }

    @Transactional(readOnly = true)
    public List<RolCatalogo> listarRoles(ContextoSesion ctx) {
        return datos.conContexto(ctx, rolesYPermisos::listarRoles);
    }

    @Transactional(readOnly = true)
    public List<PermisoCatalogo> listarPermisos(ContextoSesion ctx) {
        return datos.conContexto(ctx, rolesYPermisos::listarPermisos);
    }
}
