package bo.aportaya.auditoria.infraestructura;

import bo.aportaya.auditoria.dominio.DisponibilidadDelServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio.Estado;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Lee y escribe {@code auditoria.disponibilidad_servicio_dia}: un renglon por servicio y dia UTC con los minutos
 * medidos, los minutos ARRIBA y la ultima vez que no lo estuvo.
 *
 * <p>Se guarda el resumen del dia y no cada medicion: catorce filas por dia alcanzan para la ventana del SLO, y el
 * rol del servicio no tiene DELETE para purgar una tabla de muestras.
 */
@Component
public class DisponibilidadRepositorio {

    /**
     * Suma un minuto. La condicion va en la escritura: si ese minuto (o uno posterior) ya se conto, el UPDATE no
     * toca nada, asi que un reintento o una segunda instancia no lo cuentan dos veces.
     *
     * @return si el minuto se conto
     */
    public boolean sumarMinuto(DSLContext dsl, String servicio, Estado estado, Instant minuto) {
        OffsetDateTime en = minuto.atOffset(ZoneOffset.UTC);
        int arriba = estado == Estado.ARRIBA ? 1 : 0;
        OffsetDateTime interrupcion = estado == Estado.ARRIBA ? null : en;
        // Los casts van explicitos: con SQL plano el driver manda las fechas como texto y Postgres no las convierte.
        return dsl.execute(
                        """
                        insert into auditoria.disponibilidad_servicio_dia
                          (servicio, dia, minutos_medidos, minutos_arriba, ultima_interrupcion_en, ultimo_minuto_en)
                        values (?, ?::date, 1, ?, ?::timestamptz, ?::timestamptz)
                        on conflict (dia, servicio) do update set
                          minutos_medidos = disponibilidad_servicio_dia.minutos_medidos + 1,
                          minutos_arriba = disponibilidad_servicio_dia.minutos_arriba + excluded.minutos_arriba,
                          ultima_interrupcion_en = coalesce(
                            excluded.ultima_interrupcion_en, disponibilidad_servicio_dia.ultima_interrupcion_en),
                          ultimo_minuto_en = excluded.ultimo_minuto_en
                        where disponibilidad_servicio_dia.ultimo_minuto_en < excluded.ultimo_minuto_en
                        """,
                        servicio,
                        en.toLocalDate(),
                        arriba,
                        interrupcion,
                        en)
                > 0;
    }

    /**
     * Lo medido de cada servicio desde {@code desde} (dia UTC, inclusive). La ultima interrupcion se busca en toda la
     * historia y no solo en la ventana: que haya sido hace dos meses tambien es un dato.
     */
    public Map<String, DisponibilidadDelServicio> desde(DSLContext dsl, LocalDate desde) {
        Map<String, DisponibilidadDelServicio> porServicio = new LinkedHashMap<>();
        dsl.fetch(
                        """
                        select servicio,
                               coalesce(sum(minutos_medidos) filter (where dia >= ?::date), 0) as medidos,
                               coalesce(sum(minutos_arriba) filter (where dia >= ?::date), 0) as arriba,
                               max(ultima_interrupcion_en) as ultima_interrupcion
                          from auditoria.disponibilidad_servicio_dia
                         group by servicio
                         order by servicio
                        """,
                        desde,
                        desde)
                .forEach(r -> {
                    String servicio = r.get("servicio", String.class);
                    porServicio.put(
                            servicio,
                            new DisponibilidadDelServicio(
                                    servicio,
                                    r.get("medidos", Long.class),
                                    r.get("arriba", Long.class),
                                    Optional.ofNullable(r.get("ultima_interrupcion", OffsetDateTime.class))
                                            .map(OffsetDateTime::toInstant)));
                });
        return porServicio;
    }
}
