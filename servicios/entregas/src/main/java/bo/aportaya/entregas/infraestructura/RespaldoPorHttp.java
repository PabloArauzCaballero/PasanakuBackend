package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.RespaldoEmpresarial;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pide a {@code garantia} que cubra con la reserva el faltante de UN turno.
 *
 * <p>Se manda SOLO identificadores y, como contraste, el faltante que {@code aportes} afirmo: el
 * importe lo vuelve a confirmar {@code garantia} por su cuenta. La clave de idempotencia sale
 * del turno, no del intento: reintentar (o repetir tras una caida) devuelve la misma cobertura.
 * Cualquier falla —red, 4xx, 5xx, JSON roto— es vacio: no se sabe si cubrio, y se reintenta.
 */
@Component
public class RespaldoPorHttp implements RespaldoEmpresarial {

    private final RestClient rest;

    public RespaldoPorHttp(
            RestClient.Builder constructor, @Value("${aportaya.servicios.garantia}") String urlDeGarantia) {
        this.rest = constructor.baseUrl(urlDeGarantia).build();
    }

    @Override
    public Optional<Cobertura> cubrir(UUID grupoId, UUID periodoId, UUID turnoId, Dinero faltanteEsperado) {
        UUID clave = UUID.nameUUIDFromBytes(("cobertura-del-turno:" + turnoId).getBytes(StandardCharsets.UTF_8));
        try {
            var respuesta = rest.post()
                    .uri("/garantia/respaldo/coberturas")
                    .headers(ClienteDeServicio::propagarElToken)
                    .header("Idempotency-Key", clave.toString())
                    .body(Map.of(
                            "grupoId", grupoId.toString(),
                            "periodoId", periodoId.toString(),
                            "turnoId", turnoId.toString(),
                            "faltanteEsperado",
                                    Map.of(
                                            "monto", faltanteEsperado.toString(),
                                            "moneda", faltanteEsperado.moneda().name())))
                    .retrieve()
                    .body(Respuesta.class);
            return Optional.ofNullable(respuesta).map(r -> r.aCobertura());
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
