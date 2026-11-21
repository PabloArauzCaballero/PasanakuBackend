package bo.aportaya.garantia.infraestructura;

import bo.aportaya.garantia.dominio.puertos.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code aportes} cuanto del pozo es caja confirmada y que obligaciones faltan.
 *
 * <p>El token de quien llama viaja con la pregunta: {@code aportes} decide con su propio permiso. Tiempo maximo y
 * circuit breaker ({@link ClienteConCircuito}), sin reintento: la cobertura se puede reintentar entera porque es
 * idempotente. FALLBACK declarado: si {@code aportes} no responde, responde mal o tiene el circuito abierto el
 * resultado es vacio y quien pregunta rechaza (AP-CU23-12, denegar por omision): sin pozo afirmado no se cubre
 * nada con la reserva.
 */
@Component
public class RecaudoPorHttp implements RecaudoDelPeriodo {

    private final ClienteConCircuito cliente;
    private final CredencialDeServicio servicio;

    @Autowired
    public RecaudoPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.aportes}") String urlDeAportes,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio) {
        this.cliente = new ClienteConCircuito(constructor, urlDeAportes, "aportes", circuito);
        this.servicio = servicio;
    }

    /** Con los parametros del plan de resiliencia y una credencial a medida: la usan las pruebas del adaptador. */
    RecaudoPorHttp(RestClient.Builder constructor, String urlDeAportes, CredencialDeServicio servicio) {
        this(constructor, urlDeAportes, ConfiguracionDeCircuito.porOmision(), servicio);
    }

    /** Para pruebas y diagnostico: el estado del circuito hacia `aportes`. */
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State estadoDelCircuito() {
        return cliente.estado();
    }

    @Override
    public Optional<Recaudo> consultar(UUID periodoId) {
        // F1.L1.M07 (regla 98.6): con la identidad de SERVICIO de garantia, nunca con el token de una sesion. Sin
        // credencial no sale la peticion: vacio, y quien pregunta no avanza.
        var token = servicio.vigente();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(cliente.ejecutar(rest -> rest.get()
                            .uri("/aportes/periodos/{id}/recaudo", periodoId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                            .retrieve()
                            .body(Respuesta.class)))
                    .map(Respuesta::aRecaudo);
        } catch (RuntimeException noSePudo) {
            return Optional.empty();
        }
    }

    private record Importe(String monto, String moneda) {
        Dinero aDinero() {
            return Dinero.de(new BigDecimal(monto), Moneda.valueOf(moneda));
        }
    }

    private record Faltante(UUID obligacionId, Importe monto) {}

    private record Respuesta(
            UUID periodoId,
            UUID grupoId,
            OffsetDateTime corteEn,
            Importe pozo,
            Importe confirmado,
            Importe cubiertoMutual,
            List<Faltante> pendientes) {

        Recaudo aRecaudo() {
            return new Recaudo(
                    periodoId,
                    grupoId,
                    corteEn,
                    pozo.aDinero(),
                    confirmado.aDinero(),
                    cubiertoMutual.aDinero(),
                    pendientes.stream()
                            .map(p -> new Pendiente(p.obligacionId(), p.monto().aDinero()))
                            .toList());
        }
    }
}
