package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus;
import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus.Muestra;
import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.Indicador.Definicion;
import bo.aportaya.plataforma.dominio.Indicador.Medicion;
import bo.aportaya.plataforma.dominio.Indicador.Motivo;
import bo.aportaya.plataforma.dominio.Indicador.Unidad;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.mensajeria.CalculadorDeIndicadores;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Tableros · OPERACION: el trafico HTTP del periodo cerrado, desde Prometheus. Si Prometheus no esta (TEST todavia sin
 * Collector, o caido), cada indicador sale FUENTE_NO_CONFIGURADA y no se publica: un tablero de sistemas con ceros
 * diria «nadie uso la plataforma», que es mentira. Prometheus guarda 15 dias: un mes viejo sale sin datos.
 */
@Component
public class IndicadoresDeOperacion implements CalculadorDeIndicadores {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    private final ConsultaPrometheus prometheus;
    private final ConsultasDeTrafico consultas;

    public IndicadoresDeOperacion(ConsultaPrometheus prometheus, ConsultasDeTrafico consultas) {
        this.prometheus = prometheus;
        this.consultas = consultas;
    }

    @Override
    public List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo) {
        Instant fin = periodo.rango().fin().plusDays(1).atStartOfDay(LA_PAZ).toInstant();
        String rango = periodo.rango().dias() * 86_400 + "s";

        Optional<BigDecimal> total = escalar(consultas.solicitudes(rango, null), fin);
        Optional<BigDecimal> exitosas = escalar(consultas.solicitudes(rango, "2..|3.."), fin);
        Optional<BigDecimal> de4xx = escalar(consultas.solicitudes(rango, "4.."), fin);
        Optional<BigDecimal> de5xx = escalar(consultas.solicitudes(rango, "5.."), fin);
        long casos = total.map(t -> t.setScale(0, RoundingMode.HALF_EVEN).longValue())
                .orElse(0L);

        List<Calculado> salida = new ArrayList<>();
        salida.add(conteo("SOLICITUDES_TOTALES", "Solicitudes", total));
        salida.add(conteo("SOLICITUDES_EXITOSAS", "Solicitudes exitosas", exitosas));
        salida.add(conteo("SOLICITUDES_FALLIDAS_4XX", "Rechazadas (4xx)", de4xx));
        salida.add(conteo("SOLICITUDES_FALLIDAS_5XX", "Fallidas (5xx)", de5xx));
        Definicion tasa = new Definicion("TASA_ERROR", Unidad.PORCENTAJE, 0, true);
        salida.add(new Calculado(
                total.isPresent() && de5xx.isPresent()
                        ? Indicador.publicar(tasa, new Medicion(de5xx.get(), total.get(), null, casos), null)
                        : Indicador.sinValor(tasa, Motivo.FUENTE_NO_CONFIGURADA),
                "Tasa de error"));
        for (String[] p : new String[][] {
            {"0.5", "LATENCIA_P50", "Latencia p50"},
            {"0.95", "LATENCIA_P95", "Latencia p95"},
            {"0.99", "LATENCIA_P99", "Latencia p99"}
        }) {
            Definicion d = new Definicion(p[1], Unidad.MILISEGUNDOS, 0, false);
            Optional<BigDecimal> ms = escalar(consultas.percentilMs(p[0], rango), fin);
            salida.add(new Calculado(
                    ms.map(v -> Indicador.publicar(
                                    d, new Medicion(v.setScale(2, RoundingMode.HALF_EVEN), null, null, casos), null))
                            .orElseGet(() -> Indicador.sinValor(d, Motivo.FUENTE_NO_CONFIGURADA)),
                    p[2]));
        }
        return salida;
    }

    /** Un solo numero; vacio si Prometheus no respondio. Sin series (nadie llamo) es CERO real: la consulta corrio. */
    private Optional<BigDecimal> escalar(String promql, Instant al) {
        return prometheus
                .consultar(promql, al)
                .map(series -> series.stream().map(Muestra::valor).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static Calculado conteo(String codigo, String nombre, Optional<BigDecimal> valor) {
        Definicion d = new Definicion(codigo, Unidad.CONTEO, 0, true);
        return new Calculado(
                valor.map(v -> {
                            BigDecimal entero = v.setScale(0, RoundingMode.HALF_EVEN);
                            return Indicador.publicar(d, new Medicion(entero, null, null, entero.longValue()), null);
                        })
                        .orElseGet(() -> Indicador.sinValor(d, Motivo.FUENTE_NO_CONFIGURADA)),
                nombre);
    }
}
