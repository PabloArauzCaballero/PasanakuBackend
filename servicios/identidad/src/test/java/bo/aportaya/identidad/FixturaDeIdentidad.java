package bo.aportaya.identidad;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.jooq.DSLContext;

/**
 * Filas minimas para probar el ingreso, con las columnas del modelo y no de la
 * memoria.
 *
 * <p>Se inserta como dueno de la base —superusuario, sin RLS— porque sembrar no es lo
 * que se esta probando. Lo que se prueba corre despues, con el rol del servicio.
 */
final class FixturaDeIdentidad {

    private final DSLContext dsl;

    FixturaDeIdentidad(DSLContext dsl) {
        this.dsl = dsl;
    }

    UUID usuario(String telefono) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.usuario
                    (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento,
                     estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
                VALUES (?, ?, 'Ana', 'Quispe', ?, DATE '1990-01-01', 'ACTIVO', 'BASICO',
                        'es', 'America/La_Paz', now())
                """,
                id,
                "AY-" + id.toString().substring(0, 8),
                telefono);
        return id;
    }

    void credencial(UUID usuarioId, String hash) {
        dsl.execute(
                """
                INSERT INTO identidad.credencial_acceso
                    (id, usuario_id, hash_contrasena, algoritmo, parametros_kdf, requiere_cambio, cambiada_en)
                VALUES (gen_random_uuid(), ?, ?, 'ARGON2ID', '{}'::jsonb, false, now())
                """,
                usuarioId,
                hash);
    }

    /** Un rol de ambito GLOBAL es lo que convierte al usuario en operador. */
    UUID rolGlobal(String codigo) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.rol (id, codigo, nombre, ambito, es_sistema)
                VALUES (?, ?, ?, 'GLOBAL', false)
                """,
                id,
                codigo,
                codigo);
        return id;
    }

    /**
     * Nadie se asigna un rol a si mismo: lo impide {@code ck_asignacion_no_autoasignada}
     * (R-SEG-07), y por eso la fixtura tiene su propio otorgante.
     */
    private UUID otorgante;

    UUID otorgante() {
        if (otorgante == null) {
            // Los seis digitos salian de System.nanoTime(), y ahi el telefono se
            // repetia: con dias de encendido, los primeros seis digitos de nanoTime
            // cambian cada diez segundos, y uq_usuario_telefono_e164 no perdona dos
            // otorgantes con el mismo numero. Un aleatorio de seis digitos es lo que
            // esto necesitaba desde el principio: un numero distinto, no un reloj.
            otorgante = usuario("+59179" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        }
        return otorgante;
    }

    void asignarRol(UUID usuarioId, UUID rolId) {
        asignarRol(usuarioId, rolId, otorgante());
    }

    void asignarRol(UUID usuarioId, UUID rolId, UUID asignadaPor) {
        dsl.execute(
                """
                INSERT INTO identidad.asignacion_rol
                    (id, usuario_id, rol_id, ambito, otorgada_por, otorgada_en)
                VALUES (gen_random_uuid(), ?, ?, 'GLOBAL', ?, now())
                """,
                usuarioId,
                rolId,
                asignadaPor);
    }

    UUID permiso(String codigo, String recurso, String accion, boolean requiereMfa) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.permiso (id, codigo, descripcion, recurso, accion, requiere_mfa)
                VALUES (?, ?, 'permiso de prueba', ?, ?, ?)
                """,
                id,
                codigo,
                recurso,
                accion,
                requiereMfa);
        return id;
    }

    void darPermisoAlRol(UUID rolId, UUID permisoId) {
        dsl.execute("INSERT INTO identidad.rol_permiso (rol_id, permiso_id) VALUES (?, ?)", rolId, permisoId);
    }

    UUID sesionAbierta(UUID usuarioId) {
        UUID dispositivo = dispositivoConfiable(usuarioId, "huella-" + UUID.randomUUID());
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.sesion
                    (id, usuario_id, dispositivo_id, iniciada_en, ultima_actividad_en, expira_en, ip_origen)
                VALUES (?, ?, ?, now(), now(), now() + interval '12 hours', '127.0.0.1'::inet)
                """,
                id,
                usuarioId,
                dispositivo);
        return id;
    }

    /**
     * El correo que la persona confirmo con codigo en el alta: el token
     * {@code VERIFICACION_CORREO} consumido, vinculado a la cuenta y con la huella del
     * correo. Es lo mismo que deja CU-01, sin pasar por el envio.
     */
    void correoConfirmadoEnAlta(UUID usuarioId, String correo, String huellaDelCorreo) {
        dsl.execute("UPDATE identidad.usuario SET correo = ? WHERE id = ?", correo, usuarioId);
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                    (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion,
                     max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia,
                     canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                VALUES ('VERIFICACION_CORREO', 600, 6, 5, 2, 60, 5, 'CORREO', false, true,
                        '2026-10-07T00:00:00-04:00'::timestamptz)
                ON CONFLICT (proposito, vigente_desde) DO NOTHING
                """);
        dsl.execute(
                """
                INSERT INTO identidad.token_verificacion
                    (id, usuario_id, politica_id, tipo_token, proposito, hash_token, algoritmo_hash,
                     canal_entrega, destino_enmascarado, estado, expira_en, max_intentos, reenvios,
                     ip_origen, agente_usuario, correlation_id, clave_idempotencia, firma_hmac)
                SELECT gen_random_uuid(), ?, p.id, 'OTP', 'VERIFICACION_CORREO', gen_random_uuid()::text, 'HMAC-SHA256',
                       'CORREO', 'a***@example.com', 'CONSUMIDO', now() + interval '10 minutes', 5, 0,
                       '127.0.0.1'::inet, 'prueba', gen_random_uuid(), gen_random_uuid()::text, ?
                  FROM identidad.politica_token p
                 WHERE p.proposito = 'VERIFICACION_CORREO'
                 LIMIT 1
                """,
                usuarioId,
                huellaDelCorreo);
    }

    void factor(UUID usuarioId, String tipo, boolean activo, boolean confirmado) {
        dsl.execute(
                """
                INSERT INTO identidad.factor_mfa
                    (id, usuario_id, tipo, secreto_cifrado, version_llave, activo, es_principal, confirmado_en)
                VALUES (gen_random_uuid(), ?, ?, 'cifrado', 1, ?, true, ?::timestamptz)
                """,
                usuarioId,
                tipo,
                activo,
                confirmado ? OffsetDateTime.now().toString() : null);
    }

    UUID dispositivoConfiable(UUID usuarioId, String huella) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.dispositivo
                    (id, usuario_id, huella, plataforma, modelo, version_app, es_confiable, ultimo_uso_en)
                VALUES (?, ?, ?, 'ANDROID', 'prueba', '1.0', true, now())
                """,
                id,
                usuarioId,
                huella);
        return id;
    }

    /**
     * Ingresos normales con segundo factor, ya terminados: cada uno deja el par que deja
     * CU-04 en la vida real — primero {@code FACTOR_REQUERIDO} (no exitoso) y enseguida el
     * exitoso con el codigo. Con {@code clock_timestamp()} (no {@code now()}, que es la hora de la
     * transaccion) lo sembrado antes queda antes.
     */
    void ingresosConSegundoFactor(UUID usuarioId, int cuantos) {
        for (int i = 0; i < cuantos; i++) {
            for (var exitoso : new boolean[] {false, true}) {
                dsl.execute(
                        """
                        INSERT INTO identidad.intento_autenticacion
                            (id, usuario_id, identificador_usado, fecha_hora, exitoso, motivo_fallo,
                             ip_origen, agente_usuario, puntaje_riesgo)
                        VALUES (gen_random_uuid(), ?, 'prueba', clock_timestamp(), ?, ?, '127.0.0.1', 'prueba', 0)
                        """,
                        usuarioId,
                        exitoso,
                        exitoso ? null : "FACTOR_REQUERIDO");
            }
        }
    }

    void intentosFallidos(UUID usuarioId, int cuantos) {
        for (int i = 0; i < cuantos; i++) {
            dsl.execute(
                    """
                    INSERT INTO identidad.intento_autenticacion
                        (id, usuario_id, identificador_usado, fecha_hora, exitoso, motivo_fallo,
                         ip_origen, agente_usuario, puntaje_riesgo)
                    VALUES (gen_random_uuid(), ?, 'prueba', clock_timestamp(), false, 'CREDENCIAL_INVALIDA',
                            '127.0.0.1', 'prueba', 0)
                    """,
                    usuarioId);
        }
    }
}
