package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.BorradorOnboardingDatos;
import bo.aportaya.identidad.dominio.ResultadoConfirmacionCorreo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistencia del borrador de onboarding y resolucion del resultado post-confirmacion.
 *
 * <p>Extraido de {@link VerificacionCorreoRepositorio} para mantener el limite de 300 lineas por
 * archivo que exige el barrido de CI.
 */
@Component
public class BorradorOnboardingRepositorio {

    private final Datos datos;
    private final ProteccionDeVerificacionCorreo proteccion;

    public BorradorOnboardingRepositorio(Datos datos, ProteccionDeVerificacionCorreo proteccion) {
        this.datos = datos;
        this.proteccion = proteccion;
    }

    /**
     * Devuelve el borrador vigente o los datos parciales del usuario si existe un registro
     * incompleto, o un resultado limpio si el correo no tiene datos previos.
     */
    public ResultadoConfirmacionCorreo resolverResultado(DSLContext dsl, String correo, OffsetDateTime ahora) {
        Record borradorFila = dsl.fetchOne(
                """
                SELECT correo, telefono_e164, nombres, apellidos, fecha_nacimiento, tipo_documento,
                       numero_documento, lugar_expedicion, fecha_expiracion, actividad_economica,
                       detalle_actividad, origen_de_fondos, detalle_del_origen, monto_mensual_estimado,
                       paso_pendiente
                  FROM identidad.borrador_onboarding
                 WHERE correo = ? AND expira_en > ?::timestamptz
                """,
                correo,
                ahora);
        if (borradorFila != null) {
            return new ResultadoConfirmacionCorreo(true, true, mapearBorrador(borradorFila, correo));
        }

        Record usuarioIncompleto = dsl.fetchOne(
                """
                SELECT u.id, u.telefono_e164, u.nombres, u.apellidos, u.fecha_nacimiento,
                       d.tipo AS tipo_doc, d.lugar_expedicion, d.fecha_expiracion
                  FROM identidad.usuario u
                  LEFT JOIN identidad.documento_identidad d ON d.usuario_id = u.id
                 WHERE u.correo = ? AND u.estado IN ('INCOMPLETO_KYC', 'PRE_REGISTRO')
                """,
                correo);
        if (usuarioIncompleto != null) {
            return new ResultadoConfirmacionCorreo(true, true, mapearUsuarioIncompleto(usuarioIncompleto, correo));
        }

        return new ResultadoConfirmacionCorreo(true, false, null);
    }

