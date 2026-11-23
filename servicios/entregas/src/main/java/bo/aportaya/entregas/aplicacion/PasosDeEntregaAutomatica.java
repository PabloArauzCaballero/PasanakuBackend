package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.SagaDeEntregaAutomaticaRepositorio;
import bo.aportaya.entregas.infraestructura.SagaDeEntregaAutomaticaRepositorio.Saga;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio.Turno;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las escrituras LOCALES de la entrega automatica, cada una en su transaccion corta. La pregunta a {@code aportes} va
 * entre ellas, nunca adentro (invariante 6): por eso {@link EntregaAutomatica} no abre transaccion y delega aca.
 */
@Service
public class PasosDeEntregaAutomatica {

    /** Quien consume {@code aportes.periodo_recaudado}, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "entregas.periodo-recaudado";

    /** El proceso que fondea solo: fijo, para poder leerlo en el registro. */
    static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000004a2");

    /** Lo que la saga necesita para fondear: su estado y, si ya llego, el turno del periodo. */
    public record Pendiente(Saga saga, Optional<Turno> turno) {}

    private final Datos datos;
    private final SagaDeEntregaAutomaticaRepositorio sagas;
    private final TurnoDelGrupoRepositorio turnos;
    private final Consumidos consumidos = new Consumidos("entregas");

    public PasosDeEntregaAutomatica(
            Datos datos, SagaDeEntregaAutomaticaRepositorio sagas, TurnoDelGrupoRepositorio turnos) {
        this.datos = datos;
        this.sagas = sagas;
        this.turnos = turnos;
    }

    /** Marca el evento y registra el aviso en la MISMA transaccion. {@code false} si el evento ya se proceso. */
    @Transactional
    public boolean avisar(UUID eventoId, UUID correlationId, UUID periodoId, UUID grupoId, String motivo) {
        return datos.conContexto(contexto(correlationId.toString()), dsl -> {
            if (!consumidos.registrar(dsl, eventoId, CONSUMIDOR)) {
                return false;
            }
            sagas.avisar(dsl, periodoId, grupoId, motivo, correlationId);
            return true;
        });
    }

    @Transactional
    public Optional<Pendiente> pendiente(UUID periodoId) {
        return datos.conContexto(contexto(UUID.randomUUID().toString()), dsl -> sagas.delPeriodo(dsl, periodoId)
                .filter(s -> "INICIADA".equals(s.estado()))
                .map(s -> new Pendiente(s, turnos.delPeriodo(dsl, periodoId))));
    }

    @Transactional
    public boolean cerrar(UUID periodoId, String estado, int paso, String correlationId) {
        return datos.conContexto(contexto(correlationId), dsl -> sagas.cerrar(dsl, periodoId, estado, paso));
    }

    @Transactional
    public void postergar(UUID periodoId, String correlationId) {
        datos.conContexto(contexto(correlationId), dsl -> {
            sagas.tocar(dsl, periodoId);
            return null;
        });
    }

    @Transactional
    public List<UUID> iniciadas(OffsetDateTime antesDe, int limite) {
        return datos.conContexto(contexto(UUID.randomUUID().toString()), dsl -> sagas.iniciadas(dsl, antesDe, limite));
    }

    static ContextoSesion contexto(String correlationId) {
        return ContextoSesion.deSistema(PROCESO, new Traza(correlationId));
    }
}
