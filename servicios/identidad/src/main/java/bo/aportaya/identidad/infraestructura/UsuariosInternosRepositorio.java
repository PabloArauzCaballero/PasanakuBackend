package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.CREDENCIAL_ACCESO;
import static bo.aportaya.identidad.generado.Tables.FACTOR_MFA;
import static bo.aportaya.identidad.generado.Tables.PERMISO;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.PermisoCatalogo;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public class UsuariosInternosRepositorio {

    private final AccesosRepositorio accesos;
    private final HasheadorDeCredencial hasheador;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    public UsuariosInternosRepositorio(AccesosRepositorio accesos, HasheadorDeCredencial hasheador) {
        this.accesos = accesos;
        this.hasheador = hasheador;
    }

    public List<UsuarioInternoResumen> listar(
            DSLContext dsl, String estado, String rol, String busqueda, OffsetDateTime ahora) {
        Condition condicion = DSL.noCondition();
        if (estado != null && !estado.isBlank()) {
            condicion = condicion.and(USUARIO.ESTADO.eq(estado));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            String patron = "%" + busqueda.trim().toLowerCase() + "%";
            condicion = condicion.and(DSL.lower(USUARIO.NOMBRES)
                    .like(patron)
                    .or(DSL.lower(USUARIO.APELLIDOS).like(patron))
                    .or(DSL.lower(USUARIO.CORREO).like(patron)));
        }

        var filas = dsl.select(
                        USUARIO_INTERNO.ID,
                        USUARIO_INTERNO.USUARIO_ID,
                        USUARIO.CODIGO_PUBLICO,
                        USUARIO.NOMBRES,
                        USUARIO.APELLIDOS,
                        USUARIO.CORREO,
                        USUARIO.TELEFONO_E164,
                        USUARIO_INTERNO.DEPARTAMENTO,
                        USUARIO_INTERNO.CARGO,
                        USUARIO.ESTADO,
                        USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA,
                        USUARIO_INTERNO.BLOQUEADO_HASTA,
                        USUARIO_INTERNO.MOTIVO_BLOQUEO)
                .from(USUARIO_INTERNO)
                .join(USUARIO)
                .on(USUARIO.ID.eq(USUARIO_INTERNO.USUARIO_ID))
                .where(condicion)
                .orderBy(USUARIO.APELLIDOS.asc(), USUARIO.NOMBRES.asc())
                .fetch();

        List<UsuarioInternoResumen> resultado = new ArrayList<>();
        for (Record r : filas) {
            UUID usuarioId = r.get(USUARIO_INTERNO.USUARIO_ID);
            Set<String> rolesVigentes = accesos.codigosDeRolesVigentes(dsl, usuarioId, ahora);
            if (rol != null && !rol.isBlank() && !rolesVigentes.contains(rol)) {
                continue;
            }

            boolean tieneSegundoFactor =
                    dsl.fetchExists(dsl.selectOne().from(FACTOR_MFA).where(FACTOR_MFA.USUARIO_ID.eq(usuarioId)));

            UsuarioInternoResumen resumen = new UsuarioInternoResumen();
            resumen.setId(r.get(USUARIO_INTERNO.ID));
            resumen.setUsuarioId(usuarioId);
            resumen.setCodigoPublico(r.get(USUARIO.CODIGO_PUBLICO));
            resumen.setNombres(r.get(USUARIO.NOMBRES));
            resumen.setApellidos(r.get(USUARIO.APELLIDOS));
            resumen.setCorreo(r.get(USUARIO.CORREO));
            resumen.setTelefono(r.get(USUARIO.TELEFONO_E164));
            resumen.setDepartamento(r.get(USUARIO_INTERNO.DEPARTAMENTO));
            resumen.setCargo(r.get(USUARIO_INTERNO.CARGO));
            resumen.setEstado(r.get(USUARIO.ESTADO));
            resumen.setDebeCambiarContrasena(r.get(USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA));
            resumen.setTieneSegundoFactor(tieneSegundoFactor);
            resumen.setBloqueadoHasta(r.get(USUARIO_INTERNO.BLOQUEADO_HASTA));
            resumen.setMotivoBloqueo(r.get(USUARIO_INTERNO.MOTIVO_BLOQUEO));
            resumen.setRoles(new ArrayList<>(rolesVigentes));

            resultado.add(resumen);
        }
        return resultado;
    }

    public Optional<UsuarioInternoDetalle> consultar(DSLContext dsl, UUID usuarioId, OffsetDateTime ahora) {
        Record r = dsl.select(
                        USUARIO_INTERNO.ID,
                        USUARIO_INTERNO.USUARIO_ID,
                        USUARIO.CODIGO_PUBLICO,
                        USUARIO.NOMBRES,
                        USUARIO.APELLIDOS,
                        USUARIO.CORREO,
                        USUARIO.TELEFONO_E164,
                        USUARIO_INTERNO.DEPARTAMENTO,
                        USUARIO_INTERNO.CARGO,
                        USUARIO.ESTADO,
                        USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA,
                        USUARIO_INTERNO.BLOQUEADO_HASTA,
                        USUARIO_INTERNO.MOTIVO_BLOQUEO,
                        USUARIO.FECHA_REGISTRO)
                .from(USUARIO_INTERNO)
                .join(USUARIO)
                .on(USUARIO.ID.eq(USUARIO_INTERNO.USUARIO_ID))
                .where(USUARIO.ID.eq(usuarioId).or(USUARIO_INTERNO.ID.eq(usuarioId)))
                .fetchOne();

        if (r == null) {
            return Optional.empty();
        }

        UUID uId = r.get(USUARIO_INTERNO.USUARIO_ID);
        Set<String> rolesVigentes = accesos.codigosDeRolesVigentes(dsl, uId, ahora);
        Set<String> permisosEfectivos = PermisosEfectivos.de(accesos.asignacionesDe(dsl, uId), ahora);
        boolean tieneSegundoFactor =
                dsl.fetchExists(dsl.selectOne().from(FACTOR_MFA).where(FACTOR_MFA.USUARIO_ID.eq(uId)));

        UsuarioInternoDetalle detalle = new UsuarioInternoDetalle();
        detalle.setId(r.get(USUARIO_INTERNO.ID));
        detalle.setUsuarioId(uId);
        detalle.setCodigoPublico(r.get(USUARIO.CODIGO_PUBLICO));
        detalle.setNombres(r.get(USUARIO.NOMBRES));
        detalle.setApellidos(r.get(USUARIO.APELLIDOS));
        detalle.setCorreo(r.get(USUARIO.CORREO));
        detalle.setTelefono(r.get(USUARIO.TELEFONO_E164));
        detalle.setDepartamento(r.get(USUARIO_INTERNO.DEPARTAMENTO));
        detalle.setCargo(r.get(USUARIO_INTERNO.CARGO));
        detalle.setEstado(r.get(USUARIO.ESTADO));
        detalle.setDebeCambiarContrasena(r.get(USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA));
        detalle.setTieneSegundoFactor(tieneSegundoFactor);
        detalle.setBloqueadoHasta(r.get(USUARIO_INTERNO.BLOQUEADO_HASTA));
        detalle.setMotivoBloqueo(r.get(USUARIO_INTERNO.MOTIVO_BLOQUEO));
        detalle.setRoles(new ArrayList<>(rolesVigentes));
        detalle.setPermisosEfectivos(new ArrayList<>(permisosEfectivos));
        detalle.setCreadoEn(r.get(USUARIO.FECHA_REGISTRO));
        detalle.setActualizadoEn(ahora);

        return Optional.of(detalle);
    }

    public UsuarioInternoDetalle crear(
            DSLContext dsl, EntradaAltaUsuarioInterno entrada, UUID operadorId, OffsetDateTime ahora) {
        UUID rolId = dsl.select(ROL.ID)
                .from(ROL)
                .where(ROL.CODIGO.eq(entrada.getRolCodigo()))
                .fetchOptional(ROL.ID)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "El rol especificado no existe."));

        UUID usuarioId = UUID.randomUUID();
        String codigoPublico = "OP-" + String.format("%04d", RANDOM.nextInt(10000));

        dsl.insertInto(USUARIO)
                .set(USUARIO.ID, usuarioId)
                .set(USUARIO.CODIGO_PUBLICO, codigoPublico)
                .set(USUARIO.NOMBRES, entrada.getNombres())
                .set(USUARIO.APELLIDOS, entrada.getApellidos())
                .set(USUARIO.CORREO, entrada.getCorreo())
                .set(USUARIO.TELEFONO_E164, entrada.getTelefono())
                .set(USUARIO.FECHA_NACIMIENTO, LocalDate.of(1990, 1, 1))
                .set(USUARIO.ESTADO, "ACTIVO")
                .set(USUARIO.NIVEL_KYC, "ESTANDAR")
                .set(USUARIO.IDIOMA, "ES_BO")
                .set(USUARIO.ZONA_HORARIA, "America/La_Paz")
                .set(USUARIO.FECHA_REGISTRO, ahora)
                .set(USUARIO.VERSION, 1)
                .execute();

        dsl.insertInto(USUARIO_INTERNO)
                .set(USUARIO_INTERNO.ID, UUID.randomUUID())
                .set(USUARIO_INTERNO.USUARIO_ID, usuarioId)
                .set(USUARIO_INTERNO.DEPARTAMENTO, entrada.getDepartamento())
                .set(USUARIO_INTERNO.CARGO, entrada.getCargo())
                .set(USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA, true)
                .execute();

        // Contrasena provisional
        String provisional = generarContrasenaProvisional();
        String hash = hasheador.hashear(provisional.toCharArray());
        dsl.insertInto(CREDENCIAL_ACCESO)
                .set(CREDENCIAL_ACCESO.ID, UUID.randomUUID())
                .set(CREDENCIAL_ACCESO.USUARIO_ID, usuarioId)
                .set(CREDENCIAL_ACCESO.HASH_CONTRASENA, hash)
                .set(CREDENCIAL_ACCESO.ALGORITMO, "ARGON2ID")
                .set(CREDENCIAL_ACCESO.REQUIERE_CAMBIO, true)
                .set(CREDENCIAL_ACCESO.CAMBIADA_EN, ahora)
                .execute();

        // Asignacion inicial del rol
        accesos.asignar(dsl, usuarioId, rolId, "GLOBAL", Optional.empty(), operadorId, Optional.empty(), ahora);

        return consultar(dsl, usuarioId, ahora).orElseThrow();
    }

    public UsuarioInternoDetalle actualizar(
            DSLContext dsl,
            UUID usuarioId,
            EntradaActualizarUsuarioInterno entrada,
            UUID operadorId,
            OffsetDateTime ahora) {
        UUID uId = resolverUsuarioId(dsl, usuarioId);

        if (entrada.getDepartamento() != null && !entrada.getDepartamento().isBlank()) {
            dsl.update(USUARIO_INTERNO)
                    .set(USUARIO_INTERNO.DEPARTAMENTO, entrada.getDepartamento())
                    .where(USUARIO_INTERNO.USUARIO_ID.eq(uId))
                    .execute();
        }
        if (entrada.getCargo() != null && !entrada.getCargo().isBlank()) {
            dsl.update(USUARIO_INTERNO)
                    .set(USUARIO_INTERNO.CARGO, entrada.getCargo())
                    .where(USUARIO_INTERNO.USUARIO_ID.eq(uId))
                    .execute();
        }
        if (entrada.getEstado() != null) {
            dsl.update(USUARIO)
                    .set(USUARIO.ESTADO, entrada.getEstado().getValue())
                    .where(USUARIO.ID.eq(uId))
                    .execute();
        }

        return consultar(dsl, uId, ahora).orElseThrow();
    }

    public UsuarioInternoDetalle asignarRoles(
            DSLContext dsl,
            UUID usuarioId,
            EntradaAsignarRolesUsuarioInterno entrada,
            UUID operadorId,
            OffsetDateTime ahora) {
        UUID uId = resolverUsuarioId(dsl, usuarioId);

        // Revocar asignaciones vivas existentes
        List<UUID> asignacionesVivas = dsl.select(ASIGNACION_ROL.ID)
                .from(ASIGNACION_ROL)
                .where(ASIGNACION_ROL.USUARIO_ID.eq(uId))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .fetch(ASIGNACION_ROL.ID);

        for (UUID asigId : asignacionesVivas) {
            accesos.revocar(dsl, asigId, entrada.getMotivo(), ahora);
        }

        // Asignar los nuevos roles
        for (String codRol : entrada.getRoles()) {
            UUID rId = dsl.select(ROL.ID)
                    .from(ROL)
                    .where(ROL.CODIGO.eq(codRol))
                    .fetchOptional(ROL.ID)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "El rol " + codRol + " no existe."));

            accesos.asignar(dsl, uId, rId, "GLOBAL", Optional.empty(), operadorId, Optional.empty(), ahora);
        }

        return consultar(dsl, uId, ahora).orElseThrow();
    }

    public UsuarioInternoDetalle desbloquear(
            DSLContext dsl, UUID usuarioId, String motivo, UUID operadorId, OffsetDateTime ahora) {
        UUID uId = resolverUsuarioId(dsl, usuarioId);

        dsl.update(USUARIO_INTERNO)
                .setNull(USUARIO_INTERNO.BLOQUEADO_HASTA)
                .setNull(USUARIO_INTERNO.MOTIVO_BLOQUEO)
                .where(USUARIO_INTERNO.USUARIO_ID.eq(uId))
                .execute();

        return consultar(dsl, uId, ahora).orElseThrow();
    }

    public List<RolCatalogo> listarRoles(DSLContext dsl) {
        var filas = dsl.select(
                        ROL.ID, ROL.CODIGO, ROL.NOMBRE, ROL.AMBITO, ROL.DEPARTAMENTO, ROL.DESCRIPCION, ROL.ES_SISTEMA)
                .from(ROL)
                .orderBy(ROL.DEPARTAMENTO.asc().nullsLast(), ROL.NOMBRE.asc())
                .fetch();

        List<RolCatalogo> lista = new ArrayList<>();
        for (Record r : filas) {
            UUID rolId = r.get(ROL.ID);
            Set<String> perms = accesos.permisosDelRol(dsl, rolId);

            RolCatalogo rc = new RolCatalogo();
            rc.setId(rolId);
            rc.setCodigo(r.get(ROL.CODIGO));
            rc.setNombre(r.get(ROL.NOMBRE));
            rc.setAmbito(r.get(ROL.AMBITO));
            rc.setDepartamento(r.get(ROL.DEPARTAMENTO));
            rc.setDescripcion(r.get(ROL.DESCRIPCION));
            rc.setEsSistema(r.get(ROL.ES_SISTEMA));
            rc.setPermisos(new ArrayList<>(perms));
            lista.add(rc);
        }
        return lista;
    }

    public List<PermisoCatalogo> listarPermisos(DSLContext dsl) {
        var filas = dsl.select(
                        PERMISO.ID,
                        PERMISO.CODIGO,
                        PERMISO.DESCRIPCION,
                        PERMISO.RECURSO,
                        PERMISO.ACCION,
                        PERMISO.NIVEL_RIESGO,
                        PERMISO.EXIGE_MOTIVO,
                        PERMISO.REQUIERE_MFA)
                .from(PERMISO)
                .orderBy(PERMISO.RECURSO.asc(), PERMISO.ACCION.asc())
                .fetch();

        List<PermisoCatalogo> lista = new ArrayList<>();
        for (Record r : filas) {
            PermisoCatalogo pc = new PermisoCatalogo();
            pc.setId(r.get(PERMISO.ID));
            pc.setCodigo(r.get(PERMISO.CODIGO));
            pc.setDescripcion(r.get(PERMISO.DESCRIPCION));
            pc.setRecurso(r.get(PERMISO.RECURSO));
            pc.setAccion(r.get(PERMISO.ACCION));
            pc.setNivelRiesgo(PermisoCatalogo.NivelRiesgoEnum.fromValue(r.get(PERMISO.NIVEL_RIESGO)));
            pc.setExigeMotivo(r.get(PERMISO.EXIGE_MOTIVO));
            pc.setRequiereMfa(r.get(PERMISO.REQUIERE_MFA));
            lista.add(pc);
        }
        return lista;
    }

    private UUID resolverUsuarioId(DSLContext dsl, UUID id) {
        return dsl.select(USUARIO.ID)
                .from(USUARIO)
                .leftJoin(USUARIO_INTERNO)
                .on(USUARIO_INTERNO.USUARIO_ID.eq(USUARIO.ID))
                .where(USUARIO.ID.eq(id).or(USUARIO_INTERNO.ID.eq(id)))
                .fetchOptional(USUARIO.ID)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "Usuario interno no encontrado."));
    }

    private String generarContrasenaProvisional() {
        StringBuilder sb = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
