package bo.aportaya.tarifas.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.tarifas.infraestructura.TarifaCongeladaRepositorio;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-20 · congelar el precio del grupo al consumir {@code grupos.grupo_creado}.
 *
 * <p>El caso de uso pide que, al crear el grupo, quede {@code tarifa_congelada_grupo} con el snapshot de los conceptos y
 * su hash. La tabla es de {@code tarifas} y {@code grupos} no escribe ahi (invariante 11), asi que es coreografia:
 * {@code grupos} crea el grupo en borrador y emite el evento; aca se congela y se responde con
 * {@code tarifas.tarifa_congelada}, que es lo que hace pasar al grupo a {@code ABIERTO_A_INSCRIPCION}. Antes nadie
 * consumia el evento y el grupo quedaba en borrador para siempre (carril L3, H1).
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido}, la tarifa y el evento de respuesta van en la
 *       MISMA transaccion.
 *   <li><b>Una sola tarifa por grupo</b> (R-TAR-07): la unicidad de la base decide. Si el grupo ya la tenia (otro
 *       camino, o un evento anterior que no llego a responder) se conserva y se vuelve a responder, para que el grupo
 *       no se quede esperando.
 *   <li><b>Rechazo sin marca</b>: un tarifario que no existe se RECHAZA (excepcion) y la transaccion deshace tambien la
 *       marca. Nunca se congela un precio inventado.
 * </ul>
 *
 * <p><b>Supuesto A2 (a confirmar con producto y finanzas):</b> el snapshot es el tarifario con todos sus conceptos
 * activos y las reglas de cada uno; el hash es el SHA-256 de su texto {@code jsonb}. El CU pide «los conceptos
 * completos» y un hash, sin fijar el formato.
 */
@Service
public class CongelarTarifaDelGrupo {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "tarifas.congelar-precio-del-grupo";

    /** El proceso que atiende el evento: fijo, para poder leerlo en el registro. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000000f6");

    public enum Resultado {
        CONGELADA,
        YA_CONGELADA,
        DUPLICADO
    }

    /** Lo que {@code grupos.grupo_creado} trae: solo identificadores. */
    public record GrupoCreado(UUID eventoId, UUID correlationId, UUID grupoId, UUID tarifarioId) {}

    private final Datos datos;
    private final TarifaCongeladaRepositorio tarifas;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("tarifas");

    public CongelarTarifaDelGrupo(Datos datos, TarifaCongeladaRepositorio tarifas, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.tarifas = tarifas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado procesar(GrupoCreado grupo) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(grupo.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, grupo.eventoId(), CONSUMIDOR)) {
                return Resultado.DUPLICADO;
            }
            String snapshot = tarifas
                    .snapshotDe(dsl, grupo.tarifarioId())
                    .orElseThrow(() -> new IllegalStateException("El tarifario del grupo no existe"));
            boolean yaEstaba = tarifas.yaCongelado(dsl, grupo.grupoId());
            var congelada = tarifas.congelar(
                    dsl,
                    grupo.grupoId(),
                    grupo.tarifarioId(),
                    snapshot,
                    TarifaCongeladaRepositorio.hashDe(snapshot),
                    reloj.ahora().atOffset(ZoneOffset.UTC));
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "tarifas.tarifa_congelada",
                            "grupo",
                            grupo.grupoId(),
                            Map.of(
                                    "grupoId", grupo.grupoId().toString(),
                                    "tarifarioId", congelada.tarifarioId().toString(),
                                    "hashSnapshot", congelada.hashSnapshot()),
                            grupo.correlationId()));
            return yaEstaba ? Resultado.YA_CONGELADA : Resultado.CONGELADA;
        });
    }
}
