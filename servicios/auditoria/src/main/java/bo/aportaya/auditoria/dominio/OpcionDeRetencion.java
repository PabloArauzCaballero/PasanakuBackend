package bo.aportaya.auditoria.dominio;

import java.util.UUID;

/** CU-130 · una politica de retencion que se puede asignar a una tabla del catalogo de datos. */
public record OpcionDeRetencion(UUID id, String entidad, int mesesRetencionActiva, String accionAlVencer) {}
