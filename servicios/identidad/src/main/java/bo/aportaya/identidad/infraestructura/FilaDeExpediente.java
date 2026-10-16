package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.DOCUMENTO_IDENTIDAD;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.VERIFICACION_KYC;

import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.plataforma.archivos.DestinoDeObjeto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jooq.Record;

/**
 * Como se lee un expediente de una fila de la cola de {@link RevisionRepositorio}.
 *
 * <p>Se separo de ese repositorio sin cambiar nada por dentro: pasaba las 300 lineas
 * del barrido.
 */
final class FilaDeExpediente {

    private FilaDeExpediente() {}

    /**
     * Una foto esta cargada cuando su clave apunta <b>a la carpeta del expediente</b>
     * — {@code identidad/<usuarioId>/} —, que es donde el unico que sube fotos las
     * pone (ADR-034, enmienda del 2026-09-17).
     *
     * <p>Desde que {@code url_anverso} y {@code hash_archivo} admiten nulos, el alta ya
     * no inventa nada y una columna nula alcanzaria. La comprobacion se queda igual por
     * las filas viejas: las que se escribieron mientras las columnas eran
     * {@code NOT NULL} llevan una clave de relleno —{@code local://documentos/…}— que
     * apunta a un objeto que nunca existio, y contarla como foto hacia que la cola
     * dijera que <b>todo el mundo</b> tiene anverso. Mirar la carpeta las descarta sin
     * tener que migrar datos.
     */
    static boolean estaCargada(String clave, UUID usuarioId) {
        return clave != null && clave.contains(DestinoDeObjeto.carpetaDeExpediente(usuarioId));
    }

    static ExpedienteDeIdentidad aExpedienteDeIdentidad(Record f) {
        UUID usuario = f.get(VERIFICACION_KYC.USUARIO_ID);
        List<String> fotos = new ArrayList<>();
        if (estaCargada(f.get(DOCUMENTO_IDENTIDAD.URL_ANVERSO), usuario)) {
            fotos.add("ANVERSO");
        }
        if (estaCargada(f.get(DOCUMENTO_IDENTIDAD.URL_REVERSO), usuario)) {
            fotos.add("REVERSO");
        }
        if (estaCargada(f.get(VERIFICACION_KYC.URL_SELFIE), usuario)) {
            fotos.add("SELFIE");
        }
        if (estaCargada(f.get(VERIFICACION_KYC.URL_PERFIL_IZQUIERDO), usuario)) {
            fotos.add("PERFIL_IZQUIERDO");
        }
        if (estaCargada(f.get(VERIFICACION_KYC.URL_PERFIL_DERECHO), usuario)) {
            fotos.add("PERFIL_DERECHO");
        }
        String tipo = f.get(DOCUMENTO_IDENTIDAD.TIPO);
        String lugar = f.get(DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION);
        return new ExpedienteDeIdentidad(
                f.get(VERIFICACION_KYC.ID),
                f.get(VERIFICACION_KYC.USUARIO_ID),
                f.get(USUARIO.NOMBRES) + " " + f.get(USUARIO.APELLIDOS),
                f.get(USUARIO.TELEFONO_E164),
                tipo == null ? null : (lugar == null ? tipo : tipo + " " + lugar),
                f.get(VERIFICACION_KYC.ESTADO),
                f.get(VERIFICACION_KYC.INICIADA_EN),
                f.get(VERIFICACION_KYC.RESUELTA_EN),
                f.get(VERIFICACION_KYC.MOTIVO_RECHAZO),
                fotos,
                f.get(DOCUMENTO_IDENTIDAD.FECHA_EXPIRACION),
                f.get(USUARIO.CORREO));
    }
}
