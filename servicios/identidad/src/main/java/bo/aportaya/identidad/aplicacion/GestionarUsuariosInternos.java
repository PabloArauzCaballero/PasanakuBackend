package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio.Asignacion;
import bo.aportaya.identidad.infraestructura.RolesYPermisosRepositorio;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.PermisoCatalogo;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, edicion, suspension, reactivacion, desbloqueo y el reemplazo de roles (deprecado) del personal interno.
 *
 * <p>Desde GU-11 (decision P-7) todo acto de gobierno pide step-up y motivo, nadie lo ejerce sobre si mismo y
 * ninguno deja la plataforma sin administrador de accesos. Desde GU-07 (P-5) cada acto queda en la bitacora y
 * pide el aviso al afectado y al oficial de cumplimiento ({@link BitacoraDeAccesos}). Editar departamento o
 * cargo no es un acto de gobierno: no pide step-up.
 */
@Service
public class GestionarUsuariosInternos {

    private final Datos datos;
    private final UsuariosInternosRepositorio repositorio;
    private final RolesYPermisosRepositorio rolesYPermisos;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final ReglasDeGobierno reglas;
    private final RetiroDeAccesos retiro;
    private final BitacoraDeAccesos bitacora;
    private final Reloj reloj;
    private final Outbox outbox;

    public GestionarUsuariosInternos(
            Datos datos,
            UsuariosInternosRepositorio repositorio,
            RolesYPermisosRepositorio rolesYPermisos,
            AccesosRepositorio accesos,
            GobiernoDeAccesosRepositorio gobierno,
            ReglasDeGobierno reglas,
            Reloj reloj,
            Outbox outbox) {
        this.datos = datos;
        this.repositorio = repositorio;
        this.rolesYPermisos = rolesYPermisos;
        this.gobierno = gobierno;
        this.reglas = reglas;
        this.retiro = new RetiroDeAccesos(accesos, gobierno);
        this.bitacora = new BitacoraDeAccesos(outbox, gobierno);
        this.reloj = reloj;
        this.outbox = outbox;
    }

