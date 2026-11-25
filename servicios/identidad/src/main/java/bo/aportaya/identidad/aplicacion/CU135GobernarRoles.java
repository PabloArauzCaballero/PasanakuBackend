package bo.aportaya.identidad.aplicacion;

import static bo.aportaya.identidad.dominio.ReglasDeRolPersonalizado.*;

import bo.aportaya.identidad.dominio.ReglasDeRolPersonalizado;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio.FilaDeRol;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio.FilaDeSolicitud;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-135 · Crear, editar y retirar roles personalizados (GU-16, decision P-9).
 *
 * <p>Cada acto exige step-up y motivo, pasa por {@link ReglasDeRolPersonalizado} y deja su evento
 * {@code identidad.rol_*} en la MISMA transaccion (outbox, regla 98.3.3). Si el acto agrega permisos de riesgo
 * {@code ALTO} o {@code CRITICO} del catalogo, no se aplica: queda una solicitud que otra persona resuelve (cuatro
 * ojos), y al resolver se vuelven a comprobar todas las reglas, porque entre la propuesta y la aprobacion el catalogo
 * y las asignaciones pudieron cambiar.
 *
 * <p>Los roles {@code es_sistema} no se editan ni se retiran, ni con cuatro ojos: es la base la que mas importa, y el
 * {@code UPDATE} del repositorio tambien lo excluye.
 */
@Service
public class CU135GobernarRoles {

    private static final int FILAS_DE_SOLICITUDES = 100;

    private final Datos datos;
    private final RolesPersonalizadosRepositorio roles;
    private final AccesosRepositorio accesos;
    private final EvidenciaDeStepUp evidencias;
    private final EventosDeRoles eventos;
    private final ReglasDeGobiernoDeRoles reglas;
    private final CambiosDeRoles cambios;
    private final Reloj reloj;

    public CU135GobernarRoles(
            Datos datos,
            RolesPersonalizadosRepositorio roles,
            AccesosRepositorio accesos,
            EvidenciaDeStepUp evidencias,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.roles = roles;
        this.accesos = accesos;
        this.evidencias = evidencias;
        this.eventos = new EventosDeRoles(outbox, reloj);
        this.reglas = new ReglasDeGobiernoDeRoles(roles, accesos, evidencias);
        this.cambios = new CambiosDeRoles(roles);
        this.reloj = reloj;
    }

    public record PedidoDeCreacion(
            String codigo, String nombre, String descripcion, Set<String> permisos, String motivo, String evidencia) {}

    public record PedidoDeEdicion(
            String nombre, String descripcion, Set<String> permisos, String motivo, String evidencia) {}

    /** {@code APLICADO} o {@code PENDIENTE_DE_APROBACION}; en lo pendiente, {@code solicitudId} y no {@code rolId}. */
    public record Resultado(String estado, UUID rolId, UUID solicitudId) {}

    public record Solicitud(
            UUID id,
            String tipo,
            UUID rolId,
            String codigo,
            String nombre,
            String descripcion,
            Set<String> permisos,
            String motivo,
            UUID propuestaPor,
            OffsetDateTime propuestaEn) {}

    // ---------------------------------------------------------------- crear

