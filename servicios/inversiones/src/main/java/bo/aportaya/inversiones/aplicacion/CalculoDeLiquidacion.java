package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.ComisionDeExito;
import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.Descomposicion;
import bo.aportaya.inversiones.dominio.DevengoDeDpf;
import bo.aportaya.inversiones.dominio.ValoracionDeCuotas;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.OperacionDelAliado;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio.Rescate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * El calculo propio de una liquidacion, contra lo que informa el aliado. Se separa del caso
 * de uso para que sea evidente que NADA de lo que viene del aliado se acepta sin recalcular:
 * el interes y la retencion de un DPF, y cuotas x valor en un fondo.
 */
@Component
public class CalculoDeLiquidacion {

    private final CatalogoRepositorio catalogo;
    private final ComprobanteRepositorio comprobantes;

    public CalculoDeLiquidacion(CatalogoRepositorio catalogo, ComprobanteRepositorio comprobantes) {
        this.catalogo = catalogo;
        this.comprobantes = comprobantes;
    }

    /** @return {@code null} si el aliado y el calculo propio no coinciden (queda registrada la discrepancia). */
    Descomposicion dpf(
            DSLContext dsl, Rescate r, Posicion p, Condiciones c, OperacionDelAliado op, OffsetDateTime ahora) {
        int base = c.baseDias().orElseThrow();
        BigDecimal tasa = c.tasaNominalAnual().orElseThrow();
        boolean anticipado = "ANTICIPADO".equals(r.tipo());
        int dias = anticipado
                ? Integer.parseInt(op.detalle().getOrDefault("diasDevengados", "-1"))
                : c.plazoDias().orElseThrow();
        BigDecimal propio = DevengoDeDpf.interes(p.principal(), tasa, Math.max(dias, 0), base);
        if (anticipado) {
            propio = propio.multiply(
                            BigDecimal.ONE.subtract(c.penalizacionAnticipo().orElse(BigDecimal.ZERO)))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        BigDecimal retencionPropia =
                DevengoDeDpf.retencion(propio, c.tasaRetencion().orElseThrow());
        BigDecimal interesExterno = decimal(op, "interesBruto");
        BigDecimal retencionExterna = decimal(op, "retencion");
        if (!op.monto()
                .map(m -> m.compareTo(p.principal().add(interesExterno).subtract(retencionExterna)) == 0)
                .orElse(false)) {
            // Lo que paga no es principal + interes - retencion: ni siquiera es coherente consigo mismo.
            throw Errores.de(
                    125,
                    2,
                    "El importe que informa el aliado no coincide con principal + interes - retencion: no se acredita.");
        }
        comprobantes.registrarConciliacion(
                dsl,
                p.id(),
                r.id(),
                p.fechaConstitucion(),
                p.fechaConstitucion().plusDays(Math.max(dias, 0)),
                propio,
                interesExterno,
                retencionPropia,
                retencionExterna,
                p.moneda(),
                ahora);
        boolean coincide = propio.compareTo(interesExterno) == 0 && retencionPropia.compareTo(retencionExterna) == 0;
        return coincide ? Descomposicion.dpf(p.principal(), propio, retencionPropia) : null;
    }

    Descomposicion fondo(
            DSLContext dsl, Rescate r, Posicion p, Condiciones c, OperacionDelAliado op, OffsetDateTime ahora) {
        BigDecimal valor = new BigDecimal(op.detalle().getOrDefault("valorCuota", "0"));
        BigDecimal producido = r.cuotas().multiply(valor).setScale(2, RoundingMode.HALF_EVEN);
        boolean coincide = valor.signum() > 0
                && op.monto().map(m -> m.compareTo(producido) == 0).orElse(false);
        if (coincide && op.fechaValor().isPresent()) {
            // Si ya tenemos el valor publicado de ese dia, tiene que ser el mismo.
            coincide = catalogo.valorDelDia(dsl, p.productoId(), op.fechaValor().get())
                    .map(v -> v.valor().compareTo(valor) == 0)
                    .orElse(true);
        }
        if (!coincide) {
            throw Errores.de(
                    125,
                    2,
                    "El importe que informa el aliado no coincide con cuotas x valor de cuota: no se acredita.");
        }
        BigDecimal liberado = ValoracionDeCuotas.costoBaseLiberado(
                comprobantes.costoBaseVigente(dsl, p.id()), r.cuotas(), p.cuotas());
        BigDecimal ganancia = producido.subtract(liberado).max(BigDecimal.ZERO);
        BigDecimal comision = BigDecimal.ZERO.setScale(2);
        if (c.tasaComisionExito().isPresent()) {
            ComisionDeExito e = ComisionDeExito.calcular(
                    r.cuotas(), valor, p.marcaMaxima(), c.tasaComisionExito().get());
            comision = e.comision().min(ganancia);
            comprobantes.registrarComision(
                    dsl,
                    p.id(),
                    r.id(),
                    r.cuotas(),
                    valor,
                    p.marcaMaxima(),
                    c.tasaComisionExito().get(),
                    e.baseElegible(),
                    comision,
                    e.cuotaNeta(),
                    e.marcaNueva(),
                    p.moneda(),
                    c.origen(),
                    ahora);
        }
        return Descomposicion.fondo(liberado, producido, comision);
    }

    private static BigDecimal decimal(OperacionDelAliado op, String clave) {
        try {
            return new BigDecimal(op.detalle().getOrDefault(clave, "-1"));
        } catch (NumberFormatException e) {
            return BigDecimal.ONE.negate();
        }
    }
}
