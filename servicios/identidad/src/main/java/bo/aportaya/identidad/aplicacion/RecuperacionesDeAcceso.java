package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import bo.aportaya.identidad.infraestructura.RecuperacionesRepositorio;
import bo.aportaya.identidad.web.generado.modelo.SalidaRecuperaciones;
import bo.aportaya.identidad.web.generado.modelo.SolicitudDeRecuperacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperacion asistida a cuatro ojos (P-4, ADR-038 §4 y §5, CU-09 §11): pedido, aprobacion de OTRA
 * persona, enfriamiento y, recien entonces, la invitacion de un solo uso.
 *
 * <p>El pedido es publico y NO revela nada: responde igual exista o no el correo, este o no activo el
 * operador, haya o no un pedido vivo. La invitacion siempre va al correo registrado, nunca a uno que
 * escriba quien pide.
 */
@Service
public class RecuperacionesDeAcceso {

    private static final Logger BITACORA = LoggerFactory.getLogger(RecuperacionesDeAcceso.class);

    private static final int LIMITE_POR_OMISION = 25;
    private static final int LIMITE_MAXIMO = 100;
    private static final int TANDA = 50;

    private final Datos datos;
    private final RecuperacionesRepositorio solicitudes;
    private final InvitacionesDeCuenta invitaciones;
    private final BitacoraDeCuenta bitacora;
    private final Reloj reloj;
    private final Duration enfriamiento;
    private final Duration vigenciaDelPedido;

    public RecuperacionesDeAcceso(
            Datos datos,
            RecuperacionesRepositorio solicitudes,
            InvitacionesDeCuenta invitaciones,
            BitacoraDeCuenta bitacora,
            Reloj reloj,
            @Value("${aportaya.acceso.recuperacion.enfriamiento:PT24H}") Duration enfriamiento,
            @Value("${aportaya.acceso.recuperacion.vigencia-del-pedido:P7D}") Duration vigenciaDelPedido) {
        this.datos = datos;
        this.solicitudes = solicitudes;
        this.invitaciones = invitaciones;
        this.bitacora = bitacora;
        this.reloj = reloj;
        this.enfriamiento = enfriamiento;
        this.vigenciaDelPedido = vigenciaDelPedido;
    }

    /** Siempre termina bien: lo que ocurrio (o no) es asunto del expediente, no de la respuesta. */
    @Transactional
    public void pedir(String correo, String alcance, String motivo, InvitacionesDeCuenta.Origen origen) {
        OffsetDateTime ahora = ahora();
        AlcanceDeCuenta pedido = AlcanceDeCuenta.dePedido(alcance)
                .orElseThrow(() -> new IllegalArgumentException("alcance de recuperacion desconocido"));
        datos.conContexto(ContextoSesion.deSistema(InvitacionesDeCuenta.PROCESO, origen.traza()), dsl -> {
            var operador = solicitudes.operadorActivoPorCorreo(dsl, correo);
            if (operador.isEmpty()) {
                return null;
            }
            UUID id = UUID.randomUUID();
            String texto = motivo == null || motivo.isBlank() ? "Pedido del propio operador" : motivo.strip();
            if (solicitudes.abrir(dsl, id, operador.get().usuarioId(), pedido.name(), null, texto, ahora)) {
                bitacora.registrar(
                        dsl,
                        BitacoraDeCuenta.Accion.RECUPERACION_SOLICITADA,
                        operador.get().usuarioId(),
                        new BitacoraDeCuenta.Actor(
                                operador.get().usuarioId(),
                                BitacoraDeCuenta.ROL_DEL_TITULAR,
                                BitacoraDeCuenta.ORIGEN_PUBLICO),
                        texto,
                        Map.of("alcance", pedido.name(), "solicitudId", id.toString()),
                        UUID.fromString(origen.traza().id()),
                        ahora);
            }
            return null;
        });
    }

    @Transactional(readOnly = true)
    public SalidaRecuperaciones listar(
            String estado, Integer limite, OffsetDateTime antesDe, UUID antesDeId, ContextoSesion ctx) {
        int tope = limite == null ? LIMITE_POR_OMISION : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        List<SolicitudDeRecuperacion> filas =
                datos.conContexto(ctx, dsl -> solicitudes.listar(dsl, estado, tope + 1, antesDe, antesDeId));
        SalidaRecuperaciones salida = new SalidaRecuperaciones();
        List<SolicitudDeRecuperacion> pagina = filas.size() > tope ? filas.subList(0, tope) : filas;
        salida.setItems(pagina);
        if (filas.size() > tope) {
            SolicitudDeRecuperacion ultima = pagina.get(pagina.size() - 1);
            salida.setSiguienteAntesDe(ultima.getSolicitadaEn());
            salida.setSiguienteAntesDeId(ultima.getId());
        }
        return salida;
    }

    @Transactional
    public SolicitudDeRecuperacion aprobar(UUID id, String motivo, ContextoSesion ctx) {
        return decidir(id, motivo, ctx, true);
    }

