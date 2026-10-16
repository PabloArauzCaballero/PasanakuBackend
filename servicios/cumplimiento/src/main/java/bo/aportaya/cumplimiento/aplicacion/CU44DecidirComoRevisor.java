package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.infraestructura.CasoLftRepositorio;
import bo.aportaya.cumplimiento.infraestructura.MonitoreoLftRepositorio;
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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-44 · la decision del oficial sobre un caso, por la puerta del backoffice (carril A · H5.S2).
 *
 * <p>Lo que la vuelve segura para exponerla por HTTP, y que {@code CU44InvestigarYReportar.decidir}
 * no garantizaba porque recibia todo del llamador:
 *
 * <ul>
 *   <li><b>El revisor es la sesion</b>, no un campo del cuerpo. Y el analista sale del caso GUARDADO:
 *       revisarse a uno mismo se rechaza contra lo que la base sabe, no contra lo que el cliente dice.
 *   <li><b>Las alertas que se cierran son las del caso</b>, leidas de la base: el cliente no puede
 *       cerrar alertas de otro expediente.
 *   <li><b>Una sola decision por caso.</b> La transicion es condicional y su resultado se verifica:
 *       el segundo intento (o el concurrente que pierde) recibe AP-CU44-07, sin segundo evento.
 * </ul>
 *
 * <p>Nunca se notifica al titular: deber de reserva (CU-44, 7a).
 */
@Service
public class CU44DecidirComoRevisor {

    private final Datos datos;
    private final CasoLftRepositorio casos;
    private final MonitoreoLftRepositorio monitoreo;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU44DecidirComoRevisor(
            Datos datos, CasoLftRepositorio casos, MonitoreoLftRepositorio monitoreo, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.casos = casos;
        this.monitoreo = monitoreo;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public Decision decidir(Entrada entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        // AP-CU44-01 · R-UIF-07: la conclusion es la unica parte que sirve despues.
        if (entrada.conclusion() == null || entrada.conclusion().trim().length() < 20) {
            throw new ErrorDeNegocio(
                    CodigoError.de(44, 1), "Una alerta no se cierra sin conclusion escrita (R-UIF-07).");
        }
        // AP-CU44-03: decidir reportar exige el ROS ya radicado (vive en auditoria, invariante 11).
        if ("REPORTAR".equals(entrada.decision()) && entrada.reporteSospechosoId() == null) {
            throw new ErrorDeNegocio(
                    CodigoError.de(44, 3),
                    "Reportar exige el reporte de operacion sospechosa ya radicado, con su narrativa y tipologia.");
        }
        return datos.conContexto(ctx, dsl -> {
            var caso = casos.porId(dsl, entrada.casoId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(44, 6), "Ese caso no existe."));
            if ("CERRADO".equals(caso.estado())) {
                return reintentoOError(dsl, entrada, ctx, caso.plazoLimite());
            }
            // AP-CU44-02 contra el analista GUARDADO en el caso, no contra uno declarado por el cliente.
            if (ctx.usuarioId().equals(caso.analistaId())) {
                throw new ErrorDeNegocio(CodigoError.de(44, 2), "La revision del caso tiene que ser independiente.");
            }
            boolean vencido = ahora.isAfter(caso.plazoLimite());
            if (!casos.decidir(
                    dsl,
                    entrada.casoId(),
                    entrada.decision(),
                    entrada.conclusion(),
                    ctx.usuarioId(),
                    entrada.reporteSospechosoId(),
                    ahora)) {
                // Otro oficial decidio entre la lectura y la escritura: una sola decision por caso.
                return reintentoOError(dsl, entrada, ctx, caso.plazoLimite());
            }
            var alertas = monitoreo.abiertasDelCaso(dsl, entrada.casoId());
            String estadoDeLasAlertas = "DESCARTAR".equals(entrada.decision()) ? "DESCARTADA" : "ESCALADA";
            for (UUID alertaId : alertas) {
                monitoreo.cerrar(dsl, alertaId, estadoDeLasAlertas, entrada.conclusion(), entrada.casoId(), ahora);
            }
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "cumplimiento.caso_decidido",
                            "caso_investigacion_lft",
                            entrada.casoId(),
                            Map.of(
                                    "decision", entrada.decision(),
                                    "fueraDePlazo", Boolean.toString(vencido),
                                    "alertasCerradas", Integer.toString(alertas.size()),
                                    // NUNCA se notifica al titular: deber de reserva.
                                    "notificarAlTitular", "false"),
                            UUID.fromString(ctx.traza().id())));
            return new Decision(
                    entrada.casoId(),
                    entrada.decision(),
                    ctx.usuarioId(),
                    alertas.size(),
                    vencido,
                    entrada.reporteSospechosoId());
        });
    }

    /**
     * Un caso ya decidido: si es la MISMA decision del MISMO oficial (su reintento tras perder la
     * respuesta) devuelve el mismo resultado sin cerrar nada ni emitir otro evento; si no, AP-CU44-07.
     */
    private Decision reintentoOError(
            org.jooq.DSLContext dsl, Entrada entrada, ContextoSesion ctx, OffsetDateTime plazo) {
        boolean reintento = casos.decisionRegistrada(dsl, entrada.casoId())
                .filter(d -> d.repite(
                        entrada.decision(), entrada.conclusion(), ctx.usuarioId(), entrada.reporteSospechosoId()))
                .isPresent();
        if (!reintento) {
            throw yaDecidido();
        }
        boolean vencido = reloj.ahora().atOffset(ZoneOffset.UTC).isAfter(plazo);
        int cerradas = monitoreo.cerradasDelCaso(dsl, entrada.casoId());
        return new Decision(
                entrada.casoId(),
                entrada.decision(),
                ctx.usuarioId(),
                cerradas,
                vencido,
                entrada.reporteSospechosoId());
    }

    private static ErrorDeNegocio yaDecidido() {
        return new ErrorDeNegocio(CodigoError.de(44, 7), "Ese caso ya tiene su decision.");
    }

    /**
     * @param reporteSospechosoId el ROS ya radicado; obligatorio si la decision es REPORTAR
     */
    public record Entrada(UUID casoId, String decision, String conclusion, UUID reporteSospechosoId) {}

    public record Decision(
            UUID casoId,
            String decision,
            UUID revisadoPor,
            int alertasCerradas,
            boolean fueraDePlazo,
            UUID reporteSospechosoId) {}
}