    @Transactional
    public UsuarioInternoDetalle crear(EntradaAltaUsuarioInterno entrada, ContextoSesion ctx) {
        String motivo = motivoExigido(entrada.getMotivo());
        reglas.exigirStepUp(ctx, entrada.getEvidenciaMfa());
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UsuarioInternoDetalle detalle = repositorio.crear(dsl, entrada, ctx.usuarioId(), ahora);
            registrar(
                    dsl,
                    HechoDeAcceso.Accion.OPERADOR_DADO_DE_ALTA,
                    detalle.getUsuarioId(),
                    motivo,
                    Map.of(
                            "rolCodigo", entrada.getRolCodigo(),
                            "departamento", entrada.getDepartamento(),
                            "cargo", entrada.getCargo()),
                    ctx,
                    ahora);
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
        boolean cambiaEstado = entrada.getEstado() != null;
        if (cambiaEstado) {
            reglas.exigirStepUp(ctx, entrada.getEvidenciaMfa());
        }
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            String motivo = entrada.getMotivo() != null ? entrada.getMotivo() : "";
            Optional<HechoDeAcceso.Accion> acto = Optional.empty();
            Map<String, Object> detalleDelActo = Map.of();
            if (cambiaEstado) {
                UUID usuario = gobierno.operador(dsl, id).orElseThrow(GobiernoDeAccesos::noHabilitado);
                ReglasDeGobierno.noSobreSiMismo(ctx, usuario);
                String antes = gobierno.estadoDelOperador(dsl, usuario).orElse("");
                String nuevo = entrada.getEstado().getValue();
                if (SUSPENDIDO.equals(nuevo) && !SUSPENDIDO.equals(antes)) {
                    RetiroDeAccesos.Retiro hecho = retiro.retirar(dsl, usuario, "SUSPENSION", motivo, ahora);
                    acto = Optional.of(HechoDeAcceso.Accion.OPERADOR_SUSPENDIDO);
                    detalleDelActo = Map.of(
                            "estadoAnterior", antes,
                            "asignacionesRevocadas",
                                    hecho.asignacionesRevocadas().size(),
                            "sesionesCerradas", hecho.sesionesCerradas());
                } else if (ACTIVO.equals(nuevo) && !ACTIVO.equals(antes)) {
                    // Vuelve SIN roles: los retiro la suspension y se otorgan de nuevo uno por uno.
                    acto = Optional.of(HechoDeAcceso.Accion.OPERADOR_REACTIVADO);
                    detalleDelActo = Map.of("estadoAnterior", antes);
                }
            }

            UsuarioInternoDetalle detalle = repositorio.actualizar(dsl, id, entrada, ctx.usuarioId(), ahora);
            UUID correlacion = BitacoraDeAccesos.correlacionDe(ctx);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.usuario_interno_actualizado",
                            "usuario_interno",
                            detalle.getUsuarioId(),
                            Map.of(
                                    "usuarioId",
                                    detalle.getUsuarioId().toString(),
                                    "motivo",
                                    motivo,
                                    "estado",
                                    detalle.getEstado() != null ? detalle.getEstado() : ""),
                            correlacion));
            if (acto.isPresent()) {
                registrar(dsl, acto.get(), detalle.getUsuarioId(), motivo, detalleDelActo, ctx, ahora);
            }
            return detalle;
        });
    }

    /**
     * DEPRECADO (GU-05): el reemplazo completo de roles. Se mantiene compatible mientras el backoffice migra a
     * {@link GobiernoDeAccesos}; desde GU-06 aplica la diferencia y deja en la bitacora cada rol que entra o sale.
     */
    @Transactional
    public UsuarioInternoDetalle asignarRoles(UUID id, EntradaAsignarRolesUsuarioInterno entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            Map<UUID, Asignacion> antes = porId(gobierno.operador(dsl, id)
                    .map(u -> gobierno.sinRevocarDe(dsl, u))
                    .orElse(List.of()));
            UsuarioInternoDetalle detalle = repositorio.asignarRoles(dsl, id, entrada, ctx.usuarioId(), ahora);
            Map<UUID, Asignacion> despues = porId(gobierno.sinRevocarDe(dsl, detalle.getUsuarioId()));
            String motivo = entrada.getMotivo();
            for (Asignacion entra : despues.values()) {
                if (!antes.containsKey(entra.id())) {
                    registrarRol(dsl, HechoDeAcceso.Accion.ROL_ASIGNADO, entra, motivo, ctx, ahora);
                }
            }
            for (Asignacion sale : antes.values()) {
                if (!despues.containsKey(sale.id())) {
                    registrarRol(dsl, HechoDeAcceso.Accion.ROL_REVOCADO, sale, motivo, ctx, ahora);
                }
            }
            return detalle;
        });
    }

    @Transactional
    public UsuarioInternoDetalle desbloquear(UUID id, String motivo, String evidenciaMfa, ContextoSesion ctx) {
        reglas.exigirStepUp(ctx, evidenciaMfa);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UUID usuario = gobierno.operador(dsl, id).orElseThrow(GobiernoDeAccesos::noHabilitado);
            ReglasDeGobierno.noSobreSiMismo(ctx, usuario);
            UsuarioInternoDetalle detalle = repositorio.desbloquear(dsl, id, motivo, ctx.usuarioId(), ahora);
            registrar(
                    dsl,
                    HechoDeAcceso.Accion.OPERADOR_DESBLOQUEADO,
                    detalle.getUsuarioId(),
                    motivo != null ? motivo : "",
                    Map.of(),
                    ctx,
                    ahora);
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

    private static final String ACTIVO = "ACTIVO";
    private static final String SUSPENDIDO = "SUSPENDIDO";

    /** AP-CU08-11: el alta no viaja sin el porque (P-7); el contrato lo deja opcional solo por compatibilidad. */
    private static String motivoExigido(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ErrorDeNegocio(CodigoError.de(8, 11), "Escribi el motivo del alta: queda en la bitacora.");
        }
        return motivo;
    }

    private void registrar(
            DSLContext dsl,
            HechoDeAcceso.Accion accion,
            UUID afectado,
            String motivo,
            Map<String, Object> detalle,
            ContextoSesion ctx,
            OffsetDateTime ahora) {
        bitacora.registrar(
                dsl,
                new HechoDeAcceso(accion, Optional.of(ctx.usuarioId()), afectado, motivo, ahora, detalle),
                afectado,
                ctx);
    }

    private void registrarRol(
            DSLContext dsl,
            HechoDeAcceso.Accion accion,
            Asignacion asignacion,
            String motivo,
            ContextoSesion ctx,
            OffsetDateTime ahora) {
        Map<String, Object> detalle = Map.of(
                "asignacionId", asignacion.id().toString(),
                "rolId", asignacion.rolId().toString(),
                "rolCodigo", asignacion.rolCodigo(),
                "ambito", asignacion.ambito(),
                "origen", "PUT /usuarios-internos/{id}/roles");
        bitacora.registrar(
                dsl,
                new HechoDeAcceso(accion, Optional.of(ctx.usuarioId()), asignacion.usuarioId(), motivo, ahora, detalle),
                asignacion.id(),
                ctx);
    }

    private static Map<UUID, Asignacion> porId(List<Asignacion> asignaciones) {
        Map<UUID, Asignacion> mapa = new LinkedHashMap<>();
        asignaciones.forEach(a -> mapa.put(a.id(), a));
        return mapa;
    }
}
