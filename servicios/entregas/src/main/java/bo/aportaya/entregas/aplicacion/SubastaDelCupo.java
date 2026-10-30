package bo.aportaya.entregas.aplicacion;

import static bo.aportaya.entregas.aplicacion.TransicionesDeSubasta.error;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.entregas.dominio.puertos.GruposParaSubasta;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio.Subasta;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CU-64 por subasta (decision de Pablo, 2026-10-30): el titular pone su cupo en subasta, cualquiera que cumpla lo que
 * pide el grupo puja con su saldo (queda retenido), al superado se le devuelve al instante, y al cierre gana la mas
 * alta. Sin votacion.
 *
 * <p>Una saga que se puede reanudar, con lo irreversible al final (regla 98.3.5):
 *
 * <ol>
 *   <li>cerrar: la puja mas alta queda GANADORA (su plata ya esta retenida);
 *   <li>{@code grupos} pasa el cupo al ganador (compensable: si no puede, la subasta FALLA y se le devuelve la plata);
 *   <li>la billetera le paga al saliente exactamente la puja (lo irreversible).
 * </ol>
 *
 * <p>Esta clase NO abre transaccion: cada paso local es de {@link TransicionesDeSubasta} y las llamadas remotas van
 * entre paso y paso, con claves que salen de la puja o de la subasta, nunca del intento.
 */
@Service
public class SubastaDelCupo {

    private static final Logger BITACORA = LoggerFactory.getLogger(SubastaDelCupo.class);
    private static final Set<Integer> HORAS = Set.of(24, 48, 72);
    private static final int LOTE = 50;

    private final GruposParaSubasta grupos;
    private final FondosDelComprador fondos;
    private final TransicionesDeSubasta pasos;
    private final TransicionesDelCierre cierre;
    private final Reloj reloj;

    public SubastaDelCupo(
            GruposParaSubasta grupos,
            FondosDelComprador fondos,
            TransicionesDeSubasta pasos,
            TransicionesDelCierre cierre,
            Reloj reloj) {
        this.grupos = grupos;
        this.fondos = fondos;
        this.pasos = pasos;
        this.cierre = cierre;
        this.reloj = reloj;
    }

    public record Apertura(UUID grupoId, UUID cupoId, Dinero base, int horas, String clave) {}

    public record ResultadoDePuja(UUID pujaId, String estado) {}

    public record Abierta(UUID subastaId, boolean esNueva) {}

