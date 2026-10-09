package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.AsientoPropuesto;
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
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-23 (respaldo empresarial) · Anular una cobertura con contra-asiento. */
@Service
public class CU23ReversarCobertura {

    private final Datos datos;
    private final RespaldoRepositorio respaldo;
    private final CoberturaRespaldoRepositorio coberturas;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU23ReversarCobertura(
            Datos datos,
            RespaldoRepositorio respaldo,
            CoberturaRespaldoRepositorio coberturas,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.respaldo = respaldo;
        this.coberturas = coberturas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    public record SalidaReversa(UUID coberturaId, Dinero revertido, boolean esNueva) {}

    /**
     * Contra-asiento: la cobertura se anula (p. ej. la entrega se anulo antes de salir).
     * No se edita nada: nace un movimiento inverso y el asiento inverso. Una cobertura con
     * recuperaciones no se reversa por aca: hay plata de terceros de por medio.
     */
    @Transactional
    public SalidaReversa reversar(UUID coberturaId, ClaveIdempotencia clave, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var sinCandado = coberturas
                    .ver(dsl, coberturaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(23, 11), "Esa cobertura no existe."));
            // Orden de candados: reserva antes que cobertura (igual que cubrir y recuperar).
            var reserva = respaldo.bloquear(dsl, sinCandado.reservaId()).orElseThrow();
            var cobertura = coberturas.bloquear(dsl, coberturaId).orElseThrow();
            if ("REVERSADA".equals(cobertura.estado())) {
                return new SalidaReversa(coberturaId, cobertura.faltante(), false);
            }
            if (!"APLICADA".equals(cobertura.estado())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(23, 11), "La cobertura ya tiene recuperaciones: no se reversa a mano.");
            }
            var antes = reserva.cifras();
            var despues = new RespaldoEmpresarial.Reserva(
                    antes.reservado(),
                    antes.aplicado().menos(cobertura.faltante()),
                    antes.recuperado(),
                    antes.liberado());
            if (!coberturas.marcarReversada(dsl, coberturaId)) {
                throw new ErrorDeNegocio(CodigoError.de(23, 11), "La cobertura cambio mientras se reversaba.");
            }
            respaldo.movimiento(
                    dsl,
                    reserva.id(),
                    "REVERSA_APLICACION",
                    cobertura.faltante(),
                    despues,
                    "COBERTURA",
                    coberturaId,
                    "mov:" + clave.valor(),
                    ctx.usuarioId(),
                    reserva.responsableId(),
                    ahora);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "garantia.cobertura_empresarial_reversada",
                            "cobertura_respaldo",
                            coberturaId,
                            Map.of(
                                    "monto", cobertura.faltante().toString(),
                                    "moneda", cobertura.faltante().moneda().name(),
                                    "asiento",
                                            AsientoPropuesto.reversaDeAplicacion(coberturaId, cobertura.faltante())
                                                    .comoCarga()),
                            UUID.fromString(ctx.traza().id())));
            return new SalidaReversa(coberturaId, cobertura.faltante(), true);
        });
    }
}
