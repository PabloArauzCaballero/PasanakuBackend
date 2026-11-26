package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.RegistroDeMedida.Expediente;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-134 · A-1 (Pablo, 2026-11-26): una medida que llego a firme retiene la entrega del participante aunque venza o
 * se cierre. Nada se libera solo: lo hace un operador con su segundo factor y un fundamento, y queda quien, cuando y
 * por que (R-SEG-17). La liberacion no mueve dinero: {@code entregas} deja de ver la retencion y el desembolso sigue
 * su curso normal.
 */
@Service
public class CU134LiberarEntrega {

    private final Datos datos;
    private final MedidasDeCuentaRepositorio medidas;
    private final EfectosDeMedida efectos;
    private final Reloj reloj;

    public CU134LiberarEntrega(
            Datos datos,
            MedidasDeCuentaRepositorio medidas,
            AccesosRepositorio accesos,
            EvidenciaDeStepUp evidencias,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.medidas = medidas;
        this.efectos = new EfectosDeMedida(medidas, accesos, evidencias, outbox);
        this.reloj = reloj;
    }

    /** Idempotente: liberar de nuevo una entrega ya liberada devuelve el expediente sin otro evento. */
    @Transactional
    public CU134GestionarMedida.Vista ejecutar(
            UUID expedienteId, String fundamento, String evidenciaMfa, ContextoSesion ctx) {
        efectos.exigirSegundoFactor(ctx, evidenciaMfa);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            Expediente e = medidas.paraActualizar(dsl, expedienteId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(134, 10), "No existe ese expediente."));
            CU134ConsultarParticipante.noSobreSiMismo(ctx, e.usuarioId());
            if (e.firmeEn() == null) {
                throw new ErrorDeNegocio(
                        CodigoError.de(134, 12), "Esta medida no llego a firme: no retiene ninguna entrega.");
            }
            if (medidas.liberarEntrega(dsl, e.id(), ctx.usuarioId(), fundamento.strip(), ahora) == 1) {
                efectos.emitir(dsl, "identidad.medida_cuenta_entrega_liberada", e.id(), e.usuarioId(), ctx);
            }
            return efectos.vista(dsl, expedienteId);
        });
    }
}
