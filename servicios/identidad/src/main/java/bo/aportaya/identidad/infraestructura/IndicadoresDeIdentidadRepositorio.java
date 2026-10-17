package bo.aportaya.identidad.infraestructura;

import java.time.LocalDate;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Tableros · altas y KYC aprobados por dia de La Paz. Solo conteos: ningun dato de la persona sale de aca (regla 90.2).
 */
@Repository
public class IndicadoresDeIdentidadRepositorio {

    public long registrados(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return dsl.fetchOne(
                        """
                        SELECT count(*) FROM identidad.usuario
                         WHERE (fecha_registro AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                        """,
                        desde,
                        hasta)
                .get(0, Long.class);
    }

    public long kycAprobados(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return dsl.fetchOne(
                        """
                        SELECT count(*) FROM identidad.verificacion_kyc
                         WHERE estado = 'APROBADA' AND resuelta_en IS NOT NULL
                           AND (resuelta_en AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                        """,
                        desde,
                        hasta)
                .get(0, Long.class);
    }
}
