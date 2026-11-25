package bo.aportaya.identidad.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CU-134 · el expediente de una medida, su linea de tiempo y su apelacion, tal como los lee el repositorio.
 * Son datos sin comportamiento: viven en el dominio para que la web los traduzca sin depender de la
 * persistencia.
 */
public final class RegistroDeMedida {

    private RegistroDeMedida() {}

    public record Expediente(
            UUID id,
            UUID usuarioId,
            UUID politicaId,
            String causal,
            String medida,
            boolean cautelar,
            String motivo,
            String estado,
            String estadoCuentaPrevio,
            UUID propuestaPor,
            OffsetDateTime propuestaEn,
            OffsetDateTime notificadaEn,
            OffsetDateTime fechaLimiteDescargo,
            UUID decididaPor,
            String resultadoDecision,
            String fundamentoDecision,
            OffsetDateTime fechaLimiteApelacion,
            OffsetDateTime vigenteHasta,
            OffsetDateTime prescribeEn,
            OffsetDateTime firmeEn,
            OffsetDateTime cerradaEn) {}

    public record Hito(String estadoAnterior, String estado, String motivo, OffsetDateTime ocurridoEn) {}

    public record Apelacion(UUID id, String estado, OffsetDateTime fechaLimiteResolucion) {}
}
