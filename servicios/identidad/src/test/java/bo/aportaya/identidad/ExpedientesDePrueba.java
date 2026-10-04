package bo.aportaya.identidad;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Expedientes de identidad armados a mano para las pruebas de la revision (CU02*Test):
 * usuario, documento, verificacion y las claves de sus fotos. Datos sinteticos.
 */
final class ExpedientesDePrueba {
    private static DSLContext dsl;
    private static FixturaDeIdentidad fixtura;

    private ExpedientesDePrueba() {}

    static void usar(DSLContext contexto) {
        dsl = contexto;
        fixtura = new FixturaDeIdentidad(contexto);
    }

    /** Un expediente EN_REVISION con las cinco fotos en su carpeta y el vencimiento dado. */
    static UUID expedienteCompleto(UUID usuario, String vencimiento) {
        UUID verificacion = expediente(usuario, "EN_REVISION", true);
        dsl.execute(
                "UPDATE identidad.documento_identidad SET url_anverso = ?, url_reverso = ? WHERE usuario_id = ?",
                clave(usuario, "anverso"),
                clave(usuario, "reverso"),
                usuario);
        dsl.execute(
                """
                UPDATE identidad.verificacion_kyc
                   SET url_selfie = ?, url_perfil_izquierdo = ?, url_perfil_derecho = ?
                 WHERE id = ?
                """,
                clave(usuario, "selfie"),
                clave(usuario, "perfil-izquierdo"),
                clave(usuario, "perfil-derecho"),
                verificacion);
        if (vencimiento != null) {
            conVencimiento(verificacion, vencimiento);
        }
        return verificacion;
    }

    static void conVencimiento(UUID verificacion, String fecha) {
        dsl.execute(
                """
                UPDATE identidad.documento_identidad d SET fecha_expiracion = ?::date
                  FROM identidad.verificacion_kyc k
                 WHERE k.id = ? AND d.usuario_id = k.usuario_id
                """,
                fecha,
                verificacion);
    }

    static UUID usuario() {
        return fixtura.usuario(
                "+59177" + String.format("%06d", Math.abs(UUID.randomUUID().hashCode() % 1_000_000)));
    }

    static UUID expediente(UUID usuario, String estado, boolean conDocumento) {
        UUID verificacion = UUID.randomUUID();
        UUID documento = null;
        if (conDocumento) {
            documento = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO identidad.documento_identidad
                      (id, usuario_id, tipo, numero_cifrado, version_llave, hash_numero,
                       lugar_expedicion, pais_emision, estado)
                    VALUES (?, ?, 'CI', 'cifrado', 1, ?, 'LP', 'BO', 'EN_REVISION')
                    """,
                    documento,
                    usuario,
                    UUID.randomUUID().toString().replace("-", "").repeat(2));
        }
        dsl.execute(
                """
                INSERT INTO identidad.verificacion_kyc
                  (id, usuario_id, documento_id, nivel_solicitado, estado, iniciada_en)
                VALUES (?, ?, ?, 'BASICO', ?, now())
                """,
                verificacion,
                usuario,
                documento,
                estado);
        return verificacion;
    }

    static String clave(UUID usuario, String cara) {
        return "s3://identidad/" + usuario + "/" + cara + "-" + UUID.randomUUID() + ".jpg";
    }

    static ContextoSesion contexto(UUID revisor) {
        return ContextoSesion.deSistema(revisor, new Traza(UUID.randomUUID().toString()));
    }
}