    public Abierta abrir(Apertura a, ContextoSesion ctx) {
        if (!HORAS.contains(a.horas())) {
            throw error(3, "La subasta dura 24, 48 o 72 horas.");
        }
        if (a.base().esNegativo() || a.base().esCero()) {
            throw error(3, "El precio base tiene que ser positivo.");
        }
        var hechos = grupos.subastabilidad(a.grupoId(), a.cupoId());
        switch (hechos.respuesta()) {
            case OK -> {}
            case NO_ES_TITULAR, NO_EXISTE -> throw error(1, "Ese cupo no es tuyo: no podes subastarlo.");
            default -> throw error(8, "No se pudo confirmar el cupo con grupos: no se abre la subasta.");
        }
        var cupo = hechos.cupo().orElseThrow();
        if (!cupo.subastable()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(136, 1),
                    "SALIENTE_CON_DEUDA".equals(cupo.motivo().orElse(""))
                            ? "Salda tus aportes pendientes antes de subastar el cupo."
                            : "Ese cupo ya no se puede traspasar.",
                    Map.of("motivo", cupo.motivo().orElse("CUPO_NO_TRASPASABLE")));
        }
        if (a.base().moneda() != cupo.aporte().moneda()) {
            throw error(3, "El precio base va en la moneda del grupo.");
        }
        var cierra = reloj.ahora().plus(Duration.ofHours(a.horas())).atOffset(ZoneOffset.UTC);
        var abierta = pasos.abrir(
                new Subasta(
                        null,
                        a.grupoId(),
                        a.cupoId(),
                        cupo.participanteId(),
                        ctx.usuarioId(),
                        cupo.nombreDelGrupo(),
                        cupo.periodicidad(),
                        cupo.aporte(),
                        a.base(),
                        Optional.empty(),
                        "ABIERTA",
                        cierra,
                        Optional.empty(),
                        Optional.empty(),
                        0),
                a.clave(),
                ctx);
        return new Abierta(abierta.subasta().id(), abierta.esNueva());
    }

    /**
     * Pujar: anotar (PENDIENTE) → retener la plata → fijar (VIGENTE si sigue siendo la mas alta) → devolver la que quedo
     * superada. Si la billetera no responde, la puja queda PENDIENTE y se reanuda repitiendo con la MISMA clave.
     */
    public ResultadoDePuja pujar(
            UUID subastaId, Dinero monto, boolean aceptoReglamento, String clave, ContextoSesion ctx) {
        if (!aceptoReglamento) {
            throw new ErrorDeNegocio(
                    CodigoError.de(136, 6),
                    "Para pujar tenes que aceptar el reglamento del grupo.",
                    Map.of("motivo", "REGLAMENTO_NO_ACEPTADO"));
        }
        var s = pasos.ver(subastaId, ctx);
        var elegibilidad = grupos.elegibilidad(s.grupoId());
        if (elegibilidad.respuesta() != GruposParaSubasta.Respuesta.OK) {
            throw error(8, "No se pudo confirmar con grupos que puedas entrar: no se puja.");
        }
        if (!elegibilidad.elegible()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(136, 6),
                    "Todavia no podes entrar a este grupo.",
                    Map.of("motivo", elegibilidad.motivo().orElse("NO_ELEGIBLE")));
        }
        var puja = pasos.anotarPuja(subastaId, ctx.usuarioId(), monto, clave, ctx);
        if (!"PENDIENTE".equals(puja.estado())) {
            return new ResultadoDePuja(puja.id(), puja.estado());
        }
        var retencion = fondos.retener(ctx.usuarioId(), subastaId, monto, "puja:" + puja.id() + ":retener");
        if (retencion.resultado() == FondosDelComprador.Resultado.SALDO_INSUFICIENTE) {
            pasos.rechazarPuja(puja.id(), ctx);
            throw error(7, "No tenes saldo disponible para esa puja.");
        }
        if (retencion.resultado() != FondosDelComprador.Resultado.OK) {
            return new ResultadoDePuja(puja.id(), "PENDIENTE");
        }
        String estado = pasos.fijarPuja(puja.id(), retencion.referencia(), ctx);
        devolverSuperadas(Optional.of(subastaId), ctx);
        if ("SUPERADA".equals(estado) || "LIBERADA".equals(estado)) {
            throw pasos.vigente(subastaId, ctx)
                    .map(p -> TransicionesDeSubasta.superada(p.monto()))
                    .orElseGet(() -> error(5, "La subasta ya cerro."));
        }
        return new ResultadoDePuja(puja.id(), estado);
    }

    /** El saliente retira el cupo mientras nadie pujo. Devuelve el id para releer la vista. */
    public UUID cancelar(UUID subastaId, ContextoSesion ctx) {
        return pasos.cancelar(subastaId, ctx).id();
    }

    /** Devuelve la plata de las pujas superadas; la que la billetera no confirma queda para el proximo intento. */
    public int devolverSuperadas(Optional<UUID> subastaId, ContextoSesion ctx) {
        int devueltas = 0;
        for (var p : pasos.porDevolver(subastaId, LOTE, ctx)) {
            if (fondos.devolver(p.retencionRef().orElseThrow(), "puja:" + p.id() + ":devolver")
                    == FondosDelComprador.Resultado.OK) {
                pasos.devuelta(p.id(), ctx);
                devueltas++;
            }
        }
        return devueltas;
    }

    /** Sigue la saga de cierre desde donde quedo. Idempotente: llamarla de mas no traspasa ni paga otra vez. */
    public String avanzar(UUID subastaId, ContextoSesion ctx) {
        while (true) {
            var s = pasos.ver(subastaId, ctx);
            switch (s.estado()) {
                case "ABIERTA" -> {
                    if (s.cierraEn().toInstant().isAfter(reloj.ahora())) {
                        return s.estado();
                    }
                    cierre.cerrar(subastaId, ctx);
                    if ("ABIERTA".equals(pasos.ver(subastaId, ctx).estado())) {
                        return "ABIERTA";
                    }
                }
                case "ADJUDICADA" -> {
                    var ganadora = pasos.puja(s.pujaGanadoraId().orElseThrow(), ctx);
                    var t = grupos.traspasar(
                            s.grupoId(), s.cupoId(), s.id(), ganadora.postorUsuarioId(), s.participanteOrigenId());
                    if (t.respuesta() == GruposParaSubasta.Respuesta.RECHAZADO) {
                        cierre.fallida(subastaId, "grupos no pudo pasar el cupo al ganador.", ctx);
                        devolverSuperadas(Optional.of(subastaId), ctx);
                        continue;
                    }
                    if (t.respuesta() != GruposParaSubasta.Respuesta.OK) {
                        return s.estado();
                    }
                    cierre.traspasada(subastaId, t.traspasoId().orElseThrow(), ctx);
                }
                case "LIQUIDANDO" -> {
                    var ganadora = pasos.puja(s.pujaGanadoraId().orElseThrow(), ctx);
                    var pago = fondos.pagarAlVendedor(
                            ganadora.retencionRef().orElseThrow(),
                            s.vendedorUsuarioId(),
                            ganadora.monto(),
                            Dinero.cero(ganadora.monto().moneda()),
                            "subasta:" + s.id() + ":pagar");
                    if (pago.resultado() != FondosDelComprador.Resultado.OK) {
                        return s.estado();
                    }
                    cierre.vendida(subastaId, pago.referencia(), ctx);
                }
                default -> {
                    return s.estado();
                }
            }
        }
    }

    /** Lo que corre el trabajo programado: cerrar las vencidas, seguir las que quedaron a mitad y devolver lo superado. */
    public int correrCierres(ContextoSesion ctx) {
        int avanzadas = 0;
        for (UUID id : pasos.porAvanzar(LOTE, ctx)) {
            try {
                avanzar(id, ctx);
                avanzadas++;
            } catch (ErrorDeNegocio | IllegalStateException unaSola) {
                // Una subasta que hoy no avanza no frena a las demas; queda para la proxima corrida.
                BITACORA.warn(
                        "la subasta {} no avanzo: {}", id, unaSola.getClass().getSimpleName());
            }
        }
        devolverSuperadas(Optional.empty(), ctx);
        return avanzadas;
    }
}
