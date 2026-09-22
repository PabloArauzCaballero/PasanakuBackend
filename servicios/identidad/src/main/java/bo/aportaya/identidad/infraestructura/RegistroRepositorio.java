package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.CONSENTIMIENTO;
import static bo.aportaya.identidad.generado.Tables.CREDENCIAL_ACCESO;
import static bo.aportaya.identidad.generado.Tables.DOCUMENTO_IDENTIDAD;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.VERIFICACION_KYC;

import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** El alta del usuario y lo que la acompaña dentro de `identidad`. */
@Component
public class RegistroRepositorio {

    public boolean telefonoYaRegistrado(DSLContext dsl, String telefono) {
        return dsl.fetchExists(dsl.selectFrom(USUARIO)
                .where(USUARIO.TELEFONO_E164.eq(telefono))
                .and(USUARIO.ELIMINADO_EN.isNull()));
    }

    /**
     * El duplicado se mide por numero Y lugar de expedicion: el mismo numero de CI
     * existe en dos departamentos y pertenece a dos personas distintas. Preguntar
     * solo por el numero le negaba la cuenta a la segunda.
     */
    public boolean documentoYaRegistrado(DSLContext dsl, String hashNumero, String lugarExpedicion) {
        return dsl.fetchExists(dsl.selectFrom(DOCUMENTO_IDENTIDAD)
                .where(DOCUMENTO_IDENTIDAD.HASH_NUMERO.eq(hashNumero))
                .and(
                        lugarExpedicion == null
                                ? DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION.isNull()
                                : DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION.eq(lugarExpedicion)));
    }

    public UUID crearUsuario(
            DSLContext dsl,
            String codigoPublico,
            String nombres,
            String apellidos,
            String telefono,
            String correo,
            LocalDate fechaNacimiento,
            String estado,
            OffsetDateTime ahora) {
        return dsl.insertInto(USUARIO)
                .set(USUARIO.CODIGO_PUBLICO, codigoPublico)
                .set(USUARIO.NOMBRES, nombres)
                .set(USUARIO.APELLIDOS, apellidos)
                .set(USUARIO.TELEFONO_E164, telefono)
                .set(USUARIO.CORREO, correo)
                .set(USUARIO.FECHA_NACIMIENTO, fechaNacimiento)
                .set(USUARIO.ESTADO, estado)
                .set(USUARIO.NIVEL_KYC, "NINGUNO")
                .set(USUARIO.IDIOMA, "es")
                .set(USUARIO.ZONA_HORARIA, "America/La_Paz")
                .set(USUARIO.FECHA_REGISTRO, ahora)
                .returning(USUARIO.ID)
                .fetchOne(USUARIO.ID);
    }

    /**
     * La credencial con la que se va a ingresar, en la misma transaccion del alta.
     *
     * <p>Antes no existia: CU-01 creaba la persona y nadie creaba su credencial, asi
     * que terminar los ocho pasos dejaba una cuenta a la que no se podia entrar. Los
     * parametros del KDF se guardan junto al hash porque endurecerlos maniana tiene
     * que poder hacerse sin invalidar los hashes de ayer: se sabe con cuales se
     * calculo cada uno.
     */
    public UUID guardarCredencial(
            DSLContext dsl,
            UUID usuarioId,
            String hashContrasena,
            String algoritmo,
            String parametrosKdf,
            OffsetDateTime ahora) {
        return dsl.insertInto(CREDENCIAL_ACCESO)
                .set(CREDENCIAL_ACCESO.USUARIO_ID, usuarioId)
                .set(CREDENCIAL_ACCESO.HASH_CONTRASENA, hashContrasena)
                .set(CREDENCIAL_ACCESO.ALGORITMO, algoritmo)
                .set(CREDENCIAL_ACCESO.PARAMETROS_KDF, org.jooq.JSONB.valueOf(parametrosKdf))
                .set(CREDENCIAL_ACCESO.REQUIERE_CAMBIO, false)
                .set(CREDENCIAL_ACCESO.CAMBIADA_EN, ahora)
                .returning(CREDENCIAL_ACCESO.ID)
                .fetchOne(CREDENCIAL_ACCESO.ID);
    }

