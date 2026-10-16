package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.infraestructura.CorridaRepositorio.Corrida;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Una corrida de un trabajo, para que operaciones vea cuando corrio, quien la pidio y como termino. */
public record VistaCorrida(
        UUID id,
        String operacion,
        String clave,
        String origen,
        String estado,
        Map<String, String> resultado,
        String error,
        OffsetDateTime iniciadaEn,
        OffsetDateTime terminadaEn) {

    static VistaCorrida de(Corrida c) {
        return new VistaCorrida(
                c.id(),
                c.operacion(),
                c.clave(),
                c.origen(),
                c.estado(),
                OperacionesProgramables.leerResultado(c.resultado()),
                c.error(),
                c.iniciadaEn(),
                c.terminadaEn());
    }
}
