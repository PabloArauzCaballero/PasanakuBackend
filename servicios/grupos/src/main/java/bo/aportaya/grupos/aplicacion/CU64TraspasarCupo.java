package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.TipoDeAcuerdo;
import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.infraestructura.AcuerdoRepositorio;
import bo.aportaya.grupos.infraestructura.TraspasoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-64 · Traspasar un cupo.
 *
 * <p>Dos afirmaciones que sostienen todo lo demas:
 *
 * <ul>
 *   <li><b>El turno no se toca.</b> La posicion en el calendario es del cupo, no de
 *       la persona: si se moviera, traspasar seria una forma de adelantar el propio
 *       turno, y el sorteo dejaria de significar nada.
 *   <li><b>La deuda no viaja con el cupo.</b> Las obligaciones vencidas se quedan con
 *       quien las genero; solo las futuras pasan al entrante.
 * </ul>
 */
@Service
public class CU64TraspasarCupo {

    private final Datos datos;
    private final TraspasoRepositorio traspasos;
    private final AcuerdoRepositorio acuerdos;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU64TraspasarCupo(
            Datos datos, TraspasoRepositorio traspasos, AcuerdoRepositorio acuerdos, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.traspasos = traspasos;
        this.acuerdos = acuerdos;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public UUID ejecutar(EntradaTraspaso entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var cupo = traspasos
                    .estadoDelCupo(dsl, entrada.cupoId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(64, 1), "Ese cupo no existe."));

            // Un id de acuerdo no es un acuerdo: si viene, tiene que existir, ser de este grupo,
            // ser el voto de un traspaso y haber sido APROBADO. Si no viene, vale lo que diga quien
            // llama sobre si el grupo lo exige.
            boolean acuerdoValido = entrada.acuerdoId().isEmpty()
                    ? entrada.hayAcuerdoSiSeExige()
                    : entrada.acuerdoId()
                            .flatMap(id -> acuerdos.porId(dsl, id))
                            .filter(a -> a.grupoId().equals(cupo.grupoId()))
                            .filter(a -> TipoDeAcuerdo.ADMISION_REEMPLAZO.name().equals(a.tipo()))
                            .filter(a -> "APROBADO".equals(a.estado()))
                            .isPresent();

            TraspasoAdmisible.impedimento(
                            cupo.estado(),
                            cupo.turnoCobrado(),
                            entrada.salienteAlDia(),
                            entrada.kycDelEntrante(),
                            entrada.kycMinimoDelGrupo(),
                            entrada.reputacionDelEntrante(),
                            entrada.reputacionMinimaDelGrupo(),
                            acuerdoValido)
                    .ifPresent(motivo -> {
                        throw new ErrorDeNegocio(CodigoError.de(64, motivo.numero()), motivo.mensaje());
                    });

            UUID saliente = cupo.participanteId()
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(64, 1), "Ese cupo no tiene titular."));

            UUID traspaso = traspasos.registrar(
                    dsl,
                    entrada.cupoId(),
                    saliente,
                    entrada.entranteId(),
                    MOTIVO_CODIFICADO,
                    entrada.derechoDeCobroTransferido(),
                    entrada.acuerdoId(),
                    ahora);

            traspasos.traspasar(dsl, entrada.cupoId(), entrada.entranteId(), saliente, MOTIVO_CODIFICADO, ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.cupo_traspasado",
                            "traspaso_cupo",
                            traspaso,
                            Map.of(
                                    "grupoId",
                                    cupo.grupoId().toString(),
                                    "cupoId",
                                    entrada.cupoId().toString(),
                                    "motivo",
                                    entrada.motivo()),
                            UUID.fromString(ctx.traza().id())));
            return traspaso;
        });
    }

    /**
     * La columna `traspaso_cupo.motivo` es un codigo cerrado (REEMPLAZO_POR_MORA | RETIRO | VENTA) y
     * el contrato recibe texto libre de 10 a 300 caracteres: guardar el texto violaba el CHECK y
     * respondia 500 (B21b). Este caso de uso es el traspaso/venta de un cupo, asi que el codigo es
     * VENTA; el texto libre viaja en el evento. SUPUESTO a confirmar con producto (PLAN, B21b).
     */
    static final String MOTIVO_CODIFICADO = "VENTA";

    public record EntradaTraspaso(
            UUID cupoId,
            UUID entranteId,
            String motivo,
            boolean salienteAlDia,
            String kycDelEntrante,
            String kycMinimoDelGrupo,
            int reputacionDelEntrante,
            int reputacionMinimaDelGrupo,
            boolean hayAcuerdoSiSeExige,
            boolean derechoDeCobroTransferido,
            Optional<UUID> acuerdoId) {}
}
