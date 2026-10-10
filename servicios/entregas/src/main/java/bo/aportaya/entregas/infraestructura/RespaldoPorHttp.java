package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.RespaldoEmpresarial;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pide a {@code garantia} que cubra con la reserva el faltante de UN turno.
 *
 * <p>Se manda SOLO identificadores y, como contraste, el faltante que {@code aportes} afirmo: el
 * importe lo vuelve a confirmar {@code garantia} por su cuenta. La clave de idempotencia sale
 * del turno, no del intento: reintentar (o repetir tras una caida) devuelve la misma cobertura.
 *
 * <p>Tiempo maximo y circuit breaker ({@link ClienteConCircuito}). FALLBACK declarado: cualquier falla —red, 4xx,
 * 5xx, JSON roto o circuito abierto— es vacio: no se sabe si cubrio, y se reintenta con la MISMA clave (con el
 * circuito abierto no sale ninguna peticion, asi que no hay cobertura duplicada ni a medias).
 */
@Component
public class RespaldoPorHttp implements RespaldoEmpresarial {

    private final ClienteConCircuito garantia;

    @Autowired
    public RespaldoPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.garantia}") String urlDeGarantia,
            ConfiguracionDeCircuito circuito) {
        this.garantia = new ClienteConCircuito(constructor, urlDeGarantia, "garantia", circuito);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    RespaldoPorHttp(RestClient.Builder constructor, String urlDeGarantia) {
        this(constructor, urlDeGarantia, ConfiguracionDeCircuito.porOmision());
    }

    /** Para pruebas y diagnostico: el estado del circuito hacia `garantia`. */
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State estadoDelCircuito() {
        return garantia.estado();
    }

    @Override
    public Optional<Cobertura> cubrir(UUID grupoId, UUID periodoId, UUID turnoId, Dinero faltanteEsperado) {
        UUID idempotencyKey =
                UUID.nameUUIDFromBytes(("cobertura-del-turno:" + turnoId).getBytes(StandardCharsets.UTF_8));
        try {
            return garantia.pedir(
                            "/garantia/respaldo/coberturas",
                            Map.of(
                                    "grupoId", grupoId.toString(),
                                    "periodoId", periodoId.toString(),
                                    "turnoId", turnoId.toString(),
                                    "faltanteEsperado",
                                            Map.of(
                                                    "monto", faltanteEsperado.toString(),
                                                    "moneda",
                                                            faltanteEsperado
                                                                    .moneda()
                                                                    .name())),
                            idempotencyKey.toString(),
                            Respuesta.class)
                    .map(Respuesta::aCobertura);
        } catch (RuntimeException noSePudo) {
            return Optional.empty();
        }
    }

    private record Importe(String monto, String moneda) {
        Dinero aDinero() {
            return Dinero.de(new BigDecimal(monto), Moneda.valueOf(moneda));
        }
    }

    private record Respuesta(String resultado, UUID coberturaId, Importe cubierto, Importe sinCubrir) {
        Cobertura aCobertura() {
            return new Cobertura(Resultado.valueOf(resultado), coberturaId, cubierto.aDinero(), sinCubrir.aDinero());
        }
    }
}
