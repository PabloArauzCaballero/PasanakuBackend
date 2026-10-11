package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.AsientoPropuesto;
import bo.aportaya.garantia.dominio.PlanDeCaja;
import bo.aportaya.garantia.dominio.RespaldoEmpresarial;
import bo.aportaya.garantia.infraestructura.CoberturaRespaldoRepositorio;
import bo.aportaya.garantia.infraestructura.RespaldoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-23 (respaldo empresarial) · Cubrir con la reserva lo que no llego al corte.
 *
 * <p>Ejemplo del plan: pozo 6.000 y 5.000 confirmados dejan un faltante de 1.000 que sale
 * de la reserva del ciclo. Todo o nada: si la reserva no alcanza no se cubre una parte
 * (ambiguedad A4); se informa {@code INSUFICIENTE} y el llamador abre el incidente y
 * conserva la deuda.
 *
 * <p>Una cobertura viva por turno: la reserva se bloquea <b>primero</b> y recien con el
 * candado puesto se busca si el turno ya estaba cubierto, de modo que dos pedidos
 * simultaneos del mismo turno terminan en una sola cobertura y la misma respuesta.
 */
@Service
public class CU23CubrirConRespaldo {

    private static final int LARGO_CLAVE = 80;

    private final Datos datos;
    private final RespaldoRepositorio respaldo;
    private final CoberturaRespaldoRepositorio coberturas;
    private final Outbox outbox;
    private final Reloj reloj;
    private final ContingenciaDeCaja contingencia;

