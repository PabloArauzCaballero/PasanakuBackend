package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.DOCUMENTO_IDENTIDAD;
import static bo.aportaya.identidad.generado.Tables.VERIFICACION_KYC;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * La fecha de vencimiento del documento que completa el titular mientras su expediente sigue abierto
 * (CU-02). Separada de {@link RevisionRepositorio} sin cambiar el SQL: aquel es la cola y la decision.
 */
@Repository
public class FechaDeExpiracionRepositorio {

    /** Bloquea la revisión antes de escribir para que una decisión humana concurrente gane o espere. */
    public void actualizarFechaExpiracion(DSLContext dsl, UUID usuarioId, LocalDate fecha) {
        var revision = dsl.select(VERIFICACION_KYC.ID, VERIFICACION_KYC.ESTADO)
                .from(VERIFICACION_KYC)
                .where(VERIFICACION_KYC.USUARIO_ID.eq(usuarioId))
                .orderBy(VERIFICACION_KYC.INICIADA_EN.desc())
                .limit(1)
                .forUpdate()
                .fetchOptional();
        if (revision.isEmpty()) {
            throw new ErrorDeDominio("Todavía no hay un expediente abierto para esa persona.");
        }
        String estado = revision.get().get(VERIFICACION_KYC.ESTADO);
        if (!"PENDIENTE".equals(estado) && !"EN_REVISION".equals(estado)) {
            throw new ErrorDeNegocio(CodigoError.de(1, 8), "La revisión ya terminó y no admite cambios.");
        }
        int filas = dsl.update(DOCUMENTO_IDENTIDAD)
                .set(DOCUMENTO_IDENTIDAD.FECHA_EXPIRACION, fecha)
                .where(DOCUMENTO_IDENTIDAD.USUARIO_ID.eq(usuarioId))
                .execute();
        if (filas == 0) {
            throw new ErrorDeDominio("Todavía no hay un documento cargado para esa persona.");
        }
    }
}
