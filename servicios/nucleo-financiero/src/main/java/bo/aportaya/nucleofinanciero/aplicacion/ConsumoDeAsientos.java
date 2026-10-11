package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU24RegistrarAsiento.EntradaAsiento;
import bo.aportaya.nucleofinanciero.dominio.OrigenAsiento;
import bo.aportaya.nucleofinanciero.dominio.Partida;
import bo.aportaya.nucleofinanciero.dominio.puertos.CuentasLogicas;
import bo.aportaya.nucleofinanciero.infraestructura.AsientoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaContableRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D5 · Registrar en el mayor los asientos que otros servicios PROPONEN en sus eventos.
 *
 * <p>Garantia (cobertura empresarial, reserva de respaldo) emite junto a cada hecho un
 * {@code asiento = {origenTipo, origenId, partidas[{cuenta, debe, haber, moneda}]}} con cuentas
 * LOGICAS: no escribe el libro (invariante 12). Esta clase lo escribe, con CU-24, con estas garantias:
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} y el asiento se escriben
 *       en la MISMA transaccion. Si el proceso muere antes del commit no hay marca ni asiento y el
 *       reproceso lo hace; si muere despues, la marca ya esta y el reproceso es un no-op. La entrega
 *       de Kafka es al menos una vez; el efecto, exactamente una.
 *   <li><b>Sin mapeo no se registra</b>: una cuenta logica sin codigo del plan rechaza el evento
 *       entero ({@code AP-CU24-02}); no se inventa el plan de cuentas.
 *   <li><b>Reversa = contra-asiento</b>: un evento {@code *_reversada} no inserta "otro asiento
 *       parecido": busca el original del mismo hecho y lo reversa con CU-24, que enlaza
 *       {@code asiento_reversa_id}. Si el original todavia no llego se rechaza para reintentarse
 *       ({@code AP-CU24-06}); si las partidas del evento no son el inverso del original, se rechaza
 *       ({@code AP-CU24-05}). Nada se edita ni se borra.
 * </ul>
 */
@Service
public class ConsumoDeAsientos {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "nucleo-financiero.asientos-de-eventos";