    @Transactional
    public BorradorOnboardingDatos guardar(
            UUID verificacionId, BorradorOnboardingDatos datosBorrador, OffsetDateTime ahora, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> guardarEnDsl(dsl, verificacionId, datosBorrador, ahora));
    }

    /** Se llama desde dentro de una transaccion ya abierta (VerificacionCorreoRepositorio). */
    public void eliminar(DSLContext dsl, String correo) {
        dsl.execute("DELETE FROM identidad.borrador_onboarding WHERE correo = ?", correo);
    }

    private BorradorOnboardingDatos guardarEnDsl(
            DSLContext dsl, UUID verificacionId, BorradorOnboardingDatos datosBorrador, OffsetDateTime ahora) {
        Record token = dsl.fetchOne(
                """
                SELECT firma_hmac, estado, expira_en
                  FROM identidad.token_verificacion
                 WHERE id = ? AND proposito = ?
                """,
                verificacionId,
                VerificacionCorreoRepositorio.PROPOSITO);
        if (token == null) {
            throw new ErrorDeNegocio(CodigoError.de(1, 9), "El token de verificacion no existe.");
        }
        validarEstadoToken(token);
        validarCorreoDelToken(token, datosBorrador.correo());

        OffsetDateTime expira = ahora.plusDays(7);
        dsl.execute(
                """
                INSERT INTO identidad.borrador_onboarding
                (correo, telefono_e164, nombres, apellidos, fecha_nacimiento, tipo_documento,
                 numero_documento, lugar_expedicion, fecha_expiracion, actividad_economica,
                 detalle_actividad, origen_de_fondos, detalle_del_origen, monto_mensual_estimado,
                 paso_pendiente, actualizado_en, expira_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz)
                ON CONFLICT (correo) DO UPDATE
                SET telefono_e164 = EXCLUDED.telefono_e164,
                    nombres = EXCLUDED.nombres,
                    apellidos = EXCLUDED.apellidos,
                    fecha_nacimiento = EXCLUDED.fecha_nacimiento,
                    tipo_documento = EXCLUDED.tipo_documento,
                    numero_documento = EXCLUDED.numero_documento,
                    lugar_expedicion = EXCLUDED.lugar_expedicion,
                    fecha_expiracion = EXCLUDED.fecha_expiracion,
                    actividad_economica = EXCLUDED.actividad_economica,
                    detalle_actividad = EXCLUDED.detalle_actividad,
                    origen_de_fondos = EXCLUDED.origen_de_fondos,
                    detalle_del_origen = EXCLUDED.detalle_del_origen,
                    monto_mensual_estimado = EXCLUDED.monto_mensual_estimado,
                    paso_pendiente = EXCLUDED.paso_pendiente,
                    actualizado_en = EXCLUDED.actualizado_en,
                    expira_en = EXCLUDED.expira_en
                """,
                datosBorrador.correo(),
                datosBorrador.telefonoE164(),
                datosBorrador.nombres(),
                datosBorrador.apellidos(),
                datosBorrador.fechaNacimiento(),
                datosBorrador.tipoDocumento(),
                datosBorrador.numeroDocumento(),
                datosBorrador.lugarExpedicion(),
                datosBorrador.fechaExpiracion(),
                datosBorrador.actividadEconomica(),
                datosBorrador.detalleActividad(),
                datosBorrador.origenDeFondos(),
                datosBorrador.detalleOrigen(),
                datosBorrador.montoMensualEstimado(),
                datosBorrador.pasoPendiente() != null ? datosBorrador.pasoPendiente() : "datos",
                ahora,
                expira);
        return datosBorrador;
    }

    private void validarEstadoToken(Record token) {
        String estado = token.get("estado", String.class);
        if (!"CONSUMIDO".equals(estado) && !"ENVIADO".equals(estado)) {
            throw new ErrorDeNegocio(CodigoError.de(1, 9), "El token no esta disponible para guardar borrador.");
        }
    }

    private void validarCorreoDelToken(Record token, String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ErrorDeNegocio(CodigoError.de(1, 7), "El correo es obligatorio para el borrador.");
        }
        String huella = proteccion.huellaDestino(correo);
        if (!java.security.MessageDigest.isEqual(
                token.get("firma_hmac", String.class).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                huella.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            throw new ErrorDeNegocio(CodigoError.de(1, 9), "La verificacion no corresponde al correo indicado.");
        }
    }

    private static BorradorOnboardingDatos mapearBorrador(Record fila, String correo) {
        return new BorradorOnboardingDatos(
                fila.get("correo", String.class),
                fila.get("telefono_e164", String.class),
                fila.get("nombres", String.class),
                fila.get("apellidos", String.class),
                fila.get("fecha_nacimiento", LocalDate.class),
                fila.get("tipo_documento", String.class),
                fila.get("numero_documento", String.class),
                fila.get("lugar_expedicion", String.class),
                fila.get("fecha_expiracion", LocalDate.class),
                fila.get("actividad_economica", String.class),
                fila.get("detalle_actividad", String.class),
                fila.get("origen_de_fondos", String.class),
                fila.get("detalle_del_origen", String.class),
                fila.get("monto_mensual_estimado", String.class),
                fila.get("paso_pendiente", String.class));
    }

    private static BorradorOnboardingDatos mapearUsuarioIncompleto(Record fila, String correo) {
        String tipoDoc = fila.get("tipo_doc", String.class);
        String paso = tipoDoc != null ? "actividad" : "documento";
        return new BorradorOnboardingDatos(
                correo,
                fila.get("telefono_e164", String.class),
                fila.get("nombres", String.class),
                fila.get("apellidos", String.class),
                fila.get("fecha_nacimiento", LocalDate.class),
                tipoDoc,
                null,
                fila.get("lugar_expedicion", String.class),
                fila.get("fecha_expiracion", LocalDate.class),
                null,
                null,
                null,
                null,
                null,
                paso);
    }
}
