package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.PasarelaDePagos;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-21, el pago de la cuota con el saldo de la billetera (F1.L1.M11, ANF-DEFECTO-001). El cobro en si lo hace
 * {@link CU21CobrarAporte}; aca se decide lo que antes dictaba el cliente.
 */
@Service
public class CU21PagarConSaldo {

    private static final Logger BITACORA = LoggerFactory.getLogger(CU21PagarConSaldo.class);

    private final Datos datos;
    private final ObligacionRepositorio obligaciones;
    private final PagoRepositorio pagos;
    private final CU21CobrarAporte cobro;
    private final PasarelaDePagos pasarela;

    public CU21PagarConSaldo(
            Datos datos,
            ObligacionRepositorio obligaciones,
            PagoRepositorio pagos,
            CU21CobrarAporte cobro,
            PasarelaDePagos pasarela) {
        this.datos = datos;
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.cobro = cobro;
        this.pasarela = pasarela;
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
        Lectura lectura = datos.conContexto(ctx, dsl -> {
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
            return new Lectura(pendienteLeido, esReintento);
        });
        Dinero pendiente = lectura.pendiente();
        return cobro.acreditar(
                new CU21CobrarAporte.EntradaCobro(
                        claveIdempotencia,
                        obligacionId,
                        pendiente,
                        Dinero.cero(pendiente.moneda()),
                        CANAL_SALDO,
                        referenciaDelCobro(lectura, claveIdempotencia, obligacionId),
                        Optional.empty(),
                        false,
                        periodoAbierto),
                ctx);
    }

    private record Lectura(Dinero pendiente, boolean esReintento) {}

    /**
     * Con una pasarela que lo pide (hoy solo el doble de {@code local}/{@code test}) la primera vez que se paga se abre una
     * orden y su referencia queda en el pago, para recorrer orden → aviso → conciliacion. Nunca cambia lo que responde el
     * cobro: un reintento, una cuota sin pendiente o una orden que la pasarela rechaza siguen con la referencia del saldo, y
     * el cobro responde lo que respondia (los errores de la obligacion los da {@link CU21CobrarAporte#acreditar}).
     */
    private String referenciaDelCobro(Lectura lectura, String claveIdempotencia, UUID obligacionId) {
        String delSaldo = referenciaDelSaldo(obligacionId, claveIdempotencia);
        if (!pasarela.registraOrdenPorCobro()
                || lectura.esReintento()
                || lectura.pendiente().esCero()
                || lectura.pendiente().esNegativo()) {
            return delSaldo;
        }
        try {
            return pasarela.crearOrdenDeCobro(new PasarelaDePagos.SolicitudDeCobro(
                            obligacionId + ":" + claveIdempotencia, lectura.pendiente()))
                    .referenciaProveedor();
        } catch (IllegalArgumentException | IllegalStateException e) {
            BITACORA.warn("la pasarela no abrio la orden del cobro: se usa la referencia del saldo");
            return delSaldo;
        }
    }

    /** El unico canal que admite el cobro de la persona (P-06): el saldo de su billetera. */
    public static final String CANAL_SALDO = "BILLETERA_MOVIL";

    /** La referencia de un pago con saldo: la fija el servidor, nunca el cliente (cabe en VARCHAR(80)). */
    public static String referenciaDelSaldo(UUID obligacionId, String claveIdempotencia) {
        return "SALDO-" + obligacionId + "-" + claveIdempotencia;
    }
}
