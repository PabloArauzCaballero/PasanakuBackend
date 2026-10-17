package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.IndicadorRepositorio;
import bo.aportaya.auditoria.infraestructura.IndicadorRepositorio.Calculado;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-98 · el trabajo de calculo que faltaba, del lado de {@code auditoria}: guarda en {@code indicador_kpi} lo que
 * cada servicio dueño calculo y publico ({@code <modulo>.indicador_calculado}). Despues {@link CU98PublicarTablero}
 * lo publica con meta, privacidad y variacion.
 *
 * <p>Solo llegan periodos CERRADOS (tableros, ambiguedad A7): la unicidad del modelo no deja actualizar un valor, asi
 * que un periodo abierto quedaria congelado en su primer calculo.
 */
@Service
public class RegistroDeIndicadores {

    public static final String CONSUMIDOR = "auditoria.indicadores-calculados";

    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000098");
    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    public enum Resultado {
        GUARDADO,
        YA_ESTABA,
        DUPLICADO,
        /** AP-CU98-02: el codigo no tiene definicion vigente; sin ella el numero no se puede interpretar. */
        SIN_DEFINICION
    }

    public record IndicadorCalculado(
            UUID eventoId,
            UUID correlationId,
            String codigo,
            String nombre,
            BigDecimal valor,
            String unidad,
            String periodo,
            long casos,
            boolean provisorio) {}

    private final Datos datos;
    private final IndicadorRepositorio indicadores;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("auditoria");

    public RegistroDeIndicadores(Datos datos, IndicadorRepositorio indicadores, Reloj reloj) {
        this.datos = datos;
        this.indicadores = indicadores;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado registrar(IndicadorCalculado e) {
        ContextoSesion ctx =
                ContextoSesion.deSistema(PROCESO, new Traza(e.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, e.eventoId(), CONSUMIDOR)) {
                return Resultado.DUPLICADO;
            }
            var definicion = indicadores.definicionVigente(dsl, e.codigo(), inicio(e.periodo()));
            if (definicion.isEmpty()) {
                // Se marca consumido igual: reprocesarlo no le crea una definicion. Queda el aviso.
                return Resultado.SIN_DEFINICION;
            }
            boolean nuevo = indicadores.guardar(
                    dsl,
                    new Calculado(
                            definicion.get(),
                            e.codigo(),
                            e.nombre(),
                            e.valor(),
                            e.unidad(),
                            e.periodo(),
                            e.casos(),
                            e.provisorio(),
                            reloj.ahora().atOffset(ZoneOffset.UTC)));
            return nuevo ? Resultado.GUARDADO : Resultado.YA_ESTABA;
        });
    }

    /** El primer instante del periodo AAAA, AAAA-MM o AAAA-MM-DD, en hora de La Paz. */
    static OffsetDateTime inicio(String periodo) {
        LocalDate dia =
                switch (periodo.length()) {
                    case 4 -> LocalDate.of(Integer.parseInt(periodo), 1, 1);
                    case 7 -> LocalDate.parse(periodo + "-01");
                    case 10 -> LocalDate.parse(periodo);
                    default -> throw new IllegalArgumentException("Periodo invalido: " + periodo);
                };
        return dia.atStartOfDay(LA_PAZ).toOffsetDateTime();
    }
}
