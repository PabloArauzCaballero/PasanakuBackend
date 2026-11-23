package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio.Turno;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F3.L2.M01 · {@code entregas} guarda los turnos que publica {@code grupos} ({@code grupos.turnos_asignados}).
 *
 * <p>Es la mitad de la saga que le dice a la entrega automatica A QUIEN se le entrega el pozo de cada periodo, sin
 * llamar a {@code grupos} en el camino del dinero (regla 98.2.5):
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} y la copia van en la MISMA transaccion
 *       ({@link Resultado#DUPLICADO}).
 *   <li><b>Fuera de orden</b>: cada turno se aplica solo con una version mayor a la guardada; una version vieja que
 *       llega tarde no pisa nada (cuenta como descartada).
 * </ul>
 *
 * <p>El productor todavia no existe: el contrato es el pacto del consumidor
 * {@code contratos/pactos/grupos-entregas-turnos-asignados.json}.
 */
@Service
public class AplicarTurnosAsignados {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "entregas.turnos-asignados";

    /** El proceso que atiende el evento: fijo, para poder leerlo en el registro. */
    static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000004a1");

    public enum Resultado {
        APLICADO,
        DUPLICADO
    }

    public record Salida(Resultado resultado, int aplicados, int descartados) {}

    /** Lo que trae {@code grupos.turnos_asignados}: solo identificadores. */
    public record TurnosAsignados(UUID eventoId, UUID correlationId, UUID grupoId, List<Turno> turnos) {}

    private final Datos datos;
    private final TurnoDelGrupoRepositorio turnos;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("entregas");

    public AplicarTurnosAsignados(Datos datos, TurnoDelGrupoRepositorio turnos, Reloj reloj) {
        this.datos = datos;
        this.turnos = turnos;
        this.reloj = reloj;
    }

    @Transactional
    public Salida procesar(TurnosAsignados evento) {
        for (Turno t : evento.turnos()) {
            if (!t.grupoId().equals(evento.grupoId())) {
                throw new IllegalArgumentException("El turno " + t.turnoId() + " no es del grupo del evento");
            }
            if (t.version() < 1) {
                throw new IllegalArgumentException("El turno " + t.turnoId() + " trae una version invalida");
            }
        }
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(evento.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, evento.eventoId(), CONSUMIDOR)) {
                return new Salida(Resultado.DUPLICADO, 0, 0);
            }
            var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
            int aplicados = 0;
            for (Turno t : evento.turnos()) {
                if (turnos.aplicar(dsl, t, ahora)) {
                    aplicados++;
                }
            }
            return new Salida(Resultado.APLICADO, aplicados, evento.turnos().size() - aplicados);
        });
    }
}
