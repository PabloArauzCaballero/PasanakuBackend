package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.infraestructura.CorridaRepositorio;
import bo.aportaya.inversiones.infraestructura.CorridaRepositorio.Reserva;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * El envoltorio de toda operacion de {@link OperacionesProgramables}: reserva la clave de idempotencia en
 * {@code corrida_trabajo}, corre el trabajo y deja escrito como termino.
 *
 * <ul>
 *   <li>Misma operacion, misma clave y mismo contenido ya terminada: devuelve lo guardado, no vuelve a correr.
 *   <li>Misma clave con otro contenido: rechazo; con una corrida en curso no vencida: rechazo.
 *   <li>Una corrida fallida, o en curso pero vieja (se cayo el proceso), se retoma con la misma clave.
 *   <li>Cada corrida cuenta en {@code inversiones.corridas} por operacion, estado y origen.
 * </ul>
 */
final class EjecucionDeCorridas {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, String>> MAPA = new TypeReference<>() {};

    private final Datos datos;
    private final Transaccionar tx;
    private final CorridaRepositorio corridas;
    private final Reloj reloj;
    private final MeterRegistry metricas;
    private final Duration caducidadEnCurso;

    EjecucionDeCorridas(
            Datos datos,
            Transaccionar tx,
            CorridaRepositorio corridas,
            Reloj reloj,
            MeterRegistry metricas,
            Duration caducidadEnCurso) {
        this.datos = datos;
        this.tx = tx;
        this.corridas = corridas;
        this.reloj = reloj;
        this.metricas = metricas;
        this.caducidadEnCurso = caducidadEnCurso;
    }

    Map<String, String> ejecutar(
            String operacion,
            int caso,
            int codigoContenidoDistinto,
            String clave,
            String contenido,
            String origen,
            ContextoSesion ctx,
            Supplier<Map<String, String>> trabajo) {
        String hash = hash(operacion + "|" + contenido);
        Reserva reserva = tx.en(() -> datos.conContexto(
                ctx, dsl -> corridas.reservar(dsl, operacion, clave, hash, origen, ahora(), caducidadEnCurso)));
        switch (reserva) {
            case CONTENIDO_DISTINTO ->
                throw Errores.de(
                        caso,
                        codigoContenidoDistinto,
                        "Esa clave de idempotencia ya se uso con otro contenido: no se corre de nuevo.");
            case EN_PROCESO ->
                throw Errores.de(
                        caso,
                        codigoContenidoDistinto + 1,
                        "Esa operacion ya esta en curso con la misma clave: espera a que termine.");
            case REPETIDA -> {
                contar(operacion, "REPETIDA", origen);
                return leerResultado(
                        tx.en(() -> datos.conContexto(ctx, dsl -> corridas.porClave(dsl, operacion, clave)))
                                .orElseThrow()
                                .resultado());
            }
            case CORRER -> {
                // sigue abajo
            }
        }
        Map<String, String> resultado;
        try {
            resultado = trabajo.get();
        } catch (RuntimeException fallo) {
            String error = fallo instanceof ErrorDeNegocio negocio
                    ? negocio.codigo().valor()
                    : fallo.getClass().getSimpleName();
            terminar(ctx, operacion, clave, "FALLIDA", null, error);
            contar(operacion, "FALLIDA", origen);
            throw fallo;
        }
        String estado = "true".equals(resultado.get("cortada")) ? "PARCIAL" : "OK";
        terminar(ctx, operacion, clave, estado, escribir(resultado), null);
        contar(operacion, estado, origen);
        return resultado;
    }

    private void terminar(
            ContextoSesion ctx, String operacion, String clave, String estado, String resultado, String error) {
        tx.en(() -> datos.conContexto(ctx, dsl -> {
            corridas.terminar(dsl, operacion, clave, estado, resultado, error, ahora());
            return null;
        }));
    }

    private void contar(String operacion, String estado, String origen) {
        Counter.builder("inversiones.corridas")
                .tag("operacion", operacion)
                .tag("estado", estado)
                .tag("origen", origen)
                .register(metricas)
                .increment();
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }

    private static String hash(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String escribir(Map<String, String> m) {
        try {
            return JSON.writeValueAsString(m);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el resultado de la corrida", e);
        }
    }

    static Map<String, String> leerResultado(String json) {
        try {
            return json == null ? Map.of() : JSON.readValue(json, MAPA);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Resultado de corrida ilegible", e);
        }
    }
}