    @Transactional
    public SolicitudDeRecuperacion rechazar(UUID id, String motivo, ContextoSesion ctx) {
        return decidir(id, motivo, ctx, false);
    }

    private SolicitudDeRecuperacion decidir(UUID id, String motivo, ContextoSesion ctx, boolean aprobar) {
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            var fila = solicitudes
                    .paraDecidir(dsl, id)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(9, 11), "No encontramos esa solicitud."));
            UUID titular = fila.get("usuario_id", UUID.class);
            // Nadie decide su propia recuperacion. La base tambien lo rechaza (R-SEG-16); esto da el error claro.
            if (titular.equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(9, 9), "No podes decidir tu propia solicitud: lo hace otra persona.");
            }
            String estado = fila.get("estado", String.class);
            UUID decidioAntes = fila.get("decidida_por", UUID.class);
            boolean repeticion = aprobar
                    && ctx.usuarioId().equals(decidioAntes)
                    && ("APROBADA".equals(estado) || "EMITIDA".equals(estado) || "COMPLETADA".equals(estado));
            if (repeticion) {
                return solicitudes.porId(dsl, id).orElseThrow();
            }
            if (!"PENDIENTE".equals(estado)) {
                throw new ErrorDeNegocio(CodigoError.de(9, 12), "Esa solicitud ya fue decidida o venció.");
            }
            boolean escrito = aprobar
                    ? solicitudes.aprobar(dsl, id, ctx.usuarioId(), motivo, ahora, ahora.plus(enfriamiento))
                    : solicitudes.rechazar(dsl, id, ctx.usuarioId(), motivo, ahora);
            if (!escrito) {
                throw new ErrorDeNegocio(CodigoError.de(9, 12), "Esa solicitud ya fue decidida o venció.");
            }
            SolicitudDeRecuperacion actualizada = solicitudes.porId(dsl, id).orElseThrow();
            bitacora.registrar(
                    dsl,
                    aprobar
                            ? BitacoraDeCuenta.Accion.RECUPERACION_APROBADA
                            : BitacoraDeCuenta.Accion.RECUPERACION_RECHAZADA,
                    titular,
                    new BitacoraDeCuenta.Actor(ctx.usuarioId(), ctx.rol(), BitacoraDeCuenta.ORIGEN_PANEL),
                    motivo,
                    Map.of("alcance", actualizada.getAlcance(), "solicitudId", id.toString()),
                    UUID.fromString(ctx.traza().id()),
                    ahora);
            return actualizada;
        });
    }

    /**
     * El trabajo: emite la invitacion de las aprobadas cuyo enfriamiento ya paso y cierra lo abandonado.
     * Dos corridas (o dos replicas) no emiten dos veces: la fila se toma con SKIP LOCKED y la transicion
     * lleva su precondicion.
     *
     * @return cuantas invitaciones emitio
     */
    @Transactional
    public int emitirLasQueYaPuedenSalir(Traza traza) {
        OffsetDateTime ahora = ahora();
        ContextoSesion ctx = ContextoSesion.deSistema(InvitacionesDeCuenta.PROCESO, traza);
        return datos.conContexto(ctx, dsl -> {
            solicitudes.vencerLoAbandonado(dsl, ahora, ahora.minus(vigenciaDelPedido));
            int emitidas = 0;
            for (var aprobada : solicitudes.listasParaEmitir(dsl, ahora, TANDA)) {
                boolean activo = dsl.fetchExists(dsl.selectOne()
                        .from("identidad.usuario")
                        .where("id = ? AND estado = 'ACTIVO'", aprobada.usuarioId()));
                if (!activo) {
                    solicitudes.vencer(dsl, aprobada.id());
                    continue;
                }
                AlcanceDeCuenta alcance =
                        AlcanceDeCuenta.dePedido(aprobada.alcance()).orElseThrow();
                UUID token;
                try {
                    token = invitaciones.emitirYEnviar(
                            dsl,
                            aprobada.usuarioId(),
                            alcance,
                            BitacoraDeCuenta.Accion.RECUPERACION_EMITIDA,
                            new BitacoraDeCuenta.Actor(
                                    InvitacionesDeCuenta.PROCESO, ctx.rol(), BitacoraDeCuenta.ORIGEN_TAREA),
                            "Enfriamiento cumplido",
                            UUID.fromString(traza.id()),
                            "0.0.0.0",
                            "tarea-programada");
                } catch (IllegalStateException sinCorreo) {
                    // Un operador sin correo no puede recibir nada: se cierra el pedido y la tanda sigue.
                    BITACORA.warn("recuperacion {} sin destino de correo; se vence", aprobada.id());
                    solicitudes.vencer(dsl, aprobada.id());
                    continue;
                }
                if (solicitudes.marcarEmitida(dsl, aprobada.id(), token, ahora)) {
                    emitidas++;
                }
            }
            if (emitidas > 0) {
                BITACORA.info("recuperaciones emitidas: {}", emitidas);
            }
            return emitidas;
        });
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}
