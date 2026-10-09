package bo.aportaya.inversiones.infraestructura.clientes;

import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.Estado;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.OperacionDelAliado;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.ProductoDelAliado;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Interpreta el contenido YA verificado (firma y frescura) de las respuestas del aliado.
 * Cualquier forma inesperada es «no disponible»: nunca se completa un dato que falta.
 */
final class LecturaDeRespuestas {

    private LecturaDeRespuestas() {}

    static List<ProductoDelAliado> productos(JsonNode payload) {
        var salida = new ArrayList<ProductoDelAliado>();
        try {
            for (JsonNode p : payload.path("productos")) {
                salida.add(new ProductoDelAliado(
                        p.path("codigo").asText(),
                        TipoProducto.valueOf(p.path("tipo").asText()),
                        p.path("nombre").asText(),
                        entero(p, "plazoDias"),
                        entero(p, "baseDias"),
                        decimal(p, "tasaNominalAnual"),
                        p.path("permiteRescateAnticipado").asBoolean(false),
                        decimal(p, "penalizacionAnticipo"),
                        entero(p, "diasRescate"),
                        texto(p, "horaCorte"),
                        new BigDecimal(p.path("montoMinimo").asText()),
                        new BigDecimal(p.path("tasaRetencion").asText()),
                        decimal(p, "tasaComisionExito"),
                        p.path("fuente").asText(),
                        Instant.parse(p.path("fechaCotizacion").asText()),
                        "SINTETICO".equals(p.path("origenDatos").asText())));
            }
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new AliadoNoDisponible("Catalogo con forma inesperada");
        }
        return salida;
    }

    static OperacionDelAliado operacion(JsonNode p, UUID esperada) {
        if (!esperada.toString().equals(p.path("referencia").asText())) {
            throw new AliadoNoDisponible("La respuesta es de otra referencia");
        }
        Map<String, String> detalle = new HashMap<>();
        p.path("detalle")
                .properties()
                .forEach(e -> detalle.put(e.getKey(), e.getValue().asText()));
        try {
            return new OperacionDelAliado(
                    esperada,
                    p.path("tipo").asText(),
                    Estado.valueOf(p.path("estado").asText()),
                    decimal(p, "monto"),
                    decimal(p, "cuotas"),
                    texto(p, "posicionExterna"),
                    p.path("transaccionProveedor").asText(),
                    texto(p, "fechaValor").map(LocalDate::parse),
                    texto(p, "liquidaEn").map(Instant::parse),
                    Map.copyOf(detalle));
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new AliadoNoDisponible("Respuesta con forma inesperada");
        }
    }

    private static Optional<String> texto(JsonNode n, String campo) {
        JsonNode v = n.path(campo);
        return v.isMissingNode() || v.isNull() ? Optional.empty() : Optional.of(v.asText());
    }

    private static Optional<Integer> entero(JsonNode n, String campo) {
        JsonNode v = n.path(campo);
        return v.isMissingNode() || v.isNull() ? Optional.empty() : Optional.of(v.asInt());
    }

    private static Optional<BigDecimal> decimal(JsonNode n, String campo) {
        return texto(n, campo).map(BigDecimal::new);
    }
}
