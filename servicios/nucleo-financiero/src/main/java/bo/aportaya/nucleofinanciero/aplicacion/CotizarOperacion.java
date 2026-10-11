package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision.Cotizacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * H11.S1.M1 · El precio se ve antes de confirmar, y lo que se cobra es lo que se vio.
 *
 * <p>El tarifario es de {@code tarifas}: aca solo se le pregunta, por su contrato, y fuera de toda
 * transaccion. Hay dos momentos. <b>Cotizar</b> le muestra a la persona base, comision, impuesto,
 * total, neto y hasta cuando vale ese precio. <b>Confirmar</b> exige que la operacion cite esa misma
 * cotizacion, que no haya vencido y que {@code tarifas} registre la aceptacion; el costo que se
 * aplica es el de la cotizacion y no otro, y la cotizacion queda guardada en la orden.
 *
 * <p>La clave con que se le pregunta a {@code tarifas} incluye el importe y la billetera: pedir otro
 * importe con la misma clave del cliente produce OTRA cotizacion (con otro identificador), y por eso
 * no se puede cobrar el precio de Bs 500 sobre una operacion de Bs 5.000.
 */
@Service
public class CotizarOperacion {

    /** Las operaciones gravadas, con el hecho generador del tarifario y la numeracion de sus codigos de error. */
    public enum Operacion {
        RECARGA("RECARGA", 10, 5, 6),
        RETIRO("RETIRO_ACREDITADO", 11, 1, 10);

        private final String hecho;
        private final int casoDeUso;
        private final int sinPrecio;
        private final int primerCodigo;

        Operacion(String hecho, int casoDeUso, int sinPrecio, int primerCodigo) {
            this.hecho = hecho;
            this.casoDeUso = casoDeUso;
            this.sinPrecio = sinPrecio;
            this.primerCodigo = primerCodigo;
        }

        CodigoError requerida() {
            return CodigoError.de(casoDeUso, primerCodigo);
        }

        CodigoError vencida() {
            return CodigoError.de(casoDeUso, primerCodigo + 1);
        }

        CodigoError noCoincide() {
            return CodigoError.de(casoDeUso, primerCodigo + 2);
        }

        CodigoError sinPrecio() {
            return CodigoError.de(casoDeUso, sinPrecio);
        }
    }

    private final CotizadorDeComision cotizador;
    private final Reloj reloj;
    private final boolean exigirPrevia;

    public CotizarOperacion(
            CotizadorDeComision cotizador,
            Reloj reloj,
            @Value("${aportaya.cotizacion.exigir-previa:true}") boolean exigirPrevia) {
        this.cotizador = cotizador;
        this.reloj = reloj;
        this.exigirPrevia = exigirPrevia;
    }

    /** Lo que se le muestra a la persona. No mueve nada ni compromete a nadie. */
    public CotizacionVista cotizar(Operacion operacion, UUID cuentaId, Dinero monto, UUID claveDelCliente) {
        var c = preguntar(operacion, cuentaId, monto, claveDelCliente.toString());
        exigirQueElCostoNoSeLleveTodo(operacion, monto, c.total());
        return new CotizacionVista(
                c.id(),
                monto,
                c.comision(),
                c.impuesto(),
                c.total(),
                monto.menos(c.total()),
                c.validaHasta(),
                c.gratuita());
    }

    /**
     * El costo a aplicar y la cotizacion que lo respalda. Con la exigencia activa, una operacion
     * gravada sin cotizacion previa, con otra cotizacion o con una vencida se rechaza.
     *
     * <p>Si la orden de esa clave ya existe ({@code yaHayOrden}) es un reintento: no se exige una
     * cotizacion vigente ni se acepta de nuevo, porque el precio ya se confirmo la primera vez; solo
     * se necesita el costo para comparar el reintento con lo que quedo escrito.
     */
    public CostoConfirmado confirmar(
            Operacion operacion,
            UUID cuentaId,
            Dinero monto,
            String claveDelCliente,
            Optional<UUID> cotizacionId,
            boolean yaHayOrden) {
        var c = preguntar(operacion, cuentaId, monto, claveDelCliente);
        exigirQueElCostoNoSeLleveTodo(operacion, monto, c.total());
        if (c.gratuita()) {
            if (cotizacionId.isPresent()) {
                throw new ErrorDeNegocio(
                        operacion.noCoincide(), "Esa operacion no tiene costo: no hay cotizacion que citar.");
            }
            return new CostoConfirmado(c.total(), Optional.empty());
        }
        if (cotizacionId.isEmpty() && !yaHayOrden) {
            if (exigirPrevia) {
                throw new ErrorDeNegocio(
                        operacion.requerida(), "Pedi primero la cotizacion y confirma el precio que te mostramos.");
            }
            return new CostoConfirmado(c.total(), c.id());
        }
        if (cotizacionId.isPresent() && !c.id().equals(cotizacionId)) {
            throw new ErrorDeNegocio(
                    operacion.noCoincide(), "Esa cotizacion no corresponde a esta operacion: pedi una nueva.");
        }
        if (!yaHayOrden) {
            OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
            if (c.validaHasta().filter(vence -> !ahora.isBefore(vence)).isPresent()) {
                throw new ErrorDeNegocio(
                        operacion.vencida(), "La cotizacion vencio: pedi una nueva antes de confirmar.");
            }
            if (!cotizador.aceptar(c.id().orElseThrow())) {
                throw new ErrorDeNegocio(
                        operacion.vencida(),
                        "No se pudo registrar que aceptaste el precio: pedi una cotizacion nueva.");
            }
        }
        return new CostoConfirmado(c.total(), c.id());
    }

    private Cotizacion preguntar(Operacion operacion, UUID cuentaId, Dinero monto, String claveDelCliente) {
        return cotizador
                .cotizar(
                        operacion.hecho, cuentaId, monto, claveParaTarifas(operacion, cuentaId, monto, claveDelCliente))
                .orElseThrow(() -> new ErrorDeNegocio(
                        operacion.sinPrecio(), "No se pudo confirmar el costo de la operacion: intentalo de nuevo."));
    }

    private static void exigirQueElCostoNoSeLleveTodo(Operacion operacion, Dinero monto, Dinero costo) {
        if (!costo.esMenorQue(monto)) {
            throw new ErrorDeNegocio(
                    operacion.noCoincide(), "El costo no puede igualar ni superar el importe de la operacion.");
        }
    }

    /** UUID estable de (operacion, billetera, importe, clave): otro importe es otra cotizacion. */
    static String claveParaTarifas(Operacion operacion, UUID cuentaId, Dinero monto, String claveDelCliente) {
        String material = "cotizacion|" + operacion.hecho + "|" + cuentaId + "|" + monto + "|" + claveDelCliente;
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public record CotizacionVista(
            Optional<UUID> cotizacionId,
            Dinero base,
            Dinero comision,
            Dinero impuesto,
            Dinero total,
            Dinero neto,
            Optional<OffsetDateTime> validaHasta,
            boolean gratuita) {}

    public record CostoConfirmado(Dinero costo, Optional<UUID> cotizacionId) {}
}
