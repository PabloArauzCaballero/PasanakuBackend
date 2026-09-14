package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.DOCUMENTO_IDENTIDAD;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.VERIFICACION_KYC;

import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/** La cola de expedientes que espera una decision humana, y su resolucion. */
@Repository
public class RevisionRepositorio {

    public List<ExpedienteDeIdentidad> enEstado(DSLContext dsl, String estado) {
        var consulta = dsl.select(
                        VERIFICACION_KYC.ID,
                        VERIFICACION_KYC.USUARIO_ID,
                        USUARIO.NOMBRES,
                        USUARIO.APELLIDOS,
                        DOCUMENTO_IDENTIDAD.TIPO,
                        DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION,
                        DOCUMENTO_IDENTIDAD.URL_ANVERSO,
                        DOCUMENTO_IDENTIDAD.URL_REVERSO,
                        VERIFICACION_KYC.URL_SELFIE,
                        VERIFICACION_KYC.ESTADO,
                        VERIFICACION_KYC.INICIADA_EN,
                        VERIFICACION_KYC.RESUELTA_EN,
                        VERIFICACION_KYC.MOTIVO_RECHAZO)
                .from(VERIFICACION_KYC)
                .join(USUARIO)
                .on(USUARIO.ID.eq(VERIFICACION_KYC.USUARIO_ID))
                .leftJoin(DOCUMENTO_IDENTIDAD)
                .on(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(VERIFICACION_KYC.USUARIO_ID));
        var filas = estado == null
                ? consulta.orderBy(VERIFICACION_KYC.INICIADA_EN).fetch()
                : consulta.where(VERIFICACION_KYC.ESTADO.eq(estado))
                        .orderBy(VERIFICACION_KYC.INICIADA_EN)
                        .fetch();
        return filas.map(RevisionRepositorio::aExpedienteDeIdentidad);
    }

    private static ExpedienteDeIdentidad aExpedienteDeIdentidad(Record f) {
        List<String> fotos = new ArrayList<>();
        if (f.get(DOCUMENTO_IDENTIDAD.URL_ANVERSO) != null) {
            fotos.add("ANVERSO");
        }
        if (f.get(DOCUMENTO_IDENTIDAD.URL_REVERSO) != null) {
            fotos.add("REVERSO");
        }
        if (f.get(VERIFICACION_KYC.URL_SELFIE) != null) {
            fotos.add("SELFIE");
        }
        String tipo = f.get(DOCUMENTO_IDENTIDAD.TIPO);
        String lugar = f.get(DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION);
        return new ExpedienteDeIdentidad(
                f.get(VERIFICACION_KYC.ID),
                f.get(VERIFICACION_KYC.USUARIO_ID),
                f.get(USUARIO.NOMBRES) + " " + f.get(USUARIO.APELLIDOS),
                tipo == null ? null : (lugar == null ? tipo : tipo + " " + lugar),
                f.get(VERIFICACION_KYC.ESTADO),
                f.get(VERIFICACION_KYC.INICIADA_EN),
                f.get(VERIFICACION_KYC.RESUELTA_EN),
                f.get(VERIFICACION_KYC.MOTIVO_RECHAZO),
                fotos);
    }

    /** La clave del objeto de una cara, para pedirle al almacen su enlace temporal. */
    public String claveDeFoto(DSLContext dsl, UUID verificacionId, String cara) {
        UUID usuario = dsl.select(VERIFICACION_KYC.USUARIO_ID)
                .from(VERIFICACION_KYC)
                .where(VERIFICACION_KYC.ID.eq(verificacionId))
                .fetchOne(VERIFICACION_KYC.USUARIO_ID);
        if (usuario == null) {
            throw new ErrorDeDominio("Ese expediente no existe");
        }
        String clave =
                switch (cara) {
                    case "SELFIE" ->
                        dsl.select(VERIFICACION_KYC.URL_SELFIE)
                                .from(VERIFICACION_KYC)
                                .where(VERIFICACION_KYC.ID.eq(verificacionId))
                                .fetchOne(VERIFICACION_KYC.URL_SELFIE);
                    case "ANVERSO" ->
                        dsl.select(DOCUMENTO_IDENTIDAD.URL_ANVERSO)
                                .from(DOCUMENTO_IDENTIDAD)
                                .where(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(usuario))
                                .fetchOne(DOCUMENTO_IDENTIDAD.URL_ANVERSO);
                    case "REVERSO" ->
                        dsl.select(DOCUMENTO_IDENTIDAD.URL_REVERSO)
                                .from(DOCUMENTO_IDENTIDAD)
                                .where(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(usuario))
                                .fetchOne(DOCUMENTO_IDENTIDAD.URL_REVERSO);
                    default -> throw new ErrorDeDominio("Esa cara no existe: " + cara);
                };
        if (clave == null) {
            throw new ErrorDeDominio("Esa foto todavia no esta cargada");
        }
        return clave;
    }

    public void resolver(
            DSLContext dsl, UUID verificacionId, String estado, String motivo, UUID revisor, OffsetDateTime ahora) {
        int filas = dsl.update(VERIFICACION_KYC)
                .set(VERIFICACION_KYC.ESTADO, estado)
                .set(VERIFICACION_KYC.MOTIVO_RECHAZO, motivo)
                .set(VERIFICACION_KYC.REVISADA_POR, revisor)
                .set(VERIFICACION_KYC.RESUELTA_EN, ahora)
                .where(VERIFICACION_KYC.ID.eq(verificacionId))
                .execute();
        if (filas == 0) {
            throw new ErrorDeDominio("Ese expediente no existe");
        }
    }
}
