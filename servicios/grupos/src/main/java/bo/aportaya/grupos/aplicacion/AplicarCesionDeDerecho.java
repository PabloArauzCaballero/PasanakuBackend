package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.infraestructura.TraspasoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carril I · el cupo cambia de titular cuando se vende un turno ({@code entregas.derecho_cedido}).
 *
 * <p>Antes nadie consumia el evento: la venta se liquidaba (el vendedor cobraba, el comprador pagaba) y en {@code grupos}
 * el cupo seguia siendo del vendedor. {@code entregas} no escribe este esquema (invariante 11): avisa, y aca se registra,
 * con las mismas garantias de un traspaso:
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} y el cambio van en la MISMA transaccion. Un
 *       reintento del broker es un no-op ({@link Resultado#DUPLICADO}).
 *   <li><b>Una sola vez por hecho</b>: si el cupo YA es del comprador (otro evento, otra ruta) no se escribe un segundo
 *       traspaso ({@link Resultado#YA_APLICADA}).
 *   <li><b>Fuera de orden</b>: la venta solo se aplica si el cupo sigue siendo del vendedor que el evento nombra. Si es
 *       de un tercero (llego antes la cesion siguiente de la cadena) se RECHAZA sin marcar el evento: el reintento la
 *       aplica cuando la anterior ya se asento. Nada se escribe a medias.
 *   <li><b>Sin cupo ajeno</b>: el cupo tiene que ser del grupo que el evento nombra y el comprador tiene que ser
 *       participante ACTIVO de ESE grupo; si no, se rechaza (el dinero ya se movio en {@code entregas}: se ve, no se oculta).
 * </ul>
 *
 * <p>Solo cambia el titular del cupo y queda el traspaso ({@code motivo=VENTA}, deuda transferida 0: la deuda no viaja con
 * el cupo, R-GRP-11). El turno no se mueve. Los estados de los participantes no se tocan. <b>Pendiente de negocio:</b> que
 * pasa con las obligaciones futuras ya generadas a nombre del vendedor (hoy se quedan con quien las genero) y si un
 * comprador con otro cupo en el mismo grupo requiere {@code permite_cupos_multiples}.
 */
@Service
public class AplicarCesionDeDerecho {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "grupos.cesion-de-derecho";

    /** El proceso que atiende el evento: fijo, para poder leerlo en el registro. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000000f3");

    private static final String MOTIVO = "VENTA";

    public enum Resultado {
        APLICADA,
        YA_APLICADA,
        DUPLICADO
    }

    public record Salida(Resultado resultado, Optional<UUID> traspasoId) {}

    /** Lo que {@code entregas.derecho_cedido} trae: solo identificadores. */
    public record Cesion(
            UUID eventoId,
            UUID correlationId,
            UUID cesionId,
            UUID turnoId,
            UUID grupoId,
            UUID cupoId,
            UUID participanteOrigenId,
            UUID participanteDestinoId) {}

    private final Datos datos;
    private final TraspasoRepositorio traspasos;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("grupos");

    public AplicarCesionDeDerecho(Datos datos, TraspasoRepositorio traspasos, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.traspasos = traspasos;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public Salida procesar(Cesion cesion) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(cesion.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, cesion.eventoId(), CONSUMIDOR)) {
                return new Salida(Resultado.DUPLICADO, Optional.empty());
            }
            var cupo = traspasos
                    .bloquearCupo(dsl, cesion.cupoId())
                    .orElseThrow(() -> rechazo("Ese cupo no existe.", cesion));
            if (!cupo.grupoId().equals(cesion.grupoId())) {
                throw rechazo("Ese cupo no es del grupo de la cesion.", cesion);
            }
            Optional<UUID> titular = cupo.participanteId();
            if (titular.filter(cesion.participanteDestinoId()::equals).isPresent()) {
                return new Salida(Resultado.YA_APLICADA, Optional.empty());
            }
            if (titular.filter(cesion.participanteOrigenId()::equals).isEmpty()) {
                throw rechazo("El cupo ya no es del vendedor: se espera la cesion anterior.", cesion);
            }
            if (!traspasos.esParticipanteActivoDe(dsl, cesion.participanteDestinoId(), cesion.grupoId())) {
                throw rechazo("El comprador no es participante activo de ese grupo.", cesion);
            }

            UUID traspaso = Datos.comoSistema(dsl, () -> {
                UUID registrado = traspasos.registrar(
                        dsl,
                        cesion.cupoId(),
                        cesion.participanteOrigenId(),
                        cesion.participanteDestinoId(),
                        MOTIVO,
                        true,
                        Optional.empty(),
                        reloj.ahora().atOffset(ZoneOffset.UTC));
                traspasos.reasignarCupo(dsl, cesion.cupoId(), cesion.participanteDestinoId());
                return registrado;
            });

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.cupo_traspasado",
                            "traspaso_cupo",
                            traspaso,
                            Map.of(
                                    "grupoId", cesion.grupoId().toString(),
                                    "cupoId", cesion.cupoId().toString(),
                                    "motivo", MOTIVO,
                                    "cesionId", cesion.cesionId().toString(),
                                    "participanteOrigenId",
                                            cesion.participanteOrigenId().toString(),
                                    "participanteDestinoId",
                                            cesion.participanteDestinoId().toString()),
                            cesion.correlationId()));
            return new Salida(Resultado.APLICADA, Optional.of(traspaso));
        });
    }

    private static ErrorDeNegocio rechazo(String mensaje, Cesion cesion) {
        // AP-CU64-01 (CUPO_NO_TRASPASABLE): el mismo codigo que declara el contrato del traspaso. Identificadores, no
        // personas.
        return new ErrorDeNegocio(
                CodigoError.de(64, 1),
                mensaje,
                Map.of(
                        "cesionId",
                        cesion.cesionId().toString(),
                        "cupoId",
                        cesion.cupoId().toString()));
    }
}
