package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.dominio.FondeoDelPozo;
import bo.aportaya.entregas.infraestructura.CesionRepositorio;
import bo.aportaya.entregas.infraestructura.EntregaRepositorio;
import bo.aportaya.entregas.infraestructura.FondeoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La mitad LOCAL de entregar el pozo completo: una transaccion, un turno, un fondeo.
 *
 * <p>Las preguntas a otros servicios ya se hicieron (fuera de la transaccion, invariante 6);
 * aca solo se escribe, de forma atomica con su evento. El turno se serializa con un candado de
 * asesoria: dos pedidos simultaneos terminan en una entrega, un fondeo y la misma respuesta.
 *
 * <p>Si el respaldo no alcanzo, la entrega nace <b>bloqueada</b> conservando el pozo completo
 * como principal, el faltante como {@code monto_pendiente} (la deuda conservada) y una
 * incidencia critica abierta. Un reintento posterior completa el MISMO fondeo y destraba la
 * MISMA entrega; no crea otra.
 */
@Service
public class RegistroDelFondeo {

    private final Datos datos;
    private final FondeoRepositorio fondeos;
    private final EntregaRepositorio entregas;
    private final CesionRepositorio cesiones;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Duration slaDeLaIncidencia;

    public RegistroDelFondeo(
            Datos datos,
            FondeoRepositorio fondeos,
            EntregaRepositorio entregas,
            CesionRepositorio cesiones,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.incidencia-fondo.sla}") Duration slaDeLaIncidencia) {
        this.datos = datos;
        this.fondeos = fondeos;
        this.entregas = entregas;
        this.cesiones = cesiones;
        this.outbox = outbox;
        this.reloj = reloj;
        this.slaDeLaIncidencia = slaDeLaIncidencia;
    }

    public record Entrada(
            UUID grupoId,
            UUID periodoId,
            UUID turnoId,
            UUID cupoId,
            UUID beneficiarioId,
            String metodoDesembolso,
            LocalDate fechaProgramada,
            String clave) {}

    public record Salida(
            UUID entregaId, UUID fondeoId, String estadoDelFondeo, FondeoDelPozo.Resultado cifras, boolean esNuevo) {}

    @Transactional
    public Salida registrar(
            Entrada e, FondeoDelPozo.Resultado cifras, UUID coberturaId, OffsetDateTime corte, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            fondeos.serializarTurno(dsl, e.turnoId());
            var previo = fondeos.delTurno(dsl, e.turnoId());

            if (previo.isPresent() && "FONDEADO".equals(previo.get().estado())) {
                var p = previo.get();
                return new Salida(p.entregaId(), p.id(), p.estado(), p.cifras(), false);
            }
            if (previo.isPresent()) {
                var p = previo.get();
                if (!fondeos.completar(dsl, p, cifras, coberturaId, corte, ahora)) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(22, 9), "Otro pedido movio el fondeo de ese turno primero: reintenta.");
                }
                if (cifras.completo()) {
                    destrabar(dsl, p.entregaId(), ahora);
                    anunciarFondeo(dsl, p.entregaId(), e.turnoId(), cifras, coberturaId, ctx);
                }
                return new Salida(
                        p.entregaId(), p.id(), cifras.completo() ? "FONDEADO" : "CON_PENDIENTE", cifras, false);
            }

            if (fondeos.entregaDelTurno(dsl, e.turnoId()).isPresent()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(22, 9), "Ese turno ya tiene una entrega liquidada por el camino anterior.");
            }
            UUID entregaId = entregas.crear(
                    dsl,
                    e.grupoId(),
                    e.periodoId(),
                    e.turnoId(),
                    e.cupoId(),
                    // Si el derecho de cobro se cedio, el pozo es de quien lo compro: el titulo manda.
                    cesiones.destinoDelTitulo(dsl, e.turnoId()).orElse(e.beneficiarioId()),
                    cifras.pozo(),
                    e.metodoDesembolso(),
                    e.fechaProgramada());
            if (!cifras.completo()) {
                entregas.cambiarEstado(dsl, entregaId, List.of("PROGRAMADA"), "BLOQUEADA_POR_FONDO_INCOMPLETO", 0);
            }
            UUID fondeoId = fondeos.crear(dsl, entregaId, e.turnoId(), cifras, coberturaId, e.clave(), corte, ahora);
            if (cifras.completo()) {
                anunciarFondeo(dsl, entregaId, e.turnoId(), cifras, coberturaId, ctx);
            } else {
                abrirIncidencia(dsl, entregaId, e.turnoId(), cifras.pendiente(), ctx, ahora);
            }
            return new Salida(entregaId, fondeoId, cifras.completo() ? "FONDEADO" : "CON_PENDIENTE", cifras, true);
        });
    }

    private void destrabar(DSLContext dsl, UUID entregaId, OffsetDateTime ahora) {
        var entrega = entregas.bloquear(dsl, entregaId).orElseThrow();
        if (!entregas.cambiarEstado(
                dsl, entregaId, List.of("BLOQUEADA_POR_FONDO_INCOMPLETO"), "PROGRAMADA", entrega.version())) {
            throw new ErrorDeNegocio(CodigoError.de(22, 9), "La entrega ya no estaba bloqueada por el fondeo.");
        }
        fondeos.incidenciaAbierta(dsl, entregaId)
                .ifPresent(id -> fondeos.resolverIncidencia(dsl, id, "Faltante cubierto por el respaldo.", ahora));
    }

    private void abrirIncidencia(
            DSLContext dsl, UUID entregaId, UUID turnoId, Dinero pendiente, ContextoSesion ctx, OffsetDateTime ahora) {
        UUID incidencia = fondeos.incidenciaAbierta(dsl, entregaId)
                .orElseGet(() -> fondeos.abrirIncidencia(
                        dsl,
                        entregaId,
                        ctx.usuarioId(),
                        "Falta extraordinaria de caja: el respaldo empresarial no cubre " + pendiente
                                + ". La entrega queda bloqueada y la deuda se conserva.",
                        (int) slaDeLaIncidencia.toHours(),
                        ahora));
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "entregas.pozo_con_faltante",
                        "entrega_fondo",
                        entregaId,
                        Map.of(
                                "turnoId", turnoId.toString(),
                                "pendiente", pendiente.toString(),
                                "moneda", pendiente.moneda().name(),
                                "incidenciaId", incidencia.toString()),
                        UUID.fromString(ctx.traza().id())));
    }

    private void anunciarFondeo(
            DSLContext dsl,
            UUID entregaId,
            UUID turnoId,
            FondeoDelPozo.Resultado c,
            UUID coberturaId,
            ContextoSesion ctx) {
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "entregas.pozo_fondeado",
                        "entrega_fondo",
                        entregaId,
                        Map.of(
                                "turnoId", turnoId.toString(),
                                "pozo", c.pozo().toString(),
                                "confirmado", c.confirmado().toString(),
                                "cubiertoMutual", c.cubiertoMutual().toString(),
                                "cubiertoEmpresa", c.cubiertoEmpresa().toString(),
                                "coberturaId", coberturaId == null ? "" : coberturaId.toString(),
                                "moneda", c.pozo().moneda().name()),
                        UUID.fromString(ctx.traza().id())));
    }
}
