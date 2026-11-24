package bo.aportaya.grupos.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * F3.L2.M01 · Un turno asignado por el sorteo, tal como lo publica {@code grupos.turnos_asignados} (pacto
 * {@code contratos/pactos/grupos-entregas-turnos-asignados.json}). Solo identificadores: ni nombres ni montos
 * (regla 90.2). {@code version} crece cada vez que el turno cambia de titular; el consumidor descarta una menor.
 */
public record TurnoAsignado(
        UUID turnoId, int numero, UUID periodoId, UUID cupoId, UUID titularParticipanteId, long version) {

    /** El cuerpo del evento, con los nombres del pacto. */
    public static Map<String, Object> carga(UUID grupoId, List<TurnoAsignado> turnos) {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("grupoId", grupoId.toString());
        carga.put(
                "turnos",
                turnos.stream()
                        .map(t -> {
                            Map<String, Object> turno = new LinkedHashMap<>();
                            turno.put("turnoId", t.turnoId().toString());
                            turno.put("numero", t.numero());
                            turno.put("periodoId", t.periodoId().toString());
                            turno.put("cupoId", t.cupoId().toString());
                            turno.put(
                                    "titularParticipanteId",
                                    t.titularParticipanteId().toString());
                            turno.put("version", t.version());
                            return (Object) turno;
                        })
                        .toList());
        return carga;
    }
}
