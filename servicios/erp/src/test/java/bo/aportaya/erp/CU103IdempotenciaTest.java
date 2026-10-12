package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor.EntradaPago;
import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor.SalidaFactura;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-103 · El pago a proveedor es idempotente (R-CTB-11, regla 91.1.4).
 *
 * <p>Amenaza (regla 90.6): el cliente, la red o la cola reintentan un pago parcial y la
 * empresa transfiere dos veces. Control: la clave de idempotencia se ampara en la
 * factura, el reintento devuelve lo que devolvio el original sin escribir nada, y la base
 * lo sostiene con {@code uq_pago_a_proveedor_clave}. Las pruebas corren contra PostgreSQL
 * real.
 */
class CU103IdempotenciaTest extends EscenarioDeFactura {

    private static final String SUMA_DE_PAGOS =
            "SELECT count(*)::int FROM erp.pago_a_proveedor WHERE factura_proveedor_id = ?";

    private EntradaPago pago(UUID facturaId, String monto, UUID clave) {
        return new EntradaPago(facturaId, new BigDecimal(monto), "BOB", "TRANSFERENCIA", clave);
    }

    private int eventosDePago(UUID facturaId) {
        return contar(
                "SELECT count(*)::int FROM erp.evento_dominio WHERE tipo = 'erp.pago_a_proveedor_registrado' AND payload->>'facturaId' = ?",
                facturaId.toString());
    }

