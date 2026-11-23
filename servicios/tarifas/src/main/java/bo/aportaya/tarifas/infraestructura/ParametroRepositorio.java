package bo.aportaya.tarifas.infraestructura;

import static bo.aportaya.tarifas.generado.Tables.PARAMETRO_FINANCIERO;
import static bo.aportaya.tarifas.generado.Tables.VERSION_PARAMETRO;

import bo.aportaya.tarifas.dominio.ParametroDelCatalogo;
import bo.aportaya.tarifas.dominio.PropuestaDeParametro;
import bo.aportaya.tarifas.dominio.VersionDelParametro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/** CU-132 · El catalogo de parametros financieros y sus versiones ({@code tarifas}). */
@Component
public class ParametroRepositorio {

    public record Nueva(
            UUID parametroId,
            Optional<String> nivel,
            BigDecimal valor,
            Optional<String> moneda,
            OffsetDateTime vigenteDesde,
            String fuente,
            String referencia,
            LocalDate fecha,
            UUID propuestoPor,
            OffsetDateTime propuestoEn,
            UUID claveIdempotencia) {}

    public List<ParametroDelCatalogo> catalogo(DSLContext dsl) {
        return dsl.selectFrom(PARAMETRO_FINANCIERO)
                .orderBy(PARAMETRO_FINANCIERO.CODIGO)
                .fetch(r -> new ParametroDelCatalogo(
                        r.getId(),
                        r.getDescripcion(),
                        new PropuestaDeParametro.Catalogo(
                                r.getCodigo(),
                                Optional.ofNullable(r.getUnidad()),
                                Boolean.TRUE.equals(r.getPorNivel()))));
    }

    public Optional<ParametroDelCatalogo> porCodigo(DSLContext dsl, String codigo) {
        return catalogo(dsl).stream()
                .filter(p -> p.catalogo().codigo().equals(codigo))
                .findFirst();
    }

    public Optional<VersionDelParametro> version(DSLContext dsl, UUID id) {
        return versiones(dsl, VERSION_PARAMETRO.ID.eq(id)).stream().findFirst();
    }

    public Optional<VersionDelParametro> porClave(DSLContext dsl, UUID clave) {
        return versiones(dsl, VERSION_PARAMETRO.CLAVE_IDEMPOTENCIA.eq(clave)).stream()
                .findFirst();
    }

    /** Las versiones aprobadas y las propuestas, de la mas reciente a la mas antigua. */
    public List<VersionDelParametro> vivas(DSLContext dsl) {
        return versiones(dsl, VERSION_PARAMETRO.ESTADO.in("APROBADA", "PROPUESTA"));
    }

    /** Todas las versiones append-only del parametro, incluidas las rechazadas, para CU-132. */
    public List<VersionDelParametro> historial(DSLContext dsl, String codigo) {
        return versiones(dsl, PARAMETRO_FINANCIERO.CODIGO.eq(codigo));
    }

    /** Inserta la propuesta; si la clave ya existe (reintento o carrera) no inserta nada y devuelve vacio. */
    public Optional<UUID> proponer(DSLContext dsl, Nueva n) {
        return dsl.insertInto(VERSION_PARAMETRO)
                .set(VERSION_PARAMETRO.PARAMETRO_ID, n.parametroId())
                .set(VERSION_PARAMETRO.NIVEL_CODIGO, n.nivel().orElse(null))
                .set(VERSION_PARAMETRO.VALOR, n.valor())
                .set(VERSION_PARAMETRO.MONEDA, n.moneda().orElse(null))
                .set(VERSION_PARAMETRO.VIGENTE_DESDE, n.vigenteDesde())
                .set(VERSION_PARAMETRO.PROCEDENCIA_FUENTE, n.fuente())
                .set(VERSION_PARAMETRO.PROCEDENCIA_REFERENCIA, n.referencia())
                .set(VERSION_PARAMETRO.PROCEDENCIA_FECHA, n.fecha())
                .set(VERSION_PARAMETRO.PROPUESTO_POR, n.propuestoPor())
                .set(VERSION_PARAMETRO.PROPUESTO_EN, n.propuestoEn())
                .set(VERSION_PARAMETRO.ESTADO, "PROPUESTA")
                .set(VERSION_PARAMETRO.CLAVE_IDEMPOTENCIA, n.claveIdempotencia())
                .onConflict(VERSION_PARAMETRO.CLAVE_IDEMPOTENCIA)
                .doNothing()
                .returning(VERSION_PARAMETRO.ID)
                .fetchOptional(VERSION_PARAMETRO.ID);
    }

    /** PROPUESTA -> APROBADA con la precondicion en la escritura; falso si ya estaba resuelta. */
    public boolean aprobar(DSLContext dsl, UUID id, UUID aprobadoPor, OffsetDateTime ahora) {
        return dsl.update(VERSION_PARAMETRO)
                        .set(VERSION_PARAMETRO.ESTADO, "APROBADA")
                        .set(VERSION_PARAMETRO.APROBADO_POR, aprobadoPor)
                        .set(VERSION_PARAMETRO.APROBADO_EN, ahora)
                        .where(VERSION_PARAMETRO.ID.eq(id))
                        .and(VERSION_PARAMETRO.ESTADO.eq("PROPUESTA"))
                        .execute()
                == 1;
    }

    private List<VersionDelParametro> versiones(DSLContext dsl, org.jooq.Condition condicion) {
        return dsl.select(VERSION_PARAMETRO.fields())
                .select(PARAMETRO_FINANCIERO.CODIGO)
                .from(VERSION_PARAMETRO)
                .join(PARAMETRO_FINANCIERO)
                .on(PARAMETRO_FINANCIERO.ID.eq(VERSION_PARAMETRO.PARAMETRO_ID))
                .where(condicion)
                .orderBy(VERSION_PARAMETRO.VIGENTE_DESDE.desc(), VERSION_PARAMETRO.ID.desc())
                .fetch(ParametroRepositorio::aVersion);
    }

    private static VersionDelParametro aVersion(Record r) {
        return new VersionDelParametro(
                r.get(VERSION_PARAMETRO.ID),
                r.get(VERSION_PARAMETRO.PARAMETRO_ID),
                r.get(PARAMETRO_FINANCIERO.CODIGO),
                Optional.ofNullable(r.get(VERSION_PARAMETRO.NIVEL_CODIGO)),
                r.get(VERSION_PARAMETRO.VALOR),
                Optional.ofNullable(r.get(VERSION_PARAMETRO.MONEDA)),
                r.get(VERSION_PARAMETRO.VIGENTE_DESDE),
                r.get(VERSION_PARAMETRO.PROCEDENCIA_FUENTE),
                r.get(VERSION_PARAMETRO.PROCEDENCIA_REFERENCIA),
                r.get(VERSION_PARAMETRO.PROCEDENCIA_FECHA),
                r.get(VERSION_PARAMETRO.PROPUESTO_POR),
                Optional.ofNullable(r.get(VERSION_PARAMETRO.APROBADO_POR)),
                r.get(VERSION_PARAMETRO.ESTADO));
    }
}
