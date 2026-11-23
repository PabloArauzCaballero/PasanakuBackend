package bo.aportaya.tarifas.dominio;

import java.util.UUID;

/** CU-132 · Un parámetro del catálogo cerrado de parámetros financieros (sembrado, sin valor). */
public record ParametroDelCatalogo(UUID id, String descripcion, PropuestaDeParametro.Catalogo catalogo) {}
