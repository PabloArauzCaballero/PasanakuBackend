package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.CREDENCIAL_ACCESO;
import static bo.aportaya.identidad.generado.Tables.FACTOR_MFA;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.dominio.SegregacionDeFunciones;
import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
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
    private final RolesYPermisosRepositorio rolesYPermisos;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    public UsuariosInternosRepositorio(
            AccesosRepositorio accesos, HasheadorDeCredencial hasheador, RolesYPermisosRepositorio rolesYPermisos) {
        this.accesos = accesos;
        this.hasheador = hasheador;
        this.rolesYPermisos = rolesYPermisos;
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

            resultado.add(UsuarioInternoMapeador.aResumen(r, usuarioId, rolesVigentes, tieneSegundoFactor));
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

        UsuarioInternoDetalle detalle =
                UsuarioInternoMapeador.aDetalle(r, uId, rolesVigentes, permisosEfectivos, tieneSegundoFactor);
        detalle.setActualizadoEn(ahora);

        return Optional.of(detalle);
    }

    public UsuarioInternoDetalle crear(
            DSLContext dsl, EntradaAltaUsuarioInterno entrada, UUID operadorId, OffsetDateTime ahora) {
        if (dsl.fetchExists(dsl.selectOne().from(USUARIO).where(USUARIO.CORREO.eq(entrada.getCorreo())))) {
            throw new ErrorDeNegocio(CodigoError.de(1, 2), "Ya existe un usuario registrado con ese correo.");
        }
        if (dsl.fetchExists(dsl.selectOne().from(USUARIO).where(USUARIO.TELEFONO_E164.eq(entrada.getTelefono())))) {
            throw new ErrorDeNegocio(CodigoError.de(1, 3), "Ya existe un usuario registrado con ese telefono.");
        }

        UUID rolId = dsl.select(ROL.ID)
                .from(ROL)
                .where(ROL.CODIGO.eq(entrada.getRolCodigo()))
                .fetchOptional(ROL.ID)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "El rol especificado no existe."));

        UUID usuarioId = UUID.randomUUID();
        String codigoPublico =
                "OP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);

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
            String nuevoEstado = entrada.getEstado().getValue();
            dsl.update(USUARIO)
                    .set(USUARIO.ESTADO, nuevoEstado)
                    .where(USUARIO.ID.eq(uId))
                    .execute();

            if (!"ACTIVO".equalsIgnoreCase(nuevoEstado)) {
                String motivo =
                        entrada.getMotivo() != null && !entrada.getMotivo().isBlank()
                                ? entrada.getMotivo()
                                : "Cambio de estado a " + nuevoEstado;
                accesos.cerrarSesionesDe(dsl, uId, motivo, ahora);
            }
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

        if (uId.equals(operadorId)) {
            throw new ErrorDeNegocio(CodigoError.de(8, 4), "Nadie se amplia sus propios permisos.");
        }

        Set<String> resultantes = new java.util.TreeSet<>();
        List<UUID> nuevosRolIds = new ArrayList<>();
        for (String codRol : entrada.getRoles()) {
            UUID rId = dsl.select(ROL.ID)
                    .from(ROL)
                    .where(ROL.CODIGO.eq(codRol))
                    .fetchOptional(ROL.ID)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "El rol " + codRol + " no existe."));
            nuevosRolIds.add(rId);
            resultantes.addAll(accesos.permisosDelRol(dsl, rId));
        }

        SegregacionDeFunciones.conflicto(resultantes).ifPresent(par -> {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 3),
                    "Nadie autoriza y ejecuta la misma operacion.",
                    java.util.Map.of("parIncompatible", par));
        });

        // Revocar asignaciones vivas existentes garantizando permanencia de administracion de accesos
        List<UUID> asignacionesVivas = dsl.select(ASIGNACION_ROL.ID)
                .from(ASIGNACION_ROL)
                .where(ASIGNACION_ROL.USUARIO_ID.eq(uId))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .fetch(ASIGNACION_ROL.ID);

        for (UUID asigId : asignacionesVivas) {
            boolean otorgaLaAdministracion = accesos.rolDe(dsl, asigId)
                    .map(rol -> accesos.permisosDelRol(dsl, rol).contains("ACCESOS_ADMINISTRAR"))
                    .orElse(false);
            if (otorgaLaAdministracion && accesos.cuantosConservan(dsl, "ACCESOS_ADMINISTRAR", asigId) == 0) {
                throw new ErrorDeNegocio(
                        CodigoError.de(8, 6), "La plataforma no puede quedarse sin quien administre accesos.");
            }
            accesos.revocar(dsl, asigId, entrada.getMotivo(), ahora);
        }

        // Invalidar sesiones activas del usuario al revocar/rotar roles
        accesos.cerrarSesionesDe(dsl, uId, entrada.getMotivo(), ahora);

        // Asignar los nuevos roles
        for (UUID rId : nuevosRolIds) {
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