    /** El identificador de proceso del actor Sistema; no es una persona ni una fila de usuario. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-00000000a5e1");

    private static final String SUFIJO_REVERSA = "_reversada";

    private final Datos datos;
    private final CU24RegistrarAsiento cu24;
    private final AsientoRepositorio asientos;
    private final CuentaContableRepositorio plan;
    private final CuentasLogicas logicas;
    private final Consumidos consumidos = new Consumidos("nucleo_financiero");

    public ConsumoDeAsientos(
            Datos datos,
            CU24RegistrarAsiento cu24,
            AsientoRepositorio asientos,
            CuentaContableRepositorio plan,
            CuentasLogicas logicas) {
        this.datos = datos;
        this.cu24 = cu24;
        this.asientos = asientos;
        this.plan = plan;
        this.logicas = logicas;
    }

    public enum Resultado {
        REGISTRADO,
        REVERSADO,
        DUPLICADO,
        SIN_ASIENTO
    }

    public record EventoRecibido(UUID eventoId, String tipo, UUID correlationId, JsonNode carga) {}

    public record Salida(Resultado resultado, Optional<UUID> asientoId) {}

    @Transactional
    public Salida procesar(EventoRecibido evento) {
        JsonNode asiento = evento.carga().path("asiento");
        if (asiento.isMissingNode() || asiento.isNull()) {
            // Un evento sin partidas (p. ej. respaldo_insuficiente) no es un error: no hay nada que asentar.
            return new Salida(Resultado.SIN_ASIENTO, Optional.empty());
        }
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(evento.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            OrigenAsiento origen = origenDe(asiento);
            UUID origenId = uuidDe(asiento, "origenId");
            List<Partida> partidas = partidasDe(asiento);
            String glosa = "Evento " + evento.tipo() + " " + evento.eventoId();

            if (!consumidos.registrar(dsl, evento.eventoId(), CONSUMIDOR)) {
                return new Salida(Resultado.DUPLICADO, Optional.empty());
            }
            if (evento.tipo().endsWith(SUFIJO_REVERSA)) {
                UUID original = asientos.originalSinReversar(dsl, origen.name(), origenId)
                        .orElseThrow(() -> new ErrorDeNegocio(
                                CodigoError.de(24, 6),
                                "El asiento que este evento reversa todavia no esta registrado: se reintenta.",
                                Map.of("origenTipo", origen.name(), "origenId", origenId.toString())));
                exigirInverso(dsl, original, partidas);
                var reversa = cu24.reversar(dsl, original, "evento " + evento.tipo(), ctx);
                return new Salida(Resultado.REVERSADO, Optional.of(reversa.asientoId()));
            }
            Optional<UUID> grupo = evento.carga().path("grupoId").isTextual()
                    ? Optional.of(uuidDe(evento.carga(), "grupoId"))
                    : Optional.empty();
            var registrado = cu24.ejecutar(dsl, new EntradaAsiento(origen, origenId, partidas, glosa, grupo), ctx);
            return new Salida(Resultado.REGISTRADO, Optional.of(registrado.asientoId()));
        });
    }

    /** Las partidas del evento, con el codigo real del plan: sin mapeo para alguna, nada se registra. */
    private List<Partida> partidasDe(JsonNode asiento) {
        List<Partida> partidas = new ArrayList<>();
        try {
            for (JsonNode p : asiento.path("partidas")) {
                String logica = p.path("cuenta").asText();
                Moneda moneda = Moneda.valueOf(p.path("moneda").asText());
                String codigo = logicas.codigoDe(logica, moneda)
                        .orElseThrow(() -> new ErrorDeNegocio(
                                CodigoError.de(24, 2),
                                "La cuenta logica «%s» no tiene codigo en el plan de cuentas para %s."
                                        .formatted(logica, moneda),
                                Map.of("cuentaLogica", logica, "moneda", moneda.name())));
                partidas.add(new Partida(
                        codigo,
                        Dinero.de(p.path("debe").asText(), moneda).monto(),
                        Dinero.de(p.path("haber").asText(), moneda).monto(),
                        moneda));
            }
        } catch (ErrorDeNegocio rechazo) {
            throw rechazo;
        } catch (IllegalArgumentException | bo.aportaya.plataforma.dominio.ErrorDeDominio ilegible) {
            throw new ErrorDeNegocio(
                    CodigoError.de(24, 7),
                    "El asiento propuesto por el evento no se puede leer: " + ilegible.getMessage());
        }
        return partidas;
    }

    private static OrigenAsiento origenDe(JsonNode asiento) {
        try {
            return OrigenAsiento.valueOf(asiento.path("origenTipo").asText());
        } catch (IllegalArgumentException ilegible) {
            throw new ErrorDeNegocio(
                    CodigoError.de(24, 7), "El origen del asiento propuesto no es uno de los admitidos.");
        }
    }

    private static UUID uuidDe(JsonNode asiento, String campo) {
        try {
            return UUID.fromString(asiento.path(campo).asText());
        } catch (IllegalArgumentException ilegible) {
            throw new ErrorDeNegocio(CodigoError.de(24, 7), "El asiento propuesto no trae un " + campo + " valido.");
        }
    }

    /** Las partidas del evento tienen que ser, cuenta por cuenta, el espejo del original: si no, hay un conflicto. */
    private void exigirInverso(org.jooq.DSLContext dsl, UUID original, List<Partida> partidas) {
        Comparator<Linea> orden = Comparator.comparing((Linea l) -> l.cuenta().toString())
                .thenComparing(Linea::debe)
                .thenComparing(Linea::haber);
        List<Linea> esperado = asientos.movimientosDe(dsl, original).stream()
                .map(m -> new Linea(m.cuentaId(), dos(m.haber()), dos(m.debe()), m.moneda()))
                .sorted(orden)
                .toList();
        List<Linea> recibido = partidas.stream()
                .map(p -> new Linea(
                        plan.porCodigo(dsl, p.cuentaCodigo()).orElseThrow().id(),
                        dos(p.debe()),
                        dos(p.haber()),
                        p.moneda().name()))
                .sorted(orden)
                .toList();
        if (!esperado.equals(recibido)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(24, 5),
                    "Las partidas del evento no son el inverso del asiento original.",
                    Map.of("asientoOriginalId", original.toString()));
        }
    }

    private static String dos(BigDecimal valor) {
        return valor.setScale(2).toPlainString();
    }

    private record Linea(UUID cuenta, String debe, String haber, String moneda) {}
}
