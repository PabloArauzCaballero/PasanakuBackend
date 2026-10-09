package bo.aportaya.inversiones;

import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.ProductoDelAliado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Los productos SINTETICOS de prueba del doble del aliado. Ninguna tasa ni plazo de aca es una cotizacion. */
final class ProductosDePrueba {

    private ProductosDePrueba() {}

    static List<ProductoDelAliado> todos() {
        return List.of(
                producto(
                        "DPF-T",
                        TipoProducto.DPF,
                        Optional.of(180),
                        Optional.of(365),
                        Optional.of(new BigDecimal("0.040000")),
                        false,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        "500.00",
                        Optional.empty()),
                producto(
                        "DPF-T-ANT",
                        TipoProducto.DPF,
                        Optional.of(360),
                        Optional.of(360),
                        Optional.of(new BigDecimal("0.045000")),
                        true,
                        Optional.of(new BigDecimal("0.500000")),
                        Optional.empty(),
                        Optional.empty(),
                        "500.00",
                        Optional.empty()),
                producto(
                        "FONDO-T",
                        TipoProducto.FONDO,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        Optional.empty(),
                        Optional.of(1),
                        Optional.of("15:00"),
                        "100.00",
                        Optional.of(new BigDecimal("0.100000"))));
    }

    static ProductoDelAliado producto(
            String codigo,
            TipoProducto tipo,
            Optional<Integer> plazo,
            Optional<Integer> base,
            Optional<BigDecimal> tasa,
            boolean anticipable,
            Optional<BigDecimal> penal,
            Optional<Integer> diasRescate,
            Optional<String> corte,
            String minimo,
            Optional<BigDecimal> comision) {
        return new ProductoDelAliado(
                codigo,
                tipo,
                "Producto de prueba " + codigo + " (SINTETICO)",
                plazo,
                base,
                tasa,
                anticipable,
                penal,
                diasRescate,
                corte,
                new BigDecimal(minimo),
                new BigDecimal("0.100000"),
                comision,
                "doble-de-prueba/SINTETICO",
                Instant.parse("2026-10-12T12:00:00Z"),
                true);
    }
}
