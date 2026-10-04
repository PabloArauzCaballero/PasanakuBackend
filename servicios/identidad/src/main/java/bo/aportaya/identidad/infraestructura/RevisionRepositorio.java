package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.DOCUMENTO_IDENTIDAD;
import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.VERIFICACION_KYC;

import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.plataforma.archivos.DestinoDeObjeto;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                        DOCUMENTO_IDENTIDAD.FECHA_EXPIRACION,
                        VERIFICACION_KYC.URL_SELFIE,
                        VERIFICACION_KYC.URL_PERFIL_IZQUIERDO,
                        VERIFICACION_KYC.URL_PERFIL_DERECHO,
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

    /**
     * El expediente mas reciente de una persona, para que ella misma pueda
     * consultar su propio estado (CU-02) sin esperar a tener sesion todavia.
     */
    public Optional<ExpedienteDeIdentidad> deUsuario(DSLContext dsl, UUID usuarioId) {
        return dsl.select(
                        VERIFICACION_KYC.ID,
                        VERIFICACION_KYC.USUARIO_ID,
                        USUARIO.NOMBRES,
                        USUARIO.APELLIDOS,
                        DOCUMENTO_IDENTIDAD.TIPO,
                        DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION,
                        DOCUMENTO_IDENTIDAD.URL_ANVERSO,
                        DOCUMENTO_IDENTIDAD.URL_REVERSO,
                        DOCUMENTO_IDENTIDAD.FECHA_EXPIRACION,
                        VERIFICACION_KYC.URL_SELFIE,
                        VERIFICACION_KYC.URL_PERFIL_IZQUIERDO,
                        VERIFICACION_KYC.URL_PERFIL_DERECHO,
                        VERIFICACION_KYC.ESTADO,
                        VERIFICACION_KYC.INICIADA_EN,
                        VERIFICACION_KYC.RESUELTA_EN,
                        VERIFICACION_KYC.MOTIVO_RECHAZO)
                .from(VERIFICACION_KYC)
                .join(USUARIO)
                .on(USUARIO.ID.eq(VERIFICACION_KYC.USUARIO_ID))
                .leftJoin(DOCUMENTO_IDENTIDAD)
                .on(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(VERIFICACION_KYC.USUARIO_ID))
                .where(VERIFICACION_KYC.USUARIO_ID.eq(usuarioId))
                .orderBy(VERIFICACION_KYC.INICIADA_EN.desc())
                .limit(1)
                .fetchOptional(RevisionRepositorio::aExpedienteDeIdentidad);
    }

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
    private static boolean estaCargada(String clave, UUID usuarioId) {
        return clave != null && clave.contains(DestinoDeObjeto.carpetaDeExpediente(usuarioId));
    }

    private static ExpedienteDeIdentidad aExpedienteDeIdentidad(Record f) {
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
                tipo == null ? null : (lugar == null ? tipo : tipo + " " + lugar),
                f.get(VERIFICACION_KYC.ESTADO),
                f.get(VERIFICACION_KYC.INICIADA_EN),
                f.get(VERIFICACION_KYC.RESUELTA_EN),
                f.get(VERIFICACION_KYC.MOTIVO_RECHAZO),
                fotos,
                f.get(DOCUMENTO_IDENTIDAD.FECHA_EXPIRACION));
    }

    /**
     * El expediente que se va a decidir, con su fila BLOQUEADA hasta el fin de la
     * transaccion: dos operadores que deciden a la vez el mismo expediente se encolan
     * en vez de pisarse.
     */
    public Optional<ExpedienteDeIdentidad> paraDecidir(DSLContext dsl, UUID verificacionId) {
        return dsl.select(
                        VERIFICACION_KYC.ID,
                        VERIFICACION_KYC.USUARIO_ID,
                        USUARIO.NOMBRES,
                        USUARIO.APELLIDOS,
                        DOCUMENTO_IDENTIDAD.TIPO,
                        DOCUMENTO_IDENTIDAD.LUGAR_EXPEDICION,
                        DOCUMENTO_IDENTIDAD.URL_ANVERSO,
                        DOCUMENTO_IDENTIDAD.URL_REVERSO,
                        DOCUMENTO_IDENTIDAD.FECHA_EXPIRACION,
                        VERIFICACION_KYC.URL_SELFIE,
                        VERIFICACION_KYC.URL_PERFIL_IZQUIERDO,
                        VERIFICACION_KYC.URL_PERFIL_DERECHO,
                        VERIFICACION_KYC.ESTADO,
                        VERIFICACION_KYC.INICIADA_EN,
                        VERIFICACION_KYC.RESUELTA_EN,
                        VERIFICACION_KYC.MOTIVO_RECHAZO)
                .from(VERIFICACION_KYC)
                .join(USUARIO)
                .on(USUARIO.ID.eq(VERIFICACION_KYC.USUARIO_ID))
                .leftJoin(DOCUMENTO_IDENTIDAD)
                .on(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(VERIFICACION_KYC.USUARIO_ID))
                .where(VERIFICACION_KYC.ID.eq(verificacionId))
                .forUpdate()
                .of(VERIFICACION_KYC)
                .fetchOptional(RevisionRepositorio::aExpedienteDeIdentidad);
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
                    case "PERFIL_IZQUIERDO" ->
                        dsl.select(VERIFICACION_KYC.URL_PERFIL_IZQUIERDO)
                                .from(VERIFICACION_KYC)
                                .where(VERIFICACION_KYC.ID.eq(verificacionId))
                                .fetchOne(VERIFICACION_KYC.URL_PERFIL_IZQUIERDO);
                    case "PERFIL_DERECHO" ->
                        dsl.select(VERIFICACION_KYC.URL_PERFIL_DERECHO)
                                .from(VERIFICACION_KYC)
                                .where(VERIFICACION_KYC.ID.eq(verificacionId))
                                .fetchOne(VERIFICACION_KYC.URL_PERFIL_DERECHO);
                    default -> throw new ErrorDeDominio("Esa cara no existe: " + cara);
                };
        // La misma regla que la cola: una clave de relleno no es una foto. Sin esto se
        // le pedia al almacen un objeto que nunca existio y salia un error de almacen
        // donde correspondia decir «todavia no esta cargada».
        if (!estaCargada(clave, usuario)) {
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
                // La precondicion va en la escritura: un expediente ya resuelto no se
                // vuelve a decidir — se pisaria quien lo reviso y cuando (trazabilidad).
                .and(VERIFICACION_KYC.ESTADO.in("PENDIENTE", "EN_REVISION"))
                .execute();
        if (filas == 0) {
            throw new ErrorDeNegocio(CodigoError.de(1, 8), "Ese expediente ya fue resuelto.");
        }
    }
}
