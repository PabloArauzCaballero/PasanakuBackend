package bo.aportaya.inversiones;

import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate.EntradaRescate;
import bo.aportaya.inversiones.aplicacion.VistaComprobante;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Lo comun de las pruebas de liquidacion: helpers de posicion, de comprobantes y de cuadre desde el mayor. */
abstract class BaseDeLiquidacion extends BaseDeInversiones {

    protected static final LocalDate DIA0 = LocalDate.of(2026, 10, 12);

    protected String codigoDe(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    protected EntradaRescate pedir(UUID posicion, String cuotas) {
        return new EntradaRescate(
                posicion,
                Optional.ofNullable(cuotas).map(BigDecimal::new),
                UUID.randomUUID().toString());
    }

    protected UUID fondo(String monto, String valorDeEntrada) {
        aliado.valorCuotaAplicado(valorDeEntrada);
        return posicionConfirmada(productoFondo, monto);
    }

    protected VistaComprobante liquidacion(UUID posicion) {
        return cu123.comprobantes(posicion, ctx).stream()
                .filter(c -> "LIQUIDACION".equals(c.tipo()))
                .reduce((a, b) -> b)
                .orElseThrow();
    }

    protected BigDecimal costoBase(UUID posicion) {
        return tx.en(() -> datos.conContexto(ctx, d -> comprobantes.costoBaseVigente(d, posicion)));
    }

    /** El efecto neto sobre la billetera, DESDE el mayor de comprobantes: creditos menos debitos. */
    protected BigDecimal efectoNetoSegunElMayor() {
        return dslFixtura
                .fetchOne(
                        "select coalesce(sum(case when sentido_titular = 'CREDITO' then neto else -neto end), 0)"
                                + " from inversiones.comprobante_inversion where usuario_id = ?",
                        usuario)
                .get(0, BigDecimal.class);
    }
}