    /**
     * El numero va cifrado; lo que se indexa es su hash. Nunca el numero en claro.
     *
     * <p><b>Sin foto y sin hash de archivo.</b> Esta fila nace con lo que la persona
     * escribio; el anverso llega despues, por {@code CU02GuardarFotoDelExpediente}, que
     * es quien escribe {@code url_anverso} y {@code hash_archivo} con el objeto ya
     * subido. Mientras esas columnas fueron {@code NOT NULL} aca se inventaba una clave
     * que apuntaba a nada y un hash de sesenta y cuatro ceros solo para poder insertar,
     * y la cola de verificacion leia eso como «tiene anverso».
     */
    public UUID guardarDocumento(
            DSLContext dsl, UUID usuarioId, DocumentoDeIdentidad documento, String numeroCifrado) {
        return dsl.insertInto(DOCUMENTO_IDENTIDAD)
                .set(DOCUMENTO_IDENTIDAD.USUARIO_ID, usuarioId)
                .set(DOCUMENTO_IDENTIDAD.TIPO, documento.tipo().name())
                .set(DOCUMENTO_IDENTIDAD.NUMERO_CIFRADO, numeroCifrado)
                .set(DOCUMENTO_IDENTIDAD.VERSION_LLAVE, (short) 1)
                .set(DOCUMENTO_IDENTIDAD.HASH_NUMERO, documento.hashNumero())
                .set(DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION, documento.lugarExpedicion())
                .set(DOCUMENTO_IDENTIDAD.PAIS_EMISION, documento.paisEmision())
                .set(DOCUMENTO_IDENTIDAD.ESTADO, "EN_REVISION")
                .returning(DOCUMENTO_IDENTIDAD.ID)
                .fetchOne(DOCUMENTO_IDENTIDAD.ID);
    }

    public UUID iniciarVerificacion(
            DSLContext dsl, UUID usuarioId, UUID documentoId, String nivel, OffsetDateTime ahora) {
        return dsl.insertInto(VERIFICACION_KYC)
                .set(VERIFICACION_KYC.USUARIO_ID, usuarioId)
                .set(VERIFICACION_KYC.DOCUMENTO_ID, documentoId)
                .set(VERIFICACION_KYC.NIVEL_SOLICITADO, nivel)
                .set(VERIFICACION_KYC.ESTADO, "EN_REVISION")
                .set(VERIFICACION_KYC.INICIADA_EN, ahora)
                .returning(VERIFICACION_KYC.ID)
                .fetchOne(VERIFICACION_KYC.ID);
    }

    /** Cada finalidad por separado: aceptar el contrato no es aceptar publicidad. */
    public void registrarConsentimientos(
            DSLContext dsl, UUID usuarioId, List<String> finalidades, String ip, String agente, OffsetDateTime ahora) {
        for (String finalidad : finalidades) {
            dsl.insertInto(
                            CONSENTIMIENTO,
                            CONSENTIMIENTO.USUARIO_ID,
                            CONSENTIMIENTO.TIPO,
                            CONSENTIMIENTO.VERSION_DOCUMENTO,
                            CONSENTIMIENTO.HASH_DOCUMENTO,
                            CONSENTIMIENTO.OTORGADO,
                            CONSENTIMIENTO.FECHA_HORA,
                            CONSENTIMIENTO.IP_ORIGEN,
                            CONSENTIMIENTO.AGENTE_USUARIO)
                    .values(
                            DSL.val(usuarioId),
                            DSL.val(finalidad),
                            DSL.val("1"),
                            DSL.val("0".repeat(64)),
                            DSL.val(true),
                            DSL.val(ahora),
                            comoInet(ip),
                            DSL.val(agente))
                    .execute();
        }
    }

    private static Field<Object> comoInet(String ip) {
        return DSL.field("cast({0} as inet)", Object.class, DSL.val(ip));
    }
}
