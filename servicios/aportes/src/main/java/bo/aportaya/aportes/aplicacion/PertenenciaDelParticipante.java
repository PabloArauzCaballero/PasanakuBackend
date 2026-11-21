package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.infraestructura.EstadoDelParticipanteRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F1.L1.M07 (ANF-SEG-002, BOLA) · si un participante o un pago es de quien pregunta.
 *
 * <p>La pertenencia participante → usuario es de {@code grupos}: se pregunta por el mismo camino que ya usa
 * el cobro ({@code /grupos/obligaciones/admisibilidad}, {@code esDelUsuario}), con el token de la sesion. Lo
 * que se lee de este esquema (el periodo de una obligacion, el participante de un pago) va en su propia
 * transaccion de solo lectura, y la llamada a {@code grupos} queda AFUERA de cualquier transaccion
 * (invariante 6). Si {@code grupos} no contesta, no se afirma la pertenencia: falla cerrado.
 */
@Component
public class PertenenciaDelParticipante {

    private final Datos datos;
    private final TransactionTemplate lectura;
    private final EstadoDelParticipanteRepositorio estados;
    private final ObligacionRepositorio obligaciones;
    private final PagoRepositorio pagos;
    private final HechosDeGrupos grupos;

    public PertenenciaDelParticipante(
            Datos datos,
            org.springframework.transaction.PlatformTransactionManager transacciones,
            EstadoDelParticipanteRepositorio estados,
            ObligacionRepositorio obligaciones,
            PagoRepositorio pagos,
            HechosDeGrupos grupos) {
        this.datos = datos;
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.estados = estados;
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.grupos = grupos;
    }

    /**
     * {@code true} si el participante es la persona de la sesion. Un participante sin ninguna obligacion no
     * tiene nada que mostrar: se responde {@code true} y el estado sale en cero, igual para cualquiera.
     */
    public boolean participanteEsDelUsuario(UUID participanteId, ContextoSesion ctx) {
        Optional<UUID> periodo =
                lectura.execute(t -> datos.conContexto(ctx, dsl -> estados.unPeriodoDe(dsl, participanteId)));
        return periodo.map(p -> esDelUsuario(participanteId, p)).orElse(true);
    }

    /** {@code true} si el pago es de una obligacion de la persona de la sesion; un pago inexistente, {@code false}. */
    public boolean pagoEsDelUsuario(UUID pagoId, ContextoSesion ctx) {
        Optional<ObligacionRepositorio.Obligacion> obligacion = lectura.execute(t -> datos.conContexto(
                ctx, dsl -> pagos.ver(dsl, pagoId).flatMap(p -> obligaciones.ver(dsl, p.obligacionId()))));
        return obligacion
                .map(o -> esDelUsuario(o.participanteId(), o.periodoId()))
                .orElse(false);
    }

    /** Una lectura en su propia transaccion de solo lectura: el contexto de sesion viaja con SET LOCAL. */
    <T> T enLectura(java.util.function.Supplier<T> lectura) {
        return this.lectura.execute(t -> lectura.get());
    }

    private boolean esDelUsuario(UUID participanteId, UUID periodoId) {
        return grupos.admisibilidad(participanteId, periodoId)
                .orElseThrow(() ->
                        new ErrorDeNegocio(CodigoError.de(21, 7), "No pudimos verificar ese dato: intentalo de nuevo."))
                .esDelUsuario();
    }
}