    @Test
    @DisplayName(
            "Dado un pago parcial ya registrado con una clave de idempotencia · Cuando se repite el mismo pago con la misma clave · Entonces el sistema responde lo mismo que la primera vez y no registra otro pago")
    void criterio4() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("2500.00", enPeriodo(1)), aprobador));
        var parcial = pago(factura.facturaId(), "1000.00", UUID.randomUUID());

        var primera = transaccion.execute(t -> facturaCU.pagar(parcial, tesoreria));
        var repetida = transaccion.execute(t -> facturaCU.pagar(parcial, tesoreria));

        assertThat(repetida).isEqualTo(primera);
        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el mismo pago parcial con la misma clave devuelve lo mismo y no escribe nada mas")
    void reintentoDelPagoParcial() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("5000.00", enPeriodo(1)), aprobador));
        var clave = UUID.randomUUID();
        var parcial = pago(factura.facturaId(), "2000.00", clave);

        var primero = transaccion.execute(t -> facturaCU.pagar(parcial, tesoreria));
        var segundo = transaccion.execute(t -> facturaCU.pagar(parcial, tesoreria));

        assertThat(primero.estado()).isEqualTo("PAGADA_PARCIAL");
        assertThat(segundo).isEqualTo(primero);
        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isEqualTo(1);
        // Un solo evento = un solo asiento que nucleo-financiero escribe a partir de el:
        // el reintento no pide un segundo asiento.
        assertThat(eventosDePago(factura.facturaId())).isEqualTo(1);

        // Otro pago (otra clave) entra normalmente, y el reintento del primero sigue
        // respondiendo lo que respondio el primero, no el estado de hoy.
        var siguiente = transaccion.execute(
                t -> facturaCU.pagar(pago(factura.facturaId(), "1000.00", UUID.randomUUID()), tesoreria));
        assertThat(siguiente.montoPagado()).isEqualByComparingTo("3000.00");
        var repetido = transaccion.execute(t -> facturaCU.pagar(parcial, tesoreria));
        assertThat(repetido).isEqualTo(primero);
        assertThat(repetido.montoPagado()).isEqualByComparingTo("2000.00");
        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isEqualTo(2);
    }

    @Test
    @DisplayName("reintento: el pago que cancela la factura, repetido, sigue respondiendo PAGADA y no excede el saldo")
    void reintentoDelPagoTotal() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("900.00", enPeriodo(1)), aprobador));
        var total = pago(factura.facturaId(), "900.00", UUID.randomUUID());

        var primero = transaccion.execute(t -> facturaCU.pagar(total, tesoreria));
        // Sin la clave este segundo intento fallaria con «excede el saldo»: con ella es
        // el mismo pago, ya hecho.
        var segundo = transaccion.execute(t -> facturaCU.pagar(total, tesoreria));

        assertThat(primero.estado()).isEqualTo("PAGADA");
        assertThat(segundo).isEqualTo(primero);
        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: la misma clave con otro pago se rechaza y no se paga nada")
    void reintentoConOtroPago() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("5000.00", enPeriodo(1)), aprobador));
        var clave = UUID.randomUUID();
        transaccion.execute(t -> facturaCU.pagar(pago(factura.facturaId(), "1000.00", clave), tesoreria));

        // Responder con el pago viejo le haria creer al cliente que el de 4000 se hizo.
        assertThatThrownBy(() ->
                        transaccion.execute(t -> facturaCU.pagar(pago(factura.facturaId(), "4000.00", clave), tesoreria)))
                .hasMessageContaining("otro pago");
        assertThatThrownBy(() -> transaccion.execute(t -> facturaCU.pagar(
                        new EntradaPago(
                                factura.facturaId(), new BigDecimal("1000.00"), "BOB", "CHEQUE", clave),
                        tesoreria)))
                .hasMessageContaining("otro pago");
        // La clave tampoco la reusa otra persona: seria otro autorizante del mismo pago.
        var otraTesoreria = contextoDe(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(
                        t -> facturaCU.pagar(pago(factura.facturaId(), "1000.00", clave), otraTesoreria)))
                .hasMessageContaining("otro pago");

        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: la clave se ampara en la factura, la misma clave en otra factura es otro pago")
    void laClaveSeAmparaEnLaFactura() {
        var una = transaccion.execute(t -> facturaCU.registrar(factura("1000.00", enPeriodo(1)), aprobador));
        var otra = transaccion.execute(t -> facturaCU.registrar(factura("1000.00", enPeriodo(1)), aprobador));
        var clave = UUID.randomUUID();

        transaccion.execute(t -> facturaCU.pagar(pago(una.facturaId(), "1000.00", clave), tesoreria));
        transaccion.execute(t -> facturaCU.pagar(pago(otra.facturaId(), "1000.00", clave), tesoreria));

        assertThat(contar(SUMA_DE_PAGOS, una.facturaId())).isEqualTo(1);
        assertThat(contar(SUMA_DE_PAGOS, otra.facturaId())).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: el mismo pago reintentado en simultaneo se registra una sola vez y todos reciben lo mismo")
    void concurrenciaDelMismoPago() throws Exception {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("5000.00", enPeriodo(1)), aprobador));
        var parcial = pago(factura.facturaId(), "2000.00", UUID.randomUUID());
        int hilos = 4;

        var barrera = new CyclicBarrier(hilos);
        var salidas = Collections.synchronizedList(new ArrayList<SalidaFactura>());
        var errores = Collections.synchronizedList(new ArrayList<Throwable>());
        var trabajadores = new ArrayList<Thread>();
        for (int i = 0; i < hilos; i++) {
            var hilo = new Thread(() -> {
                try {
                    barrera.await();
                    salidas.add(transaccion.execute(t -> facturaCU.pagar(parcial, tesoreria)));
                } catch (Exception e) {
                    errores.add(e);
                }
            });
            trabajadores.add(hilo);
            hilo.start();
        }
        for (var hilo : trabajadores) {
            hilo.join();
        }

        // El FOR UPDATE de la factura serializa los reintentos: el segundo ya ve el pago
        // del primero. Ninguno falla y ninguno paga de mas.
        assertThat(errores).isEmpty();
        assertThat(salidas).hasSize(hilos).containsOnly(salidas.get(0));
        assertThat(salidas.get(0).montoPagado()).isEqualByComparingTo("2000.00");
        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isEqualTo(1);
        assertThat(eventosDePago(factura.facturaId())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza por R-CTB-11: la base no admite dos pagos de una factura con la misma clave")
    void rechazaRCTB11() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("3000.00", enPeriodo(1)), aprobador));
        var clave = UUID.randomUUID();
        transaccion.execute(t -> facturaCU.pagar(pago(factura.facturaId(), "1000.00", clave), tesoreria));

        // Aunque alguien escriba el pago por fuera del caso de uso (o el candado de la
        // factura se pierda), la base corta el duplicado: entre un SELECT y el INSERT
        // entra el segundo.
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO erp.pago_a_proveedor
                            (factura_proveedor_id, monto, moneda, forma_pago, fecha_pago, autorizado_por, clave_idempotencia)
                        VALUES (?, 1000, 'BOB', 'TRANSFERENCIA', now(), ?, ?)
                        """,
                        factura.facturaId(),
                        tesoreria.usuarioId(),
                        clave))
                .contains("uq_pago_a_proveedor_clave");

        // Y los pagos anteriores a la regla (sin clave) no compiten entre si: dos NULL no
        // son iguales, asi que el esquema se puede aplicar sobre una base poblada.
        var sinClave = transaccion.execute(t -> {
            for (int i = 0; i < 2; i++) {
                dsl.execute(
                        """
                        INSERT INTO erp.pago_a_proveedor
                            (factura_proveedor_id, monto, moneda, forma_pago, fecha_pago, autorizado_por)
                        VALUES (?, 100, 'BOB', 'TRANSFERENCIA', now(), ?)
                        """,
                        factura.facturaId(),
                        tesoreria.usuarioId());
            }
            t.setRollbackOnly();
            return true;
        });
        assertThat(sinClave).isTrue();
    }

    @Test
    @DisplayName("rechaza por R-CTB-11: un pago sin clave de idempotencia no entra al caso de uso")
    void rechazaPagoSinClave() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("500.00", enPeriodo(1)), aprobador));

        assertThatThrownBy(() -> new EntradaPago(factura.facturaId(), new BigDecimal("500.00"), "BOB", "EFECTIVO", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clave de idempotencia");
        assertThat(contar(SUMA_DE_PAGOS, factura.facturaId())).isZero();
    }
}
