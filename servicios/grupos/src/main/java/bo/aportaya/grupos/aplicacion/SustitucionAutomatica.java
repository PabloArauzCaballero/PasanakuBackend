package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.infraestructura.SustitucionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * H5.S1.M7 · que pasa con los grupos de un organizador cuando su habilitacion se REVOCA (evento
 * {@code organizador.habilitacion_revocada}).
 *
 * <p><b>Modo manual (por omision, {@code aportaya.grupos.sustitucion.automatica=false}).</b> No se cambia a nadie: por
 * cada grupo vivo se emite {@code grupos.sustitucion_requerida} para que una persona de backoffice sustituya al
 * administrador con {@code POST /grupos/{id}/administrador/sustitucion}. Es el supuesto vigente: la decide una persona.
 *
 * <p><b>Modo automatico (interruptor encendido).</b> Se elige al entrante con una regla determinista y SINTETICA, que
 * NO es una decision de negocio tomada: el participante ACTIVO mas antiguo (por fecha de ingreso, luego id) que
 * {@code organizador} confirme como habilitado. Si ninguno lo esta, o {@code organizador} no responde, no se sustituye a
 * nadie y el grupo se escala a backoffice ({@code motivo=SIN_CANDIDATO_HABILITADO}). La sustitucion usa el mismo caso de
 * uso y el mismo registro append-only que la manual; no toca obligaciones ni retira al saliente.
 *
 * <p>Idempotente: la clave de cada sustitucion sale de (evento, grupo) y los avisos a backoffice se emiten una sola vez
 * por evento (marca de evento consumido en la misma transaccion que el aviso). Las preguntas a otros servicios van
 * FUERA de transaccion (invariante 6). Decision pendiente: negocio y cumplimiento (REPORTE, «Decisiones»).
 */
@Service
public class SustitucionAutomatica {

    public static final String CONSUMIDOR = "grupos.sustitucion-por-revocacion";

    /** El proceso que atiende el evento: fijo, para poder leerlo en el registro. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000000f2");

    public enum Resultado {
        /** Modo manual: se aviso a backoffice y no se cambio a nadie. */
        ESCALADO_A_BACKOFFICE,
        /** Modo automatico: nadie habilitado o sin respuesta de organizador; se aviso a backoffice. */
        SIN_CANDIDATO_HABILITADO,
        /** Modo automatico: el administrador quedo sustituido. */
        SUSTITUIDO
    }

    public record Efecto(UUID grupoId, Resultado resultado, Optional<UUID> entranteParticipanteId) {}

    private final Datos datos;
    private final TransactionTemplate transaccion;
    private final SustitucionRepositorio sustituciones;
    private final SustituirAdministrador sustituir;
    private final HechosDeOtrosServicios afuera;
    private final Outbox outbox;
    private final Consumidos consumidos;
    private final boolean automatica;

    public SustitucionAutomatica(
            Datos datos,
            TransactionTemplate transaccion,
            SustitucionRepositorio sustituciones,
            SustituirAdministrador sustituir,
            HechosDeOtrosServicios afuera,
            Outbox outbox,
            @Value("${aportaya.grupos.sustitucion.automatica}") boolean automatica) {
        this.datos = datos;
        this.transaccion = transaccion;
        this.sustituciones = sustituciones;
        this.sustituir = sustituir;
        this.afuera = afuera;
        this.outbox = outbox;
        this.consumidos = new Consumidos("grupos");
        this.automatica = automatica;
    }

    /** Para pruebas y diagnostico: en que modo corre. */
    public boolean esAutomatica() {
        return automatica;
    }

    public List<Efecto> alRevocarseElOrganizador(UUID eventoId, UUID organizadorId, UUID correlacionId) {
        ContextoSesion ctx = ContextoSesion.deSistema(PROCESO, new Traza(correlacionId.toString()));
        List<UUID> grupos = transaccion.execute(
                e -> datos.conContexto(ctx, dsl -> sustituciones.gruposVigentesDelOrganizador(dsl, organizadorId)));

        List<Efecto> efectos = new ArrayList<>();
        for (UUID grupo : grupos) {
            efectos.add(automatica ? sustituirOEscalar(eventoId, organizadorId, grupo, ctx) : escalar(grupo));
        }
        avisarAUnaPersona(eventoId, organizadorId, efectos, correlacionId, ctx);
        return List.copyOf(efectos);
    }

    private Efecto escalar(UUID grupo) {
        return new Efecto(grupo, Resultado.ESCALADO_A_BACKOFFICE, Optional.empty());
    }

    private Efecto sustituirOEscalar(UUID eventoId, UUID organizadorId, UUID grupo, ContextoSesion ctx) {
        UUID clave = UUID.nameUUIDFromBytes(
                ("sustitucion-automatica:" + eventoId + ":" + grupo).getBytes(StandardCharsets.UTF_8));
        // Reproceso del mismo evento: si ya se sustituyo, ese es el resultado (no se busca otro entrante).
        var previa = transaccion.execute(e -> datos.conContexto(ctx, dsl -> sustituciones.porClave(dsl, clave)));
        if (previa.isPresent()) {
            return new Efecto(
                    grupo, Resultado.SUSTITUIDO, Optional.of(previa.get().entranteParticipanteId()));
        }
        var candidatos = transaccion.execute(e -> datos.conContexto(ctx, dsl -> sustituciones.candidatos(dsl, grupo)));
        for (var candidato : candidatos) {
            // Fuera de transaccion. Sin respuesta de organizador el resultado es vacio: no se supone habilitado.
            if (afuera.organizadorHabilitadoDelUsuario(candidato.usuarioId()).isEmpty()) {
                continue;
            }
            var registro = transaccion.execute(e -> sustituir.ejecutarAutomatica(
                    new SustituirAdministrador.Entrada(
                            grupo,
                            candidato.usuarioId(),
                            clave,
                            "AUTOMATICA: se revoco la habilitacion del organizador " + organizadorId
                                    + "; queda el participante activo mas antiguo que organizador confirma habilitado."),
                    ctx));
            return new Efecto(grupo, Resultado.SUSTITUIDO, Optional.of(registro.entranteParticipanteId()));
        }
        return new Efecto(grupo, Resultado.SIN_CANDIDATO_HABILITADO, Optional.empty());
    }

    /** Un aviso por grupo que una persona tiene que resolver, emitido una sola vez por evento. */
    private void avisarAUnaPersona(
            UUID eventoId, UUID organizadorId, List<Efecto> efectos, UUID correlacionId, ContextoSesion ctx) {
        List<Efecto> pendientes = efectos.stream()
                .filter(e -> e.resultado() != Resultado.SUSTITUIDO)
                .toList();
        transaccion.executeWithoutResult(e -> datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, eventoId, CONSUMIDOR)) {
                return null;
            }
            for (Efecto pendiente : pendientes) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "grupos.sustitucion_requerida",
                                "grupo",
                                pendiente.grupoId(),
                                Map.of(
                                        "organizadorId",
                                        organizadorId.toString(),
                                        "motivo",
                                        pendiente.resultado() == Resultado.SIN_CANDIDATO_HABILITADO
                                                ? "SIN_CANDIDATO_HABILITADO"
                                                : "REVOCACION_MODO_MANUAL"),
                                correlacionId));
            }
            return null;
        }));
    }
}
