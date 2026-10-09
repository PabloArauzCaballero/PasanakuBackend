package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Descomposicion;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Un comprobante tal como lo ve la persona: principal, interes, impuesto y comision por separado. */
public record VistaComprobante(
        UUID id, String tipo, String sentidoTitular, Descomposicion d, String origen, OffsetDateTime emitidoEn) {}