    @Transactional
    public Resultado crear(PedidoDeCreacion p, ContextoSesion ctx) {
        ReglasDeGobiernoDeRoles.validarForma(p.codigo(), p.nombre(), p.descripcion(), p.permisos(), p.motivo());
        reglas.exigirStepUp(ctx, p.evidencia());
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            if (roles.existeCodigo(dsl, p.codigo())) {
                throw new ErrorDeNegocio(CODIGO_EN_USO, "Ya hay un rol con ese codigo.");
            }
            Map<String, String> riesgo = reglas.validarPermisos(dsl, p.permisos());
            if (exigeCuatroOjos(p.permisos(), riesgo)) {
                UUID solicitud = roles.insertarSolicitud(
                        dsl,
                        "CREAR",
                        null,
                        PropuestaDeRol.serializar(p.codigo(), p.nombre(), p.descripcion(), p.permisos()),
                        p.motivo(),
                        ctx.usuarioId(),
                        ahora);
                eventos.emitir(
                        dsl,
                        "identidad.rol_solicitado",
                        solicitud,
                        ctx,
                        p.motivo(),
                        Map.of("tipo", "CREAR", "rolCodigo", p.codigo(), "solicitudId", solicitud.toString()));
                return new Resultado("PENDIENTE_DE_APROBACION", null, solicitud);
            }
            UUID rolId =
                    cambios.aplicarCreacion(dsl, p.codigo(), p.nombre(), p.descripcion(), p.permisos(), ctx, ahora);
            eventos.emitir(
                    dsl,
                    "identidad.rol_creado",
                    rolId,
                    ctx,
                    p.motivo(),
                    EventosDeRoles.detalleDeCreacion(p.codigo(), p.permisos()));
            return new Resultado("APLICADO", rolId, null);
        });
    }

    // --------------------------------------------------------------- editar

    @Transactional
    public Resultado editar(UUID rolId, PedidoDeEdicion p, ContextoSesion ctx) {
        ReglasDeGobiernoDeRoles.validarForma("ROL_EXISTENTE", p.nombre(), p.descripcion(), p.permisos(), p.motivo());
        reglas.exigirStepUp(ctx, p.evidencia());
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeRol rol = reglas.editable(dsl, rolId);
            Map<String, String> riesgo = reglas.validarPermisos(dsl, p.permisos());
            Set<String> actuales = accesos.permisosDelRol(dsl, rolId);
            Set<String> agregados = diferencia(p.permisos(), actuales);
            Set<String> quitados = diferencia(actuales, p.permisos());
            reglas.exigirQueNadieQuedeEnConflicto(dsl, rolId, p.permisos(), ahora);

            if (exigeCuatroOjos(agregados, riesgo)) {
                UUID solicitud = roles.insertarSolicitud(
                        dsl,
                        "EDITAR",
                        rolId,
                        PropuestaDeRol.serializar(rol.codigo(), p.nombre(), p.descripcion(), p.permisos()),
                        p.motivo(),
                        ctx.usuarioId(),
                        ahora);
                eventos.emitir(
                        dsl,
                        "identidad.rol_solicitado",
                        rolId,
                        ctx,
                        p.motivo(),
                        Map.of("tipo", "EDITAR", "rolCodigo", rol.codigo(), "solicitudId", solicitud.toString()));
                return new Resultado("PENDIENTE_DE_APROBACION", rolId, solicitud);
            }
            cambios.aplicarEdicion(dsl, rolId, p.nombre(), p.descripcion(), agregados, quitados);
            eventos.emitir(
                    dsl,
                    "identidad.rol_editado",
                    rolId,
                    ctx,
                    p.motivo(),
                    EventosDeRoles.detalleDeEdicion(rol.codigo(), agregados, quitados));
            return new Resultado("APLICADO", rolId, null);
        });
    }

    // -------------------------------------------------------------- retirar

    @Transactional
    public Resultado retirar(UUID rolId, String motivo, String evidencia, ContextoSesion ctx) {
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
        reglas.exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeRol rol = reglas.editable(dsl, rolId);
            if (!roles.titularesVigentes(dsl, rolId, ahora).isEmpty()) {
                throw new ErrorDeNegocio(
                        CON_ASIGNACIONES_VIGENTES, "Revoca primero las asignaciones vigentes de este rol.");
            }
            if (!roles.retirar(dsl, rolId, ahora)) {
                throw new ErrorDeNegocio(ROL_INEXISTENTE, "Ese rol ya no se puede retirar.");
            }
            eventos.emitir(dsl, "identidad.rol_retirado", rolId, ctx, motivo, Map.of("rolCodigo", rol.codigo()));
            return new Resultado("APLICADO", rolId, null);
        });
    }

    // ------------------------------------------------------------ cuatro ojos

    @Transactional(readOnly = true)
    public List<Solicitud> solicitudesPendientes(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> roles.solicitudesPendientes(dsl, FILAS_DE_SOLICITUDES).stream()
                .map(PropuestaDeRol::leer)
                .toList());
    }

    @Transactional
    public Resultado aprobar(UUID solicitudId, String motivo, String evidencia, ContextoSesion ctx) {
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
        reglas.exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeSolicitud fila = reglas.pendiente(dsl, solicitudId, ctx);
            Solicitud s = PropuestaDeRol.leer(fila);
            Map<String, String> riesgo = reglas.validarPermisos(dsl, s.permisos());
            if (!roles.resolver(dsl, solicitudId, "APROBADA", ctx.usuarioId(), motivo, ahora)) {
                throw new ErrorDeNegocio(SOLICITUD_YA_RESUELTA, "Otra persona ya resolvio esta solicitud.");
            }
            if ("CREAR".equals(s.tipo())) {
                if (roles.existeCodigo(dsl, s.codigo())) {
                    throw new ErrorDeNegocio(CODIGO_EN_USO, "Ya hay un rol con ese codigo.");
                }
                UUID rolId =
                        cambios.aplicarCreacion(dsl, s.codigo(), s.nombre(), s.descripcion(), s.permisos(), ctx, ahora);
                eventos.emitir(
                        dsl,
                        "identidad.rol_creado",
                        rolId,
                        ctx,
                        motivo,
                        EventosDeRoles.detalleDeAprobacion(
                                s, EventosDeRoles.detalleDeCreacion(s.codigo(), s.permisos())));
                return new Resultado("APLICADO", rolId, solicitudId);
            }
            FilaDeRol rol = reglas.editable(dsl, s.rolId());
            Set<String> actuales = accesos.permisosDelRol(dsl, s.rolId());
            Set<String> agregados = diferencia(s.permisos(), actuales);
            Set<String> quitados = diferencia(actuales, s.permisos());
            reglas.exigirQueNadieQuedeEnConflicto(dsl, s.rolId(), s.permisos(), ahora);
            cambios.aplicarEdicion(dsl, s.rolId(), s.nombre(), s.descripcion(), agregados, quitados);
            eventos.emitir(
                    dsl,
                    "identidad.rol_editado",
                    s.rolId(),
                    ctx,
                    motivo,
                    EventosDeRoles.detalleDeAprobacion(
                            s, EventosDeRoles.detalleDeEdicion(rol.codigo(), agregados, quitados)));
            return new Resultado("APLICADO", s.rolId(), solicitudId);
        });
    }

    @Transactional
    public Resultado rechazar(UUID solicitudId, String motivo, String evidencia, ContextoSesion ctx) {
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
        reglas.exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeSolicitud fila = reglas.pendiente(dsl, solicitudId, ctx);
            if (!roles.resolver(dsl, solicitudId, "RECHAZADA", ctx.usuarioId(), motivo, ahora)) {
                throw new ErrorDeNegocio(SOLICITUD_YA_RESUELTA, "Otra persona ya resolvio esta solicitud.");
            }
            eventos.emitir(
                    dsl,
                    "identidad.rol_solicitud_rechazada",
                    fila.rolId().orElse(solicitudId),
                    ctx,
                    motivo,
                    Map.of("tipo", fila.tipo(), "solicitudId", solicitudId.toString()));
            return new Resultado("RECHAZADA", fila.rolId().orElse(null), solicitudId);
        });
    }

    // ------------------------------------------------------------------ utilidades

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    private static Set<String> diferencia(Set<String> a, Set<String> b) {
        Set<String> d = new TreeSet<>(a);
        d.removeAll(b);
        return d;
    }
}