    public CU23CubrirConRespaldo(
            Datos datos,
            RespaldoRepositorio respaldo,
            CoberturaRespaldoRepositorio coberturas,
            Outbox outbox,
            Reloj reloj,
            ContingenciaDeCaja contingencia) {
        this.contingencia = contingencia;
        this.datos = datos;
        this.respaldo = respaldo;
        this.coberturas = coberturas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record Linea(UUID obligacionId, Dinero monto) {}

    public record EntradaCobertura(
            UUID grupoId,
            UUID periodoId,
            UUID turnoId,
            Dinero pozo,
            Dinero confirmado,
            Dinero cubiertoMutual,
            List<Linea> lineas,
            OffsetDateTime corte,
            ClaveIdempotencia clave) {

        public Dinero faltante() {
            return pozo.menos(confirmado).menos(cubiertoMutual);
        }
    }

    /** Resultado de negocio, no excepcion: el llamador decide que hacer con cada uno. */
    public enum Resultado {
        APLICADA,
        INSUFICIENTE,
        SIN_RESERVA
    }

    public record SalidaCobertura(
            Resultado resultado,
            UUID coberturaId,
            UUID reservaId,
            Dinero cubierto,
            Dinero sinCubrir,
            Dinero disponibleDespues,
            Dinero exposicionDespues,
            boolean esNueva) {}

    @Transactional
    public SalidaCobertura cubrir(EntradaCobertura e, ContextoSesion ctx) {
        validar(e);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        Dinero cero = Dinero.cero(e.pozo().moneda());

        return datos.conContexto(ctx, dsl -> {
            var reserva = respaldo.bloquearVigenteDelGrupo(dsl, e.grupoId()).orElse(null);
            if (reserva == null) {
                insuficiente(dsl, e, cero, e.faltante(), "SIN_RESERVA", null, ahora, ctx);
                return new SalidaCobertura(Resultado.SIN_RESERVA, null, null, cero, e.faltante(), cero, cero, true);
            }

            var viva = coberturas.bloquearVivaDelTurno(dsl, e.turnoId());
            var porClave = coberturas.porClave(dsl, e.clave().valor());
            var previa = viva.isPresent() ? viva : porClave;
            if (previa.isPresent()) {
                var p = previa.get();
                if (!p.turnoId().equals(e.turnoId())
                        || !p.faltante().equals(e.faltante())
                        || "REVERSADA".equals(p.estado())) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(23, 6), "Ese turno o esa clave ya tienen otra cobertura distinta.");
                }
                var c = reserva.cifras();
                return new SalidaCobertura(
                        Resultado.APLICADA,
                        p.id(),
                        p.reservaId(),
                        p.faltante(),
                        cero,
                        c.disponible(),
                        c.exposicion(),
                        false);
            }

            var decision = RespaldoEmpresarial.decidir(reserva.cifras(), e.faltante());
            if (!decision.cubre()) {
                insuficiente(
                        dsl,
                        e,
                        decision.disponible(),
                        decision.sinCubrir(),
                        "INSUFICIENTE",
                        reserva.responsableId(),
                        ahora,
                        ctx);
                return new SalidaCobertura(
                        Resultado.INSUFICIENTE,
                        null,
                        reserva.id(),
                        cero,
                        decision.sinCubrir(),
                        decision.disponible(),
                        reserva.cifras().exposicion(),
                        true);
            }

            UUID coberturaId = coberturas.crear(
                    dsl,
                    new CoberturaRespaldoRepositorio.Alta(
                            reserva.id(),
                            e.grupoId(),
                            e.periodoId(),
                            e.turnoId(),
                            e.pozo(),
                            e.confirmado(),
                            e.cubiertoMutual(),
                            e.faltante(),
                            e.corte(),
                            e.clave().valor(),
                            ctx.usuarioId(),
                            reserva.responsableId()),
                    ahora);
            for (Linea linea : e.lineas()) {
                coberturas.agregarLinea(dsl, coberturaId, linea.obligacionId(), linea.monto());
            }

            // Si este grupo estaba en contingencia de caja, ya no: el faltante quedo cubierto.
            contingencia.resolver(
                    dsl, e.grupoId(), e.turnoId(), UUID.fromString(ctx.traza().id()), ahora);

            var antes = reserva.cifras();
            var despues = new RespaldoEmpresarial.Reserva(
                    antes.reservado(), antes.aplicado().mas(e.faltante()), antes.recuperado(), antes.liberado());
            respaldo.movimiento(
                    dsl,
                    reserva.id(),
                    "APLICACION",
                    e.faltante(),
                    despues,
                    "COBERTURA",
                    coberturaId,
                    "mov:" + e.clave().valor(),
                    ctx.usuarioId(),
                    reserva.responsableId(),
                    ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "garantia.cobertura_empresarial_aplicada",
                            "cobertura_respaldo",
                            coberturaId,
                            Map.of(
                                    "reservaId", reserva.id().toString(),
                                    "grupoId", e.grupoId().toString(),
                                    "turnoId", e.turnoId().toString(),
                                    "periodoId", e.periodoId().toString(),
                                    "faltante", e.faltante().toString(),
                                    "moneda", e.faltante().moneda().name(),
                                    "exposicion", despues.exposicion().toString(),
                                    "responsableId", reserva.responsableId().toString(),
                                    "asiento",
                                            AsientoPropuesto.aplicacion(coberturaId, e.faltante())
                                                    .comoCarga()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaCobertura(
                    Resultado.APLICADA,
                    coberturaId,
                    reserva.id(),
                    e.faltante(),
                    cero,
                    despues.disponible(),
                    despues.exposicion(),
                    true);
        });
    }

    /** El faltante ya no existe (pagos tardios llenaron el pozo): la contingencia de ese grupo se cierra. */
    @Transactional
    public boolean cerrarContingencia(UUID grupoId, UUID turnoId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(
                ctx,
                dsl -> contingencia.resolver(
                        dsl, grupoId, turnoId, UUID.fromString(ctx.traza().id()), ahora));
    }

    /** Respaldo insuficiente sobre un compromiso existente: incidente, contencion y escalamiento (H8.S1.M6). */
    private void insuficiente(
            org.jooq.DSLContext dsl,
            EntradaCobertura e,
            Dinero disponible,
            Dinero sinCubrir,
            String motivo,
            UUID responsable,
            OffsetDateTime ahora,
            ContextoSesion ctx) {
        contingencia.insuficiente(
                dsl,
                new PlanDeCaja(
                        e.grupoId(),
                        e.turnoId(),
                        e.faltante(),
                        disponible,
                        sinCubrir,
                        motivo,
                        Optional.ofNullable(responsable),
                        e.lineas().stream()
                                .map(l -> new PlanDeCaja.Pendiente(l.obligacionId(), l.monto()))
                                .toList()),
                UUID.fromString(ctx.traza().id()),
                ahora);
    }

    private void validar(EntradaCobertura e) {
        Dinero faltante = e.faltante();
        if (faltante.esNegativo() || faltante.esCero()) {
            throw new ErrorDeNegocio(CodigoError.de(23, 9), "No hay faltante que cubrir: el pozo ya esta fondeado.");
        }
        Dinero suma = e.lineas().stream().map(Linea::monto).reduce(Dinero.cero(faltante.moneda()), Dinero::mas);
        var vistas = new HashSet<UUID>();
        boolean repetida = e.lineas().stream().anyMatch(l -> !vistas.add(l.obligacionId()));
        boolean noPositiva = e.lineas().stream()
                .anyMatch(l -> l.monto().esNegativo() || l.monto().esCero());
        if (!suma.equals(faltante) || repetida || noPositiva) {
            throw new ErrorDeNegocio(
                    CodigoError.de(23, 9),
                    "Las obligaciones faltantes tienen que sumar exactamente el faltante del corte.",
                    Map.of("faltante", faltante.toString(), "lineas", suma.toString()));
        }
        if (e.clave().valor().length() > LARGO_CLAVE) {
            throw new ErrorDeNegocio(CodigoError.de(23, 6), "La clave de idempotencia supera " + LARGO_CLAVE + ".");
        }
    }
}
