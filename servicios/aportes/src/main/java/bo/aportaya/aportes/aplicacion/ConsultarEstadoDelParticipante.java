package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.EstadoDePagos;
import bo.aportaya.aportes.infraestructura.EstadoDelParticipanteRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que este servicio le contesta a los otros sobre un participante.
 *
 * <p>No es un caso de uso de la boveda: es la consulta que hace posible que
 * {@code grupos} y {@code nucleo-financiero} decidan sin leer este esquema. Sin ella,
 * cada uno tendria que mirar {@code aportes.obligacion_aporte} —el invariante 11— o
 * recibir el dato del cliente, que es peor: el que pide seria quien afirma estar al dia.
 *
 * <p>Solo lee, y aun asi abre transaccion: el contexto viaja con {@code SET LOCAL}
 * dentro de ella, y sin transaccion la politica de fila no aplica.
 */
@Service
public class ConsultarEstadoDelParticipante {

    private final Datos datos;
    private final EstadoDelParticipanteRepositorio estados;
    private final PertenenciaDelParticipante pertenencia;

    public ConsultarEstadoDelParticipante(
            Datos datos, EstadoDelParticipanteRepositorio estados, PertenenciaDelParticipante pertenencia) {
        this.datos = datos;
        this.estados = estados;
        this.pertenencia = pertenencia;
    }

    /**
     * F1.L1.M07 · el estado del participante, solo si es la persona de la sesion. Vacio si es de otro: la web
     * responde 404 sin confirmar que existe. La pregunta a {@code grupos} va FUERA de la transaccion
     * (invariante 6); por eso este metodo no es transaccional y la lectura del estado abre la suya.
     */
    public java.util.Optional<EstadoDePagos> delUsuario(UUID participanteId, ContextoSesion ctx) {
        if (!pertenencia.participanteEsDelUsuario(participanteId, ctx)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(pertenencia.enLectura(() -> ejecutarSinTransaccion(participanteId, ctx)));
    }

    private EstadoDePagos ejecutarSinTransaccion(UUID participanteId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> estados.de(dsl, participanteId));
    }

    @Transactional(readOnly = true)
    public EstadoDePagos ejecutar(UUID participanteId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> estados.de(dsl, participanteId));
    }

    @Transactional(readOnly = true)
    public int morososDelGrupo(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> estados.morososDelGrupo(dsl, grupoId));
    }
}
