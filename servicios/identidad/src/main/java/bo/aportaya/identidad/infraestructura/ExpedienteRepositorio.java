package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.DOCUMENTO_IDENTIDAD;
import static bo.aportaya.identidad.generado.Tables.VERIFICACION_KYC;

import bo.aportaya.plataforma.archivos.ArchivoGuardado;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Donde queda anotada cada foto del expediente.
 *
 * <p>En la columna va la **clave de objeto**, nunca una URL publica (ADR-034). El
 * binario lo entrega el servicio dueño —este— validando sesion y permisos, y esa
 * lectura queda en la bitacora como cualquier otra lectura de dato personal.
 */
@Repository
public class ExpedienteRepositorio {

    public void anotarAnverso(DSLContext dsl, UUID usuarioId, ArchivoGuardado archivo) {
        actualizarDocumento(dsl, usuarioId, DOCUMENTO_IDENTIDAD.URL_ANVERSO, archivo, true);
    }

    public void anotarReverso(DSLContext dsl, UUID usuarioId, ArchivoGuardado archivo) {
        actualizarDocumento(dsl, usuarioId, DOCUMENTO_IDENTIDAD.URL_REVERSO, archivo, false);
    }

    /**
     * La prueba de vida no es del documento sino de la verificacion: es la cara de
     * quien dice ser, no el papel.
     */
    public void anotarSelfie(DSLContext dsl, UUID usuarioId, ArchivoGuardado archivo) {
        int filas = dsl.update(VERIFICACION_KYC)
                .set(VERIFICACION_KYC.URL_SELFIE, archivo.clave().toString())
                .where(VERIFICACION_KYC.USUARIO_ID.eq(usuarioId))
                .execute();
        if (filas == 0) {
            throw new ErrorDeDominio("Todavia no hay un expediente abierto para esa persona");
        }
    }

    private void actualizarDocumento(
            DSLContext dsl,
            UUID usuarioId,
            org.jooq.TableField<?, String> columna,
            ArchivoGuardado archivo,
            boolean guardarHash) {
        var paso = dsl.update(DOCUMENTO_IDENTIDAD).set(columna, archivo.clave().toString());
        // `hash_archivo` es una sola columna para las dos caras: se queda con la del
        // anverso, que es la que lleva la foto y los datos. Inventar una columna para
        // el reverso seria cambiar el modelo desde el codigo.
        if (guardarHash) {
            paso = paso.set(DOCUMENTO_IDENTIDAD.HASH_ARCHIVO, archivo.hashSha256());
        }
        int filas = paso.where(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(usuarioId)).execute();
        if (filas == 0) {
            throw new ErrorDeDominio("Todavia no hay un documento cargado para esa persona");
        }
    }
}
