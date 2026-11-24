package bo.aportaya.identidad.aplicacion;

import static bo.aportaya.identidad.dominio.ReglasDeRolPersonalizado.*;

import bo.aportaya.identidad.dominio.PermisosEfectivos;
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
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.jooq.DSLContext;
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

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int LIMITE_DE_SOLICITUDES = 100;

    private final Datos datos;
    private final RolesPersonalizadosRepositorio roles;
    private final AccesosRepositorio accesos;
    private final EvidenciaDeStepUp evidencias;
    private final Outbox outbox;
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
        this.outbox = outbox;
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
        validarForma(p.codigo(), p.nombre(), p.descripcion(), p.permisos(), p.motivo());
        exigirStepUp(ctx, p.evidencia());
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            if (roles.existeCodigo(dsl, p.codigo())) {
                throw new ErrorDeNegocio(CODIGO_EN_USO, "Ya hay un rol con ese codigo.");
            }
            Map<String, String> riesgo = validarPermisos(dsl, p.permisos());
            if (exigeCuatroOjos(p.permisos(), riesgo)) {
                UUID solicitud = roles.insertarSolicitud(
                        dsl,
                        "CREAR",
                        null,
                        propuesta(p.codigo(), p.nombre(), p.descripcion(), p.permisos()),
                        p.motivo(),
                        ctx.usuarioId(),
                        ahora);
                emitir(
                        dsl,
                        "identidad.rol_solicitado",
                        solicitud,
                        ctx,
                        p.motivo(),
                        Map.of("tipo", "CREAR", "rolCodigo", p.codigo(), "solicitudId", solicitud.toString()));
                return new Resultado("PENDIENTE_DE_APROBACION", null, solicitud);
            }
            UUID rolId = aplicarCreacion(dsl, p.codigo(), p.nombre(), p.descripcion(), p.permisos(), ctx, ahora);
            emitir(dsl, "identidad.rol_creado", rolId, ctx, p.motivo(), detalleDeCreacion(p.codigo(), p.permisos()));
            return new Resultado("APLICADO", rolId, null);
        });
    }

    // --------------------------------------------------------------- editar

    @Transactional
    public Resultado editar(UUID rolId, PedidoDeEdicion p, ContextoSesion ctx) {
        validarForma("ROL_EXISTENTE", p.nombre(), p.descripcion(), p.permisos(), p.motivo());
        exigirStepUp(ctx, p.evidencia());
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeRol rol = editable(dsl, rolId);
            Map<String, String> riesgo = validarPermisos(dsl, p.permisos());
            Set<String> actuales = accesos.permisosDelRol(dsl, rolId);
            Set<String> agregados = diferencia(p.permisos(), actuales);
            Set<String> quitados = diferencia(actuales, p.permisos());
            exigirQueNadieQuedeEnConflicto(dsl, rolId, p.permisos(), ahora);

            if (exigeCuatroOjos(agregados, riesgo)) {
                UUID solicitud = roles.insertarSolicitud(
                        dsl,
                        "EDITAR",
                        rolId,
                        propuesta(rol.codigo(), p.nombre(), p.descripcion(), p.permisos()),
                        p.motivo(),
                        ctx.usuarioId(),
                        ahora);
                emitir(
                        dsl,
                        "identidad.rol_solicitado",
                        rolId,
                        ctx,
                        p.motivo(),
                        Map.of("tipo", "EDITAR", "rolCodigo", rol.codigo(), "solicitudId", solicitud.toString()));
                return new Resultado("PENDIENTE_DE_APROBACION", rolId, solicitud);
            }
            aplicarEdicion(dsl, rolId, p.nombre(), p.descripcion(), agregados, quitados);
            emitir(
                    dsl,
                    "identidad.rol_editado",
                    rolId,
                    ctx,
                    p.motivo(),
                    detalleDeEdicion(rol.codigo(), agregados, quitados));
            return new Resultado("APLICADO", rolId, null);
        });
    }

    // -------------------------------------------------------------- retirar

    @Transactional
    public Resultado retirar(UUID rolId, String motivo, String evidencia, ContextoSesion ctx) {
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
        exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeRol rol = editable(dsl, rolId);
            if (!roles.titularesVigentes(dsl, rolId, ahora).isEmpty()) {
                throw new ErrorDeNegocio(
                        CON_ASIGNACIONES_VIGENTES, "Revoca primero las asignaciones vigentes de este rol.");
            }
            if (!roles.retirar(dsl, rolId, ahora)) {
                throw new ErrorDeNegocio(ROL_INEXISTENTE, "Ese rol ya no se puede retirar.");
            }
            emitir(dsl, "identidad.rol_retirado", rolId, ctx, motivo, Map.of("rolCodigo", rol.codigo()));
            return new Resultado("APLICADO", rolId, null);
        });
    }

    // ------------------------------------------------------------ cuatro ojos

    @Transactional(readOnly = true)
    public List<Solicitud> solicitudesPendientes(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> roles.solicitudesPendientes(dsl, LIMITE_DE_SOLICITUDES).stream()
                .map(CU135GobernarRoles::aSolicitud)
                .toList());
    }

    @Transactional
    public Resultado aprobar(UUID solicitudId, String motivo, String evidencia, ContextoSesion ctx) {
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
        exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeSolicitud fila = pendiente(dsl, solicitudId, ctx);
            Solicitud s = aSolicitud(fila);
            Map<String, String> riesgo = validarPermisos(dsl, s.permisos());
            if (!roles.resolver(dsl, solicitudId, "APROBADA", ctx.usuarioId(), motivo, ahora)) {
                throw new ErrorDeNegocio(SOLICITUD_YA_RESUELTA, "Otra persona ya resolvio esta solicitud.");
            }
            if ("CREAR".equals(s.tipo())) {
                if (roles.existeCodigo(dsl, s.codigo())) {
                    throw new ErrorDeNegocio(CODIGO_EN_USO, "Ya hay un rol con ese codigo.");
                }
                UUID rolId = aplicarCreacion(dsl, s.codigo(), s.nombre(), s.descripcion(), s.permisos(), ctx, ahora);
                emitir(
                        dsl,
                        "identidad.rol_creado",
                        rolId,
                        ctx,
                        motivo,
                        detalleDeAprobacion(s, detalleDeCreacion(s.codigo(), s.permisos())));
                return new Resultado("APLICADO", rolId, solicitudId);
            }
            FilaDeRol rol = editable(dsl, s.rolId());
            Set<String> actuales = accesos.permisosDelRol(dsl, s.rolId());
            Set<String> agregados = diferencia(s.permisos(), actuales);
            Set<String> quitados = diferencia(actuales, s.permisos());
            exigirQueNadieQuedeEnConflicto(dsl, s.rolId(), s.permisos(), ahora);
            aplicarEdicion(dsl, s.rolId(), s.nombre(), s.descripcion(), agregados, quitados);
            emitir(
                    dsl,
                    "identidad.rol_editado",
                    s.rolId(),
                    ctx,
                    motivo,
                    detalleDeAprobacion(s, detalleDeEdicion(rol.codigo(), agregados, quitados)));
            return new Resultado("APLICADO", s.rolId(), solicitudId);
        });
    }

    @Transactional
    public Resultado rechazar(UUID solicitudId, String motivo, String evidencia, ContextoSesion ctx) {
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
        exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            FilaDeSolicitud fila = pendiente(dsl, solicitudId, ctx);
            if (!roles.resolver(dsl, solicitudId, "RECHAZADA", ctx.usuarioId(), motivo, ahora)) {
                throw new ErrorDeNegocio(SOLICITUD_YA_RESUELTA, "Otra persona ya resolvio esta solicitud.");
            }
            emitir(
                    dsl,
                    "identidad.rol_solicitud_rechazada",
                    fila.rolId().orElse(solicitudId),
                    ctx,
                    motivo,
                    Map.of("tipo", fila.tipo(), "solicitudId", solicitudId.toString()));
            return new Resultado("RECHAZADA", fila.rolId().orElse(null), solicitudId);
        });
    }

    // ---------------------------------------------------------------- reglas

    private static void validarForma(
            String codigo, String nombre, String descripcion, Set<String> permisos, String motivo) {
        if (!codigoValido(codigo) && !"ROL_EXISTENTE".equals(codigo)) {
            throw new ErrorDeNegocio(
                    CODIGO_INVALIDO,
                    "El codigo va en mayusculas, sin espacios, de 3 a 30 caracteres, y no puede empezar con"
                            + " SERVICIO_.");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ErrorDeNegocio(CODIGO_INVALIDO, "El nombre es obligatorio (hasta 60 caracteres).");
        }
        if (descripcion != null && descripcion.length() > LARGO_MAXIMO_DESCRIPCION) {
            throw new ErrorDeNegocio(CODIGO_INVALIDO, "La descripcion admite hasta 255 caracteres.");
        }
        if (permisos == null || permisos.isEmpty()) {
            throw new ErrorDeNegocio(SIN_PERMISOS, "Un rol necesita al menos un permiso.");
        }
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
    }

    /** Los permisos existen y no rompen la segregacion; devuelve su riesgo. */
    private Map<String, String> validarPermisos(DSLContext dsl, Set<String> permisos) {
        Set<String> faltan = roles.inexistentes(dsl, permisos);
        if (!faltan.isEmpty()) {
            throw new ErrorDeNegocio(
                    PERMISO_INEXISTENTE, "Hay permisos que no existen en el catalogo.", Map.of("permisos", faltan));
        }
        conflicto(permisos).ifPresent(par -> {
            throw new ErrorDeNegocio(
                    PAR_INCOMPATIBLE, "Nadie autoriza y ejecuta la misma operacion.", Map.of("parIncompatible", par));
        });
        return roles.riesgoDe(dsl, permisos);
    }

    /** El nuevo conjunto no puede dejar a un titular vigente con un par incompatible (R-SEG-04 sobre lo ya otorgado). */
    private void exigirQueNadieQuedeEnConflicto(DSLContext dsl, UUID rolId, Set<String> nuevos, OffsetDateTime ahora) {
        for (UUID titular : roles.titularesVigentes(dsl, rolId, ahora)) {
            Set<String> resultantes = new TreeSet<>(PermisosEfectivos.de(accesos.asignacionesDe(dsl, titular), ahora));
            resultantes.addAll(nuevos);
            conflicto(resultantes).ifPresent(par -> {
                throw new ErrorDeNegocio(
                        ASIGNADO_QUEDARIA_EN_CONFLICTO,
                        "Un titular de este rol quedaria con un par incompatible de permisos.",
                        Map.of("parIncompatible", par));
            });
        }
    }

    private FilaDeRol editable(DSLContext dsl, UUID rolId) {
        FilaDeRol rol =
                roles.rol(dsl, rolId).orElseThrow(() -> new ErrorDeNegocio(ROL_INEXISTENTE, "Ese rol no existe."));
        if (rol.esSistema()) {
            throw new ErrorDeNegocio(ROL_DE_SISTEMA, "Los roles del sistema no se editan ni se retiran.");
        }
        if (rol.retirado()) {
            throw new ErrorDeNegocio(ROL_INEXISTENTE, "Ese rol ya esta retirado.");
        }
        return rol;
    }

    private FilaDeSolicitud pendiente(DSLContext dsl, UUID solicitudId, ContextoSesion ctx) {
        FilaDeSolicitud fila = roles.solicitud(dsl, solicitudId)
                .orElseThrow(() -> new ErrorDeNegocio(SOLICITUD_INEXISTENTE, "Esa solicitud no existe."));
        if (!"PENDIENTE".equals(fila.estado())) {
            throw new ErrorDeNegocio(SOLICITUD_YA_RESUELTA, "Esa solicitud ya se resolvio.");
        }
        if (fila.propuestaPor().equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(
                    APROBADOR_ES_EL_PROPONENTE,
                    "Lo tiene que resolver otra persona: no podes resolver lo que propusiste.");
        }
        return fila;
    }

    private void exigirStepUp(ContextoSesion ctx, String evidencia) {
        if (evidencia == null || !evidencias.consumir(ctx.usuarioId(), evidencia)) {
            throw new ErrorDeNegocio(
                    STEP_UP_REQUERIDO, "Confirma esta accion con tu segundo factor y volve a intentarlo.");
        }
    }

    // --------------------------------------------------------------- escritura

    private UUID aplicarCreacion(
            DSLContext dsl,
            String codigo,
            String nombre,
            String descripcion,
            Set<String> permisos,
            ContextoSesion ctx,
            OffsetDateTime ahora) {
        UUID rolId = roles.insertarRol(dsl, codigo, nombre, descripcion, ctx.usuarioId(), ahora);
        roles.agregarPermisos(dsl, rolId, permisos);
        return rolId;
    }

    private void aplicarEdicion(
            DSLContext dsl,
            UUID rolId,
            String nombre,
            String descripcion,
            Set<String> agregados,
            Set<String> quitados) {
        if (!roles.actualizarRol(dsl, rolId, nombre, descripcion)) {
            throw new ErrorDeNegocio(ROL_DE_SISTEMA, "Ese rol no se puede editar.");
        }
        roles.agregarPermisos(dsl, rolId, agregados);
        roles.quitarPermisos(dsl, rolId, quitados);
    }

    // ------------------------------------------------------------------ eventos

    private void emitir(
            DSLContext dsl,
            String tipo,
            UUID agregadoId,
            ContextoSesion ctx,
            String motivo,
            Map<String, Object> detalle) {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("version", "1");
        carga.put("actorId", ctx.usuarioId().toString());
        carga.put("actorRol", ctx.rol());
        carga.put("origen", "PANEL_ADMIN");
        carga.put("motivo", motivo);
        carga.put("ocurridoEn", ahora().toString());
        carga.putAll(detalle);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        "rol",
                        agregadoId,
                        carga,
                        UUID.fromString(ctx.traza().id())));
    }

    private static Map<String, Object> detalleDeCreacion(String codigo, Set<String> permisos) {
        return Map.of("rolCodigo", codigo, "permisosAgregados", List.copyOf(new TreeSet<>(permisos)));
    }

    private static Map<String, Object> detalleDeEdicion(String codigo, Set<String> agregados, Set<String> quitados) {
        return Map.of(
                "rolCodigo", codigo,
                "permisosAgregados", List.copyOf(new TreeSet<>(agregados)),
                "permisosQuitados", List.copyOf(new TreeSet<>(quitados)));
    }

    private static Map<String, Object> detalleDeAprobacion(Solicitud s, Map<String, Object> base) {
        Map<String, Object> detalle = new LinkedHashMap<>(base);
        detalle.put("solicitudId", s.id().toString());
        detalle.put("propuestaPor", s.propuestaPor().toString());
        return detalle;
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

    private static String propuesta(String codigo, String nombre, String descripcion, Set<String> permisos) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", codigo);
        m.put("nombre", nombre);
        m.put("descripcion", descripcion);
        m.put("permisos", List.copyOf(new TreeSet<>(permisos)));
        try {
            return JSON.writeValueAsString(m);
        } catch (JsonProcessingException imposible) {
            throw new IllegalStateException("La propuesta de rol no se puede serializar", imposible);
        }
    }

    @SuppressWarnings("unchecked")
    private static Solicitud aSolicitud(FilaDeSolicitud fila) {
        try {
            Map<String, Object> m = JSON.readValue(fila.propuestaJson(), Map.class);
            Set<String> permisos = new TreeSet<>((List<String>) m.get("permisos"));
            return new Solicitud(
                    fila.id(),
                    fila.tipo(),
                    fila.rolId().orElse(null),
                    (String) m.get("codigo"),
                    (String) m.get("nombre"),
                    (String) m.get("descripcion"),
                    permisos,
                    fila.motivo(),
                    fila.propuestaPor(),
                    fila.propuestaEn());
        } catch (JsonProcessingException ilegible) {
            throw new IllegalStateException("La propuesta guardada de la solicitud no se puede leer", ilegible);
        }
    }
}
