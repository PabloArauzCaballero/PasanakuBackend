package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.CREDENCIAL_ACCESO;
import static bo.aportaya.identidad.generado.Tables.ROL;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import bo.aportaya.identidad.dominio.RolesParaPersonalInterno;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class UsuariosInternosRepositorio {

    private final AccesosRepositorio accesos;
    private final HasheadorDeCredencial hasheador;
    private final RolesYPermisosRepositorio rolesYPermisos;
    private final LecturaDeUsuariosInternos lectura;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    public UsuariosInternosRepositorio(
            AccesosRepositorio accesos, HasheadorDeCredencial hasheador, RolesYPermisosRepositorio rolesYPermisos) {
        this.accesos = accesos;
        this.hasheador = hasheador;
        this.rolesYPermisos = rolesYPermisos;
        this.lectura = new LecturaDeUsuariosInternos(accesos);
    }

    public List<UsuarioInternoResumen> listar(DSLContext dsl, FiltroDeUsuariosInternos filtro, OffsetDateTime ahora) {
        return lectura.listar(dsl, filtro, ahora);
    }

    public Optional<UsuarioInternoDetalle> consultar(DSLContext dsl, UUID usuarioId, OffsetDateTime ahora) {
        return lectura.consultar(dsl, usuarioId, ahora);
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
                .set(USUARIO.NIVEL_KYC, "NINGUNO")
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
        HasheadorDeCredencial.Parametros kdf = hasheador.parametros();
        dsl.insertInto(CREDENCIAL_ACCESO)
                .set(CREDENCIAL_ACCESO.ID, UUID.randomUUID())
                .set(CREDENCIAL_ACCESO.USUARIO_ID, usuarioId)
                .set(CREDENCIAL_ACCESO.HASH_CONTRASENA, hash)
                .set(CREDENCIAL_ACCESO.ALGORITMO, kdf.algoritmo())
                .set(CREDENCIAL_ACCESO.PARAMETROS_KDF, org.jooq.JSONB.valueOf(kdf.comoJson()))
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
        List<String> ajenos = rolesYPermisos.noAsignablesAPersonalInterno(dsl, entrada.getRoles());
        if (!ajenos.isEmpty()) {
            throw new ErrorDeNegocio(
                    RolesParaPersonalInterno.ROL_NO_ASIGNABLE,
                    "Esos roles no se otorgan a personal interno.",
                    Map.of("roles", ajenos));
        }

        RotacionDeRolesInternos.rotar(accesos, dsl, uId, entrada.getRoles(), entrada.getMotivo(), operadorId, ahora);

        return consultar(dsl, uId, ahora).orElseThrow();
    }

    public UsuarioInternoDetalle desbloquear(
            DSLContext dsl, UUID usuarioId, String motivo, UUID operadorId, OffsetDateTime ahora) {
        UUID uId = resolverUsuarioId(dsl, usuarioId);

        // IDCU-SEG-089: se libera el modelo que lee el login (`bloqueo_cuenta`).
        accesos.liberarBloqueos(dsl, uId, operadorId, ahora);

        // La columna vieja se sigue limpiando hasta que el cambio de esquema la retire
        // (F1.L4.M01 / F0.L4.M04): la vista de detalle todavia la muestra.
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
