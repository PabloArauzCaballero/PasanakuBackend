package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.AsientoPropuesto;
import bo.aportaya.garantia.dominio.RespaldoEmpresarial;
import bo.aportaya.garantia.infraestructura.CoberturaRespaldoRepositorio;
import bo.aportaya.garantia.infraestructura.RespaldoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
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
 * CU-23 (respaldo empresarial) · Un aporte tardio devuelve a la empresa lo que adelanto.
 *
 * <p>La empresa puso de su bolsillo lo que un miembro no pagó; cuando ese miembro paga
 * tarde, <b>el dinero vuelve a la empresa, no al pozo</b>: el beneficiario ya cobró el
 * pozo completo y no se le entrega una segunda vez, y el miembro no paga dos veces
 * porque su pago se imputa a una sola linea (ambiguedad A3: sin interes ni recargo
 * nuevos hasta que comercial lo defina).
 *
 * <p>Cada pago recupera una sola vez: {@code uq_recuperacion_respaldo_pago_id} lo
 * garantiza en la base y el segundo intento devuelve el resultado del primero. Si el
 * pago supera lo adelantado, el excedente no es de la empresa y se informa.
 */
@Service
public class CU23RecuperarRespaldo {

    private final Datos datos;
    private final RespaldoRepositorio respaldo;
    private final CoberturaRespaldoRepositorio coberturas;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU23RecuperarRespaldo(
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

    public record EntradaRecuperacion(UUID pagoId, UUID obligacionId, Dinero pago) {}

    public enum Resultado {
        RECUPERADA,
        YA_RECUPERADA,
        AGOTADA,
        SIN_COBERTURA
    }

    public record SalidaRecuperacion(
            Resultado resultado, Dinero recuperado, Dinero excedente, Dinero exposicionDespues, boolean esNueva) {}

    @Transactional
    public SalidaRecuperacion recuperar(EntradaRecuperacion e, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        Dinero cero = Dinero.cero(e.pago().moneda());

        return datos.conContexto(ctx, dsl -> {
            // Sin candado primero: hay que saber de que reserva es para bloquearla ANTES.
            var visible = coberturas.lineaViva(dsl, e.obligacionId(), e.pago().moneda(), false);
            if (visible.isEmpty()) {
                var yaImputado = coberturas.recuperacionDelPago(dsl, e.pagoId());
                if (yaImputado.isPresent()) {
                    return new SalidaRecuperacion(
                            Resultado.YA_RECUPERADA,
                            Dinero.de(yaImputado.get(), e.pago().moneda()),
                            cero,
                            cero,
                            false);
                }
                return new SalidaRecuperacion(Resultado.SIN_COBERTURA, cero, e.pago(), cero, true);
            }
            var cobertura = coberturas.ver(dsl, visible.get().coberturaId()).orElseThrow();
            var reserva = respaldo.bloquear(dsl, cobertura.reservaId()).orElseThrow();
            cobertura = coberturas.bloquear(dsl, cobertura.id()).orElseThrow();
            var linea = coberturas
                    .lineaViva(dsl, e.obligacionId(), e.pago().moneda(), true)
                    .orElseThrow();

            var yaImputado = coberturas.recuperacionDelPago(dsl, e.pagoId());
            if (yaImputado.isPresent()) {
                return new SalidaRecuperacion(
                        Resultado.YA_RECUPERADA,
                        Dinero.de(yaImputado.get(), e.pago().moneda()),
                        cero,
                        reserva.cifras().exposicion(),
                        false);
            }

            var calculo = RespaldoEmpresarial.recuperar(linea.cubierto(), linea.recuperado(), e.pago());
            if (calculo.aplicado().esCero()) {
                return new SalidaRecuperacion(
                        Resultado.AGOTADA, cero, e.pago(), reserva.cifras().exposicion(), true);
            }

            var antes = reserva.cifras();
            var despues = new RespaldoEmpresarial.Reserva(
                    antes.reservado(), antes.aplicado(), antes.recuperado().mas(calculo.aplicado()), antes.liberado());
            UUID movimientoId = respaldo.movimiento(
                    dsl,
                    reserva.id(),
                    "RECUPERACION",
                    calculo.aplicado(),
                    despues,
                    "PAGO",
                    e.pagoId(),
                    "rec:" + e.pagoId(),
                    ctx.usuarioId(),
                    reserva.responsableId(),
                    ahora);
            coberturas.registrarRecuperacion(dsl, linea.id(), e.pagoId(), movimientoId, calculo.aplicado(), ahora);
            coberturas.sumarRecuperado(dsl, linea, cobertura, calculo.aplicado());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "garantia.respaldo_recuperado",
                            "cobertura_respaldo",
                            cobertura.id(),
                            Map.of(
                                    "pagoId", e.pagoId().toString(),
                                    "obligacionId", e.obligacionId().toString(),
                                    "recuperado", calculo.aplicado().toString(),
                                    "excedente", calculo.excedente().toString(),
                                    "moneda", e.pago().moneda().name(),
                                    "exposicion", despues.exposicion().toString(),
                                    "asiento",
                                            AsientoPropuesto.recuperacion(movimientoId, calculo.aplicado())
                                                    .comoCarga()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaRecuperacion(
                    Resultado.RECUPERADA, calculo.aplicado(), calculo.excedente(), despues.exposicion(), true);
        });
    }
}
