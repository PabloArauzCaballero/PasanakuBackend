package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-20 · el grupo pasa de {@code BORRADOR} a {@code ABIERTO_A_INSCRIPCION} cuando {@code tarifas} congelo su precio
 * ({@code tarifas.tarifa_congelada}).
 *
 * <p>Antes nadie consumia {@code grupos.grupo_creado} ni escribia el estado: todo grupo creado por la API quedaba
 * {@code BORRADOR} para siempre, y con eso ningun enlace de invitacion se podia consultar ni aceptar (hallazgo H1 del
 * carril L3 contra TEST). El diseno ya lo decia —CU-20 y {@code CU20CrearGrupo}— : nace en borrador porque «un grupo
 * sin precio congelado no puede recibir a nadie»; este es el paso que faltaba.
 *
 * <p><b>Supuesto A1 (a confirmar con producto y con el dueno de {@code nucleo-financiero}).</b> CU-20 y el contrato
 * de {@code POST /grupos} dicen que el grupo abre «cuando los dos respondieron» (precio congelado y cuenta del grupo
 * abierta). La cuenta del grupo todavia no la abre nadie ({@code nucleo-financiero} no consume
 * {@code grupo_creado}) y la unica razon que el diseno da para el borrador es el precio. Se abre con el precio
 * congelado; el dia que {@code nucleo-financiero} responda, se agrega su respuesta como segunda condicion aca.
 * Hasta entonces un grupo puede inscribir gente antes de tener cuenta; cobrar sigue exigiendola.
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} y el cambio van en la MISMA transaccion.
 *   <li><b>Precondicion en la escritura</b> (regla 96.3.2): el {@code UPDATE} lleva {@code estado='BORRADOR'}; un
 *       grupo que ya avanzo ({@code CONFORMADO}, {@code ACTIVO}, {@code CANCELADO}...) no retrocede ni se reabre.
 *   <li><b>Rechazo sin marca</b>: un grupo que no existe se RECHAZA (excepcion) y la transaccion deshace tambien la
 *       marca, asi que el reintento lo aplica cuando exista (el evento puede llegar antes que el grupo se vea).
 * </ul>
 */
@Service
public class AbrirInscripcionDelGrupo {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "grupos.apertura-por-tarifa-congelada";

    /** El proceso que atiende el evento: fijo, para poder leerlo en el registro. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000000f4");

    static final String MOTIVO = "Apertura automatica: tarifas congelo el precio del grupo";

    public enum Resultado {
        ABIERTO,
        YA_ABIERTO,
        DUPLICADO
    }

    /** Lo que {@code tarifas.tarifa_congelada} trae: solo identificadores. */
    public record PrecioCongelado(UUID eventoId, UUID correlationId, UUID grupoId, UUID tarifarioId) {}

    private final Datos datos;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("grupos");

    public AbrirInscripcionDelGrupo(Datos datos, Reloj reloj) {
        this.datos = datos;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado procesar(PrecioCongelado precio) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(precio.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, precio.eventoId(), CONSUMIDOR)) {
                return Resultado.DUPLICADO;
            }
            // Bloquea la fila: dos eventos del mismo grupo (o un cambio de estado concurrente) se ordenan aca.
            var grupo = dsl.fetchOne("SELECT estado FROM grupos.grupo WHERE id=? FOR UPDATE", precio.grupoId());
            if (grupo == null) {
                throw new IllegalStateException("El grupo del precio congelado no existe todavia");
            }
            int abiertos = dsl.execute(
                    "UPDATE grupos.grupo SET estado='ABIERTO_A_INSCRIPCION' WHERE id=? AND estado='BORRADOR'",
                    precio.grupoId());
            if (abiertos == 0) {
                return Resultado.YA_ABIERTO;
            }
            // El historial atribuye al creador (la FK exige un usuario real); el motivo dice que fue el sistema.
            dsl.execute(
                    """
                    INSERT INTO grupos.historial_estado_grupo
                        (grupo_id, estado_anterior, estado_nuevo, motivo, ejecutado_por, fecha_hora)
                    SELECT ?, 'BORRADOR', 'ABIERTO_A_INSCRIPCION', ?, a.creador_id, ?
                      FROM grupos.alta_grupo a WHERE a.grupo_id = ?
                    """,
                    precio.grupoId(),
                    MOTIVO,
                    reloj.ahora().atOffset(ZoneOffset.UTC),
                    precio.grupoId());
            return Resultado.ABIERTO;
        });
    }
}
