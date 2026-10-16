package bo.aportaya.aportes.infraestructura.clientes;

import bo.aportaya.aportes.aplicacion.CalendarioDelPeriodo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a `grupos` el calendario de aportes de un periodo (H8.S1.M1):
 * {@code GET /grupos/periodos/{periodoId}/calendario-de-aportes}. El productor real vive en `grupos`
 * ({@code consultarCalendarioDeAportes}); aca solo se consume.
 *
 * <p>Con tiempo maximo y circuit breaker ({@link ClienteConCircuito}) y FALLO CERRADO: si `grupos` no
 * contesta, tiene el circuito abierto, responde 4xx/5xx, o el cuerpo no es el del contrato (campo
 * faltante, importe mal formado, moneda desconocida), el resultado es vacio y {@code GenerarObligacionesDelPeriodo}
 * rechaza con {@code PERIODO_NO_CONFIRMADO}: para dinero no se inventa un cupo ni un importe. El token de quien
 * dispara la generacion viaja en la llamada; `grupos` decide con SU permiso ({@code ADMIN_PLATAFORMA}).
 */
@Component
public class CalendarioDelPeriodoPorHttp implements CalendarioDelPeriodo {

    private final ClienteConCircuito grupos;

    public CalendarioDelPeriodoPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.grupos}") String url,
            ConfiguracionDeCircuito circuito) {
        this.grupos = new ClienteConCircuito(constructor, url, "grupos-calendario", circuito);
    }

    @Override
    public Optional<Periodo> deLosCupos(UUID periodoId) {
        try {
            return grupos.consultar("/grupos/periodos/" + periodoId + "/calendario-de-aportes", Respuesta.class)
                    .flatMap(Respuesta::aPeriodo);
        } catch (RuntimeException noSePudo) {
            // 5xx o cuerpo ilegible: no se sabe, y quien pregunta rechaza (denegar por omision).
            return Optional.empty();
        }
    }

    /** Para pruebas y diagnostico: el estado del circuito hacia `grupos`. */
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State estadoDelCircuito() {
        return grupos.estado();
    }

    private record Importe(String monto, String moneda) {
        Dinero aDinero() {
            return Dinero.de(new BigDecimal(monto), Moneda.valueOf(moneda));
        }
    }

    private record CupoDeGrupos(UUID cupoId, UUID participanteId, Importe montoEsperado) {
        Optional<Cupo> aCupo() {
            if (cupoId == null || participanteId == null || montoEsperado == null) {
                return Optional.empty();
            }
            return Optional.of(new Cupo(cupoId, participanteId, montoEsperado.aDinero()));
        }
    }

    private record Respuesta(
            UUID periodoId,
            UUID grupoId,
            Integer numero,
            String estado,
            LocalDate fechaLimitePago,
            LocalDate fechaFinGracia,
            List<CupoDeGrupos> cupos) {

        /** Vacio ante cualquier campo que el contrato exige y no vino: no se completa lo que grupos no dijo. */
        Optional<Periodo> aPeriodo() {
            if (periodoId == null
                    || grupoId == null
                    || numero == null
                    || estado == null
                    || fechaLimitePago == null
                    || fechaFinGracia == null
                    || cupos == null) {
                return Optional.empty();
            }
            var convertidos = cupos.stream().map(CupoDeGrupos::aCupo).toList();
            if (convertidos.stream().anyMatch(Optional::isEmpty)) {
                return Optional.empty();
            }
            return Optional.of(new Periodo(
                    periodoId,
                    grupoId,
                    numero,
                    estado,
                    fechaLimitePago,
                    fechaFinGracia,
                    convertidos.stream().map(Optional::get).toList()));
        }
    }
}
