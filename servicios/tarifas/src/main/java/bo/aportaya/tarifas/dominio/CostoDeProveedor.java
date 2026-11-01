package bo.aportaya.tarifas.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/** Lo que costo un proveedor en un periodo y una moneda: suma exacta, nunca mezclando monedas (regla 91.1.2). */
public record CostoDeProveedor(
        UUID proveedorId, String periodo, String moneda, BigDecimal costoTotal, int operaciones) {}
