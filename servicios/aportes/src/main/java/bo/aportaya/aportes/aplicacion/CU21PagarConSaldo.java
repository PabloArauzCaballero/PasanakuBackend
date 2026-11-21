package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-21, el pago de la cuota con el saldo de la billetera (F1.L1.M11, ANF-DEFECTO-001). El cobro en si lo hace
 * {@link CU21CobrarAporte}; aca se decide lo que antes dictaba el cliente.
 */
@Service
public class CU21PagarConSaldo {

    private final Datos datos;
    private final ObligacionRepositorio obligaciones;
    private final PagoRepositorio pagos;
    private final CU21CobrarAporte cobro;

    public CU21PagarConSaldo(
            Datos datos, ObligacionRepositorio obligaciones, PagoRepositorio pagos, CU21CobrarAporte cobro) {
        this.datos = datos;
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.cobro = cobro;
    }

    /**
     * F1.L1.M11: paga el pendiente de la cuota con el saldo de la billetera. Nada del cobro sale del cliente: el monto
     * es el pendiente leido aca, dentro de la misma transaccion que el cobro; el canal es el saldo (P-06); la
     * referencia la fija el servidor a partir de la obligacion y la clave. Si entre la lectura y el candado otro pago
     * bajo el pendiente, {@link CU21CobrarAporte#acreditar} lo ve con el candado en la mano y responde AP-CU21-02: nunca se cobra de
     * mas.
     */
    @Transactional
    public CU21CobrarAporte.SalidaCobro pagarConSaldo(
            String claveIdempotencia,
            UUID obligacionId,
            Optional<Dinero> montoDeclarado,
            boolean periodoAbierto,
            ContextoSesion ctx) {
        Dinero pendiente = datos.conContexto(ctx, dsl -> {
            var pendienteLeido = obligaciones
                    .ver(dsl, obligacionId)
                    .map(o -> o.saldo().pendiente())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(21, 1), "Esa obligacion no existe."));
            // El reintento con la misma clave devuelve el original (lo resuelve acreditar): el monto declarado
            // solo se compara en el primer intento, cuando el pendiente todavia es el de la cuota.
            boolean esReintento = pagos.porClaveIdempotencia(dsl, obligacionId, claveIdempotencia)
                    .isPresent();
            if (!esReintento
                    && montoDeclarado.isPresent()
                    && !montoDeclarado.get().equals(pendienteLeido)) {
                throw new ErrorDeNegocio(CodigoError.de(21, 2), "El monto no coincide: falta " + pendienteLeido + ".");
            }
            return pendienteLeido;
        });
        return cobro.acreditar(
                new CU21CobrarAporte.EntradaCobro(
                        claveIdempotencia,
                        obligacionId,
                        pendiente,
                        Dinero.cero(pendiente.moneda()),
                        CANAL_SALDO,
                        referenciaDelSaldo(obligacionId, claveIdempotencia),
                        Optional.empty(),
                        false,
                        periodoAbierto),
                ctx);
    }

    /** El unico canal que admite el cobro de la persona (P-06): el saldo de su billetera. */
    public static final String CANAL_SALDO = "BILLETERA_MOVIL";

    /** La referencia de un pago con saldo: la fija el servidor, nunca el cliente (cabe en VARCHAR(80)). */
    public static String referenciaDelSaldo(UUID obligacionId, String claveIdempotencia) {
        return "SALDO-" + obligacionId + "-" + claveIdempotencia;
    }
}
